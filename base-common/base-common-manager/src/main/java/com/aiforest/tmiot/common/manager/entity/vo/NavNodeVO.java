package com.aiforest.tmiot.common.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavNodeVO {
    @JsonProperty("node_id")
    private Long id;

    @JsonProperty("node_order")
    private Integer nodeOrder;

    @JsonProperty("grid_x")
    private Integer gridX;

    @JsonProperty("grid_y")
    private Integer gridY;
}