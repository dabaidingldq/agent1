package com.yupi.yuaiagent.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaiagent.context.HrRequestContext;
import com.yupi.yuaiagent.context.HrRequestContextHolder;
import com.yupi.yuaiagent.model.audit.AuditLogRecord;
import com.yupi.yuaiagent.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AuditLogAspect(AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditLog)")
    public Object doAudit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        HrRequestContext context = null;
        try {
            context = HrRequestContextHolder.getContext();
        } catch (Exception ignored) {
        }

        String requestParams = toJson(joinPoint.getArgs());
        boolean success = true;
        String resultSummary = "success";

        try {
            Object result = joinPoint.proceed();
            resultSummary = summarizeResult(result);
            return result;
        } catch (Throwable e) {
            success = false;
            resultSummary = e.getMessage();
            throw e;
        } finally {
            try {
                auditLogService.save(AuditLogRecord.builder()
                        .userId(context == null ? null : context.getUserId())
                        .roleName(context == null || context.getRole() == null ? null : context.getRole().name())
                        .operationName(auditLog.operationName())
                        .operationModule(auditLog.operationModule())
                        .requestParams(requestParams)
                        .resultSummary(resultSummary)
                        .successFlag(success)
                        .build());
            } catch (Exception e) {
                log.error("save audit log error", e);
            }
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    private String summarizeResult(Object result) {
        if (result == null) {
            return "null";
        }
        String value = String.valueOf(result);
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}