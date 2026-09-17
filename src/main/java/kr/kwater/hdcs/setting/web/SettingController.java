package kr.kwater.hdcs.setting.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.setting.service.SettingService;
import kr.kwater.hdcs.setting.vo.DeviceConfigVO;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping("/list")
    public ResponseEntity<List<DeviceConfigVO>> getConfigList() throws Exception {
        return ResponseEntity.ok(settingService.getDeviceConfigList());
    }

    @GetMapping("/groups")
    public ResponseEntity<List<String>> getGroups() throws Exception {
        return ResponseEntity.ok(settingService.getGroupNames());
    }

    @PostMapping("/register/csv")
    public ResponseEntity<Map<String, Object>> registerFromCsv(
            @RequestBody List<Map<String, String>> rows) throws Exception {
        int count = settingService.registerFromCsv(rows);
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return ResponseEntity.ok(result);
    }
}
