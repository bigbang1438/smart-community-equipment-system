package com.smart.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.community.common.BusinessException;
import com.smart.community.common.PageResult;
import com.smart.community.entity.Contract;
import com.smart.community.mapper.ContractMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class ContractService extends ServiceImpl<ContractMapper, Contract> {

    /** 即将到期提醒阈值（天） */
    private static final int EXPIRING_DAYS = 30;

    /**
     * 动态计算合同状态并落库：EXPIRED已过期 / EXPIRING即将到期 / VALID有效
     */
    public String resolveStatus(Contract c) {
        if (c.getEndDate() == null) {
            return "VALID";
        }
        LocalDate today = LocalDate.now();
        if (c.getEndDate().isBefore(today)) {
            return "EXPIRED";
        }
        if (!c.getEndDate().isAfter(today.plusDays(EXPIRING_DAYS))) {
            return "EXPIRING";
        }
        return "VALID";
    }

    public PageResult<Contract> page(int page, int size, String status, String keyword) {
        LambdaQueryWrapper<Contract> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Contract::getContractNo, keyword)
                    .or().like(Contract::getContractName, keyword)
                    .or().like(Contract::getVendor, keyword));
        }
        if (StringUtils.hasText(status)) {
            qw.eq(Contract::getStatus, status);
        }
        qw.orderByAsc(Contract::getEndDate);
        Page<Contract> p = page(new Page<>(page, size), qw);
        p.getRecords().forEach(c -> c.setStatus(resolveStatus(c)));
        return PageResult.of(p);
    }

    public Contract add(Contract contract) {
        // 状态由服务端按到期日期强制计算，禁止客户端注入
        contract.setStatus(resolveStatus(contract));
        save(contract);
        return contract;
    }

    public Contract updateContract(Contract contract) {
        if (contract.getId() == null) {
            throw new BusinessException("参数错误");
        }
        Contract exist = getById(contract.getId());
        if (exist == null) {
            throw new BusinessException("合同不存在");
        }
        // 未传的字段保留原值；状态按最终到期日期重算（避免未传 endDate 时误算为 VALID）
        if (contract.getEndDate() == null) {
            contract.setEndDate(exist.getEndDate());
        }
        if (contract.getStartDate() == null) {
            contract.setStartDate(exist.getStartDate());
        }
        contract.setStatus(resolveStatus(contract));
        updateById(contract);
        return getById(contract.getId());
    }

    /**
     * 到期提醒列表：已过期 + 30天内到期
     */
    public List<Contract> reminders() {
        LambdaQueryWrapper<Contract> qw = new LambdaQueryWrapper<>();
        qw.le(Contract::getEndDate, LocalDate.now().plusDays(EXPIRING_DAYS));
        qw.orderByAsc(Contract::getEndDate);
        List<Contract> list = list(qw);
        list.forEach(c -> c.setStatus(resolveStatus(c)));
        return list;
    }

    public long countExpiringOrExpired() {
        return count(new LambdaQueryWrapper<Contract>()
                .le(Contract::getEndDate, LocalDate.now().plusDays(EXPIRING_DAYS)));
    }
}
