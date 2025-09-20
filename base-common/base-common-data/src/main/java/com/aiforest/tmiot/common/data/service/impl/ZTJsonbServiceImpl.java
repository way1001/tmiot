package com.aiforest.tmiot.common.data.service.impl;

import com.aiforest.tmiot.common.data.entity.model.ZTJsonbDO;
import com.aiforest.tmiot.common.data.mapper.ZTJsonbMapper;
import com.aiforest.tmiot.common.data.service.ZTJsonbService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ZTJsonbServiceImpl
        extends ServiceImpl<ZTJsonbMapper, ZTJsonbDO>
        implements ZTJsonbService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveJson(JsonNode json) {
        ZTJsonbDO entity = new ZTJsonbDO();
        entity.setData(json);
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public JsonNode getJsonById(Long id) {
        return Optional.ofNullable(baseMapper.selectById(id))
                .map(ZTJsonbDO::getData)
                .orElse(null);
    }

    @Override
    public IPage<JsonNode> pageJson(long current, long size) {
        Page<ZTJsonbDO> page = Page.of(current, size);
        page.addOrder(OrderItem.desc("id"));   // 可按需要换成别的字段
        Page<ZTJsonbDO> doPage = baseMapper.selectPage(page, null);
        return doPage.convert(ZTJsonbDO::getData);
    }
}
