interface VmCloudDiskVO {
  id: string;
  region?: string;
  zone?: string;
  diskId?: string;
  diskName?: string;
  diskType?: string;
  category?: string;
  status: string;
  diskChargeType?: string;
  description?: string;
  size?: number;
  device?: string;
  accountId: string;
  datastoreId?: string;
  instanceUuid: string;
  serverId?: string; // vm_cloud_server 主键
  vmInstanceName?: string;
  workspaceId?: string;
  sourceId?: string;
  projectId?: string;
  bootable?: boolean;
  imageId?: string;
  deleteWithInstance?: string;
  createTime?: string;
  updateTime?: string;
  organizationName?: string;
  workspaceName?: string;
  accountName?: string;
  platform: string;
}
interface ListVmCloudDiskRequest {
  pageSize: number;
  currentPage: number;
}
interface AttachDiskRequest {
  id: string;
  instanceUuid: string;
  deleteWithInstance?: boolean;
}
interface BatchAttachDiskRequest {
  ids: string[];
  instanceUuid: string;
  deleteWithInstance?: boolean;
}
interface EnlargeDiskRequest {
  id: string;
  instanceUuid?: string;
  newDiskSize: number;
}
interface ListVmRequest {
  accountId: string;
  zone: string;
}
export type {
  VmCloudDiskVO,
  ListVmCloudDiskRequest,
  AttachDiskRequest,
  EnlargeDiskRequest,
  BatchAttachDiskRequest,
  ListVmRequest,
};
