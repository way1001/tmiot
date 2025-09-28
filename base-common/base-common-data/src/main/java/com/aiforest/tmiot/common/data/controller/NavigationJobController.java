package com.aiforest.tmiot.common.data.controller;

import com.aiforest.tmiot.common.data.biz.impl.ScheduleForDataServiceImpl;
import com.aiforest.tmiot.common.entity.R;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/schedule/navigation-job")
public class NavigationJobController {

    @Resource
    private ScheduleForDataServiceImpl scheduleForDataService;

    /**
     * 启动导航任务
     *
     * @return 操作结果
     */
//    @PostMapping("/start")
//    public Mono<R<String>> startNavigationJob() {
//        try {
//            String result = scheduleForDataService.startNavigationJob();
//            return Mono.just(R.ok(result));
//        } catch (Exception e) {
//            log.error("启动导航任务失败: {}", e.getMessage(), e);
//            return Mono.just(R.fail("启动导航任务失败: " + e.getMessage()));
//        }
//    }
    @PostMapping("/start")
    public Mono<R<String>> startNavigationJob(@RequestParam(required = true, name = "sceneId") String sceneId) {
        try {
            // 校验场景ID非空
            if (sceneId == null || sceneId.trim().isEmpty()) {
                return Mono.just(R.fail("场景ID不能为空"));
            }
            // 调用服务层方法，传入场景ID
            String result = scheduleForDataService.startNavigationJob(sceneId);
            return Mono.just(R.ok(result));
        } catch (Exception e) {
            log.error("启动导航任务失败（场景ID：{}）: {}", sceneId, e.getMessage(), e);
            return Mono.just(R.fail("启动导航任务失败: " + e.getMessage()));
        }
    }

    /**
     * 停止导航任务
     *
     * @return 操作结果
     */
    @PostMapping("/stop")
    public Mono<R<String>> stopNavigationJob() {
        try {
            String result = scheduleForDataService.stopNavigationJob();
            return Mono.just(R.ok(result));
        } catch (Exception e) {
            log.error("停止导航任务失败: {}", e.getMessage(), e);
            return Mono.just(R.fail("停止导航任务失败: " + e.getMessage()));
        }
    }

    /**
     * 查询导航任务状态
     *
     * @return 任务状态信息
     */
    @PostMapping("/status")
    public Mono<R<String>> getNavigationJobStatus() {
        try {
            String status = scheduleForDataService.getNavigationJobStatus();
            return Mono.just(R.ok(status));
        } catch (Exception e) {
            log.error("查询导航任务状态失败: {}", e.getMessage(), e);
            return Mono.just(R.fail("查询导航任务状态失败: " + e.getMessage()));
        }
    }
}