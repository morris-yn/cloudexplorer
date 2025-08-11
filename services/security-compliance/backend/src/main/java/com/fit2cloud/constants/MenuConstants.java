package com.fit2cloud.constants;

import com.fit2cloud.common.constants.RoleConstants;
import com.fit2cloud.dto.module.Menu;
import com.fit2cloud.dto.module.MenuPermission;
import com.fit2cloud.dto.module.Menus;
import com.fit2cloud.service.MenuService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

@Component
public class MenuConstants {

    public static List<Menu> MENUS;

    @Resource
    private MenuService menuService;

    @Value("${spring.application.name}")
    public void setModule(String module) {

        MENUS = MENUS_BUILDER.module(module).build().getMenus();

        //推送到redis
        menuService.init(module, MENUS);

    }

    private static final Menus.Builder MENUS_BUILDER = new Menus.Builder()
            .menu(new Menu.Builder().name("overview")
                    .title("总览")
                    .path("/overview")
                    .saveRecent(true)
                    .componentPath("/src/views/overview/index.vue")
                    .icon("icon_moments-categories_outlined")
                    .requiredPermission(
                            new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(PermissionConstants.GROUP.OVERVIEW,
                                            PermissionConstants.OPERATE.READ))
                    .order(1))
            .menu(new Menu.Builder().name("scan").title("扫描检查")
                    .componentPath("/src/views/scan/index.vue")
                    .path("/scan")
                    .icon("icon_scan")
                    .saveRecent(true)
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(PermissionConstants.GROUP.SCAN,
                                    PermissionConstants.OPERATE.READ))
                    .order(2).operationRoute(new Menu.Builder()
                            .name("details")
                            .title("详情")
                            .componentPath("/src/views/scan/details.vue")
                            .path("/details/:compliance_rule_id/:cloud_account_id")
                            .requiredPermission(new MenuPermission.Builder()
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .permission(PermissionConstants.GROUP.SCAN, PermissionConstants.OPERATE.READ))))
            .menu(new Menu.Builder()
                    .name("rule")
                    .title("规则设置")
                    .saveRecent(true)
                    .componentPath("/src/views/rule/index.vue")
                    .path("/rule")
                    .icon("icon_rule")
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(PermissionConstants.GROUP.RULE, PermissionConstants.OPERATE.READ))
                    .order(3))
            .menu(new Menu.Builder()
                    .name("insurance_statute")
                    .title("风险条例")
                    .saveRecent(true)
                    .componentPath("/src/views/insurance_statute/index.vue")
                    .path("/insurance_statute")
                    .icon("icon_risk_regulations")
                    .requiredPermission(new MenuPermission.Builder()
                            .role(RoleConstants.ROLE.ADMIN)
                            .permission(PermissionConstants.GROUP.INSURANCE, PermissionConstants.OPERATE.READ))
                    .order(4));


}
