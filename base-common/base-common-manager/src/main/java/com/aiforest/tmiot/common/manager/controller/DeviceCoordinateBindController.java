package com.aiforest.tmiot.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.common.base.BaseController;
import com.aiforest.tmiot.common.constant.service.ManagerConstant;
import com.aiforest.tmiot.common.entity.R;
import com.aiforest.tmiot.common.enums.ResponseEnum;
import com.aiforest.tmiot.common.manager.entity.bo.DeviceCoordinateBindBO;
import com.aiforest.tmiot.common.manager.entity.builder.DeviceCoordinateBindBuilder;
import com.aiforest.tmiot.common.manager.entity.query.DeviceCoordinateBindQuery;
import com.aiforest.tmiot.common.manager.entity.vo.DeviceCoordinateBindVO;
import com.aiforest.tmiot.common.manager.service.DeviceCoordinateBindService;
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

@Slf4j
@RestController
@RequestMapping(ManagerConstant.DEVICE_COORDINATE_BIND_URL_PREFIX)
public class DeviceCoordinateBindController implements BaseController {

    private final DeviceCoordinateBindBuilder builder;
    private final DeviceCoordinateBindService service;

    public DeviceCoordinateBindController(DeviceCoordinateBindBuilder builder,
                                          DeviceCoordinateBindService service) {
        this.builder = builder;
        this.service = service;
    }

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DeviceCoordinateBindVO vo) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DeviceCoordinateBindBO bo = builder.buildBOByVO(vo);
                bo.setTenantId(tenantId);
                service.save(bo);
                return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

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

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DeviceCoordinateBindVO vo) {
        try {
            DeviceCoordinateBindBO bo = builder.buildBOByVO(vo);
            service.update(bo);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @GetMapping("/id/{id}")
    public Mono<R<DeviceCoordinateBindVO>> selectById(@NotNull @PathVariable("id") Long id) {
        try {
            DeviceCoordinateBindBO bo = service.selectById(id);
            return Mono.just(R.ok(builder.buildVOByBO(bo)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @GetMapping("/nav_node_id/{id}")
    public Mono<R<List<DeviceCoordinateBindVO>>> selectByNavNodeId(@NotNull @PathVariable("id") Long id) {
        try {
            return Mono.just(R.ok(builder.buildVOListByBOList(service.selectByNavNodeId(id))));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @GetMapping("/device_id/{id}")
    public Mono<R<List<DeviceCoordinateBindVO>>> selectByDeviceId(@NotNull @PathVariable("id") Long id) {
        try {
            return Mono.just(R.ok(builder.buildVOListByBOList(service.selectByDeviceId(id))));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    @GetMapping("/attribute_id/{id}")
    public Mono<R<List<DeviceCoordinateBindVO>>> selectByAttributeId(@NotNull @PathVariable("id") Long id) {
        try {
            return Mono.just(R.ok(builder.buildVOListByBOList(service.selectByAttributeId(id))));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

//    @PostMapping("/list")
//    public Mono<R<Page<DeviceCoordinateBindVO>>> list(@RequestBody(required = false) DeviceCoordinateBindQuery query) {
//        return getTenantId().flatMap(tenantId -> {
//            try {
//                DeviceCoordinateBindQuery q = Objects.isNull(query) ? new DeviceCoordinateBindQuery() : query;
//                q.setTenantId(tenantId);
//                Page<DeviceCoordinateBindBO> pageBO = service.selectByPage(q);
//                Page<DeviceCoordinateBindVO> pageVO = builder.buildVOPageByBOPage(pageBO);
//                return Mono.just(R.ok(pageVO));
//            } catch (Exception e) {
//                log.error(e.getMessage(), e);
//                return Mono.just(R.fail(e.getMessage()));
//            }
//        });
//    }
}