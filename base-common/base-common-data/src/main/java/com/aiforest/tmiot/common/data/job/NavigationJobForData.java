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

import cn.hutool.core.collection.ConcurrentHashSet;
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
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
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
        //devicePointsMetadata.loadCache(1L);
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
    public static Map<String, Object> buildSnapshotMap(List<PointValueBO> allLatest,
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

                            if (isZt) {
                                return buildZtSnapshot(dev, nodeMap);
                            } else {
                                return buildTmSnapshot(dev, nodeMap);
                            }
                        }));
    }

    /* ---------------- 构造 DeviceSnapshotBO ---------------- */
    private static DeviceSnapshotBO buildZtSnapshot(DevicePointLatestBO dev,
                                                    Map<String, PointNodeBO> nodeMap) {
        DeviceSnapshotBO ds = new DeviceSnapshotBO();
        ds.setId(String.valueOf(dev.getId()));
        ds.setDeviceCode(dev.getDeviceCode());
        ds.setDeviceName(dev.getDeviceName());
        ds.setStatus("ONLINE"); // 按需求自行计算
        ds.setCtrl_mode(nodeMap.get("ctrl_mode"));
        ds.setRun_state(nodeMap.get("run_state"));
        ds.setNav_mode(nodeMap.get("nav_mode"));
        ds.setBattery_voltage(nodeMap.get("battery_voltage"));
        ds.setBlind_dist(nodeMap.get("blind_dist"));
        ds.setCurrent_tag(nodeMap.get("current_tag"));
        ds.setNav_route(nodeMap.get("nav_route"));
//        ds.setLspeed(nodeMap.get("lspeed"));
//        ds.setRspeed(nodeMap.get("rspeed"));
//        ds.setWork_status(nodeMap.get("work_status"));
//        ds.setOperating_mode(nodeMap.get("operating_mode"));
//        ds.setCleaning(nodeMap.get("cleaning"));
//        ds.setFull(nodeMap.get("full"));
//        ds.setEjection(nodeMap.get("ejection"));
//        ds.setCharging(nodeMap.get("charging"));
//        ds.setExit_direction(nodeMap.get("exit_direction"));
//        ds.setEWP(nodeMap.get("EWP"));
//        ds.setTrun_count(nodeMap.get("trun_count"));
//        ds.setCAF(nodeMap.get("CAF"));
        return ds;
    }

    /* ---------------- 构造 TMDeviceSnapshotBO ---------------- */
    private static TMDeviceSnapshotBO buildTmSnapshot(DevicePointLatestBO dev,
                                                      Map<String, PointNodeBO> nodeMap) {
        TMDeviceSnapshotBO tm = new TMDeviceSnapshotBO();
        tm.setId(String.valueOf(dev.getId()));
        tm.setDeviceCode(dev.getDeviceCode());
        tm.setDeviceName(dev.getDeviceName());
        tm.setStatus("ONLINE"); // 同上
        tm.setFull(nodeMap.get("full"));
        tm.setLifting(nodeMap.get("lifting"));
        tm.setCleaning(nodeMap.get("cleaning"));
        tm.setRetainer(nodeMap.get("retainer"));
        return tm;
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


    private void runStrategy(Map<String, Object> snapshotMap, String sceneId) {

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

        for (DeviceSnapshotBO zt : ztList) {
            try {
                String currentNavMode = cal(zt.getNav_mode());
                String currentNavRoute = cal(zt.getNav_route());

                String lastNavMode = LAST_NAV_MODE.put(zt.getDeviceCode(), currentNavMode);
                String lastNavRoute = LAST_NAV_ROUTE.put(zt.getDeviceCode(), currentNavRoute);

                if (!"64".equalsIgnoreCase(cal(zt.getCtrl_mode()))) {
                    continue;                 // 不可调度
                }

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

                /* 4. 解析起点 grid */
                String tag = cal(zt.getCurrent_tag());
                if (tag.length() < 4) {
                    log.warn("ZT:{} current_tag={} 长度不足4位，跳过", zt.getDeviceCode(), tag);
                    continue;
                }
                tag = "0".repeat(6 - tag.length()) + tag;   // 右补零，不足6位自动补0
                String gridX = String.valueOf(Integer.parseInt(tag.substring(0, 3)));
                String gridY = String.valueOf(Integer.parseInt(tag.substring(3, 6)));
                NavNodeBO fromNode = gridNodeMap.get(gridKey(gridX, gridY));
                if (fromNode == null) {
                    log.warn("ZT:{} 起点(gridX={},gridY={}) 未匹配 NavNode，跳过",
                            zt.getDeviceCode(), gridX, gridY);
                    continue;
                }

                /* 5. 终点 nodeId：责任区循环 or CZS */
                String tgtBindCode;
                if (lowBattery(zt)) {
                    tgtBindCode = "CZS";
                } else {
                    List<String> duty = dutyOf(zt.getDeviceCode());
                    if (duty == null || duty.isEmpty()) {
                        log.warn("ZT:{} 未配置责任区，跳过", zt.getDeviceCode());
                        continue;
                    }
                    // 拿到本轮要发的绑定码
                    AtomicInteger idxHolder = ZT_DUTY_INDEX.computeIfAbsent(zt.getDeviceCode(), k -> new AtomicInteger(0));
                    int idx = idxHolder.getAndUpdate(i -> (i + 1) % duty.size()); // 先取再+1
                    tgtBindCode = duty.get(idx);
                }
                NavNodeBO toNode = bindNodeMap.get(tgtBindCode);
                if (toNode == null || fromNode.getId().equals(toNode.getId())) {
                    log.warn("绑定码:{} 未找到 NavNode，跳过", tgtBindCode);
                    continue;
                }

                Set<String> validGridSet = navNodes.stream()
                        .filter(n -> n.getGridX() != null && n.getGridY() != null)
                        .map(n -> n.getGridX() + "," + n.getGridY())
                        .collect(Collectors.toSet());

                List<NavPathPoint> navPathPoints = navigationPathService
                        .planPath(Long.parseLong(sceneId), fromNode.getId(), toNode.getId())
                        .stream()
                        .filter(p -> validGridSet.contains(p.getGridX() + "," + p.getGridY()))
                        .toList();

                String routeStr = toRouteStr(navPathPoints);
                write(zt.getId(), zt.getNav_route(), routeStr);

            } catch (Exception e) {
                log.error("ZT:{} 调度异常", zt.getDeviceCode(), e);
            }
        }
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
            return Integer.parseInt(cal(zt.getBattery_voltage())) < 2200;
        } catch (Exception ignore) {
            return false;
        }
    }


    /** 获取 calValue 或空串 */
    private static String cal(PointNodeBO node) {
        return node == null || node.getCalValue() == null ? "" : node.getCalValue();
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
}
