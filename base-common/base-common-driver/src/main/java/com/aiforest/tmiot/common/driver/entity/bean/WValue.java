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

package com.aiforest.tmiot.common.driver.entity.bean;

import cn.hutool.core.text.CharSequenceUtil;
import com.aiforest.tmiot.common.enums.PointTypeFlagEnum;
import com.aiforest.tmiot.common.exception.EmptyException;
import com.aiforest.tmiot.common.exception.TypeException;
import com.aiforest.tmiot.common.exception.UnSupportException;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 写数据实体类
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class WValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 值, string, 需要根据type确定真实的数据类型
     */
    private String value;

    /**
     * 类型, value type, 用于确定value的真实类型
     * <p>
     * 同位号数据类型一致
     */
    private PointTypeFlagEnum type;

    private Integer bitwise;

    private String pointCode;

    /**
     * 根据类型转换数据
     *
     * @param clazz T Class
     * @param <T>   T
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(Class<T> clazz) {
        if (Objects.isNull(type)) {
            throw new UnSupportException("Unsupported point type of " + type);
        }
        if (CharSequenceUtil.isEmpty(value)) {
            throw new EmptyException("Point value is empty");
        }
        if (Objects.isNull(bitwise)) {
            throw new EmptyException("Point bitwise is null");
        }

        final String message = "Point type is: {}, can't be cast to class: {}";
        return switch (type) {
            case STRING -> {
                if (!clazz.equals(String.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) value;
            }
            case BYTE -> {
                if (!clazz.equals(Byte.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Byte.valueOf(value);
            }
            case SHORT -> {
                if (!clazz.equals(Short.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Short.valueOf(value);
            }
            case INT -> {
                if (!clazz.equals(Integer.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Integer.valueOf(value);
            }
            case LONG -> {
                if (!clazz.equals(Long.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Long.valueOf(value);
            }
            case FLOAT -> {
                if (!clazz.equals(Float.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Float.valueOf(value);
            }
            case DOUBLE -> {
                if (!clazz.equals(Double.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                yield (T) Double.valueOf(value);
            }
            case BOOLEAN -> {
                if (!clazz.equals(Boolean.class)) {
                    throw new TypeException(message, type.getCode(), clazz.getName());
                }
                // 将布尔值转换为整数表示（true=1, false=0）
                int booleanAsInt = Boolean.valueOf(value) ? 1 : 0;

                // 与bitwise进行位操作（这里以按位与为例，可根据需求替换为&、|、^等）
                int result = booleanAsInt << bitwise;

                // 转换为Integer类型
                yield (T) Integer.valueOf(result);
                //yield (T) Boolean.valueOf(value);
            }
        };
    }
}
