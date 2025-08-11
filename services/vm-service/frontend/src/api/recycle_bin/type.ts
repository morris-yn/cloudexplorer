export enum ResourceType {
  VM = "VM",
  DISK = "DISK",
}

export interface RecycleBinInfo {
  id: string;
  resourceId: string;
  resourceType: ResourceType;
  resourceName?: string;
  status?: string;
  /**
   * 资源放入回收站时间
   */
  createTime?: string;
  /**
   * 资源创建时间
   */
  resourceCreateTime: string;
  deleteTime?: string;
  recoverTime?: string;
  userId?: string;
  userName?: string;
  resourceStatus?: string;
  resourceConfig?: string;
  ipArray?: string;
  relateResource?: string;
  deleteWithInstance?: string;
  accountId?: string;
  accountName?: string;
  platform: string;
  workspaceName?: string;
  workspaceId?: string;
  organizationName?: string;
  organizationId?: string;
}

export interface ListRecycleBinRequest {
  pageSize: number;
  currentPage: number;
  [propName: string]: any;
}

export interface RecycleRequest {
  id: string;
  resourceId: string;
  resourceType: ResourceType;
}

export interface BatchRecycleRequest {
  ids: Array<string>;
  resourceIds: Array<string>;
  resourceType?: ResourceType;
}
