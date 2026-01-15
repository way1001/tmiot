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

package com.aiforest.tmiot.common.data.biz.impl;

import com.aiforest.tmiot.common.constant.driver.ScheduleConstant;
import com.aiforest.tmiot.common.data.biz.ScheduleForDataService;
import com.aiforest.tmiot.common.data.job.HourlyJobForData;
import com.aiforest.tmiot.common.data.job.NavigationJobForData;
import com.aiforest.tmiot.common.data.job.PointValueJob;
import com.aiforest.tmiot.common.quartz.QuartzService;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ScheduleForDataServiceImpl implements ScheduleForDataService {

    @Value("${data.point.batch.interval}")
    private Integer interval;

    @Resource
    private QuartzService quartzService;

    // 新增：存储当前生效的场景ID（可选，用于状态查询时返回）
    @Getter
    private String currentSceneId;

    // ---------------- 新增：NavigationJob 状态管理 ----------------
    // 任务唯一标识（Group + Name，与 initial() 中创建的任务一致）
    private static final JobKey NAVIGATION_JOB_KEY = JobKey.jobKey(
            "navigation-strategy-schedule-job",
            ScheduleConstant.DATA_SCHEDULE_GROUP
    );
    // Getter（供 Controller 访问状态，可选）
    // 原子类记录任务状态（默认 false：停止）
    @Getter
    private final AtomicBoolean navigationJobEnabled = new AtomicBoolean(false);

    @Override
    public void initial() {
        try {
            quartzService.createJobWithInterval(ScheduleConstant.DATA_SCHEDULE_GROUP, "data-point-value-schedule-job", interval, DateBuilder.IntervalUnit.SECOND, PointValueJob.class);

            // 自定义调度
            quartzService.createJobWithCron(ScheduleConstant.DATA_SCHEDULE_GROUP, "hourly-job", "0 0 0/1 * * ?", HourlyJobForData.class);

           // 先判断任务是否已存在，避免重复创建
            if (!quartzService.checkJobExists(NAVIGATION_JOB_KEY)) {
                JobDataMap initDataMap = new JobDataMap();
                quartzService.createJobWithIntervalAndPause(
                        ScheduleConstant.DATA_SCHEDULE_GROUP,
                        "navigation-strategy-schedule-job",
                        3,
                        DateBuilder.IntervalUnit.SECOND,
                        NavigationJobForData.class,
                        initDataMap // 传入初始化数据（空）
                );
                log.info("NavigationJob 任务创建成功（默认停止）");
            }
            quartzService.startScheduler();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }

    // ---------------- 新增：外部控制接口（供 Controller 调用） ----------------
    /**
     * 启动 NavigationJob 任务
     * @return 操作结果（成功/失败原因）
     */
    public String startNavigationJob(String sceneId) {
        // 1. 前置校验：场景ID（已有，保留）
        if (sceneId == null || sceneId.trim().isEmpty()) {
            return "场景ID不能为空";
        }
        // 2. 校验调度器是否已启动
        try {
            if (!quartzService.isSchedulerStarted()) { // 需在 QuartzService 中新增该方法
                return "调度器未启动，无法恢复任务";
            }
        } catch (SchedulerException e) {
            log.error("查询调度器状态失败", e);
            return "查询调度器状态异常：" + e.getMessage();
        }

        try {
            // 3. 校验任务是否存在
            if (!quartzService.checkJobExists(NAVIGATION_JOB_KEY)) {
                return "NavigationJob 任务不存在，请重启系统触发初始化";
            }

            // 4. 校验任务是否已启动（本地状态）
            if (navigationJobEnabled.get()) {
                updateSceneId(sceneId);
                return "任务已启动，已更新场景ID为：" + sceneId;
            }

            // 5. 核心操作：先更新场景ID，再恢复触发器
            updateSceneId(sceneId);
            TriggerKey triggerKey = TriggerKey.triggerKey(NAVIGATION_JOB_KEY.getName() + "-trigger", NAVIGATION_JOB_KEY.getGroup());
            // 优先恢复触发器（而非 resumeJob，因 resumeJob 会恢复该任务的所有触发器，更精准）
            quartzService.resumeTrigger(triggerKey); // 需在 QuartzService 中新增 resumeTrigger 方法
            // 6. 同步本地状态（仅在恢复成功后更新）
            navigationJobEnabled.set(true);

            // 7. 验证实际状态（可选，增强可靠性）
            if (quartzService.getTriggerState(triggerKey) == Trigger.TriggerState.NORMAL) {
                log.info("NavigationJob 启动成功，场景ID：{}", sceneId);
                return "任务启动成功，场景ID：" + sceneId;
            } else {
                navigationJobEnabled.set(false); // 回滚本地状态
                return "任务恢复失败：Quartz 触发器状态异常";
            }

        } catch (SchedulerException e) {
            // 异常时回滚本地状态
            navigationJobEnabled.set(false);
            log.error("启动 NavigationJob 失败（场景ID：{}）", sceneId, e);
            return "启动失败：" + e.getMessage();
        }
    }

    /**
     * 停止 NavigationJob 任务
     * @return 操作结果（成功/失败原因）
     */
    public String stopNavigationJob() {
        try {
            // 1. 检查任务是否存在
            if (!quartzService.checkJobExists(NAVIGATION_JOB_KEY)) {
                return "NavigationJob 任务不存在";
            }

            // 2. 检查任务是否已停止（避免重复停止）
            if (!navigationJobEnabled.get()) {
                return "NavigationJob 已处于停止状态";
            }

            // 3. 停止任务（暂停触发器）
            quartzService.pauseJob(NAVIGATION_JOB_KEY);
            // 4. 更新状态为停止
            navigationJobEnabled.set(false);
            log.info("NavigationJob 任务停止成功");
            return "NavigationJob 停止成功";

        } catch (SchedulerException e) {
            log.error("停止 NavigationJob 失败", e);
            return "停止失败：" + e.getMessage();
        }
    }

    /**
     * 查询 NavigationJob 当前状态
     * @return 状态描述（启用/停用）
     */
    public String getNavigationJobStatus() {
        return navigationJobEnabled.get() ? "任务已启用（运行中）" : "任务已停用（默认状态）";
    }

    /**
     * 私有工具方法：更新场景ID（同步到JobDataMap和本地变量）
     * @param sceneId 新场景ID
     * @throws SchedulerException Quartz操作异常
     */
    private void updateSceneId(String sceneId) throws SchedulerException {
        // 1. 更新本地存储的场景ID
        this.currentSceneId = sceneId;

        // 2. 构建JobDataMap，传递场景ID到定时任务
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("sceneId", sceneId); // 键"sceneId"需与任务层一致

        // 3. 调用Quartz服务更新任务数据
        quartzService.updateJobData(NAVIGATION_JOB_KEY, jobDataMap);
        log.debug("NavigationJob 场景ID更新为：{}", sceneId);
    }

}
