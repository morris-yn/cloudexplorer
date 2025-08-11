package com.fit2cloud.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fit2cloud.base.entity.Organization;
import com.fit2cloud.base.entity.Workspace;
import com.fit2cloud.base.mapper.BaseOrganizationMapper;
import com.fit2cloud.base.mapper.BaseWorkspaceMapper;
import com.fit2cloud.base.service.IBaseWorkspaceService;
import com.fit2cloud.common.utils.CurrentUserUtils;
import com.fit2cloud.response.NodeTree;
import com.fit2cloud.service.OrganizationCommonService;
import com.fit2cloud.service.WorkspaceCommonService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author fit2cloud
 * @since
 */
@Service
public class BaseWorkspaceServiceImpl extends ServiceImpl<BaseWorkspaceMapper, Workspace> implements IBaseWorkspaceService {
    @Resource
    BaseOrganizationMapper baseOrganizationMapper;
    @Resource
    OrganizationCommonService organizationCommonService;
    @Resource
    WorkspaceCommonService workspaceCommonService;

    @Override
    public List<NodeTree> workspaceTree(Boolean isShowAllOrg) {
        QueryWrapper<Organization> orgQueryWrapper = new QueryWrapper<>();
        QueryWrapper<Workspace> workspaceQueryWrapper = new QueryWrapper<>();

        List<Workspace> workspaces = new ArrayList<>();

        // 当前角色如果是组织管理员
        if (CurrentUserUtils.isOrgAdmin()) {
            List<String> orgIdList = new ArrayList<>();
            orgIdList.add(CurrentUserUtils.getOrganizationId());
            orgIdList.addAll(organizationCommonService.getOrgIdsByParentId(CurrentUserUtils.getOrganizationId()));

            orgQueryWrapper.lambda().in(Organization::getId, orgIdList);

            List workspaceIdList = workspaceCommonService.getWorkspaceIdsByOrgIds(orgIdList);
            if (CollectionUtils.isNotEmpty(workspaceIdList)) {
                workspaceQueryWrapper.lambda().in(Workspace::getId, workspaceIdList);
                workspaces = list(workspaceQueryWrapper);
            }
        } else {
            workspaces = list(workspaceQueryWrapper);
        }

        List<Organization> organizations = baseOrganizationMapper.selectList(orgQueryWrapper);

        if (CollectionUtils.isEmpty(organizations) || (CollectionUtils.isEmpty(organizations) && CollectionUtils.isEmpty(workspaces))) {
            return new ArrayList<>();
        }

        Map<String, String> organizationIdMap = organizations.stream().filter(organization -> StringUtils.isNotEmpty(organization.getPid())).collect(Collectors.toMap(Organization::getId, Organization::getPid));
        List<String> effectiveOrgIds = new ArrayList<>();
        List<NodeTree> trees = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(workspaces)) {
            workspaces.stream().forEach(workspace -> {
                getWorkspacesOrg(workspace.getOrganizationId(), effectiveOrgIds, organizationIdMap);
                trees.add(new NodeTree(workspace.getId(), workspace.getOrganizationId(), workspace.getName()));
            });
        }

        organizations.stream().forEach(organization -> {
            if (isShowAllOrg) {
                trees.add(new NodeTree(organization.getId(), organization.getPid(), organization.getName()));
            } else {
                if (effectiveOrgIds.contains(organization.getId())) {
                    trees.add(new NodeTree(organization.getId(), organization.getPid(), organization.getName()));
                }
            }
        });

        return buildTree(trees, organizations);
    }

    private List<NodeTree> buildTree(List<NodeTree> lists, List<Organization> organizations) {
        List<NodeTree> rootNodes = new ArrayList<>();
        lists.forEach(node -> {
            if (isParent(node.getPid(), organizations)) {
                rootNodes.add(node);
            }
            lists.forEach(tNode -> {
                if (node.getId().equalsIgnoreCase(tNode.getPid())) {
                    if (node.getChildren() == null) {
                        node.setChildren(new ArrayList<>());
                    }
                    node.getChildren().add(tNode);
                }
            });
        });
        return rootNodes;
    }

    private Boolean isParent(String pid, List<Organization> organizations) {
        return StringUtils.isEmpty(pid) || !organizations.stream().map(Organization::getId).collect(Collectors.toList()).contains(pid);
    }

    private void getWorkspacesOrg(String orgId, List<String> effectiveOrgIds, Map<String, String> organizationIdMap) {
        effectiveOrgIds.add(orgId);
        String pid = organizationIdMap.get(orgId);
        if (StringUtils.isNotEmpty(pid)) {
            getWorkspacesOrg(pid, effectiveOrgIds, organizationIdMap);
        }
    }
}
