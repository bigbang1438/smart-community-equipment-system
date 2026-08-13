package com.smart.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("contract")
public class Contract {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String contractNo;
    private String contractName;

    /** 覆盖设备类型 ELEVATOR/FIRE/PUMP/ACCESS/OTHER */
    private String deviceType;

    private String vendor;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private BigDecimal amount;
    private String contact;
    private String contactPhone;
    private String payMethod;

    /** VALID有效 / EXPIRING即将到期 / EXPIRED已过期（由查询动态计算） */
    private String status;

    private String remark;
    private LocalDateTime createTime;
}
