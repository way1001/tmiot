package com.aiforest.tmiot.common.data.entity.bo;

import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private EnableFlagEnum enableFlag;

    /** 子节点 */
    private List<NavNodeBO> navNodes;
}