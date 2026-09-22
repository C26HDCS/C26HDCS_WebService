package kr.kwater.hdcs.setting.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveChanges(
            @RequestBody List<Map<String, Object>> items) throws Exception {
        int count = settingService.saveChanges(items);
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/groups/manage")
    public ResponseEntity<List<Map<String, Object>>> getGroupList() throws Exception {
        return ResponseEntity.ok(settingService.getGroupList());
    }

    @PostMapping("/groups/add")
    public ResponseEntity<?> addGroup(@RequestBody Map<String, String> body) {
        try {
            settingService.addGroup(body.get("groupName"));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            String msg = e.getMessage();
            err.put("message", (msg != null && msg.contains("uq_tb_device_group_name"))
                    ? "이미 존재하는 그룹명입니다." : (msg != null ? msg : "추가 중 오류가 발생했습니다."));
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/groups/{groupId}")
    public ResponseEntity<?> updateGroup(@PathVariable long groupId,
                                         @RequestBody Map<String, String> body) {
        try {
            settingService.updateGroup(groupId, body.get("groupName"));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            String msg = e.getMessage();
            err.put("message", (msg != null && msg.contains("uq_tb_device_group_name"))
                    ? "이미 존재하는 그룹명입니다." : (msg != null ? msg : "수정 중 오류가 발생했습니다."));
            return ResponseEntity.badRequest().body(err);
        }
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable long groupId) {
        try {
            settingService.deleteGroup(groupId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            Map<String, String> err = new HashMap<>();
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
