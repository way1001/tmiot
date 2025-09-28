package com.aiforest.tmiot.common.data.entity.bo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavNodeBO {
    private Long id;
    private Long overviewId;
    private Integer nodeOrder;
    private Integer gridX;
    private Integer gridY;
    private DeviceCoordinateBindBO coordinateBind;


}