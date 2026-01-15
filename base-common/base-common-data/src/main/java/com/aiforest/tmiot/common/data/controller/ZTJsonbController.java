package com.aiforest.tmiot.common.data.controller;

import com.aiforest.tmiot.common.data.biz.PowerStatsService;
import com.aiforest.tmiot.common.data.entity.vo.StatsRespVO;
import com.aiforest.tmiot.common.data.service.ZTJsonbService;
import com.aiforest.tmiot.common.entity.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/zt-jsonb")
@RequiredArgsConstructor
public class ZTJsonbController {

//    private final ZTJsonbService service;
//
//    @PostMapping
//    public Long save(@RequestBody JsonNode json) {
//        return service.saveJson(json);
//    }
//
//    @GetMapping("/{id}")
//    public JsonNode one(@PathVariable Long id) {
//        return service.getJsonById(id);
//    }
//
//    @GetMapping("/list")
//    public IPage<JsonNode> list(
//            @RequestParam(defaultValue = "1") long current,
//            @RequestParam(defaultValue = "10") long size) {
//        return service.pageJson(current, size);
//    }

    private final PowerStatsService service;

    @GetMapping("/batch")
    public Mono<R<Map<String, StatsRespVO>>> queryBatch(@RequestParam("deviceIds") List<String> deviceIds,
                                                        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                        @RequestParam("end")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return Mono.just(R.ok(service.getStatsBatch(deviceIds, start, end)));
    }
}
