import { get, post } from "@commons/request";
import type Result from "@commons/request/Result";
import type { Page } from "@commons/request/Result";
import type { Ref } from "vue";
import type {
  BatchRecycleRequest,
  ListRecycleBinRequest,
  RecycleBinInfo,
  RecycleRequest,
} from "@/api/recycle_bin/type";

export function listRecycleBins(
  req: ListRecycleBinRequest,
  loading?: Ref<boolean>
): Promise<Result<Page<RecycleBinInfo>>> {
  return get("api/vm/recycleBin/page", req, loading);
}

/**
 * 删除单个资源
 * @param deleteResource
 * @param loading
 */
export function deleteResource(
  recycleId: string,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post(
    `/api/vm/recycleBin/deleteResource/${recycleId}`,
    null,
    null,
    loading
  );
}

/**
 * 删除单个云主机
 * @param req
 * @param loading
 */
export function deleteVm(
  req: RecycleRequest,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post("/api/vm/recycleBin/deleteVm", null, req, loading);
}

/**
 * 删除单个云磁盘
 * @param req
 * @param loading
 */
export function deleteDisk(
  req: RecycleRequest,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post("/api/vm/recycleBin/deleteDisk", null, req, loading);
}

/**
 * 恢复单个云主机
 * @param req
 * @param loading
 */
export function recoverVm(
  req: RecycleRequest,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post("/api/vm/recycleBin/recoverVm", null, req, loading);
}

/**
 * 恢复单个磁盘
 * @param req
 * @param loading
 */
export function recoverDisk(
  req: RecycleRequest,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post("/api/vm/recycleBin/recoverDisk", null, req, loading);
}

/**
 * 批量删除资源
 * @param batchDeleteResource
 * @param loading
 */
export function batchDeleteResource(
  recycleIds: Array<string>,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post(
    "/api/vm/recycleBin/batchDeleteResource",
    null,
    { recycleIds: recycleIds },
    loading
  );
}

/**
 * 批量删除云主机
 * * @param req
 * @param loading
 */
export function batchDeleteVm(
  req: BatchRecycleRequest,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post("/api/vm/recycleBin/batchDeleteVm", null, req, loading);
}

/**
 * 批量删除磁盘
 * @param req
 * @param loading
 */
export function batchDeleteDisk(
  req: BatchRecycleRequest,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post("/api/vm/recycleBin/batchDeleteDisk", null, req, loading);
}

/**
 * 恢复单个资源
 * @param recoverResource
 * @param loading
 */
export function recoverResource(
  recycleId: string,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post(
    `/api/vm/recycleBin/recoverResource/${recycleId}`,
    null,
    null,
    loading
  );
}

/**
 * 批量恢复资源
 * @param recycleIds
 * @param loading
 */
export function batchRecoverResource(
  recycleIds: Array<string>,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post(
    "/api/vm/recycleBin/batchRecoverResource",
    null,
    { recycleIds: recycleIds },
    loading
  );
}

/**
 * 批量恢复云主机
 * @param req
 * @param loading
 */
export function batchRecoverVm(
  req: BatchRecycleRequest,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post("/api/vm/recycleBin/batchRecoverVm", null, req, loading);
}

/**
 * 批量恢复磁盘
 * @param req
 * @param loading
 */
export function batchRecoverDisk(
  req: BatchRecycleRequest,
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return post("/api/vm/recycleBin/batchRecoverDisk", null, req, loading);
}

/**
 * 获取当前回收站的开启状态
 */
export function getRecycleEnableStatus(
  loading?: Ref<boolean>
): Promise<Result<boolean>> {
  return get("api/vm/recycleBin/getRecycleEnableStatus", null, loading);
}

const RecycleBinsApi = {
  listRecycleBins,
  batchRecoverResource,
  batchRecoverVm,
  batchRecoverDisk,
  batchDeleteResource,
  batchDeleteVm,
  batchDeleteDisk,
  deleteResource,
  deleteVm,
  deleteDisk,
  recoverResource,
  recoverVm,
  recoverDisk,
  getRecycleEnableStatus,
};

export default RecycleBinsApi;
