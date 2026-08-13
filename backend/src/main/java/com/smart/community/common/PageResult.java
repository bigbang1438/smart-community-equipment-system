package com.smart.community.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页响应
 */
@Data
public class PageResult<T> {

    private Long total;
    private Long page;
    private Long size;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.total = page.getTotal();
        r.page = page.getCurrent();
        r.size = page.getSize();
        r.records = page.getRecords();
        return r;
    }
}
