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

package com.aiforest.tmiot.common.constant.driver;

import com.aiforest.tmiot.common.constant.common.ExceptionConstant;
import com.aiforest.tmiot.common.constant.common.SymbolConstant;

/**
 * 策略工厂 相关常量
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
public class StrategyConstant {

    private StrategyConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 存储相关的策略工厂 相关常量
     *
     * @author way
     * @version 2025.7.0
     * @since 2022.1.0
     */
    public static class Storage {

        public static final String REPOSITORY_PREFIX = "repository" + SymbolConstant.COLON;
        public static final String POSTGRES = "postgres";
        public static final String INFLUXDB = "influxdb";
        public static final String TDENGINE = "tdengine";
        public static final String OPENTSDB = "opentsdb";
        public static final String MONGODB = "mongodb";
        public static final String ELASTICSEARCH = "elasticsearch";

        private Storage() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

}
