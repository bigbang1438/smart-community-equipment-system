package com.smart.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.community.common.BusinessException;
import com.smart.community.common.Md5Util;
import com.smart.community.common.PageResult;
import com.smart.community.entity.SysUser;
import com.smart.community.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    public SysUser findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    public PageResult<SysUser> page(int page, int size, String keyword) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }
        qw.orderByAsc(SysUser::getId);
        Page<SysUser> p = page(new Page<>(page, size), qw);
        p.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(p);
    }

    /**
     * 派单候选：维保人员 + 管理员
     */
    public java.util.List<SysUser> maintainers() {
        java.util.List<SysUser> list = list(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getRole, "MAINTAINER", "ADMIN")
                .eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId));
        list.forEach(u -> u.setPassword(null));
        return list;
    }

    public void add(SysUser user) {
        if (findByUsername(user.getUsername()) != null) {
            throw new BusinessException("登录名已存在");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        user.setPassword(Md5Util.encrypt(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        save(user);
    }

    public void updateUser(SysUser user) {
        if (user.getId() == null) {
            throw new BusinessException("参数错误");
        }
        SysUser exist = getById(user.getId());
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(Md5Util.encrypt(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        updateById(user);
    }

    public void deleteUser(Long id) {
        if (id == 1L) {
            throw new BusinessException("不能删除系统内置管理员");
        }
        removeById(id);
    }
}
