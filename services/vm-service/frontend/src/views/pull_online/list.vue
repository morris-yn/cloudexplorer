<script setup lang="ts">
import {ref, onMounted, onBeforeUnmount, computed} from "vue";
import VmCloudServerApi, {groupsInfo, listpull, pusherList} from "@/api/vm_cloud_server";
import type {onlineInfo, VmCloudServerVO, VmDefaultVO} from "@/api/vm_cloud_server/type";
import {useRouter, type TypesConfig} from "vue-router";
import {
  PaginationConfig,
  TableConfig,
  TableOperations,
  TableSearch,
} from "@commons/components/ce-table/type";
import {useI18n} from "vue-i18n";
import {ElMessage, ElMessageBox, ElPopover} from "element-plus";
import _ from "lodash";
import type {SimpleMap} from "@commons/api/base/type";
import BaseCloudAccountApi from "@commons/api/cloud_account";
import RecycleBinsApi from "@/api/recycle_bin";
import Grant from "@/views/vm_cloud_server/grant.vue";
import {usePermissionStore} from "@commons/stores/modules/permission";
import ButtonToolBar from "@commons/components/button-tool-bar/ButtonToolBar.vue";
import VmServerStatusIcon from "@/views/vm_cloud_server/VmServerStatusIcon.vue";
import {
  ButtonAction,
  type ButtonActionType,
} from "@commons/components/button-tool-bar/type";
import OrgTreeFilter from "@commons/components/table-filter/OrgTreeFilter.vue";
import AddDisk from "@/views/vm_cloud_server/AddDisk.vue";
import ChangeConfig from "@/views/vm_cloud_server/ChangeConfig.vue";
import {classifyIP} from "@commons/utils/util";
import PlatformIcon from "@commons/components/platform-icon/index.vue";
import InstanceStatusUtils from "@commons/utils/vm_cloud_server/InstanceStatusUtils";
import Renew from "@/views/vm_cloud_server/Renew.vue";

const {t} = useI18n();
const permissionStore = usePermissionStore();
const useRoute = useRouter();
const table = ref<any>(null);
const columns = ref([]);
const tableData = ref<Array<onlineInfo>>([]);
const groupsTableData = ref<Array<onlineInfo>>([]);
const selectedRowData = ref<Array<VmCloudServerVO>>([]);
const tableLoading = ref<boolean>(false);
const cloudAccount = ref<Array<SimpleMap<string>>>([]);
const addDiskRef = ref<InstanceType<typeof AddDisk>>();
const changeConfigRef = ref<InstanceType<typeof ChangeConfig>>();
const renewRef = ref<InstanceType<typeof Renew>>();
const loading = ref<boolean>(false);
/**
 * 表头：组织树筛选
 */
const orgTreeRef = ref();
const orgPopRef = ref();
const dialogVisible = ref<boolean>(false);
const pushCreateDialogVisible = ref<boolean>(false);
const pusher = ref("");
const groupName = ref<string>("");
const selectedOrganizationIds = computed(() =>
    orgTreeRef.value?.getSelectedIds(false)
);

const pusherOptions = ref([]);

/**
 * 表头：工作空间树筛选
 */
const workspaceTreeRef = ref();
const workspacePopRef = ref();
const selectedWorkspaceIds = computed(() =>
    workspaceTreeRef.value?.getSelectedIds(true)
);

const pushGroupsDialogVisible = ref<boolean>(false);
const groupsOptions = ref([])
const groupsMultipleSelection =ref([])
const groupsSet = ref()

/**
 * 表头：清空组织和工作空间树的选中项
 * @param field
 */
const clearCondition = (field: string) => {
  if (field === "organizationIds") {
    orgTreeRef.value.cancelChecked();
  }
  if (field === "workspaceIds") {
    workspaceTreeRef.value.cancelChecked();
  }
};

//批量操作
const instanceOperateMap: Map<string, string> = new Map();
instanceOperateMap.set("POWER_ON", t("", "启动"));
instanceOperateMap.set("POWER_OFF", t("", "关机"));
instanceOperateMap.set("REBOOT", t("", "重启"));
instanceOperateMap.set("DELETE", t("", "删除"));

// 表格头:状态筛选项
const instanceStatusForTableSelect = computed(() => {
  return _.map(
      InstanceStatusUtils.instanceStatusListForTableSelect.value,
      (s) => {
        return {
          text: s.name?.value,
          value: s.status,
        };
      }
  );
});

// 表格头:付费类型筛选项
const chargeType = [
  {text: t("commons.charge_type.prepaid"), value: "PrePaid"},
  {text: t("commons.charge_type.postpaid"), value: "PostPaid"},
];

const filterChargeType = (value: string) => {
  let status = value;
  chargeType.forEach((v) => {
    if (v.value == value) {
      status = v.text;
      return;
    }
  });
  return status;
};

// 表格头:VMTools 筛选项
const vmToolsStatus = [
  {
    text: t("vm_cloud_server.vm_tools_status.not_Installed"),
    value: "toolsNotInstalled",
  },
  {
    text: t("vm_cloud_server.vm_tools_status.not_running"),
    value: "guestToolsNotRunning",
  },
  {
    text: t("vm_cloud_server.vm_tools_status.running"),
    value: "guestToolsRunning",
  },
];

const filterVmToolsStatus = (value: string) => {
  let status = value;
  vmToolsStatus.forEach((v) => {
    if (v.value == value) {
      status = v.text;
      return;
    }
  });
  return status;
};

/**
 * 查询
 * @param condition
 */
const search = (condition: TableSearch) => {
  const params = TableSearch.toSearchParams(condition);
  VmCloudServerApi.listpull(
      {
        currentPage: tableConfig.value.paginationConfig.currentPage,
        pageSize: tableConfig.value.paginationConfig.pageSize,
        ...params,
      },
      tableLoading
  ).then((res) => {
    tableData.value = res.data.records;

    tableConfig.value.paginationConfig?.setTotal(
        res.data.total,
        tableConfig.value.paginationConfig
    );
    tableConfig.value.paginationConfig?.setCurrentPage(
        res.data.current,
        tableConfig.value.paginationConfig
    );
  });
};

const refresh = () => {
  table.value.search();
};

/**
 * 是否开启了回收站
 */
const isRecycleBinOpened = ref(true);
const getRecycleBinSetting = async () => {
  await RecycleBinsApi.getRecycleEnableStatus().then((result) => {
    isRecycleBinOpened.value = result.data;
  });
};

/**
 * 页面挂载
 */
onMounted(() => {
  search(new TableSearch());
  searchCloudAccount();
  startOperateInterval();
});
onBeforeUnmount(() => {
  stopOperateInterval();
});

const searchCloudAccount = () => {
  BaseCloudAccountApi.listAll().then((result) => {
    if (result.data.length > 0) {
      result.data.forEach(function (v) {
        const ca = {text: v.name, value: v.id};
        cloudAccount.value.push(ca);
      });
    }
  });
};

const cloudServerInterval = ref<any>();
//启动定时器
const startOperateInterval = () => {
  cloudServerInterval.value = setInterval(() => {
    const list = _.map(tableData.value, (r) => r.id);
    if (list.length === 0) {
      return;
    }
    VmCloudServerApi.getVmCloudServerByIds(list).then((res) => {
      if (res) {
        for (let i = 0; i < res.data.length; i++) {
          _.forEach(tableData.value, function (vm) {
            if (vm.id === res.data[i].id) {
              _.assign(vm, res.data[i]);
            }
          });
        }
      }
    });
  }, 6000);
};
const updateInstanceStatus = (list: Array<VmCloudServerVO>) => {
  for (const vm of list) {
    VmCloudServerApi.getVmCloudServerById(vm.id).then((res) => {
      vm.instanceStatus = res.data.instanceStatus;
    });
  }
};
//停止定时器
const stopOperateInterval = () => {
  if (cloudServerInterval.value) {
    clearInterval(cloudServerInterval.value);
  }
};

/**
 * 选中的数据
 */
const handleSelectionChange = (list: Array<VmCloudServerVO>) => {
  selectedRowData.value = list;
};

/**
 * 详情
 */
const showDetail = (row: VmCloudServerVO) => {
  useRoute.push({
    path: useRoute.currentRoute.value.path.replace(
        "/list",
        `/detail/${row.id}`
    ),
  });
};
const openRenew = (row: VmCloudServerVO) => {
  renewRef.value?.open([row]);
};
const batchOpenRenew = () => {
  if (
      selectedRowData.value.find((item) => item.instanceChargeType === "PrePaid")
  ) {
    renewRef.value?.open(selectedRowData.value);
  } else {
    ElMessage.error("未选择包年包月机器,无法续费");
  }
};

const gotoCatalog = () => {
  useRoute.push({
    path: useRoute.currentRoute.value.path.replace("/list", "/catalog"),
  });
};

/**
 * 添加磁盘
 * @param row
 */
const createDisk = (row: VmCloudServerVO) => {
  addDiskRef.value?.open(row.id);
};

/**
 * 配置变更
 * @param row
 */
const changeVmConfig = (row: VmCloudServerVO) => {
  changeConfigRef.value?.open(row.id);
};

/**
 * 验证VMTools状态
 * @param vm
 */
const checkVmToolsStatus = (vm: VmCloudServerVO) => {
  if (vm.platform === "fit2cloud_vsphere_platform") {
    return vm.vmToolsStatus == "guestToolsRunning";
  }
  return true;
};
/**
 * 开机
 * @param row
 */
const powerOn = (row: VmCloudServerVO) => {
  ElMessageBox.confirm(
      t("vm_cloud_server.message_box.confirm_power_on", "确认启动"),
      t("commons.message_box.prompt", "提示"),
      {
        confirmButtonText: t("commons.message_box.confirm", "确认"),
        cancelButtonText: t("commons.btn.cancel", "取消"),
        type: "warning",
      }
  ).then(() => {
    VmCloudServerApi.powerOn(row.id as string)
        .then((res) => {
          ElMessage.success(t("commons.msg.op_success"));
          refresh();
        })
        .catch((err) => {
          ElMessage.error(err.response.data.message);
        });
  });
};
//关机
const shutdown = (row: VmCloudServerVO) => {
  let label = t("vm_cloud_server.message_box.confirm_shutdown", "确认关机");
  let powerOff = false;
  if (!checkVmToolsStatus(row)) {
    label = t(
        "vm_cloud_server.message_box.check_vm_tools_status_confirm_shutdown",
        "当前云主机未安装VmTools或VmTools未运行，无法软关机，若继续操作则关闭电源，是否继续？"
    );
    powerOff = true;
  }
  ElMessageBox.confirm(label, t("commons.message_box.prompt", "提示"), {
    confirmButtonText: t("commons.message_box.confirm", "确认"),
    cancelButtonText: t("commons.btn.cancel", "取消"),
    type: "warning",
  }).then(() => {
    if (powerOff) {
      VmCloudServerApi.powerOff(row.id as string)
          .then((res) => {
            ElMessage.success(t("commons.msg.op_success"));
            refresh();
          })
          .catch((err) => {
            ElMessage.error(err.response.data.message);
          });
    } else {
      VmCloudServerApi.shutdownInstance(row.id as string)
          .then((res) => {
            ElMessage.success(t("commons.msg.op_success"));
            refresh();
          })
          .catch((err) => {
            ElMessage.error(err.response.data.message);
          });
    }
  });
};
//关闭电源
const powerOff = (row: VmCloudServerVO) => {
  ElMessageBox.confirm(
      t("vm_cloud_server.message_box.confirm_power_off", "确认关闭电源"),
      t("commons.message_box.prompt", "提示"),
      {
        confirmButtonText: t("commons.message_box.confirm", "确认"),
        cancelButtonText: t("commons.btn.cancel", "取消"),
        type: "warning",
      }
  ).then(() => {
    VmCloudServerApi.powerOff(row.id as string)
        .then(() => {
          ElMessage.success(t("commons.msg.op_success"));
          refresh();
        })
        .catch((err) => {
          ElMessage.error(err.response.data.message);
        });
  });
};
//重启
const reboot = (row: VmCloudServerVO) => {
  ElMessageBox.confirm(
      t("vm_cloud_server.message_box.confirm_reboot", "确认重启"),
      t("commons.message_box.prompt", "提示"),
      {
        confirmButtonText: t("commons.message_box.confirm", "确认"),
        cancelButtonText: t("commons.btn.cancel", "取消"),
        type: "warning",
      }
  ).then(() => {
    VmCloudServerApi.reboot(row.id as string)
        .then(() => {
          ElMessage.success(t("commons.msg.op_success"));
          refresh();
        })
        .catch((err) => {
          ElMessage.error(err.response.data.message);
        });
  });
};

//删除
const deleteInstance = async (row: VmCloudServerVO) => {
  if (row.instanceStatus === "Failed") {
    deleteFailedRecord(row.id);
    return;
  }
  await getRecycleBinSetting();

  const message = isRecycleBinOpened.value
      ? t(
          "vm_cloud_server.message_box.confirm_recycle",
          "回收站已开启，云主机将关机并放入回收站中"
      )
      : t(
          "vm_cloud_server.message_box.confirm_to_delete",
          "回收站已关闭，云主机将立即删除"
      );
  ElMessageBox.confirm(message, t("commons.message_box.prompt", "提示"), {
    confirmButtonText: t("commons.message_box.confirm", "确认"),
    cancelButtonText: t("commons.btn.cancel", "取消"),
    type: "warning",
  }).then(() => {
    if (isRecycleBinOpened.value) {
      VmCloudServerApi.recycleInstance(row.id as string)
          .then(() => {
            ElMessage.success(t("commons.msg.op_success"));
            refresh();
          })
          .catch((err) => {
            ElMessage.error(err.response.data.message);
          });
    } else {
      VmCloudServerApi.deleteInstance(row.id as string)
          .then(() => {
            ElMessage.success(t("commons.msg.op_success"));
            refresh();
          })
          .catch((err) => {
            ElMessage.error(err.response.data.message);
          });
    }
  });
};

// 删除创建失败机器的记录，只删除数据库记录，不调用云平台接口
const deleteFailedRecord = (cloudServerId: string) => {
  const message = t(
      "vm_cloud_server.message_box.confirm_delete_record",
      "确认删除失败记录？"
  );
  ElMessageBox.confirm(message, t("commons.message_box.prompt", "提示"), {
    confirmButtonText: t("commons.message_box.confirm", "确认"),
    cancelButtonText: t("commons.btn.cancel", "取消"),
    type: "warning",
  }).then(() => {
    VmCloudServerApi.deleteFailedRecord(cloudServerId)
        .then(() => {
          ElMessage.success(t("commons.msg.op_success"));
          refresh();
        })
        .catch((err) => {
          ElMessage.error(err.response.data.message);
        });
  });
};

/**
 * 批量操作
 */
const batchOperate = (operate: string) => {
  if (!(selectedRowData.value && selectedRowData.value.length > 0)) {
    return;
  }
  let message = t("vm_cloud_server.message_box.confirm_batch_operate", [
    instanceOperateMap.get(operate),
  ]);
  if (
      operate.toUpperCase() === "DELETED" ||
      operate.toUpperCase() === "RECYCLE_SERVER"
  ) {
    message = isRecycleBinOpened.value
        ? t(
            "vm_cloud_server.message_box.confirm_recycle",
            "回收站已开启，云主机将关机并放入回收站中"
        )
        : t(
            "vm_cloud_server.message_box.confirm_to_delete",
            "回收站已关闭，云主机将立即删除"
        );
    if (
        selectedRowData.value.every(
            (vmCloudServer) => vmCloudServer.instanceStatus === "Failed"
        )
    ) {
      message = t(
          "vm_cloud_server.message_box.confirm_batch_delete_record",
          "确认批量删除失败记录"
      );
    }
  }
  ElMessageBox.confirm(message, t("commons.message_box.prompt", "提示"), {
    confirmButtonText: t("commons.message_box.confirm", "确认"),
    cancelButtonText: t("commons.btn.cancel", "取消"),
    type: "warning",
  }).then(() => {
    VmCloudServerApi.batchOperate(_.map(selectedRowData.value, "id"), operate)
        .then(() => {
          ElMessage.success(t("commons.msg.op_success"));
          refresh();
        })
        .catch((err) => {
          ElMessage.error(err.response.data.message);
        });
  });
};
/**
 * 更多操作
 */
//授权
const authorizeBatch = () => {
  if (!(selectedRowData.value && selectedRowData.value.length > 0)) {
    ElMessage.warning(t("commons.msg.at_least_select_one", "至少选择一条数据"));
    return;
  }
  showGrantDialog();
};
const grantDialogVisible = ref<boolean>(false);
const selectedServerIds = ref<string[]>();
const showGrantDialog = () => {
  selectedServerIds.value = _.map(selectedRowData.value, "id");
  grantDialogVisible.value = true;
};

const showConfigDialog = () => {
  dialogVisible.value = true;
  VmCloudServerApi.groupsInfo().then((res) => {
    groupsTableData.value = res.data
  })

}

//删除
const deleteBatch = async () => {
  await getRecycleBinSetting();
  if (isRecycleBinOpened.value) {
    batchOperate("RECYCLE_SERVER");
  } else {
    batchOperate("DELETE");
  }
};

/**
 * 禁用批量启动
 */
const disableBatch = computed<boolean>(() => {
  return selectedRowData.value.length === 0
      ? true
      : selectedRowData.value.length > 0 &&
      selectedRowData.value.some(
          (row) => row.instanceStatus === "ToBeRecycled"
      );
});
const createAction = ref<Array<ButtonActionType>>([
  // new ButtonAction(
  //   t("commons.btn.create", "创建") +
  //     t("vm_cloud_server.label.cloudVm", "默认云主机配置"),
  //   "primary",
  //   undefined,
  //   gotoCatalog,
  //   permissionStore.hasPermission("[vm-service]CLOUD_SERVER:CREATE")
  // ),
]);
// const moreActions = ref<Array<ButtonActionType>>([
//   new ButtonAction(
//     t("vm_cloud_server.btn.power_on", "启动"),
//     undefined,
//     "POWER_ON",
//     batchOperate,
//     permissionStore.hasPermission("[vm-service]CLOUD_SERVER:START"),
//     disableBatch
//   ),
//   new ButtonAction(
//     t("commons.btn.grant", "授权"),
//     undefined,
//     undefined,
//     authorizeBatch,
//     permissionStore.hasPermission("[vm-service]CLOUD_SERVER:AUTH"),
//     disableBatch
//   ),
//   new ButtonAction(
//     t("vm_cloud_server.btn.shutdown", "关机"),
//     undefined,
//     "SHUTDOWN",
//     batchOperate,
//     permissionStore.hasPermission("[vm-service]CLOUD_SERVER:STOP"),
//     disableBatch
//   ),
//   new ButtonAction(
//     t("vm_cloud_server.btn.reboot", "重启"),
//     undefined,
//     "REBOOT",
//     batchOperate,
//     permissionStore.hasPermission("[vm-service]CLOUD_SERVER:RESTART"),
//     disableBatch
//   ),
//   new ButtonAction(
//     t("commons.btn.delete", "删除"),
//     undefined,
//     undefined,
//     deleteBatch,
//     permissionStore.hasPermission("[vm-service]CLOUD_SERVER:DELETE"),
//     disableBatch
//   ),
//   new ButtonAction(
//     t("commons.btn.renew", "续期"),
//     undefined,
//     undefined,
//     batchOpenRenew,
//     permissionStore.hasPermission("[vm-service]CLOUD_SERVER:DELETE"),
//     disableBatch
//   ),
// ]);

/**
 * 表单配置
 */
const tableConfig = ref<TableConfig>({
  searchConfig: null,
  paginationConfig: new PaginationConfig(),
  tableOperations: new TableOperations([
    TableOperations.buildButtons().newInstance(
        t("vm_cloud_server.btn.power_off", "关闭电源"),
        "primary",
        powerOff,
        undefined,
        (row: { instanceStatus: string }) => {
          return row.instanceStatus !== "Running";
        },
        permissionStore.hasPermission("[vm-service]CLOUD_SERVER:STOP")
    ),
    TableOperations.buildButtons().newInstance(
        t("vm_cloud_server.btn.reboot", "重启"),
        "primary",
        reboot,
        undefined,
        (row: { instanceStatus: string }) => {
          return row.instanceStatus !== "Running";
        },
        permissionStore.hasPermission("[vm-service]CLOUD_SERVER:RESTART")
    ),
    TableOperations.buildButtons().newInstance(
        t("commons.btn.delete", "删除"),
        "primary",
        deleteInstance,
        undefined,
        (row: { instanceStatus: string }) => {
          return (
              row.instanceStatus.toUpperCase() === "ToBeRecycled".toUpperCase() ||
              row.instanceStatus.toUpperCase() === "Deleted".toUpperCase() ||
              (row.instanceStatus.toUpperCase() !== "Running".toUpperCase() &&
                  row.instanceStatus.toUpperCase().indexOf("ING") > -1)
          );
        },
        permissionStore.hasPermission("[vm-service]CLOUD_SERVER:DELETE"),
        "#F54A45"
    ),
    TableOperations.buildButtons().newInstance(
        t("vm_cloud_disk.btn.create", "添加磁盘"),
        "primary",
        createDisk,
        undefined,
        (row: { instanceStatus: string }) => {
          return (
              row.instanceStatus.toUpperCase() === "ToBeRecycled".toUpperCase() ||
              row.instanceStatus.toUpperCase() === "Deleted".toUpperCase() ||
              (row.instanceStatus.toUpperCase() !== "Running".toUpperCase() &&
                  row.instanceStatus.toUpperCase().indexOf("ING") > -1)
          );
        },
        permissionStore.hasPermission("[vm-service]CLOUD_DISK:CREATE")
    ),
    TableOperations.buildButtons().newInstance(
        t("vm_cloud_server.btn.change_config", "配置变更"),
        "primary",
        changeVmConfig,
        undefined,
        (row: { instanceStatus: string }) => {
          return (
              row.instanceStatus === "ToBeRecycled" ||
              row.instanceStatus === "Deleted" ||
              (row.instanceStatus.toLowerCase() != "running" &&
                  row.instanceStatus.toLowerCase().indexOf("ing") > -1)
          );
        },
        permissionStore.hasPermission("[vm-service]CLOUD_SERVER:RESIZE")
    ),
  ]),
});

const getExpiredTimeMessage = (expiredTime: string) => {
  if (!expiredTime) {
    return "";
  }
  const expired = new Date(expiredTime);
  const currentDate = new Date();
  if (expired.getTime() < currentDate.getTime()) {
    return "已过期";
  } else {
    const difference = expired.getTime() - currentDate.getTime();
    if (difference < 1000 * 60 * 60 * 24 * 15) {
      return (
          "剩余" +
          Math.ceil(
              (expired.getTime() - currentDate.getTime()) / (1000 * 60 * 60 * 24)
          ) +
          "天"
      );
    }
  }
};
const getFirstIp = (list: Array<any>) => {
  if (list) {
    const publicIpItem = _.find(list, ["isPublicIp", true]);
    if (publicIpItem) {
      return publicIpItem.ip + "(公)";
    }
    return list[0].ip;
  } else {
    return "";
  }
};
/**
 * 操作按钮
 */
const buttons = ref([
  {
    label: t("设为默认"),
    icon: "",
    click: (row: VmDefaultVO) => {
      VmCloudServerApi.setVmDefault(row)
          .then(res => {
            if (res.data) refresh()
          })
    },
    show: permissionStore.hasPermission("[vm-service]CLOUD_SERVER:START"),
    disabled: (row: { isDefault: boolean }) => {
      return row.isDefault;
    }
  },
  // {
  //   label: t("vm_cloud_server.btn.shutdown", "关机"),
  //   icon: "",
  //   click: (row: VmCloudServerVO) => {
  //     shutdown(row);
  //   },
  //   show: permissionStore.hasPermission("[vm-service]CLOUD_SERVER:STOP"),
  //   disabled: (row: { instanceStatus: string }) => {
  //     return row.instanceStatus !== "Running";
  //   },
  // },
  // {
  //   label: t("vm_cloud_server.btn.power_off", "关闭电源"),
  //   icon: "",
  //   click: (row: VmCloudServerVO) => {
  //     powerOff(row);
  //   },
  //   show: permissionStore.hasPermission("[vm-service]CLOUD_SERVER:STOP"),
  //   disabled: (row: { instanceStatus: string }) => {
  //     return row.instanceStatus !== "Running";
  //   },
  // },
  // {
  //   label: t("vm_cloud_server.btn.reboot", "重启"),
  //   icon: "",
  //   click: (row: VmCloudServerVO) => {
  //     reboot(row);
  //   },
  //   show: permissionStore.hasPermission("[vm-service]CLOUD_SERVER:RESTART"),
  //   disabled: (row: { instanceStatus: string }) => {
  //     return row.instanceStatus !== "Running";
  //   },
  // },
  // {
  //   label: t("commons.btn.delete", "删除"),
  //   icon: "",
  //   click: (row: VmCloudServerVO) => {
  //     deleteInstance(row);
  //   },
  //   show: permissionStore.hasPermission("[vm-service]CLOUD_SERVER:DELETE"),
  //   disabled: (row: { instanceStatus: string }) => {
  //     return (
  //       row.instanceStatus.toUpperCase() === "ToBeRecycled".toUpperCase() ||
  //       row.instanceStatus.toUpperCase() === "Deleted".toUpperCase() ||
  //       (row.instanceStatus.toUpperCase() !== "Running".toUpperCase() &&
  //         row.instanceStatus.toUpperCase().indexOf("ING") > -1)
  //     );
  //   },
  // },
  // {
  //   label: t("vm_cloud_disk.btn.create", "添加磁盘"),
  //   icon: "",
  //   click: createDisk,
  //   show: permissionStore.hasPermission("[vm-service]CLOUD_DISK:CREATE"),
  //   disabled: (row: { instanceStatus: string }) => {
  //     return (
  //       row.instanceStatus.toUpperCase() === "ToBeRecycled".toUpperCase() ||
  //       row.instanceStatus.toUpperCase() === "Deleted".toUpperCase() ||
  //       (row.instanceStatus.toUpperCase() !== "Running".toUpperCase() &&
  //         row.instanceStatus.toUpperCase().indexOf("ING") > -1)
  //     );
  //   },
  // },
  // {
  //   label: t("vm_cloud_server.btn.change_config", "配置变更"),
  //   icon: "",
  //   click: changeVmConfig,
  //   show: permissionStore.hasPermission("[vm-service]CLOUD_SERVER:RESIZE"),
  //   disabled: (row: { instanceStatus: string }) => {
  //     return (
  //       row.instanceStatus === "ToBeRecycled" ||
  //       row.instanceStatus === "Deleted" ||
  //       (row.instanceStatus.toLowerCase() != "running" &&
  //         row.instanceStatus.toLowerCase().indexOf("ing") > -1)
  //     );
  //   },
  // },
  // {
  //   label: "续费",
  //   icon: "",
  //   click: openRenew,
  //   show: permissionStore.hasPermission("[vm-service]CLOUD_SERVER:RESIZE"),
  //   disabled: (row: { instanceChargeType: string }) => {
  //     return row.instanceChargeType === "PostPaid";
  //   },
  // },
]);
const exportData = () => {
  const condition = table?.value.getTableSearch();
  const tableParams = TableSearch.toSearchParams(condition);
  VmCloudServerApi.exportData(tableParams, loading);
};
const groupsHandleDelete = (index, id) => {
  VmCloudServerApi.deleteGroups({"id": id}).then(res => {
    if (res.data) {
      ElMessage({
        message: '删除成功',
        type: 'success'
      })
      VmCloudServerApi.groupsInfo().then((res) => {
        groupsTableData.value = res.data
      })
    } else {
      ElMessage({
        message: '删除失败',
        type: 'error'
      })
    }
  })
}
const pushCreate = () => {
  pushCreateDialogVisible.value = true
  VmCloudServerApi.pusherList().then((res) => {
    pusherOptions.value = res.data
  })
}

const pusherCreateCancel = () =>{
  pushCreateDialogVisible.value = false
  pusher.value = ""
  groupName.value = ""
}

const pusherCreateConfirm = () => {

  VmCloudServerApi.saveGroups({"groupName": groupName.value,"pusherId":pusher.value}).then(res => {
    if (res.data) {
      ElMessage({
        message: '创建成功',
        type: 'success'
      })
      VmCloudServerApi.groupsInfo().then((res) => {
        groupsTableData.value = res.data
      })
      pushCreateDialogVisible.value = false
    } else {
      ElMessage({
        message: '创建失败',
        type: 'error'
      })
    }
  })
}
const configGroup = () =>{
  if(groupsMultipleSelection.value.length > 0){
    pushGroupsDialogVisible.value = true;
    VmCloudServerApi.groupList().then((res) => {
      groupsOptions.value = res.data
    })
  }else {
    ElMessage({
      message: '请至少选择一条记录',
      type: 'warning'
    })
  }
}

const groupsHandleSelectionChange = (val) =>{
  groupsMultipleSelection.value = val;
}

const groupConfigCancel = () =>{
  pushGroupsDialogVisible.value = false
  groupsSet.value = ""
}
const groupConfigConfirm = () =>{
  pushGroupsDialogVisible.value = false

for (let i in groupsMultipleSelection.value){
  groupsMultipleSelection.value[i].groups = groupsSet.value
}

  VmCloudServerApi.groupSave(groupsMultipleSelection.value).then((res) => {
    if(res.data){
      ElMessage({
        message: '设置成功',
        type: 'success'
      })
      groupsSet.value = ""
      pushGroupsDialogVisible.value = false
      VmCloudServerApi.listpull(
          {
            currentPage: tableConfig.value.paginationConfig.currentPage,
            pageSize: tableConfig.value.paginationConfig.pageSize
          },
          tableLoading
      ).then((res) => {
        tableData.value = res.data.records;

        tableConfig.value.paginationConfig?.setTotal(
            res.data.total,
            tableConfig.value.paginationConfig
        );
        tableConfig.value.paginationConfig?.setCurrentPage(
            res.data.current,
            tableConfig.value.paginationConfig
        );
      });
    }else{
      ElMessage({
        message: '创建失败',
        type: 'error'
      })
    }
  })
}
</script>
<template>
  <div>
    <el-dialog
        title="推流端组设置"
        v-model="pushGroupsDialogVisible"
        width="40%">
      <!--        :before-close="handleClose">-->
      <div class="demo-input-suffix">
        <el-select v-model="groupsSet" style="margin-top:20px" placeholder="请选择组">
          <el-option
              v-for="item in groupsOptions"
              :key="item.id"
              :label="item.groupName"
              :value="item.id">
          </el-option>
        </el-select>
      </div>
      <div slot="footer" class="dialog-footer" style="margin-top:20px;">
        <el-button @click="groupConfigCancel">取 消</el-button>
        <el-button type="primary" @click="groupConfigConfirm">确 定</el-button>
      </div>
    </el-dialog>
    <el-dialog
        title="推流端组创建"
        v-model="pushCreateDialogVisible"
        width="40%">
      <!--        :before-close="handleClose">-->
      <div class="demo-input-suffix">
        <span>组名：</span>
        <el-input
            placeholder="请输入组名"
            prefix-icon="el-icon-search"
            style="margin-top:20px"
            v-model="groupName">
        </el-input>
        <div style="margin-top:20px;">推流id：</div>
        <el-select v-model="pusher" style="margin-top:20px" placeholder="请选择推流用户id">
          <el-option
              v-for="item in pusherOptions"
              :key="item.userId"
              :label="item.userName"
              :value="item.userName">
          </el-option>
        </el-select>
      </div>
      <div slot="footer" class="dialog-footer" style="margin-top:20px;">
        <el-button @click="pusherCreateCancel">取 消</el-button>
        <el-button type="primary" @click="pusherCreateConfirm">确 定</el-button>
       </div>
    </el-dialog>
    <el-dialog
        title="推流端组配置"
        v-model="dialogVisible"
        width="80%">
      <!--        :before-close="handleClose">-->
      <el-button type="success" @click="pushCreate">新建组</el-button>
      <el-table
          :data="groupsTableData"
          style="width: 100%">
        <el-table-column
            label="id"
            prop="id"
            v-if="false"
            width="180">
        </el-table-column>
        <el-table-column
            label="组名"
            prop="groupName"
            width="180">
        </el-table-column>
        <el-table-column
            label="推流账号"
            prop="pusherId"
            width="180">
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button
                size="mini"
                type="danger"
                @click="groupsHandleDelete(scope.$index, scope.row.id)">删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-button type="primary" @click="search(new TableSearch())">
      <template #icon>
        <!-- 刷新图标 (自定义 SVG) -->
        <svg xmlns="http://www.w3.org/2000/svg"
             width="16" height="16"
             viewBox="0 0 24 24"
             fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="23 4 23 10 17 10"></polyline>
          <polyline points="1 20 1 14 7 14"></polyline>
          <path d="M3.51 9a9 9 0 0114.13-3.36L23 10"></path>
          <path d="M20.49 15a9 9 0 01-14.13 3.36L1 14"></path>
        </svg>
      </template>
    </el-button>
    <el-button type="primary" @click="showConfigDialog">推流端配置</el-button>
    <el-button type="primary" @click="configGroup">设置组</el-button>
  </div>
  <ce-table
      localKey="vmCloudServerTable"
      v-loading="tableLoading"
      :data="tableData"
      :tableConfig="tableConfig"
      :show-selected-count="false"
      @selection-change="groupsHandleSelectionChange"
      @clearCondition="clearCondition"
      row-key="id"
      height="100%"
      ref="table"
  >
    <template #toolbar>
      <ButtonToolBar :actions="createAction || []" :ellipsis="4"/>
      <ButtonToolBar :actions="moreActions || []" :ellipsis="2"/>
    </template>
    <el-table-column type="selection"/>
    <el-table-column
        prop="isOnline"
        column-key="isOnline"
        label="是否在线"

        min-width="100px"
    >
      <template #default="scope">
        <div style="display: flex;margin-left: 25%">
          <div v-if="scope.row.isOnline"
               style="background-color: greenyellow;width: 10px;height: 10px;border: 1px solid black"></div>
          <div v-else style="background-color: red;width: 10px;height: 10px;border: 1px solid black"></div>
        </div>
      </template>
    </el-table-column>
    <el-table-column
        prop="machineCode"
        column-key="machineCode"
        label="machinecode"
        min-width="200px"
    ></el-table-column>
    <el-table-column
        prop="groups"
        column-key="groups"
        label="分组"
        min-width="120px"
    >
      <template #default="scope">

      </template>

    </el-table-column>
    <el-table-column
        prop="sendIp"
        column-key="sendIp"
        label="拉流端ip"

        min-width="200px"
    ></el-table-column>
    <el-table-column
        prop="sendLocation"
        column-key="sendLocation"
        label="拉流端位置"
        min-width="200px"
    ></el-table-column>
    <el-table-column
        prop="sendOperator"
        column-key="sendOperator"
        label="拉流端服务商"
        min-width="200px"
    ></el-table-column>
    <el-table-column
        prop="receiveIp"
        column-key="receiveIp"
        label="中继端ip"

        min-width="100px"
    ></el-table-column>

    <el-table-column
        prop="receiveServer"
        column-key="receiveServer"
        label="中继端服务器实例名称"

        min-width="200px"
    ></el-table-column>
    <el-table-column
        prop="receiveStatus"
        column-key="receiveStatus"
        label="服务器状态"
        :filters="instanceStatusForTableSelect"
        :filter-multiple="false"
        min-width="120px"
    >
      <template #default="scope">
        <div v-if="scope.row.receiveStatus != '-'" style="display: flex; align-items: center">
          <VmServerStatusIcon
              :status="scope.row.receiveStatus"
          ></VmServerStatusIcon>
          <span style="margin-left: 7px"
          >{{ InstanceStatusUtils.getStatusName(scope.row.receiveStatus) }}
          </span>
        </div>
        <div v-else>
          {{ scope.row.receiveStatus }}
        </div>
      </template>
    </el-table-column>
    <el-table-column
        prop="createTime"
        column-key="createTime"
        sortable
        label="创建时间"
        min-width="180px"
    ></el-table-column>

    <el-table-column
        prop="belong"
        column-key="belong"

        label="推流端账号"
        min-width="120px"
    ></el-table-column>
    <!--    <fu-table-operations-->
    <!--      :ellipsis="2"-->
    <!--      :columns="columns"-->
    <!--      :buttons="buttons"-->
    <!--      :label="$t('commons.operation')"-->
    <!--      ="right"-->
    <!--    />-->

    <template #buttons>
      <!-- 导出 -->
      <el-button
          :loading="loading"
          @click="exportData('xlsx')"
          style="width: 32px"
      >
        <ce-icon v-if="!loading" size="16" code="icon_bottom-align_outlined"/>
        <template #loading>
          <ce-icon
              class="is-loading"
              style="margin-left: 6px"
              size="16"
              code="Loading"
          />
        </template>
      </el-button>
      <CeTableColumnSelect :columns="columns"/>
    </template>
  </ce-table>

  <!-- 授权页面弹出框 -->
  <el-dialog
      v-model="grantDialogVisible"
      :title="$t('commons.grant')"
      width="35%"
      destroy-on-close
  >
    <Grant
        :ids="selectedServerIds || []"
        resource-type="vm"
        v-model:dialogVisible="grantDialogVisible"
        @refresh="refresh"
    />
  </el-dialog>
  <AddDisk ref="addDiskRef"></AddDisk>
  <ChangeConfig ref="changeConfigRef"></ChangeConfig>
  <Renew ref="renewRef"></Renew>
</template>
<style lang="scss" scoped>
.name-span-class {
  color: var(--el-color-primary);
}

.name-span-class:hover {
  cursor: pointer;
}

.highlight {
  color: var(--el-color-primary);
}

.role_display {
  height: 24px;
  line-height: 24px;
  display: flex;

  .role_numbers {
    cursor: pointer;
    margin-left: 8px;
    border-radius: 2px;
    padding: 0 6px;
    height: 24px;
    font-size: 14px;
    background-color: rgba(31, 35, 41, 0.1);
  }

  .role_numbers:hover {
    background-color: #ebf1ff;
    color: #3370ff;
  }
}
</style>
