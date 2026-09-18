package com.smartfactory.service;

import com.smartfactory.entity.AuditLog;
import com.smartfactory.entity.User;
import com.smartfactory.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(User user, String action, String entityType) {
        AuditLog log = new AuditLog(user, action, entityType);
        auditLogRepository.save(log);
    }

    public void log(User user, String action, String entityType, Long entityId) {
        AuditLog log = new AuditLog(user, action, entityType, entityId);
        auditLogRepository.save(log);
    }

    public void log(User user, String action, String entityType, Long entityId,
                    String ipAddress) {
        AuditLog log = new AuditLog(user, action, entityType, entityId);
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }

    public void logAuthSuccess(String username, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setAction("AUTH_LOGIN_SUCCESS");
        log.setEntityType("USER");
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }

    public void logAuthFailure(String username, String reason, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setAction("AUTH_LOGIN_FAILURE");
        log.setEntityType("USER");
        log.setNewValues("{\"username\":\"" + username + "\",\"reason\":\"" + reason + "\"}");
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
            .limit(limit)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUser(Long userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
