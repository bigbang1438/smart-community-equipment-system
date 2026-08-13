package com.smart.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("device_log")
public class DeviceLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deviceId;

    /** TEMPERATURE温度 / VIBRATION振动 / VOLTAGE电压 */
    private String metric;

    private BigDecimal value;

    private LocalDateTime recordTime;
}
