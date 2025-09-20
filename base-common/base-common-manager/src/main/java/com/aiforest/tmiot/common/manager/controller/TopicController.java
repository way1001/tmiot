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


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.common.base.BaseController;
import com.aiforest.tmiot.common.constant.service.ManagerConstant;
import com.aiforest.tmiot.common.entity.R;
import com.aiforest.tmiot.common.manager.entity.query.TopicQuery;
import com.aiforest.tmiot.common.manager.entity.vo.TopicVO;
import com.aiforest.tmiot.common.manager.service.TopicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;


@Slf4j
@RestController
@RequestMapping(ManagerConstant.TOPIC_URL_PREFIX)
public class TopicController implements BaseController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @PostMapping("/list")
    public Mono<R<Page<List<TopicVO>>>> query(@RequestBody(required = false) TopicQuery topicQuery) {
        try {
            Page<List<TopicVO>> topicVOList = topicService.query(topicQuery);
            return Mono.just(R.ok(topicVOList));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Mono.just(R.fail(e.getMessage()));
        }
    }
}
