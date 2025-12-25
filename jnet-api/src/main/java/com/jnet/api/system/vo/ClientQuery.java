package com.jnet.api.system.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.api.system.domain.Client;
import lombok.Builder;
import lombok.Data;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/24 14:19:50
 */
@Data
@Builder
public class ClientQuery<T> extends Page {

    public Client getClient() {
        return Client.builder().build();
    }
}
