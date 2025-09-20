package com.aiforest.tmiot.common.data.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class HistoryPageDTO {
//    private String deviceId;
//    private Long current = 1L;
//    private Long size = 20L;
//    private LocalDateTime start;
//    private LocalDateTime end;
    @NotBlank
    private String deviceId;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime start;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime end;
}
