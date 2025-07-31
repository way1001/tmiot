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

package com.aiforest.tmiot.common.gateway.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.aiforest.tmiot.api.center.auth.*;
import com.aiforest.tmiot.common.constant.common.RequestConstant;
import com.aiforest.tmiot.common.constant.service.AuthConstant;
import com.aiforest.tmiot.common.entity.common.RequestHeader;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.exception.UnAuthorizedException;
import com.aiforest.tmiot.common.gateway.service.FilterService;
import com.aiforest.tmiot.common.utils.JsonUtil;
import com.aiforest.tmiot.common.utils.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class FilterServiceImpl implements FilterService {

    @GrpcClient(AuthConstant.SERVICE_NAME)
    private UserLoginApiGrpc.UserLoginApiBlockingStub userLoginApiBlockingStub;
    @GrpcClient(AuthConstant.SERVICE_NAME)
    private UserApiGrpc.UserApiBlockingStub userApiBlockingStub;
    @GrpcClient(AuthConstant.SERVICE_NAME)
    private TenantApiGrpc.TenantApiBlockingStub tenantApiBlockingStub;
    @GrpcClient(AuthConstant.SERVICE_NAME)
    private TokenApiGrpc.TokenApiBlockingStub tokenApiBlockingStub;

    @Override
    public GrpcRTenantDTO getTenantDTO(ServerHttpRequest request) {
        // Get tenant code from request header
        String tenant = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TENANT);
        if (CharSequenceUtil.isEmpty(tenant)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        // Query tenant info by tenant code
        GrpcRTenantDTO entityDTO = tenantApiBlockingStub.selectByCode(GrpcCodeQuery.newBuilder().setCode(tenant).build());
        if (!entityDTO.getResult().getOk() || EnableFlagEnum.ENABLE.getIndex() != entityDTO.getData().getEnableFlag()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        return entityDTO;
    }

    @Override
    public GrpcRUserLoginDTO getLoginDTO(ServerHttpRequest request) {
        // Get user login name from request header
        String user = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_LOGIN);
        if (CharSequenceUtil.isEmpty(user)) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        // Query user login info by login name
        GrpcRUserLoginDTO entityDTO = userLoginApiBlockingStub.selectByName(GrpcNameQuery.newBuilder().setName(user).build());
        if (!entityDTO.getResult().getOk() || EnableFlagEnum.ENABLE.getIndex() != entityDTO.getData().getEnableFlag()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
        return entityDTO;
    }

    @Override
    public RequestHeader.UserHeader getUserDTO(GrpcRUserLoginDTO rUserLoginDTO, GrpcRTenantDTO rTenantDTO) {
        // Query user info by user id
        GrpcRUserDTO entityDTO = userApiBlockingStub.selectById(GrpcIdQuery.newBuilder().setId(rUserLoginDTO.getData().getBase().getId()).build());
        if (!entityDTO.getResult().getOk()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        // Build user header info
        RequestHeader.UserHeader entityBO = new RequestHeader.UserHeader();
        entityBO.setUserId(entityDTO.getData().getBase().getId());
        entityBO.setNickName(entityDTO.getData().getNickName());
        entityBO.setUserName(entityDTO.getData().getUserName());
        entityBO.setTenantId(rTenantDTO.getData().getBase().getId());
        return entityBO;
    }

    @Override
    public void checkValid(ServerHttpRequest request, GrpcRTenantDTO rTenantDTO, GrpcRUserLoginDTO rUserLoginDTO) {
        // Get token from request header and parse it
        String token = RequestUtil.getRequestHeader(request, RequestConstant.Header.X_AUTH_TOKEN);
        RequestHeader.TokenHeader entityBO = JsonUtil.parseObject(token, RequestHeader.TokenHeader.class);
        if (Objects.isNull(entityBO) || !CharSequenceUtil.isAllNotEmpty(entityBO.getSalt(), entityBO.getToken())) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }

        // Build login query and validate token
        GrpcLoginQuery login = GrpcLoginQuery.newBuilder()
                .setTenant(rTenantDTO.getData().getTenantCode())
                .setName(rUserLoginDTO.getData().getLoginName())
                .setSalt(entityBO.getSalt())
                .setToken(entityBO.getToken())
                .build();
        GrpcRTokenDTO entityDTO = tokenApiBlockingStub.checkValid(login);
        if (!entityDTO.getResult().getOk()) {
            throw new UnAuthorizedException(RequestConstant.Message.INVALID_REQUEST);
        }
    }
}
