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

package com.aiforest.tmiot.common.manager.controller;

import com.aiforest.tmiot.common.manager.entity.bo.DeviceCoordinateAttributeBO;
import com.aiforest.tmiot.common.manager.entity.bo.DevicePointLatestBO;
import com.aiforest.tmiot.common.manager.entity.builder.DeviceCoordinateAttributeBuilder;
import com.aiforest.tmiot.common.manager.entity.vo.DeviceCoordinateAttributeVO;
import com.aiforest.tmiot.common.manager.entity.vo.DeviceLatestVO;
import com.aiforest.tmiot.common.manager.service.DeviceCoordinateAttributeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.common.base.BaseController;
import com.aiforest.tmiot.common.constant.service.ManagerConstant;
import com.aiforest.tmiot.common.entity.R;
import com.aiforest.tmiot.common.enums.ResponseEnum;
import com.aiforest.tmiot.common.manager.entity.bo.DeviceBO;
import com.aiforest.tmiot.common.manager.entity.builder.DeviceBuilder;
import com.aiforest.tmiot.common.manager.entity.query.DeviceQuery;
import com.aiforest.tmiot.common.manager.entity.vo.DeviceVO;
import com.aiforest.tmiot.common.manager.service.DeviceService;
import com.aiforest.tmiot.common.utils.FileUtil;
import com.aiforest.tmiot.common.utils.ResponseUtil;
import com.aiforest.tmiot.common.valid.Add;
import com.aiforest.tmiot.common.valid.Update;
import com.aiforest.tmiot.common.valid.Upload;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备 Controller
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(ManagerConstant.DEVICE_URL_PREFIX)
public class DeviceController implements BaseController {

    private final DeviceBuilder deviceBuilder;
    private final DeviceService deviceService;
    private final DeviceCoordinateAttributeService deviceCoordinateAttributeService;
    private final DeviceCoordinateAttributeBuilder builder;

    public DeviceController(DeviceBuilder deviceBuilder, DeviceService deviceService,DeviceCoordinateAttributeService deviceCoordinateAttributeService, DeviceCoordinateAttributeBuilder builder) {
        this.deviceBuilder = deviceBuilder;
        this.deviceService = deviceService;
        this.deviceCoordinateAttributeService = deviceCoordinateAttributeService;
        this.builder = builder;
    }

    /**
     * 新增设备
     *
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody DeviceVO entityVO) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
                entityBO.setTenantId(tenantId);
                deviceService.save(entityBO);
                return Mono.just(R.ok(ResponseEnum.ADD_SUCCESS));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 根据 ID 删除设备
     *
     * @param id ID
     * @return R of String
     */
    @PostMapping("/delete/{id}")
    public Mono<R<String>> delete(@NotNull @PathVariable(value = "id") Long id) {
        try {
            deviceService.remove(id);
            return Mono.just(R.ok(ResponseEnum.DELETE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 更新设备
     *
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody DeviceVO entityVO) {
        try {
            DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
            deviceService.update(entityBO);
            return Mono.just(R.ok(ResponseEnum.UPDATE_SUCCESS));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 查询 Device
     *
     * @param id ID
     * @return DeviceVO {@link DeviceVO}
     */
    @GetMapping("/id/{id}")
    public Mono<R<DeviceVO>> selectById(@NotNull @PathVariable(value = "id") Long id) {
        try {
            DeviceBO entityBO = deviceService.selectById(id);
            DeviceVO entityVO = deviceBuilder.buildVOByBO(entityBO);
            return Mono.just(R.ok(entityVO));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 根据 ID 集合查询 Device
     *
     * @param deviceIds 设备ID集
     * @return Map(ID, DeviceVO)
     */
    @PostMapping("/ids")
    public Mono<R<Map<Long, DeviceVO>>> selectByIds(@RequestBody List<Long> deviceIds) {
        try {
            List<DeviceBO> entityBOList = deviceService.selectByIds(deviceIds);
            Map<Long, DeviceVO> deviceMap = entityBOList.stream().collect(Collectors.toMap(DeviceBO::getId, entityBO -> deviceBuilder.buildVOByBO(entityBO)));
            return Mono.just(R.ok(deviceMap));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

    /**
     * 分页查询 Device
     *
     * @param entityQuery 设备和分页参数
     * @return R Of DeviceVO Page
     */
    @PostMapping("/list")
    public Mono<R<Page<DeviceVO>>> list(@RequestBody(required = false) DeviceQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DeviceQuery query = Objects.isNull(entityQuery) ? new DeviceQuery() : entityQuery;
                query.setTenantId(tenantId);
                Page<DeviceBO> entityPageBO = deviceService.selectByPage(query);
                Page<DeviceVO> entityPageVO = deviceBuilder.buildVOPageByBOPage(entityPageBO);
                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    @PostMapping("/lists")
    public Mono<R<Page<DeviceVO>>> lists(@RequestBody(required = false) DeviceQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DeviceQuery query = Objects.isNull(entityQuery) ? new DeviceQuery() : entityQuery;
                query.setTenantId(tenantId);

                // 1. 先分页查设备
                Page<DeviceBO> entityPageBO = deviceService.selectByPage(query);
                List<DeviceBO> deviceBOList = entityPageBO.getRecords();
                List<Long> deviceIds = deviceBOList.stream()
                        .map(DeviceBO::getId)
                        .collect(Collectors.toList());

                // 2. 一次性查所有坐标属性
                List<DeviceCoordinateAttributeBO> allAttrBOList =
                        deviceCoordinateAttributeService.selectByDeviceIds(deviceIds);
                Map<Long, List<DeviceCoordinateAttributeBO>> attrMap =
                        allAttrBOList.stream()
                                .collect(Collectors.groupingBy(DeviceCoordinateAttributeBO::getDeviceId));

                // 3. 构建 VO 并填充 coordinateAttributes
                List<DeviceVO> deviceVOList = deviceBOList.stream()
                        .map(bo -> {
                            DeviceVO vo = deviceBuilder.buildVOByBO(bo);
                            List<DeviceCoordinateAttributeBO> attrBOList =
                                    attrMap.getOrDefault(bo.getId(), Collections.emptyList());
                            List<DeviceCoordinateAttributeVO> attrVOList =
                                    builder.buildVOListByBOList(attrBOList);
                            vo.setCoordinateAttributes(attrVOList);
                            return vo;
                        })
                        .collect(Collectors.toList());

                // 4. 重新组装分页结果
                Page<DeviceVO> entityPageVO = new Page<>();
                entityPageVO.setCurrent(entityPageBO.getCurrent());
                entityPageVO.setSize(entityPageBO.getSize());
                entityPageVO.setTotal(entityPageBO.getTotal());
                entityPageVO.setRecords(deviceVOList);

                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 分页查询 Device 包含point最新值
     *
     * @param entityQuery 设备和分页参数
     * @return R Of DeviceVO Page
     */
    @PostMapping("/list_latest")
    public Mono<R<Page<DeviceLatestVO>>> latest(@RequestBody(required = false) DeviceQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DeviceQuery query = Objects.isNull(entityQuery) ? new DeviceQuery() : entityQuery;
                query.setTenantId(tenantId);
                Page<DevicePointLatestBO> entityPageBO = deviceService.selectByLatestPage(query);
                Page<DeviceLatestVO> entityPageVO = deviceBuilder.buildLatestVOPageByBOPage(entityPageBO);
                return Mono.just(R.ok(entityPageVO));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 导入 Device
     *
     * @param entityVO {@link DeviceVO}
     * @return R of String
     */
    @PostMapping("/import")
    public Mono<R<String>> importDevice(@Validated(Upload.class) DeviceVO entityVO, @RequestPart("file") Mono<FilePart> filePart) {
        return getTenantId().flatMap(tenantId -> {
            try {
                DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
                entityBO.setTenantId(tenantId);
                return filePart.flatMap(part -> {
                    String filePath = FileUtil.getTempPath() + FileUtil.getRandomXlsxName();
                    File file = new File(filePath);
                    return part.transferTo(file).then(Mono.defer(() -> {
                        deviceService.importDevice(entityBO, file);
                        return Mono.just(R.ok());
                    }));
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return Mono.just(R.fail(e.getMessage()));
            }
        });
    }

    /**
     * 下载导入模板
     *
     * @param entityVO {@link DeviceVO}
     * @return 模板文件流
     */
    @PostMapping("/export/import_template")
    public ResponseEntity<Resource> importTemplate(@Validated(Upload.class) @RequestBody DeviceVO entityVO) {
        DeviceBO entityBO = deviceBuilder.buildBOByVO(entityVO);
        Path filePath = deviceService.generateImportTemplate(entityBO);
        return ResponseUtil.responseFile(filePath);
    }

    /**
     * 驱动下设备数量
     *
     * @param driverId
     * @return
     */
    @GetMapping("/getDeviceByDriverId/{driverId}")
    public Mono<R<String>> getDeviceByDriverId(@NotNull @PathVariable(value = "driverId") Long driverId) {
        try {
            List<DeviceBO> deviceBOList = deviceService.selectByDriverId(driverId);
            return Mono.just(R.ok(String.valueOf(deviceBOList.size())));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }

}
