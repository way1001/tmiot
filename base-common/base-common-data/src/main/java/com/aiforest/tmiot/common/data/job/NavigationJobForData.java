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

import com.aiforest.tmiot.common.data.biz.NavigationDataService;
import com.aiforest.tmiot.common.data.biz.PointValueCommandService;
import com.aiforest.tmiot.common.data.entity.bo.*;
import com.aiforest.tmiot.common.data.entity.dto.DeviceWithPointIds;
import com.aiforest.tmiot.common.data.entity.vo.PointValueWriteVO;
import com.aiforest.tmiot.common.data.metadata.DevicePointsMetadata;
import com.aiforest.tmiot.common.data.service.DevicePointsJsonbService;
import com.aiforest.tmiot.common.entity.bo.PointValueBO;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
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

    private final ThreadPoolExecutor threadPoolExecutor;
    private final PointValueCommandService pointValueCommandService;
    private final NavigationDataService navigationDataService;
    private final DevicePointsJsonbService devicePointsJsonbService;

    /* 临时缓存上一次点位值，生产请换成 Redis */
    private static final Map<String, String> LAST_VALUE_CACHE = new ConcurrentHashMap<>();

    public NavigationJobForData(DevicePointsJsonbService devicePointsJsonbService, NavigationDataService navigationDataService, PointValueCommandService pointValueCommandService, ThreadPoolExecutor threadPoolExecutor) {
        this.navigationDataService = navigationDataService;
        this.pointValueCommandService = pointValueCommandService;
        this.threadPoolExecutor = threadPoolExecutor;
        this.devicePointsJsonbService = devicePointsJsonbService;
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
////        log.info("hourlyJobHandler: {}", LocalDateTimeUtil.now());
//        List<DevicePointLatestBO>  devicePointLatestBOList = Optional.ofNullable(devicePointsMetadata.getCache(1L))
//                .orElse(Collections.emptyList());
//        List<DeviceWithPointIds> deviceWithPointIdsList = devicePointLatestBOList.stream()
//                .filter(d -> d.getPoints() != null)
//                .map(d -> new DeviceWithPointIds(
//                        d.getId(),                       // 设备 id
//                        d.getPoints()
//                                .stream()
//                                .map(PointBO::getId)          // 点位 id
//                                .toList()
//                ))
//                .toList();
//        List<PointValueBO> allLatest = deviceWithPointIdsList.stream()
//                .flatMap(dp -> {
//                    // 调用接口，拿到当前设备下这批点位的最新值
//                    List<PointValueBO> values = navigationDataService.latest(dp.getDeviceId(), dp.getPointIds());
//                    // 防御：接口可能返回 null，统一为空流
//                    return (values == null ? Collections.<PointValueBO>emptyList() : values).stream();
//                })
//                .toList();
//
//        Map<String, Object> result =
//                buildSnapshotMap(allLatest, devicePointLatestBOList);
//
////        result.forEach((code, snapshot) -> {
////            if (snapshot instanceof DeviceSnapshotBO) {
//////                System.out.println("ZT 设备:" + code + " => " + snapshot);
////            } else {
//////                System.out.println("TM 设备:" + code + " => " + snapshot);
////            }
////        });
//        runStrategy(result);
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
                .collect(Collectors.toMap(DevicePointLatestBO::getDeviceCode,
                        dev -> {
                            List<PointNodeBO> nodes = devicePointNodes.getOrDefault(dev.getId(), List.of());
                            Map<String, PointNodeBO> nodeMap = nodes.stream()
                                    .collect(Collectors.toMap(PointNodeBO::getPointCode, Function.identity()));

                            boolean isZt = "ZT".equalsIgnoreCase(dev.getDeviceCode());

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
        ds.setLspeed(nodeMap.get("lspeed"));
        ds.setRspeed(nodeMap.get("rspeed"));
        ds.setWork_status(nodeMap.get("work_status"));
        ds.setOperating_mode(nodeMap.get("operating_mode"));
        ds.setCleaning(nodeMap.get("cleaning"));
        ds.setFull(nodeMap.get("full"));
        ds.setEjection(nodeMap.get("ejection"));
        ds.setCharging(nodeMap.get("charging"));
        ds.setExit_direction(nodeMap.get("exit_direction"));
        ds.setEWP(nodeMap.get("EWP"));
        ds.setTrun_count(nodeMap.get("trun_count"));
        ds.setCAF(nodeMap.get("CAF"));
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

    private void runStrategy(Map<String, Object> snapshotMap) {
        try {
            DeviceSnapshotBO zt   = (DeviceSnapshotBO)   snapshotMap.get("ZT");
            TMDeviceSnapshotBO tm = (TMDeviceSnapshotBO) snapshotMap.get("TM17A");

            if (zt == null || tm == null) {
                log.warn("ZT 或 TM17A 快照缺失，跳过");
                return;
            }
            if (!"ONLINE".equalsIgnoreCase(zt.getStatus()) ||
                    !"ONLINE".equalsIgnoreCase(tm.getStatus())) {
                log.warn("ZT 或 TM17A 不在线，跳过");
                return;
            }

            /* ---------- 1. 点位声明 ---------- */
            // ZT
            PointNodeBO workStatus      = zt.getWork_status();
            PointNodeBO charging        = zt.getCharging();
            PointNodeBO cleaning        = zt.getCleaning();
            PointNodeBO ewp             = zt.getEWP();
            PointNodeBO exitDir         = zt.getExit_direction();
            PointNodeBO caf             = zt.getCAF();
            PointNodeBO full            = zt.getFull();
            PointNodeBO ejection        = zt.getEjection();
            PointNodeBO operatingMode   = zt.getOperating_mode();

            // TM17
            PointNodeBO liftControl     = tm.getLifting();      // 升降
            PointNodeBO blowingControl  = tm.getCleaning();     // 吹气/清洗
            PointNodeBO wheelProtector  = tm.getRetainer();     // 护轮
//            PointNodeBO hostRun         = tm.getHost_run();     // 运行
            PointNodeBO inching         = tm.getLifting();      // 复用 lifting 做寸行
            PointNodeBO pushing         = tm.getCleaning();     // 复用 cleaning 做推筒
            PointNodeBO wsnb            = tm.getFull();         // 满筒信号

            /* ---------- 2. 缓存旧值 ---------- */
            String oldWsnb   = getAndCache("TM17A:WSNB", wsnb);
            String oldWorkSt = getAndCache("ZT:work_status", workStatus);
            String oldOPMode = getAndCache("ZT:operating_mode", operatingMode);

            /* ---------- 3. work_status 变化才处理 ---------- */
            if (changed(workStatus, oldWorkSt) && oldWorkSt != null || (changed(operatingMode, oldOPMode) && oldOPMode != null)) {
                this.devicePointsJsonbService.save(zt);
//                return;     // 无变化直接返回
            }

            String ws = cal(workStatus);

            /* ---------- 4. 按状态一次性处理 ---------- */
            switch (ws) {
                case "70":      // 充电
                    // 前端有 CAF==0 才允许升降，后端直接按 charging 值写
                    write(tm.getId(), liftControl, "true".equals(cal(charging)) ? "true" : "false");
                    // 若 charging==true 且 lift==true 超过 10 次置 CAF=1，后端不做计数，直接写死
                    if ("true".equals(cal(charging))) {
                        write(zt.getId(), caf, "1");
                    }
                    break;

                case "60":      // 清洗
                    if ("true".equals(cal(cleaning))) {
                        write(tm.getId(), blowingControl, "true");
                    } else {
                        write(tm.getId(), blowingControl, "false");
                    }
                    break;

                case "90":      // 护轮升起
                    write(tm.getId(), wheelProtector, "true");
//                    write(tm.getId(), hostRun,        "true");
                    break;

                case "100":     // 寸行
                    if ("1".equals(cal(exitDir))) {
                        write(tm.getId(), inching, "false");
                    } else {
                        write(tm.getId(), inching, "true");
                    }
                    break;

                case "110":     // 准备出筒
                    if ("false".equals(cal(wsnb))) {
                        write(zt.getId(), ewp, "true");
                    }
                    break;

                case "120":     // 出筒
//                    write(tm.getId(), inching, "false");
                    write(tm.getId(), wheelProtector, "false");
                    write(tm.getId(), pushing, "true");
                    write(zt.getId(), ejection, "true");
                    break;
            }

            /* ---------- 5. 独立点位变化（不依赖 work_status 的） ---------- */
            // WSNB 由 false→true 时复位 operating_mode
            if (changed(wsnb, oldWsnb) && "true".equals(cal(wsnb))) {
                write(zt.getId(), operatingMode, "0");
            }

            // 满筒信号
            if ("true".equals(cal(wsnb))) {
                write(zt.getId(), full, "true");
            }

        } catch (Exception e) {
            log.error("策略执行异常", e);
        }
    }

    /* ---------------- 4. 工具方法 ---------------- */

    /** 取当前值，并缓存 */
    private String getAndCache(String key, PointNodeBO node) {
        String old = LAST_VALUE_CACHE.get(key);
        LAST_VALUE_CACHE.put(key, cal(node));
        return old;
    }

    /** 判断是否变化 */
    private boolean changed(PointNodeBO node, String oldVal) {
        return !Objects.equals(cal(node), oldVal);
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
