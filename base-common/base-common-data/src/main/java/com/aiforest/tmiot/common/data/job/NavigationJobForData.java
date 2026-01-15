/*
 * Copyright 2016-present the TM IoT original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aiforest.tmiot.common.data.job;

import com.aiforest.tmiot.common.constant.common.PrefixConstant;
import com.aiforest.tmiot.common.data.biz.NavOverviewDataService;
import com.aiforest.tmiot.common.data.biz.NavigationDataService;
import com.aiforest.tmiot.common.data.biz.NavigationPathService;
import com.aiforest.tmiot.common.data.biz.PointValueCommandService;
import com.aiforest.tmiot.common.data.entity.bo.*;
import com.aiforest.tmiot.common.data.entity.dto.DeviceWithPointIds;
import com.aiforest.tmiot.common.data.entity.vo.NavPathPoint;
import com.aiforest.tmiot.common.data.entity.vo.PointValueWriteVO;
import com.aiforest.tmiot.common.data.metadata.DevicePointsMetadata;
import com.aiforest.tmiot.common.data.metadata.NavOverviewMetadata;
import com.aiforest.tmiot.common.data.service.DevicePointsJsonbService;
import com.aiforest.tmiot.common.entity.bo.PointValueBO;
import com.aiforest.tmiot.common.enums.DeviceStatusEnum;
import com.aiforest.tmiot.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 通用: 每小时执行任务
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class NavigationJobForData extends QuartzJobBean {

    @Resource
    private DevicePointsMetadata devicePointsMetadata;

    @Resource
    private NavOverviewMetadata navOverviewMetadata;

    @Resource
    private RedisService redisService; // 新增：注入Redis服务，用于查询设备状态

    private static final Map<String, AtomicInteger> ZT_DUTY_INDEX = new ConcurrentHashMap<>();

    private final PointValueCommandService pointValueCommandService;
    private final NavigationDataService navigationDataService;
    private final NavOverviewDataService navOverviewDataService;
    private final DevicePointsJsonbService devicePointsJsonbService;
    private final NavigationPathService navigationPathService;

    /* 临时缓存上一次点位值，生产请换成 Redis */
    private static final Map<String, String> LAST_VALUE_CACHE = new ConcurrentHashMap<>();

    /* 缓存 ZT 设备的 nav_mode 和 nav_route */
    private static final Map<String, String> LAST_NAV_MODE = new ConcurrentHashMap<>();
    private static final Map<String, String> LAST_NAV_ROUTE = new ConcurrentHashMap<>();

    /* ---------------- 核心改造：增加双阶段待处理集合 ---------------- */
    // 第一阶段：当前周期标记，下个周期转移到 ztLogPending
    private static final Set<String> ztLogPendingNext = ConcurrentHashMap.newKeySet();
    // 第二阶段：下个周期转移后，下下个周期处理
    private static final Set<String> ztLogPending = ConcurrentHashMap.newKeySet();

    /* ---------------- 新增：绑定码互斥锁集合 ---------------- */
    // 简化锁定集合：key=绑定码，value=锁定的ZT设备码（替代原LOCKED_SJ/TM_BIND_CODES）
    private static final Map<String, String> LOCKED_BIND_CODES = new ConcurrentHashMap<>();
    // 超时时间（可选，防止死锁）：单位秒，超过则自动释放
    private static final long LOCK_TIMEOUT_SECONDS = 120;
    // 记录锁定时间：key=绑定码，value=锁定时间戳
    private static final Map<String, Long> LOCK_TIMESTAMPS = new ConcurrentHashMap<>();

    // 新增：缓存ZT上一周期的绑定码，用于判定是否从LCPC/RCPC补充位过来
    private static final Map<String, String> LAST_ZT_BIND_CODE = new ConcurrentHashMap<>();

    private static final Map<String, Set<Long>> ZT_DEVICE_PATH_NODE_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, Long> ZT_DEVICE_PATH_TIMESTAMPS = new ConcurrentHashMap<>();

    private static final long PATH_CACHE_TIMEOUT_SECONDS = 30;

    // SJ专属绑定码列表（可配置化）
    private static final List<String> SJ_EXCLUSIVE_CODES = List.of("SWS");
    // TM专属绑定码列表（可配置化）
    private static final List<String> TM_EXCLUSIVE_CODES = Arrays.asList("LWP", "RWP");

    // 新增：缓存延迟一个周期执行的ZT ef_status写入任务
    // 结构：key=ztDeviceId_efStatusPointId（唯一标识任务，避免重复），value=写入值（固定为"1"，可扩展）
    private static final Map<String, String> DELAYED_ZT_EF_STATUS_WRITE = new ConcurrentHashMap<>();

    private static final String SJ_PATTERN = "^(SWS|ZCS)\\d+$";

    // 新增：SWS-ZCS关联缓存（key=ZT设备码，value=SWS*_ZCS*）
    private static final Map<String, String> ZT_SWS_ZCS_MAPPING = new ConcurrentHashMap<>();
    // 电池电压阈值
    private static final int ZCS_BATTERY_THRESHOLD = 2400;

//    // SJ专属绑定码列表（可配置化）
//    private static final List<String> SJ_EXCLUSIVE_CODES = Arrays.asList("SWS1", "SRS1", "SWS2", "SRS2");
//    // TM专属绑定码列表（可配置化）
//    private static final List<String> TM_EXCLUSIVE_CODES = Arrays.asList("LWP2", "RWP2");

    public NavigationJobForData(DevicePointsJsonbService devicePointsJsonbService,
                                NavigationDataService navigationDataService,
                                PointValueCommandService pointValueCommandService,
                                NavOverviewDataService navOverviewDataService,
                                NavigationPathService navigationPathService
    ) {
        this.navigationDataService = navigationDataService;
        this.pointValueCommandService = pointValueCommandService;
        this.devicePointsJsonbService = devicePointsJsonbService;
        this.navOverviewDataService = navOverviewDataService;
        this.navigationPathService = navigationPathService;
    }
    /**
     * 任务执行
     * * <p>
     * * 具体逻辑请在 biz service 中定义
     *
     * @param context JobExecutionContext
     * @throws JobExecutionException JobExecutionException
     */
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String sceneId = jobDataMap.getString("sceneId");

        // 2. 校验场景ID（若未获取到，打印警告并跳过执行）
        if (sceneId == null || sceneId.trim().isEmpty()) {
            log.warn("NavigationJob 执行失败：未获取到有效场景ID，跳过本次执行");
            return;
        }

//        devicePointsMetadata.loadCache(1L);
        List<DevicePointLatestBO>  devicePointLatestBOList = Optional.ofNullable(devicePointsMetadata.getCache(1L))
                .orElse(Collections.emptyList());
        List<DeviceWithPointIds> deviceWithPointIdsList = devicePointLatestBOList.stream()
                .filter(d -> d.getPoints() != null)
                .map(d -> new DeviceWithPointIds(
                        d.getId(),                       // 设备 id
                        d.getPoints()
                                .stream()
                                .map(PointBO::getId)          // 点位 id
                                .toList()
                ))
                .toList();
        List<PointValueBO> allLatest = deviceWithPointIdsList.stream()
                .flatMap(dp -> {
                    // 调用接口，拿到当前设备下这批点位的最新值
                    List<PointValueBO> values = navigationDataService.latest(dp.getDeviceId(), dp.getPointIds());
                    // 防御：接口可能返回 null，统一为空流
                    return (values == null ? Collections.<PointValueBO>emptyList() : values).stream();
                })
                .toList();

        Map<String, Object> snapshotMap  =
                buildSnapshotMap(allLatest, devicePointLatestBOList);

        // ========== 调整后：消费延迟任务（snapshotMap构建完成后执行） ==========
        if (!DELAYED_ZT_EF_STATUS_WRITE.isEmpty()) {
            new ArrayList<>(DELAYED_ZT_EF_STATUS_WRITE.entrySet()).forEach(entry -> {
                String taskKey = entry.getKey();
                String writeValue = entry.getValue();
                try {
                    // 拆分任务Key：ztDeviceCode_efStatusPointCode
                    String[] keyParts = taskKey.split("_");
                    if (keyParts.length < 2) {
                        log.warn("无效的延迟写入任务Key：{}，跳过执行", taskKey);
                        DELAYED_ZT_EF_STATUS_WRITE.remove(taskKey);
                        return;
                    }
                    String ztDeviceCode = keyParts[0]; // ZT设备编码
                    String efStatusPointId = keyParts[1]; // ef_status点位编码

                    // 从snapshotMap获取ZT设备的完整快照（原生有效，无需手动构造）
                    Object ztSnapshotObj = snapshotMap.get(ztDeviceCode);
                    if (!(ztSnapshotObj instanceof DeviceSnapshotBO zt)) {
                        log.warn("未找到ZT设备{}的快照，延迟写入任务跳过", ztDeviceCode);
                        DELAYED_ZT_EF_STATUS_WRITE.remove(taskKey);
                        return;
                    }

                    // 获取ZT原生的ef_status PointNodeBO（无需手动构造，直接复用现有有效对象）
                    PointNodeBO efStatusNode = zt.getEf_status();
                    if (efStatusNode == null) {
                        log.warn("ZT设备{}无ef_status点位，延迟写入任务跳过", ztDeviceCode);
                        DELAYED_ZT_EF_STATUS_WRITE.remove(taskKey);
                        return;
                    }

                    // 执行延迟写入（复用现有write方法，参数完全有效）
                    write(zt.getId(), efStatusNode, writeValue);
                    log.info("延迟写入任务执行成功：ZT设备{}，ef_status点位{}，写入值{}",
                            ztDeviceCode, efStatusPointId, writeValue);

                    // 执行后移除任务，避免重复执行
                    DELAYED_ZT_EF_STATUS_WRITE.remove(taskKey);
                } catch (Exception e) {
                    log.error("延迟写入任务执行失败：任务Key{}", taskKey, e);
                    // 可选：失败后不移除，下一周期重试
                    // DELAYED_ZT_EF_STATUS_WRITE.remove(taskKey);
                }
            });
        }

        /* ---------------- 原步骤：处理待处理集合（逻辑不变，但处理的是转移后的 ztLogPending） ---------------- */
        processPendingLogs(snapshotMap);
        // 1. 将上一周期标记的 ztLogPendingNext 转移到 ztLogPending（待处理）
        // 2. 清空 ztLogPendingNext，为当前周期的新标记做准备
        transferPendingLogs();

        runStrategy(snapshotMap , sceneId);
    }

    /**
     * -------------------------------------------------
     * 对外暴露的唯一方法
     * -------------------------------------------------
     *
     * @param allLatest           最新值列表
     * @param devicePointLatestBOList 设备+点位元数据列表
     * @return key = deviceCode , value = DeviceSnapshotBO 或 TMDeviceSnapshotBO
     */
    public Map<String, Object> buildSnapshotMap(List<PointValueBO> allLatest,
                                                List<DevicePointLatestBO> devicePointLatestBOList) {

        /* ---------- 1. 建立索引 ---------- */
        // deviceId -> DevicePointLatestBO
        Map<Long, DevicePointLatestBO> deviceMap =
                devicePointLatestBOList.stream()
                        .collect(Collectors.toMap(DevicePointLatestBO::getId, Function.identity()));

        // (deviceId , pointId) -> PointBO
        Map<String, PointBO> pointMap =
                devicePointLatestBOList.stream()
                        .flatMap(d -> d.getPoints().stream()
                                .map(p -> Map.entry(d.getId() + "_" + /* PointBO.id */ p.getId(), p)))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        /* ---------- 2. 把 PointValueBO -> PointNodeBO ---------- */
        // deviceId -> List<PointNodeBO>
        Map<Long, List<PointNodeBO>> devicePointNodes = allLatest.stream()
                .collect(Collectors.groupingBy(PointValueBO::getDeviceId,
                        Collectors.mapping(pv -> {
                            PointBO point = pointMap.get(pv.getDeviceId() + "_" + pv.getPointId());
                            return new PointNodeBO(
                                    String.valueOf(pv.getPointId()),
                                    point == null ? "" : point.getPointName(),
                                    point == null ? "" : point.getPointCode(),
                                    point == null ? "" : point.getUnit(),
                                    pv.getRawValue(),
                                    pv.getCalValue());
                        }, Collectors.toList())));

        /* ---------- 3. 组装 Snapshot ---------- */
        return devicePointLatestBOList.stream()
                .collect(Collectors.toMap(DevicePointLatestBO::getDeviceName,
                        dev -> {
                            List<PointNodeBO> nodes = devicePointNodes.getOrDefault(dev.getId(), List.of());
                            Map<String, PointNodeBO> nodeMap = nodes.stream()
                                    .collect(Collectors.toMap(PointNodeBO::getPointCode, Function.identity()));

                            //boolean isZt = "ZT".equalsIgnoreCase(dev.getDeviceCode());
                            boolean isZt = dev.getDeviceCode() != null && dev.getDeviceCode().trim().startsWith("ZT");
                            boolean isSj = dev.getDeviceCode() != null && dev.getDeviceCode().trim().startsWith("SJ");

                            if (isZt) {
                                return buildZtSnapshot(dev, nodeMap);
                            }
                            else if (isSj) {
                                return buildSjSnapshot(dev, nodeMap);
                            }else {
                                return buildTmSnapshot(dev, nodeMap);
                            }
                        }));
    }

    /* ---------------- 构造 DeviceSnapshotBO ---------------- */
    private DeviceSnapshotBO buildZtSnapshot(DevicePointLatestBO dev,
                                             Map<String, PointNodeBO> nodeMap) {
        DeviceSnapshotBO ds = new DeviceSnapshotBO();
        ds.setId(String.valueOf(dev.getId()));
        ds.setDeviceCode(dev.getDeviceCode());
        ds.setDeviceName(dev.getDeviceName());
        Long deviceId = dev.getId();
        String realStatus = getRealDeviceStatus(deviceId);
        ds.setStatus(realStatus); // 替换原硬编码的 "ONLINE"
        ds.setCtrl_mode(nodeMap.get("ctrl_mode"));
        ds.setRun_state(nodeMap.get("run_state"));
        ds.setNav_mode(nodeMap.get("nav_mode"));
        ds.setBattery_voltage(nodeMap.get("battery_voltage"));
        ds.setBlind_dist(nodeMap.get("blind_dist"));
        ds.setCurrent_tag(nodeMap.get("current_tag"));
        ds.setNav_route(nodeMap.get("nav_route"));
        ds.setEf_status(nodeMap.get("ef_status"));
//        ds.setLspeed(nodeMap.get("lspeed"));
//        ds.setRspeed(nodeMap.get("rspeed"));
//        ds.setWork_status(nodeMap.get("work_status"));
//        ds.setOperating_mode(nodeMap.get("operating_mode"));
//        ds.setCleaning(nodeMap.get("cleaning"));
//        ds.setFull(nodeMap.get("full"));
//        ds.setEjection(nodeMap.get("ejection"));
        ds.setCharging(nodeMap.get("charging"));
//        ds.setExit_direction(nodeMap.get("exit_direction"));
//        ds.setEWP(nodeMap.get("EWP"));
//        ds.setTrun_count(nodeMap.get("trun_count"));
//        ds.setCAF(nodeMap.get("CAF"));
        return ds;
    }

    /* ---------------- 构造 TMDeviceSnapshotBO ---------------- */
    private SJDeviceSnapshotBO buildSjSnapshot(DevicePointLatestBO dev,
                                               Map<String, PointNodeBO> nodeMap) {
        SJDeviceSnapshotBO tm = new SJDeviceSnapshotBO();
        tm.setId(String.valueOf(dev.getId()));
        tm.setDeviceCode(dev.getDeviceCode());
        tm.setDeviceName(dev.getDeviceName());
        tm.setStatus("ONLINE"); // 同上
        tm.setSWS1(nodeMap.get("SWS1"));
        tm.setSRS1(nodeMap.get("SRS1"));
        tm.setSWS2(nodeMap.get("SWS2"));
        tm.setSRS2(nodeMap.get("SRS2"));
        return tm;
    }

    /* ---------------- 构造 TMDeviceSnapshotBO ---------------- */
    private TMDeviceSnapshotBO buildTmSnapshot(DevicePointLatestBO dev,
                                               Map<String, PointNodeBO> nodeMap) {
        TMDeviceSnapshotBO tm = new TMDeviceSnapshotBO();
        tm.setId(String.valueOf(dev.getId()));
        tm.setDeviceCode(dev.getDeviceCode());
        tm.setDeviceName(dev.getDeviceName());
        tm.setStatus("ONLINE"); // 同上
        tm.setLwpfull(nodeMap.get("lwpfull"));
        tm.setRwpfull(nodeMap.get("rwpfull"));
        tm.setLlifting(nodeMap.get("llifting"));
        tm.setRlifting(nodeMap.get("rlifting"));
        tm.setLcleaning(nodeMap.get("lcleaning"));
        tm.setRcleaning(nodeMap.get("rcleaning"));
        tm.setLstart(nodeMap.get("lstart"));
        tm.setRstart(nodeMap.get("rstart"));
        return tm;
    }

    /**
     * 公共方法：根据设备ID获取真实设备状态（复用Redis缓存逻辑）
     * 参考 DeviceStatusServiceImpl.getStatusMap 方法
     * @param deviceId 设备ID（Long类型）
     * @return 设备状态字符串（ONLINE/OFFLINE/MAINTAIN/FAULT）
     */
    private String getRealDeviceStatus(Long deviceId) {
        // 1. 构建Redis缓存key，与 DeviceStatusServiceImpl 保持一致
        String statusKey = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + deviceId;

        // 2. 从Redis中获取设备状态
        String deviceStatus = redisService.getKey(statusKey);

        // 3. 容错处理：Redis中无数据时，默认返回离线状态（与参考代码一致）
        if (Objects.isNull(deviceStatus) || deviceStatus.trim().isEmpty()) {
            return DeviceStatusEnum.OFFLINE.getCode();
        }

        // 4. 返回真实状态（确保状态与枚举一致）
        return deviceStatus;
    }

    /**
     * 新增方法：转移待处理集合（周期N+1执行）
     * 作用：将 ztLogPendingNext（周期N标记）转移到 ztLogPending（周期N+2处理）
     */
    private void transferPendingLogs() {
        synchronized (ztLogPending) { // 加锁确保线程安全（避免并发修改）
            // 1. 转移：将 ztLogPendingNext 的所有设备编码加入 ztLogPending
            ztLogPending.addAll(ztLogPendingNext);
            // 2. 清空：为当前周期的新标记释放空间
            ztLogPendingNext.clear();

            log.debug("transferPendingLogs: 待处理集合转移完成，当前待处理设备数：{}", ztLogPending.size());
        }
    }

    /**
     * 原方法：处理待处理集合（周期N+2执行）
     * 逻辑不变，但处理的是转移后的 ztLogPending（周期N标记的设备）
     *
     *
     * @param snapshotMap 设备快照映射，用于获取待保存设备的完整信息
     */
    private void processPendingLogs(Map<String, Object> snapshotMap) {
        synchronized (ztLogPending) {
            // 若集合为空，直接跳过
            if (ztLogPending.isEmpty()) {
                log.debug("processPendingLogs: 当前无待保存ZT标记，跳过处理");
                return;
            }

            // 记录待处理的设备编码（避免处理中集合被修改）
            List<String> pendingDevices = new ArrayList<>(ztLogPending);
            log.info("processPendingLogs: 开始处理待保存标记，共{}个设备：{}", pendingDevices.size(), pendingDevices);

            // 遍历标记设备，执行保存操作
            for (String deviceCode : pendingDevices) {
                // 从快照映射中获取设备完整信息
                Object snapshotObj = snapshotMap.get(deviceCode);
                if (!(snapshotObj instanceof DeviceSnapshotBO zt)) {
                    log.warn("processPendingLogs: 设备{}标记存在，但未找到对应快照，跳过保存", deviceCode);
                    continue;
                }

                // 执行保存逻辑
                try {
                    devicePointsJsonbService.save(zt);
                    log.debug("processPendingLogs: 设备{}保存成功", deviceCode);
                } catch (Exception e) {
                    log.error("processPendingLogs: 设备{}保存失败", deviceCode, e);
                }
            }

            // 消费完成后清空集合（避免残留标记）
            ztLogPending.clear();
            log.debug("processPendingLogs: 待保存标记处理完成，集合已清空");
        }
    }

    /**
     * 解析SJ设备的指定绑定码（SWS后可跟任意数字，如SWS1、SWS10、SWS20等）
     */
    private Map<String, SJDeviceSnapshotBO> parseSjBindCodes(List<SJDeviceSnapshotBO> sjList) {
        Map<String, SJDeviceSnapshotBO> sjBindCodeMap = new HashMap<>();
        // 匹配SWS后跟任意数字的字段名正则（如SWS1、SWS10、SWS20等）
        String swsFieldPattern = "^SWS\\d+$";

        for (SJDeviceSnapshotBO sj : sjList) {
            try {
                String sjDeviceCode = sj.getDeviceCode();
                log.debug("开始解析SJ:{}的绑定码", sjDeviceCode);

                // 通过反射获取SJDeviceSnapshotBO中所有以SWS开头的字段
                Field[] fields = SJDeviceSnapshotBO.class.getDeclaredFields();
                for (Field field : fields) {
                    String fieldName = field.getName();
                    // 过滤出符合SWS+数字格式的字段
                    if (!fieldName.matches(swsFieldPattern)) {
                        continue;
                    }

                    // 设置字段可访问（兼容私有字段）
                    field.setAccessible(true);
                    // 获取字段值（PointNodeBO类型）
                    PointNodeBO pointNode = (PointNodeBO) field.get(sj);
                    // 解析calValue值
                    String fieldValue = cal(pointNode);

                    // 仅当值为"1"且该绑定码未被其他SJ设备占用时，加入映射
                    if ("1".equals(fieldValue) && !sjBindCodeMap.containsKey(fieldName)) {
                        sjBindCodeMap.put(fieldName, sj);
                        log.info("SJ:{} {}触发，绑定码：{}", sjDeviceCode, fieldName, fieldName);
                    }
                }
            } catch (IllegalAccessException e) {
                log.error("SJ:{} 反射获取SWS字段值异常", sj.getDeviceCode(), e);
            } catch (Exception e) {
                log.error("SJ:{} 绑定码解析异常", sj.getDeviceCode(), e);
            }
        }
        return sjBindCodeMap;
    }

    /**
     * 解析TM设备的满筒绑定码（LWP/RWP后可跟任意数字，如LWP2、LWP10、RWP3等）
     * @param tmList TM设备快照列表
     * @return key=满筒绑定码（如LWP2、RWP10），value=触发该绑定码的TM设备快照
     */
    private Map<String, TMDeviceSnapshotBO> parseTmFullCodes(List<TMDeviceSnapshotBO> tmList,
                                                             Map<String, NavNodeBO> bindNodeMap) {
        Map<String, TMDeviceSnapshotBO> fullTmMap = new HashMap<>();
        // 匹配LWP/RWP后跟任意数字的字段名正则（如LWP2、LWP10、RWP3等）
//        String tmFieldPattern = "^(LWP|RWP)\\d+$";
        // 优化后：匹配以LWP/RWP开头的所有字段（忽略大小写，支持任意后缀）
        String tmFieldPattern = "^(LWP|RWP).*$";
        Pattern pattern = Pattern.compile(tmFieldPattern, Pattern.CASE_INSENSITIVE);

        // 正则2：仅提取LWP/RWP核心前缀（去掉数字捕获组，只保留前缀）
        Pattern lwpCodePattern = Pattern.compile("^(LWP|RWP)", Pattern.CASE_INSENSITIVE);

        for (TMDeviceSnapshotBO tm : tmList) {
            try {
                String tmDeviceCode = tm.getDeviceCode();
                String tmDeviceId = tm.getId();
                log.debug("开始解析TM:{}的满筒信号", tmDeviceCode);

                // 通过反射获取TMDeviceSnapshotBO中所有LWP/RWP开头的字段
                Field[] fields = TMDeviceSnapshotBO.class.getDeclaredFields();
                for (Field field : fields) {
                    String fieldName = field.getName();
                    // 过滤出符合LWP/RWP+数字格式的字段
                    Matcher matcher = pattern.matcher(fieldName);
                    if (!matcher.matches()) { // 此处使用Matcher.matches()进行全匹配
                        continue;
                    }
//                    if (!fieldName.toLowerCase().contains("lwp")) { // 不包含则跳过
//                        continue;
//                    }
                    // 设置字段可访问（兼容私有字段）
                    field.setAccessible(true);
                    // 获取字段值（PointNodeBO类型）
                    PointNodeBO pointNode = (PointNodeBO) field.get(tm);
                    // 解析calValue值（1表示满筒）
                    String fieldValue = cal(pointNode);

                    // 仅当值为"1"且该绑定码未被其他TM设备占用时，加入映射
                    if ("1".equals(fieldValue)) {
                        String pureLwpCode = extractPureLwpCode(fieldName, lwpCodePattern);
                        if (pureLwpCode == null) {
                            log.warn("TM:{} 字段名{}无法提取有效LWP/RWP编码，跳过", tmDeviceCode, fieldName);
                            continue;
                        }
                        Map<String, NavNodeBO> filteredBindNodeMap = bindNodeMap.entrySet().stream()
                                .filter(entry -> entry.getKey().toUpperCase().contains(pureLwpCode))
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (old, newVal) -> old
                                ));

                        if (filteredBindNodeMap.isEmpty()) {
                            log.warn("TM:{} 核心前缀{}未匹配到bindNodeMap中任何条目，跳过", tmDeviceCode, pureLwpCode);
                            continue;
                        }

                        // 2. 提取第一个匹配deviceId的绑定码（解决IDE误判的核心修改）
                        String firstMatchedBindCode = null;
                        // 显式遍历Map，替代流操作（IDE不会误判）
                        for (Map.Entry<String, NavNodeBO> entry : filteredBindNodeMap.entrySet()) {
                            NavNodeBO navNode = entry.getValue();
                            // 空值防护
                            if (navNode == null || navNode.getCoordinateBind() == null) {
                                continue;
                            }
                            DeviceCoordinateBindBO bind = navNode.getCoordinateBind();
                            if (bind.getAttribute() == null) {
                                continue;
                            }
                            DeviceCoordinateAttributeBO attribute = bind.getAttribute();
                            // 匹配deviceId
                            if (tmDeviceId != null && tmDeviceId.equals(attribute.getDeviceId().toString())) {
                                firstMatchedBindCode = entry.getKey();
                                break; // 找到第一个就终止循环
                            }
                        }

                        if (firstMatchedBindCode == null) {
                            log.warn("TM:{} 核心前缀{}匹配到条目，但无deviceId={}的节点，跳过",
                                    tmDeviceCode, pureLwpCode, tmDeviceId);
                            continue;
                        }

                        fullTmMap.put(firstMatchedBindCode, tm);
                        log.info("TM:{} 字段{}解析出核心编码{}，已关联（唯一标识：{}-{}）",
                                tmDeviceCode, fieldName, pureLwpCode, pureLwpCode, tmDeviceCode);
                    }
                }
            } catch (IllegalAccessException e) {
                log.error("TM:{} 反射获取LWP/RWP字段值异常", tm.getDeviceCode(), e);
            } catch (Exception e) {
                log.error("TM:{} 满筒信号解析异常", tm.getDeviceCode(), e);
            }
        }
        return fullTmMap;
    }

    /**
     * 获取ZT当前坐标对应的绑定码
     */
    private String getZtCurrentBindCode(DeviceSnapshotBO zt, Map<String, NavNodeBO> gridNodeMap, Map<String, NavNodeBO> bindNodeMap) {
        try {
            String tag = cal(zt.getCurrent_tag());
            if (tag.length() < 4) {
                log.warn("ZT:{} current_tag={} 长度不足4位，无法解析当前绑定码", zt.getDeviceCode(), tag);
                return null;
            }
            tag = "0".repeat(6 - tag.length()) + tag;
            String gridX = String.valueOf(Integer.parseInt(tag.substring(0, 3)));
            String gridY = String.valueOf(Integer.parseInt(tag.substring(3, 6)));
            NavNodeBO currentNode = gridNodeMap.get(gridKey(gridX, gridY));
            if (currentNode == null) {
                log.warn("ZT:{} 当前坐标(gridX={},gridY={}) 未匹配NavNode，无法解析绑定码", zt.getDeviceCode(), gridX, gridY);
                return null;
            }
            // 反向查找绑定码（NavNode -> 绑定码）
            for (Map.Entry<String, NavNodeBO> entry : bindNodeMap.entrySet()) {
                if (entry.getValue().getId().equals(currentNode.getId())) {
                    return entry.getKey();
                }
            }
        } catch (Exception e) {
            log.error("ZT:{} 解析当前绑定码异常", zt.getDeviceCode(), e);
        }
        return null;
    }

    /**
     * 实时计算所有ZT当前占据的绑定码映射（无全局缓存，每次调度重新计算）
     * @param ztList 所有ZT快照
     * @param gridNodeMap 坐标-节点映射
     * @param bindNodeMap 绑定码-节点映射
     * @return key=绑定码，value=ZT设备码
     */
    private Map<String, String> calculateZtOccupiedBindCodes(List<DeviceSnapshotBO> ztList,
                                                             Map<String, NavNodeBO> gridNodeMap,
                                                             Map<String, NavNodeBO> bindNodeMap) {
        Map<String, String> occupiedMap = new HashMap<>();
        for (DeviceSnapshotBO zt : ztList) {
            String ztCode = zt.getDeviceCode();
            String currentBindCode = getZtCurrentBindCode(zt, gridNodeMap, bindNodeMap);
            if (currentBindCode != null && !currentBindCode.isEmpty()) {
                // 同一绑定码被多个ZT占据时（理论上不会发生），取第一个即可（物理上不可能）
                occupiedMap.putIfAbsent(currentBindCode, ztCode);
            }
        }
        return occupiedMap;
    }

    /**
     * 简化版：判断绑定码是否被其他ZT占据（实时计算，无缓存）
     * @param code 目标绑定码
     * @param currentZtCode 当前ZT设备码
     * @param occupiedMap 所有ZT的绑定码-设备码映射（实时计算）
     * @return true=被其他ZT占据，false=未被占据/被当前ZT占据
     */
    private boolean isOccupiedByOtherZt(String code, String currentZtCode, Map<String, String> occupiedMap) {
        String occupiedZtCode = occupiedMap.get(code);
        // 逻辑：绑定码被占据 + 占据者不是当前ZT
        return occupiedZtCode != null && !occupiedZtCode.equals(currentZtCode);
    }

    /**
     * 获取所有ZT设备当前所在的坐标集合（核心：拿到所有ZT的实时位置）
     * @param ztList 所有ZT设备的快照列表
     * @return 所有ZT当前坐标的字符串集合（格式：gridX,gridY）
     */
    private Set<String> getAllZTCurrentGrids(List<DeviceSnapshotBO> ztList) {
        Set<String> allZtGrids = new HashSet<>();
        for (DeviceSnapshotBO zt : ztList) {
            try {
                // 解析单个ZT的当前坐标（复用原有解析逻辑）
                String tag = cal(zt.getCurrent_tag());
                if (tag.length() < 4) {
                    log.warn("ZT:{} current_tag长度不足，跳过坐标解析", zt.getDeviceCode());
                    continue;
                }

//                tag = "0".repeat(6 - tag.length()); // 补零到6位
                tag = String.format("%06d", Long.parseLong(tag));
                String gridX = String.valueOf(Integer.parseInt(tag.substring(0, 3)));
                String gridY = String.valueOf(Integer.parseInt(tag.substring(3, 6)));
                // 拼接坐标字符串并加入集合
                allZtGrids.add(gridX + "," + gridY);
            } catch (Exception e) {
                log.error("解析ZT:{} 当前坐标异常", zt.getDeviceCode(), e);
            }
        }
        return allZtGrids;
    }

    /**
     * 终极版：直接从 navNodes 查找所有ZT当前位置对应的Long类型nodeId集合
     * 适配 NavNodeBO.id = Long 类型，无冗余步骤，格式严格对齐
     * @param ztList 所有ZT设备的快照列表
     * @param navNodes 导航节点列表（id为Long类型）
     * @return 所有ZT当前位置对应的nodeId（Long类型）集合
     */
    private Set<Long> getAllZTCurrentNodeIds(List<DeviceSnapshotBO> ztList, List<NavNodeBO> navNodes) {
        Set<Long> allZtNodeIds = new HashSet<>();

        for (DeviceSnapshotBO zt : ztList) {
            try {
                // 1. 解析ZT当前的gridX/gridY（补零为3位字符串，和原有逻辑一致）
                String tag = cal(zt.getCurrent_tag());
                if (tag.length() < 4) {
                    log.warn("ZT:{} current_tag长度不足，跳过nodeId解析", zt.getDeviceCode());
                    continue;
                }
                tag = String.format("%06d", Long.parseLong(tag));
                String gridX = tag.substring(0, 3); // 3位补零字符串（如"001"）
                String gridY = tag.substring(3, 6); // 3位补零字符串（如"020"）

                // 2. 遍历navNodes，匹配坐标并提取Long类型的nodeId
                for (NavNodeBO node : navNodes) {
                    // 跳过gridX/gridY为空的无效节点
                    if (node.getGridX() == null || node.getGridY() == null) {
                        continue;
                    }

                    // 将NavNode的数字型gridX/gridY转为3位补零字符串，保证格式对齐
                    String nodeGridX = String.format("%03d", node.getGridX());
                    String nodeGridY = String.format("%03d", node.getGridY());

                    // 坐标匹配 + 校验nodeId非空（Long类型）
                    if (gridX.equals(nodeGridX) && gridY.equals(nodeGridY)) {
                        Long nodeId = node.getId();
                        if (nodeId != null) {
                            allZtNodeIds.add(nodeId); // 直接添加Long类型，无类型转换
                            log.debug("ZT:{} 匹配到nodeId：{}（Long类型），坐标({},{})",
                                    zt.getDeviceCode(), nodeId, gridX, gridY);
                        } else {
                            log.warn("ZT:{} 坐标({},{})匹配到NavNode，但nodeId为空", zt.getDeviceCode(), gridX, gridY);
                        }
                        break; // 匹配到即退出循环，提升效率
                    }
                }
            } catch (Exception e) {
                log.error("解析ZT:{} 当前nodeId（Long类型）异常", zt.getDeviceCode(), e);
            }
        }
        return allZtNodeIds;
    }

    /**
     * 辅助方法：获取单台ZT自身的nodeId（Long类型）
     * 单独抽离，方便复用和维护
     */
    private Long getCurrentZtNodeId(DeviceSnapshotBO zt, List<NavNodeBO> navNodes) {
        try {
            String tag = cal(zt.getCurrent_tag());
            if (tag.length() < 4) {
                return null;
            }
            tag = String.format("%06d", Long.parseLong(tag));
            String gridX = tag.substring(0, 3);
            String gridY = tag.substring(3, 6);

            for (NavNodeBO node : navNodes) {
                if (node.getGridX() == null || node.getGridY() == null) {
                    continue;
                }
                String nodeGridX = String.format("%03d", node.getGridX());
                String nodeGridY = String.format("%03d", node.getGridY());

                if (gridX.equals(nodeGridX) && gridY.equals(nodeGridY)) {
                    return node.getId(); // 返回Long类型的nodeId
                }
            }
        } catch (Exception e) {
            log.error("获取ZT:{} 自身nodeId异常", zt.getDeviceCode(), e);
        }
        return null;
    }

    /**
     * 简化版：判断ZT是否到达锁定绑定码的位置（直接对比坐标）
     * @param zt ZT设备快照
     * @param lockedCode 锁定的绑定码
     * @param bindNodeMap 绑定码-节点映射
     * @return true=已到达，false=未到达
     */
    private boolean isZtArrivedAtTarget(DeviceSnapshotBO zt, String lockedCode, Map<String, NavNodeBO> bindNodeMap) {
        try {
            // 1. 获取锁定绑定码的目标坐标
            NavNodeBO targetNode = bindNodeMap.get(lockedCode);
            if (targetNode == null) {
                log.warn("ZT:{} 锁定的绑定码{}无对应节点，无法判断到达状态", zt.getDeviceCode(), lockedCode);
                return false;
            }
            String targetGridX = targetNode.getGridX().toString();
            String targetGridY = targetNode.getGridY().toString();
            String targetGridKey = gridKey(targetGridX, targetGridY);

            // 2. 获取ZT当前坐标（从tag解析，和原逻辑保持一致）
            String ztCurrentTag = cal(zt.getCurrent_tag());
            if (ztCurrentTag == null || ztCurrentTag.length() < 4) {
                log.warn("ZT:{} 当前坐标tag异常（{}），无法判断到达状态", zt.getDeviceCode(), ztCurrentTag);
                return false;
            }
            // 格式化tag为6位（补0），前3位X，后3位Y
            ztCurrentTag = String.format("%06d", Long.parseLong(ztCurrentTag));
            String ztGridX = ztCurrentTag.substring(0, 3);
            String ztGridY = ztCurrentTag.substring(3, 6);
            String ztCurrentGridKey = gridKey(ztGridX, ztGridY);

            // 3. 坐标完全一致则判定为到达
            boolean isArrived = targetGridKey.equals(ztCurrentGridKey);
            log.debug("ZT:{} 到达判断 - 锁定码{}目标坐标{}，当前坐标{}，是否到达：{}",
                    zt.getDeviceCode(), lockedCode, targetGridKey, ztCurrentGridKey, isArrived);
            return isArrived;
        } catch (Exception e) {
            log.error("ZT:{} 判定到达锁定码{}位置异常", zt.getDeviceCode(), lockedCode, e);
            return false;
        }
    }


    private void runStrategy(Map<String, Object> snapshotMap, String sceneId) {
//        // ========== 新增：本次调度循环内，已规划有效路径的ZT节点ID缓存（线程安全） ==========
//        Set<Long> currentCyclePlannedNodeIds = ConcurrentHashMap.newKeySet();

        cleanTimeoutPathCache();

        cleanTimeoutLocks(); //todo 每隔 1 分钟调用cleanTimeoutLocks()，清理超时锁定码

        /* 0. 导航配置 */
        Optional<CusNavOverviewBO> navOpt =
                Optional.ofNullable(navOverviewMetadata.getCache(Long.parseLong(sceneId)));
        if (navOpt.isEmpty()) {
            log.warn("未配置 CusNavOverview，跳过调度");
            return;
        }
        List<NavNodeBO> navNodes = navOpt.get().getNavNodes();
        if (navNodes == null || navNodes.isEmpty()) {
            log.warn("NavNodes 为空，跳过调度");
            return;
        }

        /* 1. (gridX,gridY) -> NavNodeBO  只用于匹配起点 */
        Map<String, NavNodeBO> gridNodeMap = navNodes.stream()
                .filter(n -> n.getGridX() != null && n.getGridY() != null)
                .collect(Collectors.toMap(
                        n -> gridKey(n.getGridX().toString(), n.getGridY().toString()),
                        Function.identity(),
                        (a, b) -> a));

        /* 2. 绑定码 -> NavNodeBO  直接取终点 */
        Map<String, NavNodeBO> bindNodeMap = navNodes.stream()
                .filter(n -> n.getCoordinateBind() != null
                        && n.getCoordinateBind().getAttribute() != null
                        && n.getCoordinateBind().getAttribute().getAttributeCode() != null)
                .collect(Collectors.toMap(
                        n -> n.getCoordinateBind().getAttribute().getAttributeCode(),
                        Function.identity(),
                        (a, b) -> a));

        /* 3. 所有 ZT 快照 */
        List<DeviceSnapshotBO> ztList = snapshotMap.values().stream()
                .filter(DeviceSnapshotBO.class::isInstance)
                .map(DeviceSnapshotBO.class::cast)
                .filter(d -> d.getDeviceCode() != null
                        && d.getDeviceCode().startsWith("ZT"))
                .toList();

        // 3.2 筛选SJ设备并调度（新增）
        List<SJDeviceSnapshotBO> sjList = snapshotMap.values().stream()
                .filter(SJDeviceSnapshotBO.class::isInstance)
                .map(SJDeviceSnapshotBO.class::cast)
                .filter(d -> d.getDeviceCode() != null && d.getDeviceCode().trim().startsWith("SJ"))
                .toList();

        // 3.3 筛选TM设备并调度（新增）
        List<TMDeviceSnapshotBO> tmList = snapshotMap.values().stream()
                .filter(TMDeviceSnapshotBO.class::isInstance)
                .map(TMDeviceSnapshotBO.class::cast)
                .filter(d -> d.getDeviceCode() != null && d.getDeviceCode().trim().startsWith("TM"))
                .toList();

        // 存储 满筒绑定码 -> TM设备快照（LWP/RWP专属）
        Map<String, TMDeviceSnapshotBO> fullTmMap = parseTmFullCodes(tmList, bindNodeMap);

        // ====================== 解析SJ指定绑定码 ======================
        Map<String, SJDeviceSnapshotBO> sjBindCodeMap = parseSjBindCodes(sjList);


//        // 筛选bindNodeMap中的SJ专属绑定码
//        Map<String, NavNodeBO> sjBindNodeMap = bindNodeMap.entrySet().stream()
//                .filter(entry -> SJ_EXCLUSIVE_CODES.contains(entry.getKey()))
//                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//
//        // 筛选bindNodeMap中的TM专属绑定码
//        Map<String, NavNodeBO> tmBindNodeMap = bindNodeMap.entrySet().stream()
//                .filter(entry -> TM_EXCLUSIVE_CODES.contains(entry.getKey()))
//                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

//        Set<String> allZtCurrentGrids = getAllZTCurrentGrids(ztList);
//        Set<Long> allZtNodeIds = getAllZTCurrentNodeIds(ztList, navNodes);
        // 新增：实时计算所有ZT的绑定码占据映射（替代原refreshOccupiedBindCodes）
        Map<String, String> ztOccupiedBindCodes = calculateZtOccupiedBindCodes(ztList, gridNodeMap, bindNodeMap);

        for (DeviceSnapshotBO zt : ztList) {
            try {
                String currentNavMode = cal(zt.getNav_mode());
                String currentNavRoute = cal(zt.getNav_route());

                String lastNavMode = LAST_NAV_MODE.put(zt.getDeviceCode(), currentNavMode);
                String lastNavRoute = LAST_NAV_ROUTE.put(zt.getDeviceCode(), currentNavRoute);

                // ========== 核心新增：ZT从30→40时，释放上一轮目标绑定码 ==========
                boolean modeChangedFrom30To40 = "30".equals(lastNavMode) && "40".equals(currentNavMode);
                String ztDeviceCode = zt.getDeviceCode();
                if (modeChangedFrom30To40) {
                    // 直接调用releaseZtAllLocks，释放该ZT所有通过tryLockCode锁定的码
                    releaseZtAllLocks(ztDeviceCode);
                    log.info("ZT:{} 状态从30→40（完成路径执行），释放所有tryLockCode锁定的绑定码", ztDeviceCode);
                    // 同步清理路径缓存与时间戳
                    ZT_DEVICE_PATH_NODE_CACHE.remove(ztDeviceCode);
                    ZT_DEVICE_PATH_TIMESTAMPS.remove(ztDeviceCode);
                    log.debug("ZT:{} 路径执行完成，已清理路径节点ID缓存及时间戳", ztDeviceCode);

//                    //清理SWS-ZCS关联
//                    clearZcsSwsMapping(ztDeviceCode);

                    String finalBindCode = getZtCurrentBindCode(zt, gridNodeMap, bindNodeMap);
                    if (finalBindCode != null && !finalBindCode.isEmpty() &&
                            !finalBindCode.contains("RWP") && !finalBindCode.contains("LWP")) {
                        LAST_ZT_BIND_CODE.put(ztDeviceCode, finalBindCode);
                        log.debug("ZT:{} 完成任务（30→40），记录终点绑定码：{}", ztDeviceCode, finalBindCode);
                    }
                }

                if (!"ONLINE".equalsIgnoreCase(zt.getStatus())
                        || !"64".equalsIgnoreCase(cal(zt.getCtrl_mode()))) {
                    ZT_DEVICE_PATH_NODE_CACHE.remove(zt.getDeviceCode());
                    ZT_DEVICE_PATH_TIMESTAMPS.remove(zt.getDeviceCode());
                    continue;                 // 不可调度/设备不在线，跳过
                }

//                releaseZtAllLocks(zt.getDeviceCode());

                /* ===== 新增：nav_mode=30 时做“坐标漂移”保护 ===== */
                if ("30".equalsIgnoreCase(cal(zt.getNav_mode()))) {
                    /* 1. 解析当前坐标 */
                    String tag = cal(zt.getCurrent_tag());
                    if (tag.length() < 4) {
                        log.warn("ZT:{} current_tag={} 长度不足，无法校验坐标", zt.getDeviceCode(), tag);
                        continue;          // 坐标无效，直接跳过
                    }
                    tag = "0".repeat(6 - tag.length()) + tag;
                    String curGridX = String.valueOf(Integer.parseInt(tag.substring(0, 3)));
                    String curGridY = String.valueOf(Integer.parseInt(tag.substring(3, 6)));
                    String curGrid  = curGridX + "," + curGridY;

                    /* 2. 解析 nav_route 里的所有坐标（只要前 6 位，后面全扔） */
                    String routeStr = cal(zt.getNav_route());
                    Set<String> routeGrids = Arrays.stream(routeStr.split(";"))
                            .map(String::trim)
                            // 过滤出包含逗号且逗号前后长度足够的字符串
                            .filter(s -> s.contains(",") && s.split(",", 2).length == 2)
                            .map(s -> {
                                // 分割出逗号前的部分作为处理对象
                                String part = s.split(",", 2)[0];
                                // 确保有足够的长度
                                if (part.length() >= 6) {
                                    // 提取最后6位数字
                                    String sixDigits = part.substring(part.length() - 6);
                                    int x = Integer.parseInt(sixDigits.substring(0, 3));
                                    int y = Integer.parseInt(sixDigits.substring(3, 6));
                                    // x不补0，y保留3位补0
                                    return String.format("%d,%d", x, y);
                                }
                                return null;
                            })
                            // 过滤掉处理失败的结果
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    /* 3. 若当前点不在路线内，认为“走错路”，立即重新下发 */
                    if (!routeGrids.isEmpty() && !routeGrids.contains(curGrid)) {
                        log.warn("ZT:{} 坐标({}) 不在当前 nav_route 内，判定为路线失效，即将重新下发",
                                zt.getDeviceCode(), curGrid);
                        // 把 nav_mode 强制切回 40，触发后续逻辑重新规划
                        releaseZtAllLocks(ztDeviceCode);
                        write(zt.getId(), zt.getNav_mode(), "40");
                        continue;          // 本次循环不再做后续调度
                    }
                }
                /* ===== 新增结束 ===== */
                // 判断是否从 40 -> 30
                boolean modeChangedFrom40To30 = "40".equals(lastNavMode) && "30".equals(currentNavMode);

                // 判断 nav_route 是否发生了有效变化（非空、非0）
                boolean routeValidChanged = lastNavRoute != null
                        && !lastNavRoute.isBlank()
                        && !"0".equals(lastNavRoute)
                        && !lastNavRoute.equals(currentNavRoute);

                if (modeChangedFrom40To30 && routeValidChanged) {

                    /* ---------------- 关键修改：标记加入 ztLogPendingNext（而非原 ztLogPending） ---------------- */
                    boolean added = ztLogPendingNext.add(zt.getDeviceCode());
                    log.info("runStrategy: 设备{}满足标记条件（40→30+路线变化），添加待转移标记：{}，当前待转移数：{}",
                            zt.getDeviceCode(), added ? "成功" : "已存在", ztLogPendingNext.size());

                }

                if (!"40".equalsIgnoreCase(cal(zt.getNav_mode()))) {
                    continue;                 // 不可调度
                }

                // 1. 解析ZT当前坐标（复用原有逻辑）
                String tag = cal(zt.getCurrent_tag());
                if (tag.length() < 4) {
                    log.warn("ZT:{} nav_mode=40，但current_tag={} 长度不足4位，跳过", zt.getDeviceCode(), tag);
                    continue;
                }
                tag = "0".repeat(6 - tag.length()) + tag;
                String gridX = String.valueOf(Integer.parseInt(tag.substring(0, 3)));
                String gridY = String.valueOf(Integer.parseInt(tag.substring(3, 6)));
                String currentGridKey = gridKey(gridX, gridY);

                // ========== 第一步：判断是否在TM工作位（LWP/RWP） ==========
                boolean isAtTmWorkPos = false;
                TMDeviceSnapshotBO matchedTmDevice = null;
                String matchedTmBindCode = null;
                // 遍历TM专属绑定码（LWP/RWP）
                String tmBindCodePattern = "^(LWP|RWP)\\d+$";
                for (String tmBindCode : bindNodeMap.keySet()) {
                    // 过滤出符合LWP/RWP+数字格式的绑定码
                    if (!tmBindCode.matches(tmBindCodePattern)) {
                        continue;
                    }

                    NavNodeBO tmBindNode = bindNodeMap.get(tmBindCode);
                    if (tmBindNode == null) continue;
                    String tmGridKey = gridKey(tmBindNode.getGridX().toString(), tmBindNode.getGridY().toString());
                    if (currentGridKey.equals(tmGridKey)) {
                        isAtTmWorkPos = true;
                        matchedTmBindCode = tmBindCode;
                        // 获取匹配的TM设备快照（fullTmMap已适配任意LWP/RWP+数字格式）
                        matchedTmDevice = fullTmMap.get(tmBindCode);
                        break;
                    }
                }
                if (isAtTmWorkPos) {
                    // ========== 新增嵌套判断：优先处理 matchedTmDevice == null 且 ef_status=1 的场景 ==========
                    if (matchedTmDevice == null) {
                        // 满足条件：matchedTmDevice为null 且 zt的ef_status=1
                        if ("1".equals(cal(zt.getEf_status()))) {
                            log.info("ZT:{} nav_mode=40，在TM工作位{}但未匹配到TM设备，且ef_status=1，直接执行后续路径规划逻辑",
                                    zt.getDeviceCode(), matchedTmBindCode);
                            // 不执行continue，直接跳出当前if (isAtTmWorkPos) 代码块，向下执行fromNode逻辑
                        } else {
                            // ========== 极简核心逻辑：仅判断LAST_ZT_BIND_CODE是否为LCPC开头 ==========
                            try {
                                String ztLastBindCode = LAST_ZT_BIND_CODE.get(ztDeviceCode);

                                if (ztLastBindCode == null || ztLastBindCode.isEmpty()) {
                                    log.debug("ZT:{} 无历史绑定码，跳过TM工作位绑定码更新逻辑", ztDeviceCode);
                                    continue;
                                }

                                // 1. 提取当前TM工作位的前缀（LWP/RWP）
                                String tmPrefix = null;
                                if (matchedTmBindCode.startsWith("LWP")) {
                                    tmPrefix = "LWP";
                                } else if (matchedTmBindCode.startsWith("RWP")) {
                                    tmPrefix = "RWP";
                                }
                                if (tmPrefix == null) {
                                    log.warn("ZT:{} 匹配到的TM工作位绑定码{}非LWP/RWP格式，跳过", ztDeviceCode, matchedTmBindCode);
                                    continue;
                                }

                                // 2. 动态匹配对应的补充位前缀（LWP→LCPC，RWP→RCPC）
                                String targetSupplementPrefix = tmPrefix.equals("LWP") ? "LCPC" : "RCPC";

                                // 核心条件：LAST_ZT_BIND_CODE不为空 且 以LCPC开头
                                if (ztLastBindCode.startsWith(targetSupplementPrefix)) {
                                    // 1. 直接更新LAST_ZT_BIND_CODE为当前TM工作位的LWP码（matchedTmBindCode）
                                    LAST_ZT_BIND_CODE.put(ztDeviceCode, matchedTmBindCode);

                                    // 2. 核心：通过matchedTmBindCode找关联TM设备，置位start=1
                                    if (bindNodeMap.containsKey(matchedTmBindCode)) {
                                        NavNodeBO lwpNode = bindNodeMap.get(matchedTmBindCode);
                                        // 从LWP节点中提取关联的TM设备ID
                                        if (lwpNode != null && lwpNode.getCoordinateBind() != null
                                                && lwpNode.getCoordinateBind().getAttribute() != null) {
                                            String tmDeviceId = lwpNode.getCoordinateBind().getAttribute().getDeviceId().toString();

                                            // 遍历TM列表找到对应设备，写start=1
                                            for (TMDeviceSnapshotBO tm : tmList) {
                                                if (tm.getId().equals(tmDeviceId)) {
                                                    // 动态选择置位的点位：LWP→lstart，RWP→rstart
                                                    PointNodeBO targetStartNode = tmPrefix.equals("LWP") ? tm.getLstart() : tm.getRstart();
                                                    // 写TM设备的start点位为1
                                                    write(tm.getId(), targetStartNode, "1");
                                                    log.info("ZT:{} LAST_ZT_BIND_CODE从{}更新为{}，已将对应TM({})的start置为1",
                                                            ztDeviceCode, ztLastBindCode, matchedTmBindCode, tm.getDeviceCode());
                                                    break; // 找到对应TM后退出循环，避免无效遍历
                                                }
                                            }
                                        }
                                    }

                                    log.info("ZT:{} LAST_ZT_BIND_CODE从{}（LCPC补充位）更新为{}（LWP工作位）",
                                            ztDeviceCode, ztLastBindCode, matchedTmBindCode);
                                }
                            } catch (Exception e) {
                                log.error("ZT:{} 更新绑定码/置位llifting异常", zt.getDeviceCode(), e);
                            }

                            // 原有逻辑：matchedTmDevice为null 且 ef_status≠1，执行continue跳过
                            log.warn("ZT:{} nav_mode=40，在TM工作位{}但未匹配到TM设备，跳过", zt.getDeviceCode(), matchedTmBindCode);
                            continue;
                        }
                    } else {
                        // 2. TM工作位逻辑：检查ZT自身的ef_status（原有完整逻辑，不变）
                        // 2.1 解析TM设备的满筒位值（动态适配LWP/RWP+任意数字）
                        String tmFullValue = "";
                        try {
                            // 提取绑定码前缀（LWP/RWP）和数字后缀（如 LWP10 → 前缀LWP，后缀10）
                            String prefix = matchedTmBindCode.startsWith("LWP") ? "LWP" : "RWP";
                            String numberSuffix = matchedTmBindCode.replace(prefix, "");
                            // 转换为对应的满筒字段名（LWP10 → lwpfull10，RWP3 → rwpfull3）
                            String fieldName = (prefix.equals("LWP") ? "lwpfull" : "rwpfull");

                            // 通过反射获取TM设备对应的满筒字段值
                            Field field = TMDeviceSnapshotBO.class.getDeclaredField(fieldName);
                            field.setAccessible(true); // 兼容私有字段
                            PointNodeBO pointNode = (PointNodeBO) field.get(matchedTmDevice);
                            tmFullValue = cal(pointNode);

                            log.debug("ZT:{} 解析TM工作位{}的满筒字段{}，值为{}",
                                    zt.getDeviceCode(), matchedTmBindCode, fieldName, tmFullValue);
                        } catch (NoSuchFieldException e) {
                            log.warn("ZT:{} TM设备无{}对应的满筒字段，跳过", zt.getDeviceCode(), matchedTmBindCode);
                            continue;
                        } catch (IllegalAccessException e) {
                            log.error("ZT:{} 反射获取TM满筒字段值异常", zt.getDeviceCode(), e);
                            continue;
                        } catch (Exception e) {
                            log.error("ZT:{} 解析TM满筒位值异常", zt.getDeviceCode(), e);
                            continue;
                        }

                        // 2.2 TM满筒位=1 → 写ZT自身的ef_status=1（允许移动）
                        if ("1".equals(tmFullValue)) {
                            log.info("ZT:{} nav_mode=40，在TM工作位{}且TM满筒位=1，写入自身ef_status=1（允许移动）",
                                    zt.getDeviceCode(), matchedTmBindCode);
                            // 写入ZT自身的ef_status点位为1（延迟执行：缓存任务）
                            if ("0".equals(cal(zt.getEf_status()))) {
                                // 构建唯一任务Key：ZT设备编码 + ef_status点位编码（避免重复，无需手动构造PointNodeBO）
                                String efStatusPointId = zt.getEf_status().getPointId(); // 拿到现有有效的点位编码
                                String taskKey = zt.getDeviceCode() + "_" + efStatusPointId;
                                // 缓存任务（值固定为"1"）
                                DELAYED_ZT_EF_STATUS_WRITE.put(taskKey, "1");
                                continue;
                            }

                            // ========== 核心优化：动态选择TM的cleaning点位（LWP→lcleaning，RWP→rcleaning） ==========
                            PointNodeBO targetCleaningNode = null;
                            String cleaningFieldName = "";
                            if (matchedTmBindCode.startsWith("LWP")) {
                                targetCleaningNode = matchedTmDevice.getLcleaning();
                                cleaningFieldName = "lcleaning";
                            } else if (matchedTmBindCode.startsWith("RWP")) {
                                targetCleaningNode = matchedTmDevice.getRcleaning();
                                cleaningFieldName = "rcleaning";
                            }

                            // 空值防护：避免点位为空导致写入异常
                            if (targetCleaningNode == null) {
                                log.warn("ZT:{} TM设备{}的{}点位为空，跳过写入",
                                        zt.getDeviceCode(), matchedTmDevice.getDeviceCode(), cleaningFieldName);
                            } else {
                                write(matchedTmDevice.getId(), targetCleaningNode, "1");
                                log.debug("ZT:{} 成功写入TM设备{}的{}点位为1",
                                        zt.getDeviceCode(), matchedTmDevice.getDeviceCode(), cleaningFieldName);
                            }
//                            write(matchedTmDevice.getId(), matchedTmDevice.getRcleaning(), "1");
                        } else {
                            // 2.3 TM满筒位≠1 → 保持ZT的ef_status不变（禁止移动）
                            String ztEfStatus = cal(zt.getEf_status()); // 仅日志打印用，不影响逻辑
                            log.info("ZT:{} nav_mode=40，在TM工作位{}但TM满筒位={}≠1，禁止移动（当前ef_status={}）",
                                    zt.getDeviceCode(), matchedTmBindCode, tmFullValue, ztEfStatus);
                            continue;
                        }
                    }
                }


                String ztCurrentBindCode = getZtCurrentBindCode(zt, gridNodeMap, bindNodeMap);
                log.info("ZT:{} 当前位置对应的绑定码：{}", zt.getDeviceCode(), ztCurrentBindCode);

                // ========== 第二步：判断是否在SJ工作位（SWS1/SWS2/SRS1/SRS2） ==========
                boolean isAtSjWorkPos = false;
//                SJDeviceSnapshotBO matchedSjDevice = null;
                String matchedSjBindCode = null;
                // 正则匹配所有以SWS/SRS开头的绑定码（兼容任意数字后缀：SWS1、SWS10、SWS16等）
//                String sjPattern = "^SWS\\d+$";

                // 2. 扩展匹配：SWS* 或 ZCS*都视为工作位，最终匹配到对应的SWS*
                if (ztCurrentBindCode != null && ztCurrentBindCode.matches(SJ_PATTERN)) {
                    // 提取数字序号（SWS123 → 123；ZCS123 → 123）
                    int seq = extractNumberFromCode(ztCurrentBindCode);
                    // 无论当前是SWS还是ZCS，最终匹配到对应的SWS*作为SJ工作位码
                    matchedSjBindCode = "SWS" + seq;
                    isAtSjWorkPos = true;

//                    // 补充：如果当前是ZCS*，记录SWS-ZCS关联（用于充电逻辑）
//                    if (ztCurrentBindCode.startsWith("ZCS")) {
//                        String swsZcsKey = matchedSjBindCode + "_" + ztCurrentBindCode;
//                        ZT_SWS_ZCS_MAPPING.put(zt.getDeviceCode(), swsZcsKey);
//                    }
                }


//                for (String bindCode : bindNodeMap.keySet()) {
//                    // 过滤出符合SWS+数字格式的绑定码
//                    if (!bindCode.matches(sjPattern)) {
//                        continue;
//                    }
//
//                    NavNodeBO sjBindNode = bindNodeMap.get(bindCode);
//                    if (sjBindNode == null) {
//                        log.debug("ZT:{} 绑定码{}未匹配到NavNode，跳过", zt.getDeviceCode(), bindCode);
//                        continue;
//                    }
//
//                    String sjGridKey = gridKey(sjBindNode.getGridX().toString(), sjBindNode.getGridY().toString());
//
//                    // 匹配当前ZT坐标与SJ工作位坐标
//                    if (currentGridKey.equals(sjGridKey)) {
//                        isAtSjWorkPos = true;
//                        matchedSjBindCode = bindCode;
//                        // 获取匹配的SJ设备快照（sjBindCodeMap已适配任意SWS+数字格式）
//                        matchedSjDevice = sjBindCodeMap.get(bindCode);
//                        log.info("ZT:{} 匹配到SJ工作位{}，对应的SJ设备：{}",
//                                zt.getDeviceCode(), bindCode, matchedSjDevice != null ? matchedSjDevice.getDeviceCode() : "空");
//                        break;
//                    }
//                }

                if (isAtSjWorkPos) {
                    // 3. SJ工作位逻辑：反射动态取值，兼容任意后缀，无硬编码
                    SJDeviceSnapshotBO matchedSjDevice = sjBindCodeMap.get(matchedSjBindCode);
                    if (matchedSjDevice == null) {
                        log.warn("ZT:{} nav_mode=40，在SJ工作位{}但未匹配到SJ设备，跳过", zt.getDeviceCode(), matchedSjBindCode);
                        continue;
                    }

//                    String sjWorkValue = null;
                    PointNodeBO sjPointNode = null; // 直接声明为PointNodeBO类型
                    try {
                        // 3.1 提取点位字段名（如SWS1→sws1，SRS10→srs10）
                        String fieldName = matchedSjBindCode;
                        // 3.2 反射获取字段值（兼容任意后缀的点位）
                        Field field = matchedSjDevice.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true); // 允许访问私有字段
//                        Object fieldValue = field.get(matchedSjDevice);
                        // 3.3 解析点位值（复用cal方法）
//                        sjWorkValue = cal(fieldValue != null ? fieldValue.toString() : "");
                        sjPointNode = (PointNodeBO) field.get(matchedSjDevice); // 强转为PointNodeBO
                    } catch (NoSuchFieldException e) {
                        log.warn("ZT:{} nav_mode=40，SJ设备无{}点位字段，跳过", zt.getDeviceCode(), matchedSjBindCode);
                        continue;
                    } catch (IllegalAccessException e) {
                        log.error("ZT:{} nav_mode=40，反射获取SJ点位{}失败", zt.getDeviceCode(), matchedSjBindCode, e);
                        continue;
                    }

                    // 3.4 校验点位值，为1则写ZT的ef_status=0
                    String sjPointValue = cal(sjPointNode);
                    if ("1".equals(sjPointValue)) {
                        log.info("ZT:{} nav_mode=40，在SJ工作位{}且工作位值=1，写入自身ef_status=0",
                                zt.getDeviceCode(), matchedSjBindCode);
                        if ("1".equals(cal(zt.getEf_status()))) {
                            write(zt.getId(), zt.getEf_status(), "0");
                            continue;
                        }

                    } else if ("2".equals(sjPointValue)) {
                        // 值为2：执行电池电压检测，判断是否触发充电
                        // ========== 电池电压检测逻辑（仅在值为2时执行） ==========
                        int batteryVoltage = 0;
                        try {
                            batteryVoltage = Integer.parseInt(cal(zt.getBattery_voltage()));
                        } catch (Exception e) {
                            log.warn("ZT:{} 解析电池电压异常，默认视为正常电压", zt.getDeviceCode(), e);
                            batteryVoltage = ZCS_BATTERY_THRESHOLD + 100;
                        }

                        // 电压处理：仅判断是否触发充电，移除缓存关联（取消ZT_SWS_ZCS_MAPPING）
                        if (batteryVoltage < ZCS_BATTERY_THRESHOLD && ztCurrentBindCode.startsWith("SWS")) {
                            log.info("ZT:{} 电池电压{}低于阈值{}，触发充电（当前工作位：{}）",
                                    zt.getDeviceCode(), batteryVoltage, ZCS_BATTERY_THRESHOLD, matchedSjBindCode);

                            int swsSeq = extractNumberFromCode(matchedSjBindCode);
                            String targetZcsCode = "ZCS" + swsSeq;
                            NavNodeBO zcsNode = bindNodeMap.get(targetZcsCode);

                            if (zcsNode == null) {
                                log.warn("ZT:{} 未找到SWS{}对应的ZCS{}充电位，跳过充电逻辑",
                                        zt.getDeviceCode(), swsSeq, swsSeq);
                            } else {
//                                String swsZcsKey = matchedSjBindCode + "_" + targetZcsCode;
//                                ZT_SWS_ZCS_MAPPING.put(zt.getDeviceCode(), swsZcsKey);
                                // 移除缓存关联代码（ZT_SWS_ZCS_MAPPING.put），仅保留充电触发日志
                                log.info("ZT:{} 已匹配SWS{}和ZCS{}，触发充电", zt.getDeviceCode(), swsSeq, swsSeq);
                                // 写入充电状态，触发ZT移动到ZCS*（保留原有充电触发逻辑）
//                writeChargingState(zt, targetZcsCode, matchedSjBindCode);
                                continue;
                            }
                        }
                        continue; // 执行完后跳过后续逻辑
                    } else {
                        log.info("ZT:{} nav_mode=40，在SJ工作位{}但工作位值={}≠1，跳过",
                                zt.getDeviceCode(), matchedSjBindCode, sjPointNode);
                        continue;
                    }

                }

//                /* 4. 解析起点 grid */
//                String tag = cal(zt.getCurrent_tag());
//                if (tag.length() < 4) {
//                    log.warn("ZT:{} current_tag={} 长度不足4位，跳过", zt.getDeviceCode(), tag);
//                    continue;
//                }
//                tag = "0".repeat(6 - tag.length()) + tag;   // 右补零，不足6位自动补0
//                String gridX = String.valueOf(Integer.parseInt(tag.substring(0, 3)));
//                String gridY = String.valueOf(Integer.parseInt(tag.substring(3, 6)));
//                String currentZTGrid = gridX + "," + gridY; // 自身坐标
                NavNodeBO fromNode = gridNodeMap.get(gridKey(gridX, gridY));
                if (fromNode == null) {
                    log.warn("ZT:{} 起点(gridX={},gridY={}) 未匹配 NavNode，跳过",
                            zt.getDeviceCode(), gridX, gridY);
                    continue;
                }



                /* 5. 终点 nodeId：责任区循环 or CZS */
                String tgtBindCode = null;
                String ztEfStatus = cal(zt.getEf_status());
                // 记录本次任务锁定的绑定码（用于低电量/异常时精准释放）
                String currentLockedCode = null;


                if (lowBattery(zt)) {
                    tgtBindCode = "CZS";
                    // 仅释放本次任务中ZT锁定的码（而非所有码）
                    releaseZtAllLocks(ztDeviceCode);
//                    // 清理SWS-ZCS关联
//                    clearZcsSwsMapping(ztDeviceCode);
                    // 同步清理路径缓存与时间戳
                    ZT_DEVICE_PATH_NODE_CACHE.remove(ztDeviceCode);
                    ZT_DEVICE_PATH_TIMESTAMPS.remove(ztDeviceCode);
//                    if (currentLockedCode != null) {
//                        releaseCode(currentLockedCode, ztDeviceCode);
//                        log.info("ZT:{} 低电量，释放本次锁定码{}，前往充电位CZS", ztDeviceCode, currentLockedCode);
//                    } else {
//                        log.info("ZT:{} 低电量，无本次锁定码，直接前往充电位CZS", ztDeviceCode);
//                    }
                } else {
                    // ===== 注释/删除原有随机绑定码循环逻辑 =====
//                    List<String> duty = dutyOf(zt.getDeviceCode());
//                    if (duty == null || duty.isEmpty()) {
//                        log.warn("ZT:{} 未配置责任区，跳过", zt.getDeviceCode());
//                        continue;
//                    }
//                    // 拿到本轮要发的绑定码
//                    AtomicInteger idxHolder = ZT_DUTY_INDEX.computeIfAbsent(zt.getDeviceCode(), k -> new AtomicInteger(0));
//                    int idx = idxHolder.getAndUpdate(i -> (i + 1) % duty.size()); // 先取再+1
//                    tgtBindCode = duty.get(idx);

                    String ztLockedCode = null;
                    // 遍历锁定集合，找到当前ZT已锁定的绑定码（绑定码→ZT强关联）
                    for (Map.Entry<String, String> entry : LOCKED_BIND_CODES.entrySet()) {
                        if (ztDeviceCode.equals(entry.getValue())) {
                            ztLockedCode = entry.getKey();
                            break;
                        }
                    }
                    // 情况1：ZT已锁定绑定码 → 复用该码（未超时/未到达则不重新选）
                    if (ztLockedCode != null) {
                        // 检查是否到达目标位置 → 到达则解锁
                        boolean isArrived = isZtArrivedAtTarget(zt, ztLockedCode, bindNodeMap);
                        if (isArrived) {
                            releaseCode(ztLockedCode, ztDeviceCode);
                            log.info("ZT:{} 已到达目标绑定码{}位置，释放锁定", ztDeviceCode, ztLockedCode);
//                            ztLockedCode = null; // 清空，重新选码
                        } else {
                            // 未到达 → 复用锁定码，不重新选
                            tgtBindCode = ztLockedCode;
                            log.info("ZT:{} 已锁定绑定码{}（未到达/未超时），复用该码", ztDeviceCode, ztLockedCode);
                            // 重置锁定时间（避免中途超时）
//                            LOCK_TIMESTAMPS.put(ztLockedCode, System.currentTimeMillis() / 1000);
                        }
                    } else {


                        List<String> availableCodes = new ArrayList<>();
                        // 新增：记录需要释放的补充位（过渡时使用）
                        String needReleaseCode = null;

                        if ("1".equals(ztEfStatus)) {
                            // 步骤1：提取系统中所有SRS/SWS码（按序号1:1绑定）
                            List<String> allSrsCodes = bindNodeMap.keySet().stream()
                                    .filter(code -> code.matches("^SRS\\d+$"))
                                    .sorted(Comparator.comparingInt(this::extractNumberFromCode))
                                    .toList();

                            // 步骤2：判断ZT是否已在SRS补充位
                            String currentTransitionCode = null;
                            if (ztCurrentBindCode != null && ztCurrentBindCode.matches("^SRS\\d+$")) {
                                currentTransitionCode = ztCurrentBindCode;
                            }

                            if (currentTransitionCode != null) {
                                // 状态A：已在SRS补充位 → 尝试进入对应序号SWS工作位
                                int targetSeq = extractNumberFromCode(currentTransitionCode);
                                String targetSwsCode = "SWS" + targetSeq;
                                // 【核心修改】新增 isSwsOccupiedByZcs 校验
                                if (bindNodeMap.containsKey(targetSwsCode)
                                        && !isLocked(targetSwsCode)
                                        && !targetSwsCode.equals(ztCurrentBindCode)
                                        && !isOccupiedByOtherZt(targetSwsCode, ztDeviceCode, ztOccupiedBindCodes)
                                        && !isSwsOccupiedByZcsRealTime(targetSwsCode, ztList, gridNodeMap, bindNodeMap)) {
//                                        && !isSwsOccupiedByZcs(targetSwsCode)) {
                                    availableCodes.add(targetSwsCode);
                                    // 关键：标记需要释放的SRS补充位
                                    needReleaseCode = currentTransitionCode;
                                } else {
                                    // SWS不可用 → 继续留在当前SRS补充位
                                    if (!isLocked(currentTransitionCode)
                                            && !currentTransitionCode.equals(ztCurrentBindCode)
                                            && !isOccupiedByOtherZt(currentTransitionCode, ztDeviceCode, ztOccupiedBindCodes)) {
                                        availableCodes.add(currentTransitionCode);
                                    } else {
                                        // 当前SRS被抢占 → 重新选其他SRS补充位
                                        availableCodes = allSrsCodes.stream()
                                                .filter(code -> !isLocked(code))
                                                .filter(code -> !code.equals(ztCurrentBindCode))
                                                .filter(code -> !isOccupiedByOtherZt(code, ztDeviceCode, ztOccupiedBindCodes))
                                                .toList();
                                    }
                                }
                            } else {
                                // 状态B：未在SRS补充位 → 优先选SRS补充位
                                availableCodes = allSrsCodes.stream()
                                        .filter(code -> !isLocked(code))
                                        .filter(code -> !code.equals(ztCurrentBindCode))
                                        .filter(code -> !isOccupiedByOtherZt(code, ztDeviceCode, ztOccupiedBindCodes))
                                        .toList();
                            }
                        } else if ("0".equals(ztEfStatus)) {
                            // 步骤1：提取系统中所有CPC/WP码
                            List<String> allCpcCodes = bindNodeMap.keySet().stream()
                                    .filter(code -> code.matches("^(LCPC|RCPC)\\d+$"))
                                    .sorted((c1, c2) -> {
                                        boolean isC1LCPC = c1.startsWith("LCPC");
                                        boolean isC2LCPC = c2.startsWith("LCPC");
                                        if (isC1LCPC && !isC2LCPC) return -1;
                                        if (!isC1LCPC && isC2LCPC) return 1;
                                        return Integer.compare(extractNumberFromCode(c1), extractNumberFromCode(c2));
                                    })
                                    .toList();

                            // 步骤2：判断ZT是否已在CPC补充位
                            String currentTransitionCode = null;
                            if (ztCurrentBindCode != null && ztCurrentBindCode.matches("^(LCPC|RCPC)\\d+$")) {
                                currentTransitionCode = ztCurrentBindCode;
                            }

                            if (currentTransitionCode != null) {
                                // 状态A：已在CPC补充位 → 尝试进入对应类型+序号WP工作位
                                int targetSeq = extractNumberFromCode(currentTransitionCode);
                                String targetWpPrefix = currentTransitionCode.startsWith("LCPC") ? "LWP" : "RWP";
                                String targetWpCode = targetWpPrefix + targetSeq;
                                if (bindNodeMap.containsKey(targetWpCode)
                                        && !isLocked(targetWpCode)
                                        && !targetWpCode.equals(ztCurrentBindCode)
                                        && !isOccupiedByOtherZt(targetWpCode, ztDeviceCode, ztOccupiedBindCodes)) {
                                    availableCodes.add(targetWpCode);
                                    // 关键：标记需要释放的CPC补充位
                                    needReleaseCode = currentTransitionCode;
                                } else {
                                    // WP不可用 → 继续留在当前CPC补充位
                                    if (!isLocked(currentTransitionCode)
                                            && !currentTransitionCode.equals(ztCurrentBindCode)
                                            && !isOccupiedByOtherZt(currentTransitionCode, ztDeviceCode, ztOccupiedBindCodes)) {
                                        availableCodes.add(currentTransitionCode);
                                    } else {
                                        // 当前CPC被抢占 → 重新选其他CPC补充位
                                        availableCodes = allCpcCodes.stream()
                                                .filter(code -> !isLocked(code))
                                                .filter(code -> !code.equals(ztCurrentBindCode))
                                                .filter(code -> !isOccupiedByOtherZt(code, ztDeviceCode, ztOccupiedBindCodes))
                                                .toList();
                                    }
                                }
                            } else {
                                // 状态B：未在CPC补充位 → 优先选CPC补充位
                                availableCodes = allCpcCodes.stream()
                                        .filter(code -> !isLocked(code))
                                        .filter(code -> !code.equals(ztCurrentBindCode))
                                        .filter(code -> !isOccupiedByOtherZt(code, ztDeviceCode, ztOccupiedBindCodes))
                                        .toList();
                            }
                        } else {
                            log.warn("ZT:{} ef_status={} 异常，跳过", ztDeviceCode, ztEfStatus);
                            continue;
                        }

                        if (availableCodes.isEmpty()) {
                            log.info("ZT:{} ef_status={}，无可用绑定码，跳过", ztDeviceCode, ztEfStatus);
                            continue;
                        }

                        // 锁定本次目标码，并记录到currentLockedCode
                        tgtBindCode = availableCodes.getFirst();
                        if (tryLockCode(tgtBindCode, ztDeviceCode)) {
                            // 核心新增：过渡到工作位时，先释放补充位的锁定
                            if (needReleaseCode != null) {
                                releaseCode(needReleaseCode, ztDeviceCode);
                                log.info("ZT:{} 从补充位{}过渡到工作位{}，已释放补充位锁定",
                                        ztDeviceCode, needReleaseCode, tgtBindCode);
                            }
                            currentLockedCode = tgtBindCode;
                            log.info("ZT:{} ef_status={}，成功锁定绑定码：{}", ztDeviceCode, ztEfStatus, tgtBindCode);
                        } else {
                            log.warn("ZT:{} 绑定码{}已被抢占，重新筛选", ztDeviceCode, tgtBindCode);
                            // 过滤剩余可用码并重新排序
                            availableCodes = availableCodes.stream()
                                    .filter(code -> tryLockCode(code, ztDeviceCode))
                                    .sorted((code1, code2) -> {
                                        if ("1".equals(ztEfStatus)) {
                                            boolean c1SRS = code1.startsWith("SRS");
                                            boolean c2SRS = code2.startsWith("SRS");
                                            if (c1SRS && !c2SRS) return -1;
                                            if (!c1SRS && c2SRS) return 1;
                                            return Integer.compare(extractNumberFromCode(code1), extractNumberFromCode(code2));
                                        } else {
                                            boolean c1CPC = code1.startsWith("LCPC") || code1.startsWith("RCPC");
                                            boolean c2CPC = code2.startsWith("LCPC") || code2.startsWith("RCPC");
                                            if (c1CPC && !c2CPC) return -1;
                                            if (!c1CPC && c2CPC) return 1;
                                            boolean c1LCPC = code1.startsWith("LCPC");
                                            boolean c2LCPC = code2.startsWith("LCPC");
                                            if (c1LCPC && !c2LCPC) return -1;
                                            if (!c1LCPC && c2LCPC) return 1;
                                            return Integer.compare(extractNumberFromCode(code1), extractNumberFromCode(code2));
                                        }
                                    })
                                    .toList();

                            if (availableCodes.isEmpty()) {
                                log.info("ZT:{} 无可用绑定码（全部被抢占），跳过", ztDeviceCode);
                                continue;
                            }
                            tgtBindCode = availableCodes.getFirst();
                            // 核心新增：重新锁定时，若目标是工作位，也释放补充位
                            if (needReleaseCode != null) {
                                releaseCode(needReleaseCode, ztDeviceCode);
                                log.info("ZT:{} 重新从补充位{}过渡到工作位{}，已释放补充位锁定",
                                        ztDeviceCode, needReleaseCode, tgtBindCode);
                            }
                            currentLockedCode = tgtBindCode;
                            log.info("ZT:{} ef_status={}，重新锁定绑定码：{}", ztDeviceCode, ztEfStatus, tgtBindCode);
                        }
                    }
                }
                NavNodeBO toNode = bindNodeMap.get(tgtBindCode);
                if (toNode == null || fromNode.getId().equals(toNode.getId())) {
                    log.warn("ZT:{} 绑定码{}无效或与当前节点相同，跳过", ztDeviceCode, tgtBindCode);
                    // 仅释放本次锁定码
                    if (currentLockedCode != null) {
                        releaseCode(currentLockedCode, ztDeviceCode);
                    }
                    continue;
                }

                Set<String> validGridSet = navNodes.stream()
                        .filter(n -> n.getGridX() != null && n.getGridY() != null)
                        .map(n -> n.getGridX() + "," + n.getGridY())
                        .collect(Collectors.toSet());

                Long currentZtNodeId = getCurrentZtNodeId(zt, navNodes);
                // 重新初始化 allZtNodeIds（避免与上一个ZT的集合混淆，关键！）
                Set<Long> allZtNodeIds = getAllZTCurrentNodeIds(ztList, navNodes);
                if (currentZtNodeId != null) {
                    allZtNodeIds.remove(currentZtNodeId);
                }

                long now = System.currentTimeMillis() / 1000;
                for (Map.Entry<String, Set<Long>> entry : ZT_DEVICE_PATH_NODE_CACHE.entrySet()) {
                    String otherZtCode = entry.getKey();
                    Set<Long> otherZtPathNodeIds = entry.getValue();

                    // 排除当前ZT
                    if (zt.getDeviceCode().equals(otherZtCode)) {
                        continue;
                    }

                    // 校验该ZT是否已处于30状态，若是则跳过缓存
                    boolean isOtherZtInNavMode30 = false;
                    for (DeviceSnapshotBO otherZt : ztList) {
                        if (otherZtCode.equals(otherZt.getDeviceCode())) {
                            String otherNavMode = cal(otherZt.getNav_mode());
                            if ("30".equals(otherNavMode)) {
                                isOtherZtInNavMode30 = true;
                            }
                            break;
                        }
                    }
                    if (isOtherZtInNavMode30) {
                        continue;
                    }

                    // 缓存时效性校验
                    Long otherZtPathTime = ZT_DEVICE_PATH_TIMESTAMPS.get(otherZtCode);
                    boolean isCacheValid = otherZtPathNodeIds != null
                            && !otherZtPathNodeIds.isEmpty()
                            && otherZtPathTime != null
                            && (now - otherZtPathTime <= PATH_CACHE_TIMEOUT_SECONDS);

                    // 添加有效缓存节点
                    if (isCacheValid) {
                        allZtNodeIds.addAll(otherZtPathNodeIds);
                        log.debug("ZT:{} 加载ZT({})待执行路径节点{}个", zt.getDeviceCode(), otherZtCode, otherZtPathNodeIds.size());
                    } else {
                        ZT_DEVICE_PATH_NODE_CACHE.remove(otherZtCode);
                        ZT_DEVICE_PATH_TIMESTAMPS.remove(otherZtCode);
                    }
                }


                // 2.3 【新增】为其他 ZT 添加剩余路径上的节点（仅 nav_mode=30 有效）
                for (DeviceSnapshotBO otherZt : ztList) {
                    String otherZtCode = otherZt.getDeviceCode();
                    if (otherZtCode.equals(zt.getDeviceCode())) {
                        continue; // 跳过当前 ZT 自己
                    }

                    // 只有 nav_mode=30 的 ZT，其 nav_route 才代表有效路径
                    String otherNavMode = cal(otherZt.getNav_mode());
                    if ("30".equals(otherNavMode)) {
                        String otherRoute = cal(otherZt.getNav_route());
                        // 验证路径有效性（非空且非占位值）
                        if (otherRoute != null && !otherRoute.isEmpty()) {
                            // 解析剩余路径节点 ID 并加入障碍物
                            Set<Long> otherRemainingNodeIds = parseRemainingPathNodeIds(otherZt, otherRoute, navNodes);
                            allZtNodeIds.addAll(otherRemainingNodeIds);
                            log.debug("ZT:{} 将其他 ZT({}) 的剩余路径 {} 个节点加入障碍物",
                                    zt.getDeviceCode(), otherZtCode, otherRemainingNodeIds.size());
                        }
                    }
                }

                // 2. 新增核心逻辑：遍历所有锁定码，排除「其他ZT锁定」的绑定码对应的nodeId
                // 遍历锁定集合的副本（避免并发修改异常）
                new ArrayList<>(LOCKED_BIND_CODES.entrySet()).forEach(entry -> {
                    String lockedCode = entry.getKey();       // 被锁定的绑定码
                    String lockedZtCode = entry.getValue();   // 锁定该码的ZT设备码

                    // 核心条件：锁定者不是当前ZT（排除其他ZT的锁定码）
                    if (!zt.getDeviceCode().equals(lockedZtCode)) {
                        // 从bindNodeMap获取该锁定码对应的NavNodeBO
                        NavNodeBO lockedNode = bindNodeMap.get(lockedCode);
                        if (lockedNode != null && lockedNode.getId() != null) {
                            Long lockedNodeId = lockedNode.getId();
                            // 将其他ZT锁定码的nodeId加入排除集合
                            allZtNodeIds.add(lockedNodeId);
                            log.debug("ZT:{} 检测到其他ZT({})锁定绑定码{}，其nodeId{}已加入路径规划排除集合",
                                    zt.getDeviceCode(), lockedZtCode, lockedCode, lockedNodeId);
                        }
                    }
                });

                // ========== 合并本次循环已规划路径的节点ID到障碍物集合 ==========
//                allZtNodeIds.addAll(currentCyclePlannedNodeIds);

                List<NavPathPoint> navPathPoints = navigationPathService
                        .planPath(Long.parseLong(sceneId), fromNode.getId(), toNode.getId(), allZtNodeIds)
                        .stream()
                        .filter(p -> validGridSet.contains(p.getGridX() + "," + p.getGridY()))
                        .toList();

                if (navPathPoints != null && navPathPoints.size() > 1) {
                    String routeStr = toRouteStr(navPathPoints);
                    write(zt.getId(), zt.getNav_route(), routeStr);
                    log.debug("ZT:{} 成功生成并写入导航路径，路径点数量：{}", zt.getDeviceCode(), navPathPoints.size());
                    // ========== 提取当前ZT路径的所有节点ID，加入临时缓存集合 ==========
                    Set<Long> currentZtPathNodeIds = new HashSet<>();
                    for (NavPathPoint pathPoint : navPathPoints) {
                        // 遍历导航节点，匹配路径点的坐标，获取对应的nodeId
                        for (NavNodeBO node : navNodes) {
                            if (node.getGridX() != null && node.getGridY() != null) {
                                String nodeGridX = String.format("%03d", node.getGridX());
                                String nodeGridY = String.format("%03d", node.getGridY());
                                String pathGridX = String.format("%03d", pathPoint.getGridX());
                                String pathGridY = String.format("%03d", pathPoint.getGridY());
                                if (nodeGridX.equals(pathGridX) && nodeGridY.equals(pathGridY) && node.getId() != null) {
                                    currentZtPathNodeIds.add(node.getId());
                                    break; // 匹配到即跳出，提升效率
                                }
                            }
                        }
                    }
                    // 将当前ZT路径节点ID加入本次循环的缓存集合
//                    currentCyclePlannedNodeIds.addAll(currentZtPathNodeIds);
                    ZT_DEVICE_PATH_NODE_CACHE.put(zt.getDeviceCode(), currentZtPathNodeIds);
                    long currentTime = System.currentTimeMillis() / 1000; // 转为秒级时间戳，与LOCK_TIMESTAMPS一致
                    ZT_DEVICE_PATH_TIMESTAMPS.put(zt.getDeviceCode(), currentTime);

                    log.debug("ZT:{} 路径节点ID缓存更新完成，共{}个节点，缓存时间戳：{}",
                            zt.getDeviceCode(), currentZtPathNodeIds.size(), currentTime);

                } else {
                    ZT_DEVICE_PATH_NODE_CACHE.remove(zt.getDeviceCode());
                    ZT_DEVICE_PATH_TIMESTAMPS.remove(zt.getDeviceCode());
                    log.debug("ZT:{} 导航路径无效，已清理路径节点ID缓存及时间戳", zt.getDeviceCode());
                    // 释放本次锁定的绑定码
                    if (currentLockedCode != null) {
                        releaseCode(currentLockedCode, ztDeviceCode);
                    }
                }


            } catch (Exception e) {
                log.error("ZT:{} 调度异常", zt.getDeviceCode(), e);
                releaseZtAllLocks(zt.getDeviceCode());
            }
        }
    }

    /**
     * 尝试锁定绑定码（原子操作，防止并发）
     * @param code 绑定码
     * @param ztCode ZT设备码
     * @return 是否锁定成功
     */
    private static boolean tryLockCode(String code, String ztCode) {
        boolean locked = LOCKED_BIND_CODES.putIfAbsent(code, ztCode) == null;
        if (locked) {
            // 记录锁定时间（秒级）
            LOCK_TIMESTAMPS.put(code, System.currentTimeMillis() / 1000);
            log.debug("绑定码{}被ZT{}锁定，锁定时间戳：{}", code, ztCode, LOCK_TIMESTAMPS.get(code));
        }
        return locked;
    }
    /**
     * 检查绑定码是否被锁定
     */
    private static boolean isLocked(String code) {
        return LOCKED_BIND_CODES.containsKey(code);
    }

    /**
     * 释放指定绑定码（仅锁定者可释放）
     */
    private static void releaseCode(String code, String ztCode) {
        // 原子移除：仅当值匹配时删除
        boolean removed = LOCKED_BIND_CODES.remove(code, ztCode);
        if (removed) {
            LOCK_TIMESTAMPS.remove(code); // 修复点3：释放锁时同步删除时间戳
            log.debug("ZT{}成功释放绑定码{}", ztCode, code);
        }
    }

    /**
     * 释放ZT持有的所有锁定码
     */
    private static void releaseZtAllLocks(String ztCode) {
        // 遍历副本，避免并发修改异常
        new ArrayList<>(LOCKED_BIND_CODES.entrySet()).forEach(entry -> {
            String code = entry.getKey();
            String boundZtCode = entry.getValue();
            if (ztCode.equals(boundZtCode)) {
                LOCKED_BIND_CODES.remove(code);
                LOCK_TIMESTAMPS.remove(code); // 同步删除时间戳
                log.info("ZT:{} 释放绑定码{}", ztCode, code);
            }
        });
    }

    /**
     * 清理超时锁定码（防止死锁）
     */
    private static void cleanTimeoutLocks() {
        long now = System.currentTimeMillis() / 1000;
        // 遍历副本，避免并发修改异常
        new ArrayList<>(LOCK_TIMESTAMPS.entrySet()).forEach(entry -> {
            String code = entry.getKey();
            Long lockTime = entry.getValue();
            if (lockTime == null) {
                LOCK_TIMESTAMPS.remove(code);
                LOCKED_BIND_CODES.remove(code);
                log.warn("绑定码{}无锁定时间戳，强制释放", code);
                return;
            }
            // 判断是否超时
            if (now - lockTime > LOCK_TIMEOUT_SECONDS) {
                String ztCode = LOCKED_BIND_CODES.remove(code);
                LOCK_TIMESTAMPS.remove(code);
                log.warn("绑定码{}锁定超时（{}秒），自动释放（原锁定ZT：{}）",
                        code, LOCK_TIMEOUT_SECONDS, ztCode);
            }
        });
    }

    /**
     * 清理过期的ZT路径缓存（参考cleanTimeoutLocks实现）
     * 移除超过PATH_CACHE_TIMEOUT_SECONDS未更新的路径缓存及时间戳
     */
    private static void cleanTimeoutPathCache() {
        long now = System.currentTimeMillis() / 1000; // 当前秒级时间戳
        // 遍历时间戳缓存副本，避免并发修改异常
        new ArrayList<>(ZT_DEVICE_PATH_TIMESTAMPS.entrySet()).forEach(entry -> {
            String ztCode = entry.getKey();
            Long lastUpdateTime = entry.getValue();

            // 空值防护
            if (lastUpdateTime == null) {
                ZT_DEVICE_PATH_TIMESTAMPS.remove(ztCode);
                ZT_DEVICE_PATH_NODE_CACHE.remove(ztCode);
                log.warn("ZT:{} 路径缓存无有效时间戳，强制清理缓存", ztCode);
                return;
            }

            // 判断是否超时
            if (now - lastUpdateTime > PATH_CACHE_TIMEOUT_SECONDS) {
                // 同步清理路径缓存与时间戳
                ZT_DEVICE_PATH_NODE_CACHE.remove(ztCode);
                ZT_DEVICE_PATH_TIMESTAMPS.remove(ztCode);
                log.warn("ZT:{} 路径缓存超时（{}秒未更新），自动清理",
                        ztCode, PATH_CACHE_TIMEOUT_SECONDS);
            }
        });
    }


    // ========== 序号提取工具方法 ==========
    private int extractNumberFromCode(String code) {
        if (code == null || code.isEmpty()) return 0;
        String numberStr = code.replaceAll("[^0-9]", "");
        return numberStr.isEmpty() ? 0 : Integer.parseInt(numberStr);
    }

    /* ---------------- 4. 工具方法 ---------------- */

    public static String toRouteStr(List<NavPathPoint> points) {
        if (points == null || points.isEmpty()) {
            return "";
        }
        return points.stream()
                .map(p -> String.format("00%03d%03d,%d",
                        p.getGridX(),
                        p.getGridY(),
                        p.getAngle()))
                .collect(Collectors.joining(";"));
    }

    private List<String> dutyOf(String code) {
        if ("ZT1".equalsIgnoreCase(code)) return List.of("LWP", "LSP", "LCPC");
        if ("ZT2".equalsIgnoreCase(code)) return List.of("RWP", "RSP", "RCPC");
        return null;
    }

    private static String gridKey(String gx, String gy) {
        return gx + "," + gy;
    }

    /** 取当前值，并缓存 */
    private String getAndCache(String key, PointNodeBO node) {
        String old = LAST_VALUE_CACHE.get(key);
        LAST_VALUE_CACHE.put(key, cal(node));
        return old;
    }

    private boolean lowBattery(DeviceSnapshotBO zt) {
        try {
            return Integer.parseInt(cal(zt.getBattery_voltage())) < 2300;
        } catch (Exception ignore) {
            return false;
        }
    }


    /** 获取 calValue 或空串 */
    private static String cal(PointNodeBO node) {
        return node == null || node.getCalValue() == null ? "" : node.getCalValue();
    }

    /**重载cal方法（处理任意Object，优先解析getCalValue）*/
    private static String cal(Object value) {
        if (value == null) {
            return "";
        }
        // 如果是PointNodeBO类型，复用原有逻辑
        if (value instanceof PointNodeBO) {
            return cal((PointNodeBO) value);
        }
        // 其他类型（如String/Integer等），直接转字符串
        return value.toString().trim();
    }
    /** 写点位 */
    private void write(String deviceId, PointNodeBO node, String value) {
        if (node == null) return;
        pointValueCommandService.write(
                new PointValueWriteVO(
                        Long.valueOf(deviceId),
                        Long.valueOf(node.getPointId()),
                        value
                )
        );
    }
    // 核心工具方法：提取纯LWP/RWP前缀（无数字要求）
    private String extractPureLwpCode(String fieldName, Pattern lwpCodePattern) {
        Matcher matcher = lwpCodePattern.matcher(fieldName);
        if (matcher.find()) {
            // 仅提取LWP/RWP前缀并转大写，忽略后续所有字符（包括数字、full等）
            return matcher.group(0).toUpperCase();
        }
        return null;
    }

    /**
     * 解析 ZT 设备的剩余路径节点 ID 集合
     * 从当前位置 Tag 开始，提取 nav_route 中后续所有路径点对应的 nodeId
     * @param zt ZT 设备快照
     * @param routeStr nav_route 字符串（格式：00xxxYYY,angle;00xxxYYY,angle;...）
     * @param navNodes 导航节点列表
     * @return 剩余路径上的 nodeId 集合（不含当前位置）
     */
    private Set<Long> parseRemainingPathNodeIds(DeviceSnapshotBO zt, String routeStr, List<NavNodeBO> navNodes) {
        Set<Long> remainingNodeIds = new HashSet<>();
        try {
            // 1. 解析路径字符串为坐标列表
            List<String> routePointCoords = Arrays.stream(routeStr.split(";"))
                    .filter(s -> s.contains(","))
                    .map(s -> s.split(",", 2)[0].trim()) // 提取 "00xxxYYY" 部分
                    .filter(coord -> coord.length() >= 6)
                    .toList();

            if (routePointCoords.isEmpty()) {
                return remainingNodeIds;
            }

            // 2. 格式化当前位置 Tag（6 位补零）
            String currentTag = cal(zt.getCurrent_tag());
            if (currentTag.length() < 4) {
                return remainingNodeIds; // 无效位置，返回空
            }
//            tag = "0".repeat(6 - tag.length()) + tag;
//            String gridX = String.valueOf(Integer.parseInt(tag.substring(0, 3)));
//            String gridY = String.valueOf(Integer.parseInt(tag.substring(3, 6)));
            currentTag = String.format("%06d", Long.parseLong(currentTag));
            String currentGridX = currentTag.substring(0, 3);
            String currentGridY = currentTag.substring(3, 6);

            // 3. 定位当前位置在路径中的索引，提取后续坐标
            boolean foundCurrent = false;
            for (String coord : routePointCoords) {
//                if (coord.length() >= 6) {
                // 提取最后6位数字
                String sixDigits = coord.substring(coord.length() - 6);
//                    int x = Integer.parseInt(sixDigits.substring(0, 3));
//                    int y = Integer.parseInt(sixDigits.substring(3, 6));
//                }
                String gridX = sixDigits.substring(0, 3);
                String gridY = sixDigits.substring(3, 6);

                if (!foundCurrent) {
                    // 查找当前位置（精确匹配）
                    if (gridX.equals(currentGridX) && gridY.equals(currentGridY)) {
                        foundCurrent = true;
                        // 当前位置本身不加入障碍物，继续处理下一个点
                    }
                } else {
                    // 4. 将后续坐标转换为 nodeId 并加入集合
                    for (NavNodeBO node : navNodes) {
                        if (node.getGridX() != null && node.getGridY() != null) {
                            String nodeGridX = String.format("%03d", node.getGridX());
                            String nodeGridY = String.format("%03d", node.getGridY());
                            if (gridX.equals(nodeGridX) && gridY.equals(nodeGridY) && node.getId() != null) {
                                remainingNodeIds.add(node.getId());
                                break; // 匹配到即跳出，提升效率
                            }
                        }
                    }
                }
            }

            // 4. 若未找到当前位置（异常场景），将整个路径作为障碍物
            if (!foundCurrent) {
                log.warn("ZT:{} 未在 nav_route 中定位到当前位置 {}，将整个路径作为障碍物",
                        zt.getDeviceCode(), currentTag);
                for (String coord : routePointCoords) {
                    String gridX = coord.substring(0, 3);
                    String gridY = coord.substring(3, 6);
                    for (NavNodeBO node : navNodes) {
                        if (node.getGridX() != null && node.getGridY() != null) {
                            String nodeGridX = String.format("%03d", node.getGridX());
                            String nodeGridY = String.format("%03d", node.getGridY());
                            if (gridX.equals(nodeGridX) && gridY.equals(nodeGridY) && node.getId() != null) {
                                remainingNodeIds.add(node.getId());
                                break;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("解析 ZT:{} 剩余路径节点异常", zt.getDeviceCode(), e);
        }
        return remainingNodeIds;
    }

    /**
     * 判断目标SWS绑定码是否被ZCS关联占用
     * @param targetSwsCode 待校验的SWS码（如SWS1）
     * @return true=被占用，false=未被占用
     */
    private boolean isSwsOccupiedByZcs(String targetSwsCode) {
        // 遍历所有ZT的SWS-ZCS关联关系
        for (String swsZcsKey : ZT_SWS_ZCS_MAPPING.values()) {
            if (swsZcsKey == null || !swsZcsKey.contains("_")) {
                continue;
            }
            String[] codes = swsZcsKey.split("_");
            String associatedSws = codes[0]; // 关联的SWS码
            // 匹配目标SWS码 → 被占用
            if (targetSwsCode.equals(associatedSws)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清理ZT的SWS-ZCS关联（离开ZCS位/充电完成时调用）
     * @param ztDeviceCode ZT设备码
     */
    private void clearZcsSwsMapping(String ztDeviceCode) {
        if (ZT_SWS_ZCS_MAPPING.containsKey(ztDeviceCode)) {
            String swsZcsKey = ZT_SWS_ZCS_MAPPING.remove(ztDeviceCode);
            String[] codes = swsZcsKey.split("_");
            log.info("ZT:{} 已离开ZCS位，清除SWS{}关联，解除占用", ztDeviceCode, codes[0]);
        }
    }

    /**
     * 实时校验：目标SWS位对应的ZCS位是否有ZT存在（取消缓存，纯坐标/绑定码判断）
     * @param targetSwsCode 目标SWS码（如SWS1）
     * @param ztList 所有ZT快照列表
     * @param gridNodeMap 坐标-节点映射
     * @param bindNodeMap 绑定码-节点映射
     * @return true=ZCS位有ZT→SWS被占用，false=ZCS位无ZT→SWS可用
     */
    private boolean isSwsOccupiedByZcsRealTime(String targetSwsCode,
                                               List<DeviceSnapshotBO> ztList,
                                               Map<String, NavNodeBO> gridNodeMap,
                                               Map<String, NavNodeBO> bindNodeMap) {
        try {
            // 1. 从SWS码提取序号，生成对应ZCS码（SWS1 → ZCS1）
            int swsSeq = extractNumberFromCode(targetSwsCode);
            String targetZcsCode = "ZCS" + swsSeq;

            // 2. 找到ZCS码对应的NavNode（坐标）
            NavNodeBO zcsNode = bindNodeMap.get(targetZcsCode);
            if (zcsNode == null) {
                log.debug("SWS{}对应的ZCS{}不存在，判定为未占用", swsSeq, swsSeq);
                return false;
            }
            String zcsGridX = String.format("%03d", zcsNode.getGridX());
            String zcsGridY = String.format("%03d", zcsNode.getGridY());
            String zcsGridKey = gridKey(zcsGridX, zcsGridY);

            // 3. 遍历所有ZT，检查是否有ZT在ZCS坐标上
            for (DeviceSnapshotBO zt : ztList) {
                String ztCurrentBindCode = getZtCurrentBindCode(zt, gridNodeMap, bindNodeMap);
                // 方式1：直接判断ZT当前绑定码是否是ZCS码（更精准）
                if (targetZcsCode.equals(ztCurrentBindCode)) {
                    log.info("SWS{}对应的ZCS{}被ZT{}占用，判定为不可用",
                            swsSeq, swsSeq, zt.getDeviceCode());
                    return true;
                }
                // 方式2：兜底 - 通过坐标判断（防止绑定码解析失败）
                String ztTag = cal(zt.getCurrent_tag());
                if (ztTag.length() >= 4) {
                    ztTag = String.format("%06d", Long.parseLong(ztTag));
                    String ztGridX = ztTag.substring(0, 3);
                    String ztGridY = ztTag.substring(3, 6);
                    String ztGridKey = gridKey(ztGridX, ztGridY);
                    if (zcsGridKey.equals(ztGridKey)) {
                        log.info("SWS{}对应的ZCS{}坐标({})被ZT{}占用，判定为不可用",
                                swsSeq, swsSeq, zcsGridKey, zt.getDeviceCode());
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("实时校验SWS{}占用状态异常", targetSwsCode, e);
            return false; // 异常时默认判定为未占用，避免误拦截
        }
    }


}
