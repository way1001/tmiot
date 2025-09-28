package com.aiforest.tmiot.common.data.metadata;

import com.aiforest.tmiot.common.data.biz.NavOverviewDataService;
import com.aiforest.tmiot.common.data.entity.bo.CusNavOverviewBO;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public final class NavOverviewMetadata {
    /**
     navId -> NavOverviewBO
     */
    private final AsyncLoadingCache<Long, CusNavOverviewBO> cache;

    private final NavOverviewDataService navOverviewDataService;

    public NavOverviewMetadata(NavOverviewDataService navOverviewDataService) {
        this.navOverviewDataService = navOverviewDataService;
        this.cache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .removalListener((key, value, cause) ->
                        log.info("Remove navOverview cache, key={}, value={}, reason={}", key, value, cause))
                .buildAsync((navId, executor) ->
                        CompletableFuture.supplyAsync(() -> {
                            log.info("Load navOverview metadata by id: {}", navId);
                            return navOverviewDataService.getNavOverviewById(navId, 1L);
                        }, executor));
    }
    /**
     获取缓存
     @param navId 导航页ID
     @return NavOverviewBO
     */
    public CusNavOverviewBO getCache(long navId) {
        try {
            return cache.get(navId).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to get navOverview cache: {}", e.getMessage(), e);
            return null;
        }
    }
    /**
     主动刷新缓存
     @param navId 导航页ID
     */
    public void loadCache(long navId) {
        cache.put(navId, CompletableFuture.supplyAsync(() -> navOverviewDataService.getNavOverviewById(navId, 1L)));
    }
    /**
     删除缓存
     @param navId 导航页ID
     */
    public void removeCache(long navId) {
        cache.put(navId, CompletableFuture.completedFuture(null));
    }
}
