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

package com.aiforest.tmiot.common.data.metadata;


import com.aiforest.tmiot.common.data.biz.NavigationDataService;
import com.aiforest.tmiot.common.data.entity.bo.DevicePointLatestBO;
import com.aiforest.tmiot.common.utils.JsonUtil;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 位号元数据
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public final class DevicePointsMetadata {

    /**
     * 位号元数据缓存
     * <p>
     * pointId,pointDTO
     */
    private final AsyncLoadingCache<Long, List<DevicePointLatestBO>> cache;

    private final NavigationDataService navigationDataService;

    public DevicePointsMetadata(NavigationDataService navigationDataService) {
        this.navigationDataService = navigationDataService;
        this.cache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .removalListener((key, value, cause) -> log.info("Remove key={}, value={} cache, reason is: {}", key, value, cause))
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> {
                    log.info("Load device points metadata by id: {}", key);
                    //                    log.info("Cache point metadata: {}", JsonUtil.toJsonString(pointBO));
                    return this.navigationDataService.listDevicePoints(key);
                }, executor));
    }

    /**
     * 获取缓存, 指定租户
     *
     * @param tenantId 租户ID
     * @return List<DevicePointLatestBO>
     */
    public List<DevicePointLatestBO> getCache(long tenantId) {
        try {
            CompletableFuture<List<DevicePointLatestBO>> future = cache.get(tenantId);
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to get the point cache: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 重新加载缓存, 指定租户
     *
     * @param tenantId 租户ID
     */
    public void loadCache(long tenantId) {
        CompletableFuture<List<DevicePointLatestBO>> future = CompletableFuture.supplyAsync(() -> navigationDataService.listDevicePoints(tenantId));
        cache.put(tenantId, future);
    }

    /**
     * 删除缓存, 指定租户
     *
     * @param tenantId 租户ID
     */
    public void removeCache(long tenantId) {
        cache.put(tenantId, CompletableFuture.completedFuture(null));
    }
}
