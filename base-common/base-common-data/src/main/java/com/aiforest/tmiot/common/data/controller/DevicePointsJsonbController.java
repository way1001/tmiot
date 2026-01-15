package com.aiforest.tmiot.common.data.controller;

import com.aiforest.tmiot.common.data.entity.bo.DeviceSnapshotBO;
import com.aiforest.tmiot.common.data.entity.dto.HistoryPageDTO;
import com.aiforest.tmiot.common.data.entity.vo.DeviceSnapshotVO;
import com.aiforest.tmiot.common.data.service.DevicePointsJsonbService;
import com.aiforest.tmiot.common.entity.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/device_points_jsonb")
@RequiredArgsConstructor
public class DevicePointsJsonbController {

    private final DevicePointsJsonbService service;

    @PostMapping()
    public Mono<R<Void>> save(@RequestBody DeviceSnapshotBO bo) {
        service.save(bo);
        return Mono.just(R.ok());
    }

//    @GetMapping("/{deviceId}")
//    public Mono<R<IPage<DeviceSnapshotVO>>> page(
//            @PathVariable String deviceId,
//            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
//            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
//            @RequestParam(defaultValue = "1") long current,
//            @RequestParam(defaultValue = "10") long size) {
//
//        HistoryPageDTO dto = new HistoryPageDTO();
//        dto.setDeviceId(deviceId);
//        dto.setStart(start);
//        dto.setEnd(end);
//
//        return Mono.just(R.ok(service.page(dto, current, size)));
//    }

//    @GetMapping("/page")
//    public Mono<R<IPage<DeviceSnapshotVO>>> page(
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
//            @RequestParam(defaultValue = "1") long current,
//            @RequestParam(defaultValue = "10") long size) {
//        return Mono.just(R.ok(service.pageByTime(start, end, current, size)));
//    }

}
