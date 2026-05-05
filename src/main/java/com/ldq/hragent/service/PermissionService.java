package com.ldq.hragent.service;

import com.ldq.hragent.context.HrRequestContext;
import com.ldq.hragent.context.HrRequestContextHolder;
import com.ldq.hragent.exception.BizException;
import com.ldq.hragent.model.enums.ChatRole;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    private final OrganizationService organizationService;

    public PermissionService(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    public HrRequestContext getCurrentContext() {
        return HrRequestContextHolder.getContext();
    }

    public Long currentUserId() {
        return getCurrentContext().getUserId();
    }

    public String currentTenantId() {
        return getCurrentContext().getTenantId();
    }

    public ChatRole currentRole() {
        return getCurrentContext().getRole();
    }

    public void requireEmployeeOrAbove() {
        ChatRole role = currentRole();
        if (role != ChatRole.EMPLOYEE && role != ChatRole.HR && role != ChatRole.ADMIN) {
            throw new BizException("当前角色无权限执行该操作");
        }
    }

    public void requireHrOrAdmin() {
        ChatRole role = currentRole();
        if (role != ChatRole.HR && role != ChatRole.ADMIN) {
            throw new BizException("仅 HR 或管理员可执行该操作");
        }
    }

    public void requireAdmin() {
        if (currentRole() != ChatRole.ADMIN) {
            throw new BizException("仅管理员可执行该操作");
        }
    }

    public void requireTeamLeadOrHrOrAdmin() {
        ChatRole role = currentRole();
        if (role == ChatRole.HR || role == ChatRole.ADMIN) {
            return;
        }
        if (role == ChatRole.EMPLOYEE && organizationService.isTeamLead(currentUserId())) {
            return;
        }
        throw new BizException("仅团队负责人、HR 或管理员可执行该操作");
    }
}