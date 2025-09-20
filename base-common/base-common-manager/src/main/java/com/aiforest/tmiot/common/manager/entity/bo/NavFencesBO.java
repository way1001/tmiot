package com.aiforest.tmiot.common.manager.entity.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavFencesBO {
    private Long id;
    private Integer fencesOrder;
    private Integer gridX1;
    private Integer gridX2;
    private Integer gridY1;
    private Integer gridY2;
}