package com.aiforest.tmiot.common.data.entity.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NavPathPoint {
    private int gridX;
    private int gridY;
    private int angle;   // 0,90,180,-90
}