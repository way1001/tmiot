package com.aiforest.tmiot.common.data.biz;


import com.aiforest.tmiot.common.data.entity.vo.NavPathPoint;

import java.util.List;

public interface NavigationPathService {
    List<NavPathPoint> planPath(long overviewId,
                                long fromNodeId,
                                long toNodeId);
}
