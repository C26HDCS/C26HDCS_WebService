package kr.kwater.hdcs.alarm.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.alarm.service.AlarmService;
import kr.kwater.hdcs.alarm.vo.AlarmVO;

import java.util.List;

@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping("/list")
    public ResponseEntity<List<AlarmVO>> getList(AlarmVO vo) throws Exception {
        return ResponseEntity.ok(alarmService.getAlarmList(vo));
    }
}
