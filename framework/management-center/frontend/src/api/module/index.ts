import { get, post } from "@commons/request";
import type { Ref } from "vue";
import type Result from "@commons/request/Result";

function list(loading?: Ref<boolean>): Promise<Result<any>> {
  return get("/api/module_manage/list", undefined, loading);
}

function install(
  name: string,
  version: string,
  loading?: Ref<boolean>
): Promise<Result<any>> {
  return post(
    "/api/module_manage/install",
    undefined,
    { name: name, version: version },
    loading
  );
}

function uninstall(
  module: string,
  loading?: Ref<boolean>
): Promise<Result<any>> {
  return get(`/api/module_manage/uninstall/${module}`, undefined, loading);
}

function start(module: string, loading?: Ref<boolean>): Promise<Result<any>> {
  return get(`/api/module_manage/start/${module}`, undefined, loading);
}

function stop(module: string, loading?: Ref<boolean>): Promise<Result<any>> {
  return get(`/api/module_manage/stop/${module}`, undefined, loading);
}

const ModuleManageApi = {
  list,
  install,
  uninstall,
  start,
  stop,
};

export default ModuleManageApi;
