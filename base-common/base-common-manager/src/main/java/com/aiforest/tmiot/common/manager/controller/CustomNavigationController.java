package com.aiforest.tmiot.common.manager.controller;

import com.aiforest.tmiot.common.base.BaseController;
import com.aiforest.tmiot.common.entity.R;
import com.aiforest.tmiot.common.manager.dal.CusNavOverviewManager;
import com.aiforest.tmiot.common.manager.dal.NavFencesManager;
import com.aiforest.tmiot.common.manager.dal.NavNodesManager;
import com.aiforest.tmiot.common.manager.entity.bo.CusNavOverviewBO;
import com.aiforest.tmiot.common.manager.entity.bo.NavNodeBO;
import com.aiforest.tmiot.common.manager.entity.builder.CusNavBuilder;
import com.aiforest.tmiot.common.manager.entity.model.CusNavOverviewDO;
import com.aiforest.tmiot.common.manager.entity.model.NavFencesDO;
import com.aiforest.tmiot.common.manager.entity.model.NavNodesDO;
import com.aiforest.tmiot.common.manager.entity.vo.CusNavOverviewVO;
import com.aiforest.tmiot.common.manager.entity.vo.NavPathPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/navigation")
@RequiredArgsConstructor
public class CustomNavigationController implements BaseController {

    private final CusNavOverviewManager cusNavOverviewService;
    private final NavNodesManager navNodesService;
    private final NavFencesManager navFencesService;
    private final CusNavBuilder builder;

    /* 列表 */
    @GetMapping
    public Mono<R<List<Map<String, Object>>>> listScenes() {
        return Mono.just(R.ok(cusNavOverviewService.lambdaQuery()
                .select(CusNavOverviewDO::getId, CusNavOverviewDO::getNavName)
                .list()
                .stream()
                .map(o -> Map.<String, Object>of("id", o.getId(), "name", o.getNavName()))
                .toList()));
    }

    /* 查询单条 */
    @GetMapping("/{id}")
    public Mono<R<CusNavOverviewVO>> loadScene(@PathVariable Long id) {
        CusNavOverviewDO overview = cusNavOverviewService.getById(id);
        if (overview == null) {
            throw new RuntimeException("场景不存在");
        }
        List<NavNodesDO> nodeDos = navNodesService.lambdaQuery()
                .eq(NavNodesDO::getOverviewId, id)
                .orderByAsc(NavNodesDO::getNodeOrder)
                .list();

        List<NavFencesDO> fencesDos = navFencesService.lambdaQuery()
                .eq(NavFencesDO::getOverviewId, id)
                .orderByAsc(NavFencesDO::getFenceOrder)
                .list();

        CusNavOverviewBO bo = builder.boFromDo(overview);
        bo.setNodes(builder.nodeBoListFromDoList(nodeDos));
        bo.setFences(builder.fenceBoListFromDoList(fencesDos));

        return Mono.just(R.ok(builder.voFromBo(bo)));
    }

    /* 新建或更新 */
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Mono<R<Map<String, Long>>> createScene(@RequestBody @Validated CusNavOverviewVO vo) {
        return saveSceneInternal(vo, null);
    }

    /* -------------------- 更新 -------------------- */
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Mono<R<Map<String, Long>>> updateScene(@RequestBody @Validated CusNavOverviewVO vo,
                                                  @PathVariable Long id) {
        return saveSceneInternal(vo, id);
    }

    /* 公共逻辑 */
    private Mono<R<Map<String, Long>>> saveSceneInternal(CusNavOverviewVO vo, Long id) {
        CusNavOverviewBO bo = CusNavOverviewBO.builder()
                .id(id)                // 新建时 id 为 null
                .name(vo.getName())
                .widthPx(vo.getWidthPx())
                .heightPx(vo.getHeightPx())
                .originXPx(vo.getOriginXPx())
                .originYPx(vo.getOriginYPx())
                .nodes(builder.nodeBoListFromVoList(vo.getNodes()))
                .fences(builder.fenceBoListFromVoList(vo.getFences()))
                .build();

        CusNavOverviewDO overview = builder.doFromBo(bo);
        cusNavOverviewService.saveOrUpdate(overview);
        Long oid = overview.getId();

        // 节点增量更新逻辑
        handleNodesIncrementalUpdate(bo.getNodes(), oid, id != null);

        // 先删旧节点
        navFencesService.lambdaUpdate()
                .eq(NavFencesDO::getOverviewId, oid)
                .remove();
        // 再插入新节点
        if (bo.getFences() != null && !bo.getFences().isEmpty()) {
            List<NavFencesDO> fencesDOS = bo.getFences().stream()
                    .map(builder::fenceDoFromBo)
                    .peek(n -> n.setOverviewId(oid))
                    .toList();
            navFencesService.saveBatch(fencesDOS);
        }
        return Mono.just(R.ok(Map.of("id", oid)));
    }

    /**
     * 节点增量更新（新增、更新、删除）
     * @param newNodes 新节点列表
     * @param overviewId 场景ID
     * @param isUpdate 是否为更新操作（true:更新场景，false:新建场景）
     */
    private void handleNodesIncrementalUpdate(List<NavNodeBO> newNodes, Long overviewId, boolean isUpdate) {
        // 新节点转换为DO并设置场景ID
        List<NavNodesDO> newNodeDos = Optional.ofNullable(newNodes)
                .orElse(Collections.emptyList())
                .stream()
                .map(builder::nodeDoFromBo)
                .peek(node -> node.setOverviewId(overviewId))
                .collect(Collectors.toList());

        if (!isUpdate) {
            // 新建场景：直接保存所有节点
            if (!newNodeDos.isEmpty()) {
                navNodesService.saveBatch(newNodeDos);
            }
            return;
        }

        // 更新场景：增量处理
        // 1. 查询旧节点
        List<NavNodesDO> oldNodeDos = navNodesService.lambdaQuery()
                .eq(NavNodesDO::getOverviewId, overviewId)
                .list();

        // 2. 提取新旧节点的业务唯一标识（假设用nodeKey作为唯一标识）
        Map<Long, NavNodesDO> oldNodeMap = oldNodeDos.stream()
                .collect(Collectors.toMap(NavNodesDO::getId, Function.identity()));
        Set<Long> newNodeKeys = newNodeDos.stream()
                .map(NavNodesDO::getId)
                .collect(Collectors.toSet());

        // 3. 待删除节点：旧节点中不存在于新节点的部分
        List<Long> deleteIds = oldNodeDos.stream()
                .filter(old -> !newNodeKeys.contains(old.getId()))
                .map(NavNodesDO::getId)
                .collect(Collectors.toList());
        if (!deleteIds.isEmpty()) {
            navNodesService.removeByIds(deleteIds);
        }

        // 4. 待更新和新增节点处理
        List<NavNodesDO> toSaveOrUpdate = new ArrayList<>();
        for (NavNodesDO newNode : newNodeDos) {
            NavNodesDO oldNode = oldNodeMap.get(newNode.getId());
            if (oldNode != null) {
                // 存在旧节点：复用ID进行更新
                newNode.setId(oldNode.getId());
            }
            toSaveOrUpdate.add(newNode);
        }

        if (!toSaveOrUpdate.isEmpty()) {
            // 批量保存（存在ID则更新，不存在则新增）
            navNodesService.saveOrUpdateBatch(toSaveOrUpdate);
        }
    }


//    /* 对外接口：GET /navigation/{id}/path?from={nodeId}&to={nodeId} */
//    @GetMapping("/{id}/path")
//    public Mono<R<List<NavPathPoint>>> planPath(@PathVariable Long id,
//                                                @RequestParam Long from,
//                                                @RequestParam Long to) {
//
//        /* 1. 场景下所有节点坐标 -> 作为“可通行”集合 allowed */
//        Set<String> allowed = navNodesService.lambdaQuery()
//                .eq(NavNodesDO::getOverviewId, id)
//                .eq(NavNodesDO::getDeleted, 0)
//                .list()
//                .stream()
//                .map(n -> n.getGridX() + "," + n.getGridY())
//                .collect(Collectors.toSet());
//
//        /* 2. 起点 / 终点必须也在 allowed 中，否则直接返回 */
//        NavNodesDO start = navNodesService.getById(from);
//        NavNodesDO end   = navNodesService.getById(to);
//        if (start == null || end == null) {
//            return Mono.error(new RuntimeException("起点或终点不存在"));
//        }
//        String sKey = start.getGridX() + "," + start.getGridY();
//        String eKey = end.getGridX()   + "," + end.getGridY();
//        if (!allowed.contains(sKey) || !allowed.contains(eKey)) {
//            return Mono.error(new RuntimeException("起点或终点不在可通行格点内"));
//        }
//
//        /* 3. 计算仅经过 allowed 的最短曼哈顿路径 */
//        List<NavPathPoint> path = bfsManhattan(start, end, allowed);
//        return Mono.just(R.ok(path));
//    }
//
//    /* ================= 只允许 allowed 中坐标的 BFS ================= */
//    private List<NavPathPoint> bfsManhattan(NavNodesDO from,
//                                            NavNodesDO to,
//                                            Set<String> allowed) {
//
//        /* 1. 先把 allowed 复制出来，再往里补全 */
//        Set<String> fullAllowed = new HashSet<>(allowed);
//        fillGaps(fullAllowed);          // 关键：自动补全
//
//        /* 2. 以下 BFS 逻辑不变，只是用的集合换成 fullAllowed */
//        int sx = from.getGridX(), sy = from.getGridY();
//        int tx = to.getGridX(),   ty = to.getGridY();
//
//        int[] dx = {0, 1, 0, -1};
//        int[] dy = {-1, 0, 1, 0};
//
//        Queue<int[]> q = new LinkedList<>();
//        Map<String, int[]> prev = new HashMap<>();
//        q.offer(new int[]{sx, sy});
//        prev.put(sx + "," + sy, null);
//
//        boolean found = false;
//        while (!q.isEmpty() && !found) {
//            int[] cur = q.poll();
//            int x = cur[0], y = cur[1];
//
//            for (int d = 0; d < 4; d++) {
//                int nx = x + dx[d], ny = y + dy[d];
//                String key = nx + "," + ny;
//                if (!fullAllowed.contains(key) || prev.containsKey(key)) continue;
//
//                prev.put(key, new int[]{x, y, d});
//                if (nx == tx && ny == ty) {
//                    found = true;
//                    break;
//                }
//                q.offer(new int[]{nx, ny});
//            }
//        }
//
//        if (!found) {
//            throw new RuntimeException("无可通行路径");
//        }
//
//        /* 3. 回溯生成路径（与原来一致） */
//        LinkedList<int[]> route = new LinkedList<>();
//        int x = tx, y = ty;
//        while (true) {
//            route.addFirst(new int[]{x, y});
//            int[] p = prev.get(x + "," + y);
//            if (p == null) break;
//            x = p[0];
//            y = p[1];
//        }
//
//        List<NavPathPoint> path = new ArrayList<>();
//        for (int i = 0; i < route.size(); i++) {
//            int cx = route.get(i)[0], cy = route.get(i)[1];
//            int angle = 0;
//            if (i < route.size() - 1) {
//                int nx = route.get(i + 1)[0], ny = route.get(i + 1)[1];
//                if (nx > cx) angle = 90;
//                else if (nx < cx) angle = -90;
//                else if (ny > cy) angle = -180;
//                else angle = 0;
//            }
//            path.add(NavPathPoint.builder()
//                    .gridX(cx)
//                    .gridY(cy)
//                    .angle(angle)
//                    .build());
//        }
//        return path;
//    }
//
//    /* ========= 自动补全：把间距 (1,3] 的缺失格填上 ========= */
//    private void fillGaps(Set<String> allowed) {
//        List<int[]> points = allowed.stream()
//                .map(s -> {
//                    String[] xy = s.split(",");
//                    return new int[]{Integer.parseInt(xy[0]),
//                            Integer.parseInt(xy[1])};
//                })
//                .collect(Collectors.toList());
//
//        for (int i = 0; i < points.size(); i++) {
//            int[] p1 = points.get(i);
//            for (int j = i + 1; j < points.size(); j++) {
//                int[] p2 = points.get(j);
//                int dx = Math.abs(p1[0] - p2[0]);
//                int dy = Math.abs(p1[1] - p2[1]);
//
//                /* 只处理同一行或同一列，且间距 2~3 的 */
//                if (dx == 0 && dy > 1 && dy <= 3) {
//                    int y1 = Math.min(p1[1], p2[1]);
//                    int y2 = Math.max(p1[1], p2[1]);
//                    for (int y = y1 + 1; y < y2; y++) {
//                        allowed.add(p1[0] + "," + y);
//                    }
//                } else if (dy == 0 && dx > 1 && dx <= 3) {
//                    int x1 = Math.min(p1[0], p2[0]);
//                    int x2 = Math.max(p1[0], p2[0]);
//                    for (int x = x1 + 1; x < x2; x++) {
//                        allowed.add(x + "," + p1[1]);
//                    }
//                }
//            }
//        }
//    }
/* 对外接口：GET /navigation/{id}/path?from={nodeId}&to={nodeId} */
@GetMapping("/{id}/path")
public Mono<R<List<NavPathPoint>>> planPath(@PathVariable Long id,
                                            @RequestParam Long from,
                                            @RequestParam Long to) {

    /* 1. 获取场景下所有节点坐标 -> 作为“可通行”集合 allowed */
    Set<String> allowed = navNodesService.lambdaQuery()
            .eq(NavNodesDO::getOverviewId, id)
            .eq(NavNodesDO::getDeleted, 0)
            .list()
            .stream()
            .map(n -> n.getGridX() + "," + n.getGridY())
            .collect(Collectors.toSet());

    /* 2. 获取场景下所有围栏 */
    List<NavFencesDO> fences = navFencesService.lambdaQuery()
            .eq(NavFencesDO::getOverviewId, id)
            .eq(NavFencesDO::getDeleted, 0)
            .list();

    /* 3. 验证起点和终点 */
    NavNodesDO start = navNodesService.getById(from);
    NavNodesDO end   = navNodesService.getById(to);
    if (start == null || end == null) {
        return Mono.error(new RuntimeException("起点或终点不存在"));
    }
    String sKey = start.getGridX() + "," + start.getGridY();
    String eKey = end.getGridX()   + "," + end.getGridY();
    if (!allowed.contains(sKey) || !allowed.contains(eKey)) {
        return Mono.error(new RuntimeException("起点或终点不在可通行格点内"));
    }

    /* 4. 计算避开围栏的最短路径 */
    List<NavPathPoint> path = bfsWithFences(start, end, allowed, fences);
    return Mono.just(R.ok(path));
}

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