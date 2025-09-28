package com.aiforest.tmiot.common.manager.service;

import com.aiforest.tmiot.common.manager.entity.vo.NavPathPoint;

import java.util.List;

public interface NavigationPathService {
    List<NavPathPoint> planPath(Long overviewId, Long from, Long to);
}
