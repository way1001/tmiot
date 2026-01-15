package com.aiforest.tmiot.common.manager.service;

import com.aiforest.tmiot.common.manager.entity.vo.NavPathPoint;

import java.util.List;
import java.util.Set;

public interface NavigationPathService {
    List<NavPathPoint> planPath(Long overviewId, Long from, Long to, Set<Long> forbiddenNodeIds);
}
