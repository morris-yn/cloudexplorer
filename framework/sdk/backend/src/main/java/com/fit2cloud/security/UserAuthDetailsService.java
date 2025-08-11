package com.fit2cloud.security;

import com.fit2cloud.base.service.IBaseUserRoleService;
import com.fit2cloud.base.service.IBaseUserService;
import com.fit2cloud.dto.UserDto;
import com.fit2cloud.dto.security.SecurityUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import jakarta.annotation.Resource;
import java.util.ArrayList;


public class UserAuthDetailsService implements UserDetailsService {

    @Resource
    private IBaseUserService loginService;
    @Resource
    private IBaseUserRoleService userRoleService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserDto userDto = loginService.getUserByIdOrEmail(username);

        if (userDto == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!userDto.getEnabled()) {
            throw new RuntimeException("用户已被禁用");
        }

        SecurityUser securityUser = new SecurityUser();
        securityUser.setCurrentUserInfoDto(userDto);

        //将当前用户的授权角色更新到redis
        userRoleService.saveCachedUserRoleMap(userDto.getId());

        //无session模式，登录接口没必要返回权限，交给jwt token认证即可
        securityUser.setPermissionValueList(new ArrayList<>());

        return securityUser;
    }
}
