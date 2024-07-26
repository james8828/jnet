package com.jnet.api.system.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.api.system.domain.Menu;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/19 14:00:15
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MenuQuery<T> extends Page {
    private String userName;
    private String type;
    private Boolean enabled;
    private List<Long> roleIds;

    public Menu getMenu() {
        return new Menu();
    }
}
