package com.aiforest.tmiot.common.data.biz;


import com.aiforest.tmiot.common.data.entity.vo.NavPathPoint;

import java.util.List;
import java.util.Set;

public interface NavigationPathService {
    List<NavPathPoint> planPath(long overviewId,
                                long fromNodeId,
                                long toNodeId,
                                Set<Long> forbiddenNodeIds);
}
