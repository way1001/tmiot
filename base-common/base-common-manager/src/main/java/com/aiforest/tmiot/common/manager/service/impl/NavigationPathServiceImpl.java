package com.aiforest.tmiot.common.manager.service.impl;

import com.aiforest.tmiot.common.manager.dal.NavFencesManager;
import com.aiforest.tmiot.common.manager.dal.NavNodesManager;
import com.aiforest.tmiot.common.manager.entity.model.NavFencesDO;
import com.aiforest.tmiot.common.manager.entity.model.NavNodesDO;
import com.aiforest.tmiot.common.manager.entity.vo.NavPathPoint;
import com.aiforest.tmiot.common.manager.service.NavigationPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NavigationPathServiceImpl implements NavigationPathService {

    private final NavNodesManager navNodesService;
    private final NavFencesManager navFencesService;

    /**
     * 路径规划方法（新增禁行节点参数）
     * @param overviewId 场景ID
     * @param from 起点节点ID
     * @param to 终点节点ID
     * @param forbiddenNodeIds 禁行节点ID集合（新增参数）
     * @return 规划后的路径点列表
     */
    public List<NavPathPoint> planPath(Long overviewId, Long from, Long to, Set<Long> forbiddenNodeIds) {
    /* 与原来 Controller 中的 planPath 逻辑完全一致，这里仅把最外层提取出来 */
        /* 1. 查询基础可通行节点 */
        Set<String> allowed = navNodesService.lambdaQuery()
                .eq(NavNodesDO::getOverviewId, overviewId)
                .eq(NavNodesDO::getDeleted, 0)
                .list()
                .stream()
                .map(n -> n.getGridX() + "," + n.getGridY())
                .collect(Collectors.toSet());

        /* 2. 处理禁行节点 - 从可通行集合中移除 */
        if (forbiddenNodeIds != null && !forbiddenNodeIds.isEmpty()) {
            // 查询禁行节点对应的格点
            List<NavNodesDO> forbiddenNodes = navNodesService.lambdaQuery()
                    .in(NavNodesDO::getId, forbiddenNodeIds)
                    .eq(NavNodesDO::getOverviewId, overviewId)
                    .eq(NavNodesDO::getDeleted, 0)
                    .list();

            // 提取禁行格点的key并从allowed中移除
            Set<String> forbiddenKeys = forbiddenNodes.stream()
                    .map(n -> n.getGridX() + "," + n.getGridY())
                    .collect(Collectors.toSet());
            allowed.removeAll(forbiddenKeys);
        }

        List<NavFencesDO> fences = navFencesService.lambdaQuery()
                .eq(NavFencesDO::getOverviewId, overviewId)
                .eq(NavFencesDO::getDeleted, 0)
                .list();

        NavNodesDO start = navNodesService.getById(from);
        NavNodesDO end   = navNodesService.getById(to);
        if (start == null || end == null) {
            throw new RuntimeException("起点或终点不存在");
        }
        String sKey = start.getGridX() + "," + start.getGridY();
        String eKey = end.getGridX()   + "," + end.getGridY();
        if (!allowed.contains(sKey) || !allowed.contains(eKey)) {
            throw new RuntimeException("起点或终点不在可通行格点内");
        }

        return bfsWithFences(start, end, allowed, fences);
    }

//    // 兼容原有调用，默认不传禁行节点
//    public List<NavPathPoint> planPath(Long overviewId, Long from, Long to) {
//        return planPath(overviewId, from, to, Collections.emptySet());
//    }


    /* ================= 考虑围栏的BFS算法 ================= */
    private List<NavPathPoint> bfsWithFences(NavNodesDO from,
                                             NavNodesDO to,
                                             Set<String> allowed,
                                             List<NavFencesDO> fences) {

        /* 1. 补全可通行点 */
        Set<String> fullAllowed = new HashSet<>(allowed);
        fillGaps(fullAllowed);

        /* 2. BFS初始化 */
        int sx = from.getGridX(), sy = from.getGridY();
        int tx = to.getGridX(),   ty = to.getGridY();

        // 四个方向：上、右、下、左
        int[] dx = {0, 1, 0, -1};
        int[] dy = {-1, 0, 1, 0};

        Queue<int[]> q = new LinkedList<>();
        Map<String, int[]> prev = new HashMap<>();
        q.offer(new int[]{sx, sy});
        prev.put(sx + "," + sy, null);

        boolean found = false;
        while (!q.isEmpty() && !found) {
            int[] cur = q.poll();
            int x = cur[0], y = cur[1];

            for (int d = 0; d < 4; d++) {
                int nx = x + dx[d], ny = y + dy[d];
                String key = nx + "," + ny;

                // 检查是否可通行且未访问过
                if (!fullAllowed.contains(key) || prev.containsKey(key)) {
                    continue;
                }

                // 检查移动路径是否穿过围栏
                if (crossesAnyFence(x, y, nx, ny, fences)) {
                    continue;
                }

                prev.put(key, new int[]{x, y, d});

                // 到达终点
                if (nx == tx && ny == ty) {
                    found = true;
                    break;
                }

                q.offer(new int[]{nx, ny});
            }
        }

        if (!found) {
            throw new RuntimeException("无可通行路径（被围栏阻挡）");
        }

        /* 3. 回溯生成路径 */
        LinkedList<int[]> route = new LinkedList<>();
        int x = tx, y = ty;
        while (true) {
            route.addFirst(new int[]{x, y});
            int[] p = prev.get(x + "," + y);
            if (p == null) break;
            x = p[0];
            y = p[1];
        }

        List<NavPathPoint> path = new ArrayList<>();
        for (int i = 0; i < route.size(); i++) {
            int cx = route.get(i)[0], cy = route.get(i)[1];
            int angle = 0;
            if (i < route.size() - 1) {
                int nx = route.get(i + 1)[0], ny = route.get(i + 1)[1];
                if (nx > cx) angle = 90;
                else if (nx < cx) angle = -90;
                else if (ny > cy) angle = -180;
                else angle = 0;
            }
            path.add(NavPathPoint.builder()
                    .gridX(cx)
                    .gridY(cy)
                    .angle(angle)
                    .build());
        }
        return path;
    }

    /* ========= 检查两点之间的线段是否穿过任何围栏 ========= */
    private boolean crossesAnyFence(int x1, int y1, int x2, int y2, List<NavFencesDO> fences) {
        for (NavFencesDO fence : fences) {
            // 围栏的两个端点
            int fx1 = fence.getGridX1();
            int fy1 = fence.getGridY1();
            int fx2 = fence.getGridX2();
            int fy2 = fence.getGridY2();

            // 检查当前线段(x1,y1)-(x2,y2)是否与围栏线段(fx1,fy1)-(fx2,fy2)相交
            if (doLineSegmentsIntersect(x1, y1, x2, y2, fx1, fy1, fx2, fy2)) {
                return true;
            }
        }
        return false;
    }

    /* ========= 线段相交检测算法 ========= */
    private boolean doLineSegmentsIntersect(int x1, int y1, int x2, int y2,
                                            int x3, int y3, int x4, int y4) {
        // 计算四个方向值
        int d1 = direction(x3, y3, x4, y4, x1, y1);
        int d2 = direction(x3, y3, x4, y4, x2, y2);
        int d3 = direction(x1, y1, x2, y2, x3, y3);
        int d4 = direction(x1, y1, x2, y2, x4, y4);

        // 一般情况：线段相交
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
                ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }

        // 特殊情况：点在线段上
        if (d1 == 0 && onSegment(x3, y3, x4, y4, x1, y1)) return true;
        if (d2 == 0 && onSegment(x3, y3, x4, y4, x2, y2)) return true;
        if (d3 == 0 && onSegment(x1, y1, x2, y2, x3, y3)) return true;
        if (d4 == 0 && onSegment(x1, y1, x2, y2, x4, y4)) return true;

        return false;
    }

    /* ========= 计算方向 ========= */
    private int direction(int x1, int y1, int x2, int y2, int x3, int y3) {
        return (x3 - x1) * (y2 - y1) - (y3 - y1) * (x2 - x1);
    }

    /* ========= 检查点是否在线段上 ========= */
    private boolean onSegment(int x1, int y1, int x2, int y2, int x3, int y3) {
        return x3 <= Math.max(x1, x2) && x3 >= Math.min(x1, x2) &&
                y3 <= Math.max(y1, y2) && y3 >= Math.min(y1, y2);
    }

    /* ========= 自动补全：把间距 (1,3] 的缺失格填上 ========= */
    private void fillGaps(Set<String> allowed) {
        List<int[]> points = allowed.stream()
                .map(s -> {
                    String[] xy = s.split(",");
                    return new int[]{Integer.parseInt(xy[0]),
                            Integer.parseInt(xy[1])};
                })
                .collect(Collectors.toList());

        for (int i = 0; i < points.size(); i++) {
            int[] p1 = points.get(i);
            for (int j = i + 1; j < points.size(); j++) {
                int[] p2 = points.get(j);
                int dx = Math.abs(p1[0] - p2[0]);
                int dy = Math.abs(p1[1] - p2[1]);

                /* 只处理同一行或同一列，且间距 2~3 的 */
                if (dx == 0 && dy > 1 && dy <= 3) {
                    int y1 = Math.min(p1[1], p2[1]);
                    int y2 = Math.max(p1[1], p2[1]);
                    for (int y = y1 + 1; y < y2; y++) {
                        allowed.add(p1[0] + "," + y);
                    }
                } else if (dy == 0 && dx > 1 && dx <= 3) {
                    int x1 = Math.min(p1[0], p2[0]);
                    int x2 = Math.max(p1[0], p2[0]);
                    for (int x = x1 + 1; x < x2; x++) {
                        allowed.add(x + "," + p1[1]);
                    }
                }
            }
        }
    }
}
