package com.smart.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("repair_order")
public class RepairOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderCode;
    private Long deviceId;
    private String reporter;
    private String phone;
    private String faultDesc;

    /** HIGH紧急 / MEDIUM一般 / LOW轻微 */
    private String level;

    /** PENDING待派单 / PROCESSING维修中 / COMPLETED待验收 / VERIFIED已验收 */
    private String status;

    private String assignee;
    private String fixResult;
    private BigDecimal cost;
    private BigDecimal fixHours;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assignTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verifyTime;

    @TableField(exist = false)
    private String deviceName;

    @TableField(exist = false)
    private String deviceCode;

    @TableField(exist = false)
    private String deviceLocation;
}
