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

package com.aiforest.tmiot.common.data.controller;

import com.aiforest.tmiot.common.base.BaseController;
import com.aiforest.tmiot.common.constant.service.DataConstant;
import com.aiforest.tmiot.common.data.biz.PointValueCommandService;
import com.aiforest.tmiot.common.data.entity.vo.PointValueReadVO;
import com.aiforest.tmiot.common.data.entity.vo.PointValueWriteVO;
import com.aiforest.tmiot.common.entity.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * PointValue Controller
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.POINT_VALUE_COMMAND_URL_PREFIX)
public class PointValueCommandController implements BaseController {

    private final PointValueCommandService pointValueCommandService;

    public PointValueCommandController(PointValueCommandService pointValueCommandService) {
        this.pointValueCommandService = pointValueCommandService;
    }

    /**
     * 读指令
     *
     * @param entityVO PointValueReadVO
     * @return PointValue
     */
    @PostMapping("/read")
    public Mono<R<Boolean>> read(@Validated @RequestBody PointValueReadVO entityVO) {
        try {
            pointValueCommandService.read(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
        return Mono.just(R.ok());
    }

    /**
     * 写指令
     *
     * @param entityVO PointValueWriteVO
     * @return PointValue
     */
    @PostMapping("/write")
    public Mono<R<Boolean>> write(@Validated @RequestBody PointValueWriteVO entityVO) {
        try {
            pointValueCommandService.write(entityVO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
        return Mono.just(R.ok());
    }

}