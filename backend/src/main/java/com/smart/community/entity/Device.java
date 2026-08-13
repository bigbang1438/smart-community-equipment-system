package com.smart.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceCode;
    private String name;

    /** ELEVATOR电梯 / FIRE消防 / PUMP水泵 / ACCESS门禁 / OTHER其他 */
    private String type;

    private String model;
    private String manufacturer;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate installDate;

    private Integer serviceLifeYears;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate warrantyEnd;

    /** RUNNING运行 / FAULT故障 / REPAIRING维修中 / STOPPED停用 / SCRAPPED报废 */
    private String status;

    private Integer inspectCycle;
    private Integer maintainCycle;
    private String spec;
    private String qrCode;
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
