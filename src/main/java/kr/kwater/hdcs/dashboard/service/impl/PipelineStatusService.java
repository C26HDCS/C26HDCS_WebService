package kr.kwater.hdcs.dashboard.service.impl;

import java.util.*;
import kr.kwater.hdcs.dashboard.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineStatusService {
    private final JdbcTemplate jdbc;
    private final kr.kwater.hdcs.common.DatabaseBackoff retry = new kr.kwater.hdcs.common.DatabaseBackoff();
    @org.springframework.beans.factory.annotation.Value("${hdcs.background.enabled:true}")
    private boolean backgroundEnabled = true;

    public void refresh() {
        jdbc.queryForList("SELECT public.hdcs_prune_python_component_status()");
        List<Long> ids = jdbc.queryForList("SELECT DISTINCT device_id FROM public.v_python_component_health ORDER BY device_id", Long.class);
        for (Long id : ids) jdbc.queryForList("SELECT public.hdcs_refresh_python_device_status(?)", id);
    }

    @Scheduled(fixedDelayString = "${hdcs.pipeline-status.interval-ms:5000}", initialDelay = 15000)
    public void checkTimeouts() {
        if (!backgroundEnabled || !retry.acquire()) return;
        try { refresh(); retry.success(); }
        catch (org.springframework.dao.DataAccessException ex) {
            log.error("PIPELINE_STATUS_CHECK_FAILED reason={} retry_ms={}", ex.getClass().getSimpleName(), retry.failure());
        } catch (RuntimeException ex) {
            retry.failure();
            throw ex;
        }
    }

    public Map<String, List<PipelineComponentVO>> components() {
        Map<String, List<PipelineComponentVO>> result = new HashMap<>();
        jdbc.query("SELECT * FROM public.v_python_component_health ORDER BY device_id,data_kind,component,scope", rs -> {
            result.computeIfAbsent(rs.getString("device_id"), key -> new ArrayList<>()).add(new PipelineComponentVO(
                    rs.getString("data_kind"), rs.getString("component"), rs.getString("scope"),
                    rs.getString("effective_status"), rs.getString("detail"),
                    rs.getTimestamp("checked_at").toInstant().toString(), rs.getBoolean("expired")));
        });
        return result;
    }
}
