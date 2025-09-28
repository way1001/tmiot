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

package com.aiforest.tmiot.common.quartz;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

/**
 * Scheduler 工具类
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class QuartzService {

    @Resource
    private Scheduler scheduler;

    /**
     * 创建调度任务
     *
     * @param group        任务分组
     * @param name         任务名称
     * @param interval     时间间隔
     * @param intervalUnit 时间间隔单位
     * @param jobClass     任务执行类
     * @throws SchedulerException SchedulerException
     */
    public void createJobWithInterval(String group, String name, Integer interval, DateBuilder.IntervalUnit intervalUnit, Class<? extends Job> jobClass) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(name, group).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, group)
                .startAt(DateBuilder.futureDate(1, intervalUnit))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever())
                .startNow().build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    // ---------------- 新增方法：创建触发器后立即暂停（专门给 NavigationJob 用） ----------------
    public void createJobWithIntervalAndPause(String group, String name, int interval, DateBuilder.IntervalUnit unit, Class<? extends Job> jobClass, JobDataMap jobDataMap) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(name, group);
        if (scheduler.checkExists(jobKey)) {
            return;
        }
        //核心修复：按 IntervalUnit 动态构建 SimpleSchedule（仅调用一个间隔方法）
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule();
        // 根据单位选择对应的间隔方法
        if (unit == DateBuilder.IntervalUnit.SECOND) {
            scheduleBuilder.withIntervalInSeconds(interval); // 仅秒单位生效
        } else if (unit == DateBuilder.IntervalUnit.MINUTE) {
            scheduleBuilder.withIntervalInMinutes(interval); // 仅分单位生效
        } else if (unit == DateBuilder.IntervalUnit.HOUR) {
            scheduleBuilder.withIntervalInHours(interval);   // 仅时单位生效
        } else {
            // 如需支持天/周等其他单位，可继续扩展，避免未处理的单位
            throw new UnsupportedOperationException("暂不支持的时间单位：" + unit.name());
        }
        // 配置“无限重复”（根据业务需求，也可改为 withRepeatCount(int) 限制次数）
        scheduleBuilder.repeatForever();

        // 3. 构建触发器（无需 startNow()，创建后默认暂停，后续强制暂停双重保险）
        TriggerKey triggerKey = TriggerKey.triggerKey(name + "-trigger", group);
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobKey) // 绑定到目标任务
                .withSchedule(scheduleBuilder) // 注入正确的调度规则
                .build();
        // 2. 创建任务并绑定触发器
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobKey).setJobData(jobDataMap).storeDurably().build();
        scheduler.scheduleJob(jobDetail, trigger);

        // 3. 额外保险：强制暂停触发器（避免个别 Quartz 版本默认状态不一致）
        scheduler.pauseTrigger(triggerKey);
        log.info("任务[{}]的触发器已创建并暂停", name);
    }

    /**
     * 创建调度任务
     *
     * @param group    任务分组
     * @param name     任务名称
     * @param cron     Cron 表达式
     * @param jobClass 任务执行类
     * @throws SchedulerException SchedulerException
     */
    public void createJobWithCron(String group, String name, String cron, Class<? extends Job> jobClass) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(name, group).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, group)
                .startAt(DateBuilder.futureDate(1, DateBuilder.IntervalUnit.SECOND))
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .startNow().build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * 启动调度服务
     *
     * @throws SchedulerException SchedulerException
     */
    public void startScheduler() throws SchedulerException {
        if (!scheduler.isShutdown()) {
            scheduler.start();
        }
    }

    /**
     * 关闭调度服务
     * <p>
     * 直接关闭, 不等待未执行完的任务
     *
     * @throws SchedulerException SchedulerException
     */
    public void stopScheduler() throws SchedulerException {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * 更新任务的JobDataMap（用于传递场景ID）
     */
    public void updateJobData(JobKey jobKey, JobDataMap newJobDataMap) throws SchedulerException {
        // 1. 获取现有任务
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        if (jobDetail == null) {
            throw new SchedulerException("任务不存在：" + jobKey);
        }

        // 2. 更新JobDataMap
        JobDataMap oldDataMap = jobDetail.getJobDataMap();
        oldDataMap.putAll(newJobDataMap); // 覆盖或新增数据

        // 3. 重新注册任务（更新数据）
        scheduler.addJob(jobDetail, true);
    }


    // ---------------- 新增：任务状态管理方法 ----------------
    /**
     * 检查任务是否存在
     */
    public boolean checkJobExists(JobKey jobKey) throws SchedulerException {
        return scheduler.checkExists(jobKey);
    }

    /**
     * 暂停任务（停止执行，保留任务定义）
     */
    public void pauseJob(JobKey jobKey) throws SchedulerException {
        scheduler.pauseJob(jobKey);
    }

    /**
     * 恢复任务（继续执行）
     */
    public void resumeJob(JobKey jobKey) throws SchedulerException {
        scheduler.resumeJob(jobKey);
    }

    // Getter（供外部访问调度器，可选）
    public Scheduler getScheduler() {
        return scheduler;
    }

    // 检查调度器是否已启动
    public boolean isSchedulerStarted() throws SchedulerException {
        return scheduler.isStarted();
    }
    // 恢复触发器
    public void resumeTrigger(TriggerKey triggerKey) throws SchedulerException {
        scheduler.resumeTrigger(triggerKey);
    }
    // 获取触发器实际状态
    public Trigger.TriggerState getTriggerState(TriggerKey triggerKey) throws SchedulerException {
        return scheduler.getTriggerState(triggerKey);
    }

}
