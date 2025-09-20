package com.aiforest.tmiot.common.manager.entity.builder;

import com.aiforest.tmiot.common.manager.entity.bo.CusNavOverviewBO;
import com.aiforest.tmiot.common.manager.entity.bo.NavFencesBO;
import com.aiforest.tmiot.common.manager.entity.bo.NavNodeBO;
import com.aiforest.tmiot.common.manager.entity.model.CusNavOverviewDO;
import com.aiforest.tmiot.common.manager.entity.model.NavFencesDO;
import com.aiforest.tmiot.common.manager.entity.model.NavNodesDO;
import com.aiforest.tmiot.common.manager.entity.vo.CusNavOverviewVO;
import com.aiforest.tmiot.common.manager.entity.vo.NavFencesVO;
import com.aiforest.tmiot.common.manager.entity.vo.NavNodeVO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CusNavBuilder {
    NavNodeBO nodeBoFromVo(NavNodeVO vo);
    List<NavNodeBO> nodeBoListFromVoList(List<NavNodeVO> list);

    /* DO -> BO */
    @Mapping(source = "navName", target = "name")
    CusNavOverviewBO boFromDo(CusNavOverviewDO entity);

    List<NavNodeBO> nodeBoListFromDoList(List<NavNodesDO> list);

    /* BO -> VO */
    CusNavOverviewVO voFromBo(CusNavOverviewBO entity);

    List<NavNodeVO> nodeVoListFromBoList(List<NavNodeBO> list);

    /* VO -> BO */
    @Mapping(source = "name", target = "navName")
    CusNavOverviewDO doFromBo(CusNavOverviewBO entity);

    List<NavNodesDO> nodeDoListFromBoList(List<NavNodeBO> list);

    /* 简化单个节点转换 */
    NavNodeBO nodeBoFromDo(NavNodesDO entity);
    NavNodeVO nodeVoFromBo(NavNodeBO entity);
    NavNodesDO nodeDoFromBo(NavNodeBO entity);

    /* ———————— Fence 专用 ———————— */
    /* DO -> BO */
    NavFencesBO fenceBoFromDo(NavFencesDO entity);
    List<NavFencesBO> fenceBoListFromDoList(List<NavFencesDO> list);

    /* BO -> VO */
    NavFencesVO fenceVoFromBo(NavFencesBO entity);
    List<NavFencesVO> fenceVoListFromBoList(List<NavFencesBO> list);

    /* VO -> BO */
    NavFencesBO fenceBoFromVo(NavFencesVO entity);
    List<NavFencesBO> fenceBoListFromVoList(List<NavFencesVO> list);

    /* BO -> DO */
    NavFencesDO fenceDoFromBo(NavFencesBO entity);
    List<NavFencesDO> fenceDoListFromBoList(List<NavFencesBO> list);

}