/*
 * Copyright 2026-present the TM IoT original author or authors.
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
package com.aiforest.tmiot.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.common.base.BaseController;
import com.aiforest.tmiot.common.constant.service.ManagerConstant;
import com.aiforest.tmiot.common.entity.R;
import com.aiforest.tmiot.common.enums.ResponseEnum;
import com.aiforest.tmiot.common.exception.NotFoundException;
import com.aiforest.tmiot.common.manager.entity.bo.DeviceCoordinateAttributeBO;
import com.aiforest.tmiot.common.manager.entity.builder.DeviceCoordinateAttributeBuilder;
import com.aiforest.tmiot.common.manager.entity.query.DeviceCoordinateAttributeQuery;
import com.aiforest.tmiot.common.manager.entity.vo.DeviceCoordinateAttributeVO;
import com.aiforest.tmiot.common.manager.service.DeviceCoordinateAttributeService;
import com.aiforest.tmiot.common.valid.Add;
import com.aiforest.tmiot.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 设备坐标属性 Controller
 *
 * @author way
 * @version 2026.7.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DEVICE_COORDINATE_ATTRIBUTE_URL_PREFIX)
public class DeviceCoordinateAttributeController implements BaseController {

    private final DeviceCoordinateAttributeBuilder builder;
    private final DeviceCoordinateAttributeService service;

    public DeviceCoordinateAttributeController(
            DeviceCoordinateAttributeBuilder builder,
            DeviceCoordinateAttributeService service) {
        this.builder = builder;
        this.service = service;
    }

    /* ---------- 增 ---------- */

    @PostMapping("/add")
    public Mono<R<String>> add(
            @Validated(Add.class) @RequestBody DeviceCoordinateAttributeVO vo) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DeviceCoordinateAttributeBO bo = builder.buildBOByVO(vo);
                bo.setTenantId(tenantId);
                service.save(bo);
                return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /* ---------- 删 ---------- */

    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable("id") Long id) {
        try {
            service.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /* ---------- 改 ---------- */

    @PostMapping("/update")
    public Mono<R<String>> update(
            @Validated(Update.class) @RequestBody DeviceCoordinateAttributeVO vo) {
        try {
            DeviceCoordinateAttributeBO bo = builder.buildBOByVO(vo);
            service.update(bo);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /* ---------- 查 ---------- */

    @GetMapping("/id/{id}")
    public Mono<R<DeviceCoordinateAttributeVO>> selectById(
            @NotNull @PathVariable("id") Long id) {
        try {
            DeviceCoordinateAttributeBO bo = service.selectById(id);
            return Mono.just(R.ok(builder.buildVOByBO(bo)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据设备ID查询其下所有坐标属性
     */
    @GetMapping("/device_id/{id}")
    public Mono<R<List<DeviceCoordinateAttributeVO>>> selectByDeviceId(
            @NotNull @PathVariable("id") Long id) {
        try {
            List<DeviceCoordinateAttributeBO> bos = service.selectByDeviceId(id);
            return Mono.just(R.ok(builder.buildVOListByBOList(bos)));
        } catch (NotFoundException e) {
            return Mono.just(R.ok(Collections.emptyList()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 分页查询（支持关键字、设备ID等）
     */
    @PostMapping("/list")
    public Mono<R<Page<DeviceCoordinateAttributeVO>>> list(
            @RequestBody(required = false) DeviceCoordinateAttributeQuery query) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DeviceCoordinateAttributeQuery q =
                        Objects.isNull(query) ? new DeviceCoordinateAttributeQuery() : query;
                q.setTenantId(tenantId);
                Page<DeviceCoordinateAttributeBO> pageBO = service.selectByPage(q);
                Page<DeviceCoordinateAttributeVO> pageVO = builder.buildVOPageByBOPage(pageBO);
                return Mono.just(R.ok(pageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }
}