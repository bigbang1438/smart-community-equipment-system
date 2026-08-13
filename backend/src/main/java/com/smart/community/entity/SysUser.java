package com.smart.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码（仅写入时接收，不返回前端） */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String realName;

    /** ADMIN管理员 / MAINTAINER维保 / INSPECTOR巡检 */
    private String role;

    private String phone;

    /** 1启用 0禁用 */
    private Integer status;

    private LocalDateTime createTime;
}
