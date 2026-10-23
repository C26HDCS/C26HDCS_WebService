package kr.kwater.hdcs.log.service;

public record AuditEvent(String recordKey, String actor, String type, String message, String occurredAt) {
}
