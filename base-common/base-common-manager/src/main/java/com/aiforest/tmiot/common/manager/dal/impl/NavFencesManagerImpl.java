package com.aiforest.tmiot.common.manager.dal.impl;

import com.aiforest.tmiot.common.manager.entity.model.NavFencesDO;
import com.aiforest.tmiot.common.manager.mapper.NavFencesMapper;
import com.aiforest.tmiot.common.manager.dal.NavFencesManager;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 导航围栏表：存储导航场景中绘制的水平/垂直围栏直线信息 服务实现类
 * </p>
 *
 * @author way
 * @since 2025.09.19
 */
@Service
public class NavFencesManagerImpl extends ServiceImpl<NavFencesMapper, NavFencesDO> implements NavFencesManager {

}
