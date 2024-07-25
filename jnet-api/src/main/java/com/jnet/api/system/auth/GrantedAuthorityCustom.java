package com.jnet.api.system.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/24 17:20:52
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GrantedAuthorityCustom implements GrantedAuthority {

    private String authority;

    @Override
    public String getAuthority() {
        return authority;
    }
}
