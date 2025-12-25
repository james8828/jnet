package com.jnet.api.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;


/**
 * @author mugw
 * @version 2.6.0
 * @description 自定义分页对象
 * @date 2025/5/15 16:03:54
 */
public class CustomPage<T> extends Page<T>{

    public CustomPage() {
        super();
    }

    public CustomPage(long current, long size) {
        super(current, size);
    }

    public CustomPage(long current, long size, long total) {
        super(current, size, total);
    }

    public CustomPage(long current, long size, long total, boolean searchCount) {
        super(current, size, total, searchCount);
    }

    public CustomPage(PageQueryVO req) {
        super(req.getCurrent().longValue() , req.getSize().longValue());
    }



}
