package com.aiforest.tmiot.common.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CusNavOverviewVO {
    private Long id;
    @JsonProperty("name")
    private String name;

    @JsonProperty("width_px")
    private Integer widthPx;

    @JsonProperty("height_px")
    private Integer heightPx;

    @JsonProperty("origin_x_grid")
    private Integer originXPx;

    @JsonProperty("origin_y_grid")
    private Integer originYPx;

    @JsonProperty("nodes")
    private List<NavNodeVO> nodes;

    @JsonProperty("fences")
    private List<NavFencesVO> fences;
}