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

package com.aiforest.tmiot.common.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.aiforest.tmiot.common.entity.base.BaseVO;
import com.aiforest.tmiot.common.entity.ext.DeviceExt;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.valid.Add;
import com.aiforest.tmiot.common.valid.Update;
import com.aiforest.tmiot.common.valid.Upload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;
import java.util.Set;

/**
 * 设备 VO
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DeviceVO extends BaseVO {

    /**
     * 设备名称
     */
    @NotBlank(message = "设备名称不能为空",
            groups = {Add.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "设备名称格式无效",
            groups = {Add.class, Update.class})
    private String deviceName;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 驱动ID
     */
    @NotNull(message = "驱动ID不能为空",
            groups = {Add.class, Update.class, Upload.class})
    private Long driverId;

    /**
     * 设备拓展信息
     */
    private DeviceExt deviceExt;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 签名
     */
    private String signature;

    /**
     * 版本
     */
    private Integer version;

    // 附加字段
    @NotNull(message = "模版ID集不能为空",
            groups = {Upload.class})
    private Set<Long> profileIds;

    // 新增字段，用于承载该设备下的坐标属性列表
    private List<DeviceCoordinateAttributeVO> coordinateAttributes;
}
