package com.smart.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("device_task")
public class DeviceTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskCode;

    /** INSPECT巡检 / MAINTAIN保养 */
    private String taskType;

    private Long deviceId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    private String executor;

    /** PENDING待执行 / COMPLETED已完成 / OVERDUE已逾期 */
    private String status;

    /** NORMAL正常 / ABNORMAL异常 */
    private String result;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkTime;

    private String location;
    private String photo;
    private String checkItems;
    private String remark;

    private LocalDateTime createTime;

    /** 关联设备信息（非表字段） */
    @TableField(exist = false)
    private String deviceName;

    @TableField(exist = false)
    private String deviceCode;

    @TableField(exist = false)
    private String deviceLocation;

    @TableField(exist = false)
    private String deviceType;
}
