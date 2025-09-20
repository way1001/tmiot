package com.aiforest.tmiot.common.manager.entity.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author way
 * @since 2025.08.30
 */
@Getter
@Setter
@ToString
@TableName("tm_nav_nodes")
public class NavNodesDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联自定导航ID
     */
    @TableField("overview_id")
    private Long overviewId;

    /**
     * 节点序号
     */
    @TableField("node_order")
    private Integer nodeOrder;

    /**
     * 网格坐标 gx
     */
    @TableField("grid_x")
    private Integer gridX;

    /**
     * 网格坐标 gy
     */
    @TableField("grid_y")
    private Integer gridY;

    /**
     * 逻辑删除标识, 0:未删除, 1:已删除
     */
    @TableLogic
    @TableField("deleted")
    private Byte deleted;
}
