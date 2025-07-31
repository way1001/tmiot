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
import com.aiforest.tmiot.common.constant.common.PrefixConstant;
import com.aiforest.tmiot.common.constant.common.SuffixConstant;

/**
 * 元数据 相关常量
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
public class MetadataConstant {

    private MetadataConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 模板元数据 相关常量
     *
     * @author way
     * @version 2025.7.0
     * @since 2022.1.0
     */
    public static class Profile {

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.PROFILE;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.PROFILE;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.PROFILE;

        private Profile() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

    /**
     * 模板元数据 相关常量
     *
     * @author way
     * @version 2025.7.0
     * @since 2022.1.0
     */
    public static class Point {

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.POINT;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.POINT;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.POINT;

        private Point() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

    /**
     * 设备元数据 相关常量
     *
     * @author way
     * @version 2025.7.0
     * @since 2022.1.0
     */
    public static class Device {

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.DEVICE;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.DEVICE;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.DEVICE;

        private Device() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

    /**
     * 驱动信息元数据 相关常量
     *
     * @author way
     * @version 2025.7.0
     * @since 2022.1.0
     */
    public static class DriverConfig {

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.DRIVER_ATTRIBUTE_CONFIG;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.DRIVER_ATTRIBUTE_CONFIG;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.DRIVER_ATTRIBUTE_CONFIG;

        private DriverConfig() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

    /**
     * 位号信息元数据 相关常量
     *
     * @author way
     * @version 2025.7.0
     * @since 2022.1.0
     */
    public static class PointConfig {

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.POINT_ATTRIBUTE_CONFIG;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.POINT_ATTRIBUTE_CONFIG;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.POINT_ATTRIBUTE_CONFIG;

        private PointConfig() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

}
