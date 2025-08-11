package com.fit2cloud.dto.permission;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fit2cloud.common.constants.RoleConstants;
import com.fit2cloud.dto.CeBaseObject;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionGroup extends CeBaseObject {

    @Serial
    private static final long serialVersionUID = -8540444956359825987L;
    @Getter
    private String id;
    @Getter
    private String name;

    @Getter
    @Setter
    private List<Permission> permissions;

    private PermissionGroup() {
    }

    public PermissionGroup module(String module) {
        setModule(module);
        if (this.permissions != null) {
            for (Permission permission : this.permissions) {
                permission.module(module);
            }
        }
        return this;
    }

    public static class Builder {

        private final PermissionGroup permissionGroup;

        public Builder() {
            permissionGroup = new PermissionGroup();
        }

        public Builder module(String module) {
            this.permissionGroup.setModule(module);
            if (this.permissionGroup.permissions != null) {
                for (Permission permission : this.permissionGroup.permissions) {
                    permission.module(module);
                }
            }
            return this;
        }

        public Builder id(@NotNull String id) {
            this.permissionGroup.id = id;
            return this;
        }

        public Builder name(@NotNull String name) {
            this.permissionGroup.name = name;
            return this;
        }

        public Builder permission(@NotNull Permission.Builder permissionBuilder) {
            if (this.permissionGroup.permissions == null) {
                this.permissionGroup.permissions = new ArrayList<>();
            }
            Permission permission = permissionBuilder.group(this.permissionGroup).build();

            //去重
            boolean contains = false;
            for (Permission p : this.permissionGroup.permissions) {
                if (p.equals(permission)) {
                    contains = true;
                    for (RoleConstants.ROLE role : permission.getRoles()) {
                        p.getRoles().add(role);
                    }

                    break;
                }
            }
            if (!contains) {
                this.permissionGroup.permissions.add(permission);
            }
            return this;
        }


        public PermissionGroup build() {
            return this.permissionGroup;
        }

    }

}
