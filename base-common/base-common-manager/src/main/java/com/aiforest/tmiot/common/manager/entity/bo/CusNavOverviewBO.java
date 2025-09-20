package com.aiforest.tmiot.common.manager.entity.bo;

import lombok.*;

import java.util.List;

/**
 * Custom Navigation Overview BO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CusNavOverviewBO {
    private Long id;
    private String name;
    private Integer widthPx;
    private Integer heightPx;
    private Integer originXPx;
    private Integer originYPx;

    /** 子节点 */
    private List<NavNodeBO> nodes;
    private List<NavFencesBO> fences;
}