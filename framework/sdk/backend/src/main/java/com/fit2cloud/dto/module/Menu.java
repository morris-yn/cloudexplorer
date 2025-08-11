package com.fit2cloud.dto.module;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fit2cloud.dto.CeBaseObject;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Menu extends CeBaseObject {

    @Serial
    private static final long serialVersionUID = -6921007234387348678L;
    @Getter
    private String name;
    @Getter
    private String title;
    @Getter
    private String componentPath;

    private String path;

    @Getter
    private String icon;

    @Getter
    private int order;

    @Getter
    private String redirect;

    @Getter
    private boolean saveRecent;
    @Getter
    private boolean quickAccess;
    @Getter
    private String quickAccessName;
    @Getter
    private String quickAccessIcon;

    @Getter
    private List<MenuPermission> requiredPermissions;

    @Getter
    private List<Menu> children;

    @Getter
    private List<Menu> operations;

    @Getter
    private List<Menu> childOperations;

    @JsonIgnore
    private Menu parent;


    public Menu module(String module) {
        setModule(module);
        if (this.children != null) {
            for (Menu menu : this.children) {
                menu.module(module);
            }
        }
        if (this.operations != null) {
            for (Menu menu : this.operations) {
                menu.module(module);
            }
        }
        if (this.childOperations != null) {
            for (Menu menu : this.childOperations) {
                menu.module(module);
            }
        }
        if (this.requiredPermissions != null) {
            for (MenuPermission permission : this.requiredPermissions) {
                permission.module(module);
            }
        }
        return this;
    }


    public String getPath() {
        return (this.parent != null ? this.parent.getPath() : "") + this.path;
    }

    private Menu() {
    }

    public static class Builder {
        private final Menu menu;

        public Builder() {
            this.menu = new Menu();
        }

        public Builder module(String module) {
            this.menu.setModule(module);
            if (this.menu.children != null) {
                for (Menu menu : this.menu.children) {
                    menu.module(module);
                }
            }
            if (this.menu.operations != null) {
                for (Menu menu : this.menu.operations) {
                    menu.module(module);
                }
            }
            if (this.menu.childOperations != null) {
                for (Menu menu : this.menu.childOperations) {
                    menu.module(module);
                }
            }
            if (this.menu.requiredPermissions != null) {
                for (MenuPermission permission : this.menu.requiredPermissions) {
                    permission.module(module);
                }
            }
            return this;
        }

        public Builder name(String name) {
            this.menu.name = name;
            return this;
        }

        public Builder title(String title) {
            this.menu.title = title;
            return this;
        }

        public Builder componentPath(String componentPath) {
            this.menu.componentPath = componentPath;
            return this;
        }

        public Builder path(String path) {
            this.menu.path = path;
            return this;
        }

        public Builder icon(String icon) {
            this.menu.icon = icon;
            return this;
        }

        public Builder order(int order) {
            this.menu.order = order;
            return this;
        }

        public Builder redirect(String redirect) {
            this.menu.redirect = redirect;
            return this;
        }

        public Builder saveRecent(boolean saveRecent) {
            this.menu.saveRecent = saveRecent;
            return this;
        }

        public Builder quickAccess(boolean quickAccess) {
            this.menu.quickAccess = quickAccess;
            return this;
        }

        public Builder quickAccessName(String quickAccessName) {
            this.menu.quickAccessName = quickAccessName;
            return this;
        }

        public Builder quickAccessIcon(String quickAccessIcon) {
            this.menu.quickAccessIcon = quickAccessIcon;
            return this;
        }

        private Builder parent(Menu.Builder menuBuilder) {
            this.menu.parent = menuBuilder.build();
            return this;
        }

        public Builder requiredPermission(MenuPermission.Builder menuPermissionBuilder) {
            if (this.menu.requiredPermissions == null) {
                this.menu.requiredPermissions = new ArrayList<>();
            }
            menuPermissionBuilder.module(this.menu.getModule());
            this.menu.requiredPermissions.add(menuPermissionBuilder.build());
            return this;
        }

        public Builder childMenu(Menu.Builder menuBuilder) {
            if (this.menu.children == null) {
                this.menu.children = new ArrayList<>();
            }
            this.menu.children.add(menuBuilder.parent(this).build());
            return this;
        }

        public Builder operationRoute(Menu.Builder menuBuilder) {
            if (this.menu.operations == null) {
                this.menu.operations = new ArrayList<>();
            }
            this.menu.operations.add(menuBuilder.parent(this).build());
            return this;
        }

        public Builder childOperationRoute(Menu.Builder menuBuilder) {
            if (this.menu.childOperations == null) {
                this.menu.childOperations = new ArrayList<>();
            }
            this.menu.childOperations.add(menuBuilder.parent(this).build());
            return this;
        }

        public Menu build() {
            return menu;
        }
    }


}
