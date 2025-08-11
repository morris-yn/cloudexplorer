import { defineStore } from "pinia";
import { useUserStore } from "./user";
import type { Module } from "@commons/api/module/type";
import { listModules } from "@commons/api/module";
import type { Menu } from "@commons/api/menu/type";
import { listMenus } from "@commons/api/menu";
import type { SimpleMap } from "@commons/api/base/type";
import type { RouteModule } from "@commons/api/module/type";
import type { UnwrapRef } from "vue";
import _ from "lodash";

interface ModuleStoreObject {
  modules?: Array<Module>;
  menus: SimpleMap<Array<Menu>>;
  // 当前模块的名称 注意当前变量只会在基座进行修改,如果单启 子模块currentModuleName就是当前模块名称
  currentModuleName?: string;
}

export function getUrl(module: Module): string {
  return (
    window.location.protocol +
    "//" +
    window.location.host +
    (module.basePath ? module.basePath : "")
  );
}

export const useModuleStore = defineStore({
  id: "module",
  state: (): ModuleStoreObject => ({
    modules: undefined,
    menus: <SimpleMap<Array<Menu>>>{},
    currentModuleName: import.meta.env.VITE_APP_NAME,
  }),
  getters: {
    runningModules(state: any): Array<Module> {
      return _.map(state.modules, (module: Module) => {
        return {
          ...module,
          requiredPermissions: _.flatMap(
            _.get(state.menus, module.id, []),
            (menu: Menu) => {
              return menu.requiredPermissions;
            }
          ),
        };
      });
      //return state.modules;
    },
    runningMenus(state: any): SimpleMap<Array<Menu>> {
      return state.menus;
    },
    routeModules(state: any): Array<RouteModule> {
      return _.map(state.runningModules, (module: UnwrapRef<Module>) => {
        const routeModule: RouteModule = {
          path: "/" + module.id,
          name: module.id,
          componentPath: "/src/views/MicroAppRouteView/index.vue",
          props: {
            moduleName: module.id,
            name: module.id,
            url: getUrl(module),
            baseRoute: _.defaultTo(module.basePath, "/"),
          },
          requiredPermissions: module.requiredPermissions,
        };
        return routeModule;
      });
    },
    currentModule(state: any): Module | undefined {
      // 如果是基座
      return _.find(
        state.modules,
        (module: Module) => module.id === state.currentModuleName
      );
    },
    currentModuleMenu(state: any): Array<Menu> {
      return _.get(state.menus, state.currentModuleName, []);
    },
  },
  actions: {
    updateCurrentModuleName(moduleName: string) {
      this.currentModuleName = moduleName;
    },
    async refreshModules(source?: string) {
      console.debug("refreshModules source: " + source);
      const userStore = useUserStore();
      await userStore.getCurrentUser();

      if (!userStore.isLogin) {
        return;
      }
      if (this.modules) {
        console.debug("ok");
        return;
      } else {
        console.debug("init");
      }
      const modules = (await listModules()).data;
      if (modules) {
        this.modules = modules;
      }
      await this.refreshMenus();
    },
    async refreshMenus() {
      const userStore = useUserStore();
      if (!userStore.isLogin) {
        return;
      }
      const menus = (await listMenus()).data;
      if (menus) {
        this.menus = menus;
      }
    },
  },
});
