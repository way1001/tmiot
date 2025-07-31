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

package com.aiforest.tmiot.common.auth.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aiforest.tmiot.common.auth.biz.DictionaryForAuthService;
import com.aiforest.tmiot.common.auth.dal.TenantManager;
import com.aiforest.tmiot.common.auth.entity.model.TenantDO;
import com.aiforest.tmiot.common.dal.entity.bo.DictionaryBO;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DictionaryForAuthServiceImpl implements DictionaryForAuthService {

    @Resource
    private TenantManager tenantManager;

    @Override
    public List<DictionaryBO> tenantDictionary() {
        LambdaQueryWrapper<TenantDO> wrapper = Wrappers.<TenantDO>query().lambda();
        wrapper.eq(TenantDO::getEnableFlag, EnableFlagEnum.ENABLE);
        List<TenantDO> entityDOList = tenantManager.list(wrapper);

        return entityDOList.stream().map(entityDO -> {
            DictionaryBO driverDictionary = new DictionaryBO();
            driverDictionary.setLabel(entityDO.getTenantName());
            driverDictionary.setValue(entityDO.getId().toString());
            return driverDictionary;
        }).toList();
    }

}
