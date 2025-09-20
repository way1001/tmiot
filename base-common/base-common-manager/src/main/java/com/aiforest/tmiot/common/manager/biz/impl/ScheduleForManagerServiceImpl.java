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

package com.aiforest.tmiot.common.manager.biz.impl;

import com.aiforest.tmiot.common.constant.driver.ScheduleConstant;
import com.aiforest.tmiot.common.manager.biz.ScheduleForManagerService;
import com.aiforest.tmiot.common.manager.job.HourlyJobForManager;
import com.aiforest.tmiot.common.quartz.QuartzService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

/**
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ScheduleForManagerServiceImpl implements ScheduleForManagerService {

    @Resource
    private QuartzService quartzService;

    @Override
    public void initial() {
        try {
            // 自定义调度
            quartzService.createJobWithCron(ScheduleConstant.MANAGER_SCHEDULE_GROUP, "hourly-job", "0 0 0/1 * * ?", HourlyJobForManager.class);

            quartzService.startScheduler();
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }
}
