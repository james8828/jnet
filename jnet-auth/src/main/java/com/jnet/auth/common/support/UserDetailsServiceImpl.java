package com.jnet.auth.common.support;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jnet.auth.domain.Role;
import com.jnet.auth.domain.RoleUser;
import com.jnet.auth.mapper.RoleMapper;
import com.jnet.auth.mapper.RoleUserMapper;
import com.jnet.auth.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.jnet.auth.domain.User;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements CustomUserDetailsService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleUserMapper roleUserMapper;

    @Resource
    private RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        QueryWrapper<User> queryWrapper = Wrappers.query();
        queryWrapper.eq("user_name",username);
        User user = userMapper.selectOne(queryWrapper);
        List<Role> roleList = new ArrayList<>();
        if (ObjectUtils.isNotEmpty(user)){
            Integer userId = user.getUserId();
            QueryWrapper<RoleUser> roleUserQueryWrapper = Wrappers.query();
            roleUserQueryWrapper.eq("user_id",userId);
            List<RoleUser> roleUserList = roleUserMapper.selectList(roleUserQueryWrapper);
            QueryWrapper<Role> roleQueryWrapper = Wrappers.query();
            roleQueryWrapper.in("role_id",roleUserList.stream().map(RoleUser::getRoleId).collect(Collectors.toList()));
            roleList = roleMapper.selectList(roleQueryWrapper);
        }

        Collection<? extends GrantedAuthority> authorities = roleList
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_".concat(role.getRoleName())))
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(user.getUserName(),
                user.getPassword(),
                true,
                true,
                true,
                true,
                authorities);
    }

    @Override
    public boolean supports(String accountType) {
        return false;
    }

    @Override
    public UserDetails loadUserByMobile(String mobile) {
        return null;
    }

    @Override
    public UserDetails loadUserByUserId(String userId) throws UsernameNotFoundException {
        return null;
    }
}
