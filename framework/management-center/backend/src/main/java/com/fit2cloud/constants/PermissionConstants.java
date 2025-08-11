package com.fit2cloud.constants;

import com.fit2cloud.common.constants.RoleConstants;
import com.fit2cloud.dto.permission.ModulePermission;
import com.fit2cloud.dto.permission.Permission;
import com.fit2cloud.dto.permission.PermissionGroup;
import com.fit2cloud.service.BasePermissionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模块内权限
 * 需要实现 MODULE_PERMISSION_BUILDER
 * 当前模块内判断主要是 groupId 和 operate
 * 也可以通过判断id，但是id需要带上模块名与require组合，如："[management-center]USER:READ"
 */
@Component
@DependsOn({"flyway", "flywayInitializer"})
public class PermissionConstants {

    public static class GROUP {
        public static final String USER = "USER";
        public static final String ROLE = "ROLE";

        public static final String WORKSPACE = "WORKSPACE";

        public static final String CLOUD_ACCOUNT = "CLOUD_ACCOUNT";
        public static final String ORGANIZATION = "ORGANIZATION";
        public static final String SYSTEM_SETTING = "SYSTEM_SETTING";

        public static final String PARAMS_SETTING = "PARAMS_SETTING";
        public static final String ABOUT = "ABOUT";

        public static final String SYS_LOG = "SYS_LOG";

        public static final String OPERATED_LOG = "OPERATED_LOG";

        public static final String MODULE_MANAGE = "MODULE_MANAGE";

        //...
    }

    public static class OPERATE {
        public static final String READ = "READ";
        public static final String EDIT = "EDIT";
        public static final String EDIT_PASSWORD = "EDIT_PASSWORD";
        public static final String CREATE = "CREATE";
        public static final String DELETE = "DELETE";
        public static final String NOTIFICATION_SETTING = "NOTIFICATION_SETTING";
        public static final String SYNC_RESOURCE = "SYNC_RESOURCE";
        public static final String SYNC_BILL = "SYNC_BILL";
        public static final String SYNC_SETTING = "SYNC_SETTING";
        public static final String CLEAR_POLICY = "CLEAR_POLICY";
        //...
    }

    public static ModulePermission MODULE_PERMISSION;

    /**
     * 可以通过id找到对应权限
     */
    public static Map<String, Permission> PERMISSION_MAP = null;

    @Resource
    private BasePermissionService basePermissionService;

    @Value("${spring.application.name}")
    public void setModule(String module) {
        PermissionConstants.MODULE_PERMISSION = MODULE_PERMISSION_BUILDER
                .module(module)
                .build();

        PERMISSION_MAP = PermissionConstants.MODULE_PERMISSION.getGroups().stream()
                .map(PermissionGroup::getPermissions)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Permission::getId, permission -> permission));

        //推送到redis
        basePermissionService.init(module, PermissionConstants.MODULE_PERMISSION);
    }

    private static final ModulePermission.Builder MODULE_PERMISSION_BUILDER = new ModulePermission.Builder()
            .group(
                    //用户管理
                    new PermissionGroup.Builder()
                            .id(GROUP.USER)
                            .name("permission.manage.user.base")
                            .permission(
                                    //查看用户
                                    new Permission.Builder()
                                            .operate(OPERATE.READ)
                                            .name("permission.manage.user.read")
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(
                                    //创建用户
                                    new Permission.Builder()
                                            .require(OPERATE.READ)
                                            .operate(OPERATE.CREATE)
                                            .name("permission.manage.user.create")
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(
                                    //编辑用户
                                    new Permission.Builder()
                                            .require(OPERATE.READ)
                                            .operate(OPERATE.EDIT)
                                            .name("permission.manage.user.edit")
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(
                                    //编辑用户
                                    new Permission.Builder()
                                            .require(OPERATE.READ)
                                            .operate(OPERATE.EDIT_PASSWORD)
                                            .name("permission.manage.user.edit_password")
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(
                                    //删除用户
                                    new Permission.Builder()
                                            .require(OPERATE.READ)
                                            .operate(OPERATE.DELETE)
                                            .name("permission.manage.user.delete")
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(
                                    //用户通知设置
                                    new Permission.Builder()
                                            .require(OPERATE.READ)
                                            .operate(OPERATE.NOTIFICATION_SETTING)
                                            .name("permission.manage.user.notification_setting")
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .role(RoleConstants.ROLE.ORGADMIN)
                            )
                    //...
            )
            .group(
                    //权限管理
                    new PermissionGroup.Builder()
                            .id(GROUP.ROLE)
                            .name("permission.manage.role.base")
                            .permission(
                                    new Permission.Builder()
                                            .operate(OPERATE.READ)
                                            .name("permission.manage.role.read")
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(
                                    new Permission.Builder()
                                            .require(OPERATE.READ)
                                            .operate(OPERATE.CREATE)
                                            .name("permission.manage.role.create")
                                            .role(RoleConstants.ROLE.ADMIN)

                            )
                            .permission(
                                    new Permission.Builder()
                                            .require(OPERATE.READ)
                                            .operate(OPERATE.EDIT)
                                            .name("permission.manage.role.edit")
                                            .role(RoleConstants.ROLE.ADMIN)
                            )
                            .permission(
                                    new Permission.Builder()
                                            .require(OPERATE.READ)
                                            .operate(OPERATE.DELETE)
                                            .name("permission.manage.role.delete")
                                            .role(RoleConstants.ROLE.ADMIN)
                            )
            )
            // 组织相关权限配置
            .group(
                    new PermissionGroup.Builder()
                            .id(GROUP.ORGANIZATION)
                            .name("permission.manage.organization.base")
                            .permission(new Permission.Builder()
                                    .operate(OPERATE.READ)
                                    .name("permission.manage.organization.read")
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(new Permission.Builder()
                                    .require(OPERATE.READ)
                                    .operate(OPERATE.CREATE)
                                    .name("permission.manage.organization.create")
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(new Permission.Builder()
                                    .require(OPERATE.READ)
                                    .operate(OPERATE.EDIT)
                                    .name("permission.manage.organization.edit")
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(new Permission.Builder()
                                    .require(OPERATE.READ)
                                    .operate(OPERATE.DELETE)
                                    .name("permission.manage.organization.delete")
                                    .role(RoleConstants.ROLE.ADMIN)
                                    .role(RoleConstants.ROLE.ORGADMIN)
                            )
            )
            //工作空间
            .group(
                    new PermissionGroup.Builder()
                            .id(GROUP.WORKSPACE)
                            .name("permission.manage.workspace.base")
                            .permission(
                                    //查看工作空间
                                    new Permission.Builder()
                                            .operate(OPERATE.READ)
                                            .name("permission.manage.workspace.read")
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(
                                    //创建工作空间
                                    new Permission.Builder()
                                            .require(OPERATE.READ)
                                            .operate(OPERATE.CREATE)
                                            .name("permission.manage.workspace.create")
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(
                                    //编辑工作空间
                                    new Permission.Builder()
                                            .require(OPERATE.READ)
                                            .operate(OPERATE.EDIT)
                                            .name("permission.manage.workspace.edit")
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .role(RoleConstants.ROLE.ORGADMIN)
                            )
                            .permission(
                                    //编辑工作空间
                                    new Permission.Builder()
                                            .require(OPERATE.READ)
                                            .operate(OPERATE.DELETE)
                                            .name("permission.manage.workspace.delete")
                                            .role(RoleConstants.ROLE.ADMIN)
                                            .role(RoleConstants.ROLE.ORGADMIN)
                            )
                    //...
            ).group(
                    new PermissionGroup.Builder()
                            .id(GROUP.SYS_LOG)
                            .name("permission.manage.sys_log.base")
                            .permission(
                                    //查看系统日志
                                    new Permission.Builder()
                                            .operate(OPERATE.READ)
                                            .name("permission.manage.sys_log.read")
                                            .role(RoleConstants.ROLE.ADMIN)
                            )

                    //...
            ).group(
                    new PermissionGroup.Builder()
                            .id(GROUP.OPERATED_LOG)
                            .name("permission.manage.operated_log.base")
                            .permission(
                                    //查看操作日志
                                    new Permission.Builder()
                                            .operate(OPERATE.READ)
                                            .name("permission.manage.operated_log.read")
                                            .role(RoleConstants.ROLE.ADMIN)
                            )

                    //...
            )
            // 云账号相关权限
            .group(
                    new PermissionGroup.Builder()
                            .id(GROUP.CLOUD_ACCOUNT)
                            .name("permission.manage.cloud_account.base")
                            .permission(new Permission.Builder()
                                    .operate(OPERATE.READ)
                                    .name("permission.manage.cloud_account.read")
                                    .role(RoleConstants.ROLE.ADMIN))
                            .permission(new Permission.Builder()
                                    .require(OPERATE.READ)
                                    .operate(OPERATE.CREATE)
                                    .name("permission.manage.cloud_account.create")
                                    .role(RoleConstants.ROLE.ADMIN))
                            .permission(new Permission.Builder()
                                    .require(OPERATE.READ)
                                    .operate(OPERATE.EDIT)
                                    .name("permission.manage.cloud_account.edit")
                                    .role(RoleConstants.ROLE.ADMIN))
                            .permission(new Permission.Builder()
                                    .require(OPERATE.READ)
                                    .operate(OPERATE.DELETE)
                                    .name("permission.manage.cloud_account.delete")
                                    .role(RoleConstants.ROLE.ADMIN))
                            .permission(new Permission.Builder()
                                    .require(OPERATE.READ)
                                    .operate(OPERATE.SYNC_RESOURCE)
                                    .name("permission.manage.cloud_account.sync_resource")
                                    .role(RoleConstants.ROLE.ADMIN))
                            .permission(new Permission.Builder()
                                    .require(OPERATE.READ)
                                    .operate(OPERATE.SYNC_BILL)
                                    .name("permission.manage.cloud_account.sync_bill")
                                    .role(RoleConstants.ROLE.ADMIN))
                            .permission(new Permission.Builder()
                                    .require(OPERATE.READ)
                                    .operate(OPERATE.SYNC_SETTING)
                                    .name("permission.manage.cloud_account.sync_setting")
                                    .role(RoleConstants.ROLE.ADMIN))
            )
            // 参数设置相关权限
            .group(
                    new PermissionGroup.Builder()
                            .id(GROUP.PARAMS_SETTING)
                            .name("permission.manage.params_setting.base")
                            .permission(new Permission.Builder()
                                    .operate(OPERATE.READ)
                                    .name("permission.manage.params_setting.read")
                                    .role(RoleConstants.ROLE.ADMIN))
                            .permission(new Permission.Builder()
                                    .require(OPERATE.READ)
                                    .operate(OPERATE.EDIT)
                                    .name("permission.manage.params_setting.edit")
                                    .role(RoleConstants.ROLE.ADMIN))
            )
            .group(
                    new PermissionGroup.Builder()
                            .id(GROUP.ABOUT)
                            .name("permission.manage.about.base")
                            .permission(new Permission.Builder()
                                    .operate(OPERATE.READ)
                                    .name("permission.manage.params_setting.read")
                                    .role(RoleConstants.ROLE.ADMIN))
            )
            // 模块
            .group(
                    new PermissionGroup.Builder()
                            .id(GROUP.MODULE_MANAGE)
                            .name("permission.manage.module.base")
                            .permission(new Permission.Builder()
                                    .operate(OPERATE.READ)
                                    .name("permission.manage.module.read")
                                    .role(RoleConstants.ROLE.ADMIN))
                            .permission(new Permission.Builder()
                                    .require(OPERATE.READ)
                                    .operate(OPERATE.EDIT)
                                    .name("permission.manage.module.edit")
                                    .role(RoleConstants.ROLE.ADMIN))
            )
            //...
            ;


}
