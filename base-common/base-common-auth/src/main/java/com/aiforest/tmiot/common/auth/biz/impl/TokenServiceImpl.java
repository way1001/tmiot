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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RandomUtil;
import com.aiforest.tmiot.common.auth.biz.TokenService;
import com.aiforest.tmiot.common.auth.entity.bean.TokenValid;
import com.aiforest.tmiot.common.auth.entity.bo.TenantBO;
import com.aiforest.tmiot.common.auth.entity.bo.TenantBindBO;
import com.aiforest.tmiot.common.auth.entity.bo.UserLoginBO;
import com.aiforest.tmiot.common.auth.entity.bo.UserPasswordBO;
import com.aiforest.tmiot.common.auth.service.TenantBindService;
import com.aiforest.tmiot.common.auth.service.TenantService;
import com.aiforest.tmiot.common.auth.service.UserLoginService;
import com.aiforest.tmiot.common.auth.service.UserPasswordService;
import com.aiforest.tmiot.common.constant.common.ExceptionConstant;
import com.aiforest.tmiot.common.exception.UnAuthorizedException;
import com.aiforest.tmiot.common.utils.DecodeUtil;
import com.aiforest.tmiot.common.utils.KeyUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 令牌服务接口实现类
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    @Resource
    private TenantService tenantService;
    @Resource
    private UserLoginService userLoginService;
    @Resource
    private UserPasswordService userPasswordService;
    @Resource
    private TenantBindService tenantBindService;

    @Override
    public String generateSalt(String loginName, String tenantCode) {
        TenantBO tenantBO = tenantService.selectByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        return RandomUtil.randomString(16);
    }

    @Override
    public String generateToken(String loginName, String salt, String password, String tenantCode) {
        TenantBO tenantBO = tenantService.selectByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        UserLoginBO userLogin = userLoginService.selectByLoginName(loginName, false);
        if (Objects.isNull(userLogin)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        TenantBindBO tenantBindBO = tenantBindService.selectByTenantIdAndUserId(tenantBO.getId(), userLogin.getUserId());
        if (Objects.isNull(tenantBindBO)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        UserPasswordBO userPasswordBO = userPasswordService.selectById(userLogin.getUserPasswordId());
        if (Objects.isNull(userPasswordBO)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        if (CharSequenceUtil.isEmpty(salt)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        String md5Password = DecodeUtil.md5(userPasswordBO.getLoginPassword(), salt);
        if (!md5Password.equals(password)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }
        return KeyUtil.generateToken(loginName, salt, tenantBO.getId());
    }

    @Override
    public TokenValid checkValid(String loginName, String salt, String token, String tenantCode) {
        TenantBO tenantBO = tenantService.selectByCode(tenantCode);
        if (Objects.isNull(tenantBO)) {
            throw new UnAuthorizedException(ExceptionConstant.NO_AVAILABLE_AUTH);
        }

        TokenValid tokenValid = new TokenValid(false, null);
        if (CharSequenceUtil.isBlank(token)) {
            return tokenValid;
        }

        try {
            Claims claims = KeyUtil.parserToken(loginName, salt, token, tenantBO.getId());
            tokenValid.setValid(true);
            tokenValid.setExpireTime(claims.getExpiration());
            return tokenValid;
        } catch (Exception e) {
            return tokenValid;
        }
    }

}
