package com.aiforest.tmiot.common.manager.entity.bo;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavNodeBO {
    private Long id;
    private Integer nodeOrder;
    private Integer gridX;
    private Integer gridY;
}