package com.aiforest.tmiot.common.data.service;

import com.aiforest.tmiot.common.data.entity.model.ZTJsonbDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.databind.JsonNode;

public interface ZTJsonbService extends IService<ZTJsonbDO> {

    /** 新增一条记录，直接落库 */
    Long saveJson(JsonNode json);

    /** 根据主键查询单条记录 */
    JsonNode getJsonById(Long id);

    /** 分页查询 JSON 列表 */
    IPage<JsonNode> pageJson(long current, long size);
}
