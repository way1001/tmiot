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

package com.aiforest.tmiot.common.auth.generator;

import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.google.common.collect.ImmutableMap;
import com.aiforest.tmiot.common.utils.MybatisUtil;

/**
 * 自动代码生成工具
 * <p>
 * 注意:
 * <p>
 * 当前配置仅用于 base-common-auth 服务模块, 如果需要用于其他模块请重新配置 path 参数。
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
public class MybatisGenerator {
    public static void main(String[] args) {
        generator();
    }

    public static void generator() {
        String path = System.getProperty("user.dir") + "/base-common/base-common-auth/src/main";
        MybatisUtil.defaultGenerator("base-postgres", "55432", "tm", "tm_auth", "tm", "tm123")
                .globalConfig(builder -> MybatisUtil.defaultGlobalConfig(builder, path))
                .dataSourceConfig(MybatisUtil::defaultDataSourceConfig)
                .packageConfig(builder -> builder
                        .parent("com.aiforest.tmiot.common.auth")
                        .entity("entity.model")
                        .service("dal")
                        .serviceImpl("dal.impl")
                        .mapper("mapper")
                        .pathInfo(ImmutableMap.of(
                                OutputFile.service, path + "/java/com/aiforest/tmiot/common/auth/dal",
                                OutputFile.serviceImpl, path + "/java/com/aiforest/tmiot/common/auth/dal/impl",
                                OutputFile.xml, path + "/resources/mapping"))
                )
                .templateEngine(new FreemarkerTemplateEngine())
                .strategyConfig(MybatisUtil::defaultStrategyConfig)
                .strategyConfig(builder -> builder
                        .addInclude(
                                "tm_api",
                                "tm_menu",
                                "tm_resource",
                                "tm_role",
                                "tm_role_resource_bind",
                                "tm_role_user_bind",
                                "tm_driver_token",
                                "tm_tenant",
                                "tm_tenant_bind",
                                "tm_user",
                                "tm_user_login",
                                "tm_user_password"
                        )
                ).execute();
    }
}