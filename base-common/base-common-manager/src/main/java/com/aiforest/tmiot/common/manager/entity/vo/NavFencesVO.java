package com.aiforest.tmiot.common.manager.entity.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavFencesVO {
    @JsonProperty("fences_id")
    private Long id;

    @JsonProperty("fence_order")
    private Integer fencesOrder;

    @JsonProperty("grid_x1")
    private Integer gridX1;

    @JsonProperty("grid_x2")
    private Integer gridX2;

    @JsonProperty("grid_y1")
    private Integer gridY1;

    @JsonProperty("grid_y2")
    private Integer gridY2;
}