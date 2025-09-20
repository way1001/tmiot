package com.aiforest.tmiot.common.data.controller;

import com.aiforest.tmiot.common.data.service.ZTJsonbService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/zt-jsonb")
@RequiredArgsConstructor
public class ZTJsonbController {

    private final ZTJsonbService service;

    @PostMapping
    public Long save(@RequestBody JsonNode json) {
        return service.saveJson(json);
    }

    @GetMapping("/{id}")
    public JsonNode one(@PathVariable Long id) {
        return service.getJsonById(id);
    }

    @GetMapping("/list")
    public IPage<JsonNode> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return service.pageJson(current, size);
    }
}
