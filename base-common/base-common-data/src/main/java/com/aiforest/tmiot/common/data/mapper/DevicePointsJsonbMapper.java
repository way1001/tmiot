/*
 * Copyright 2016-present the TM IoT original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aiforest.tmiot.common.data.mapper;

import com.aiforest.tmiot.common.data.entity.bo.DevicePointJsonbBattVBO;
import com.aiforest.tmiot.common.data.entity.dto.HistoryPageDTO;
import com.aiforest.tmiot.common.data.entity.model.DevicePointsJsonbDO;
import com.aiforest.tmiot.common.data.entity.query.DeviceTimeRangeQuery;
import com.aiforest.tmiot.common.data.entity.vo.DeviceSnapshotVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 规则表 Mapper 接口
 * </p>
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
public interface DevicePointsJsonbMapper extends BaseMapper<DevicePointsJsonbDO> {
    /** 自动映射 VO，无需 ResultMap，无需 MapStruct */
//    @Select("SELECT " +
//            "  snapshot ->> 'deviceCode'   AS device_code," +
//            "  snapshot ->> 'deviceName'   AS device_name," +
//            "  snapshot ->> 'status'       AS status," +
//            "  (snapshot -> 'lspeed' ->> 'calValue')::int AS left_speed," +
//            "  (snapshot -> 'rspeed' ->> 'calValue')::int AS right_speed," +
//            "  (snapshot -> 'basicState' ->> 'calValue')::int AS basic_state," +
//            "  (snapshot -> 'workStatus' ->> 'calValue')::int AS work_status," +
//            "  (snapshot -> 'operatingMode' ->> 'calValue')::int AS operating_mode," +
//            "  TO_CHAR(create_time, 'YYYY-MM-DD HH24:MI:SS') AS create_time " +
//            "FROM t_device_ponits_jsonb " +
//            "WHERE device_id = #{dto.deviceId} " +
//            "  AND create_time BETWEEN COALESCE(#{dto.start}, '-infinity') AND COALESCE(#{dto.end}, 'infinity') " +
//            "ORDER BY create_time DESC")
//    IPage<DeviceSnapshotVO> selectVoPage(Page<?> page,
//                                         @Param("dto") HistoryPageDTO dto);
//
//    @Select("""
//        SELECT * FROM t_device_ponits_jsonb
//        WHERE create_time BETWEEN #{start} AND #{end}
//        ORDER BY create_time DESC
//        """)
//    IPage<DevicePointsJsonbDO> selectByTime(Page<DevicePointsJsonbDO> page,
//                                            @Param("start") LocalDateTime start,
//                                            @Param("end") LocalDateTime end);
    /** 1. 各设备任务数量 */
    @Select({
            "<script>",
            "SELECT device_id, COUNT(*) AS cnt",
            "FROM t_device_points_jsonb",
            "WHERE device_id IN",
            "<foreach collection='dto.deviceIds' item='d' open='(' separator=',' close=')'>",
            "#{d}",
            "</foreach>",
            "AND create_time BETWEEN #{dto.start} AND #{dto.end}",
            "GROUP BY device_id",
            "</script>"
    })
    List<Map<String, Object>> countTasksBatch(@Param("dto") DeviceTimeRangeQuery dto);

    /** 2. 各设备导航路径段数 */
    @Select({
            "<script>",
            "SELECT device_id,",
            "COALESCE(SUM(array_length(string_to_array(snapshot->'nav_route'->>'calValue', ';'), 1)), 0) AS cnt",
            "FROM t_device_points_jsonb",
            "WHERE device_id IN",
            "<foreach collection='dto.deviceIds' item='d' open='(' separator=',' close=')'>",
            "#{d}",
            "</foreach>",
            "AND create_time BETWEEN #{dto.start} AND #{dto.end}",
            "GROUP BY device_id",
            "</script>"
    })
    List<Map<String, Object>> countNavSegmentsBatch(@Param("dto") DeviceTimeRangeQuery dto);

    /** 3. 各设备电压曲线（一次性拉全表，Java 分组） */
    @Select({
            "<script>",
            "SELECT device_id,",
            "(snapshot->'battery_voltage'->>'calValue')::int AS voltage,",
            "create_time",
            "FROM t_device_points_jsonb",
            "WHERE device_id IN",
            "<foreach collection='dto.deviceIds' item='d' open='(' separator=',' close=')'>",
            "#{d}",
            "</foreach>",
            "AND create_time BETWEEN #{dto.start} AND #{dto.end}",
            "ORDER BY device_id, create_time ASC",
            "</script>"
    })
    List<DevicePointJsonbBattVBO> listVoltageCurveBatch(@Param("dto") DeviceTimeRangeQuery dto);
}
