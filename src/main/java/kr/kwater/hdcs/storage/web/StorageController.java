package kr.kwater.hdcs.storage.web;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import kr.kwater.hdcs.storage.service.StorageService;
import kr.kwater.hdcs.storage.vo.StorageDeleteRequestVO;
import kr.kwater.hdcs.storage.vo.StorageDeleteResultVO;
import kr.kwater.hdcs.storage.vo.StorageDeleteTargetVO;
import kr.kwater.hdcs.storage.vo.StorageDeviceVO;
import kr.kwater.hdcs.storage.vo.StorageFileVO;
import kr.kwater.hdcs.storage.vo.StorageRetentionPolicyVO;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @GetMapping("/devices")
    public ResponseEntity<List<StorageDeviceVO>> getDevices() {
        return ResponseEntity.ok(storageService.getDevices());
    }

    @GetMapping("/files")
    public ResponseEntity<List<StorageFileVO>> getFiles(@RequestParam String path) {
        try {
            return ResponseEntity.ok(storageService.getFiles(path));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/files")
    public ResponseEntity<StorageDeleteResultVO> deleteFiles(@RequestBody StorageDeleteRequestVO request) {
        try {
            return ResponseEntity.ok(storageService.deleteFiles(request.getDirectoryPath(), request.getFilePaths()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/delete-targets")
    public ResponseEntity<List<StorageDeleteTargetVO>> getDeleteTargets() {
        return ResponseEntity.ok(storageService.getDeleteTargets());
    }

    @PostMapping("/delete-targets")
    public ResponseEntity<StorageDeleteTargetVO> createDeleteTarget(@RequestBody StorageDeleteTargetVO request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(storageService.createDeleteTarget(request));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/delete-targets/{id}")
    public ResponseEntity<StorageDeleteTargetVO> updateDeleteTarget(@PathVariable Long id,
                                                                    @RequestBody StorageDeleteTargetVO request) {
        try {
            return ResponseEntity.ok(storageService.updateDeleteTarget(id, request));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/delete-targets/{id}")
    public ResponseEntity<Void> deleteDeleteTarget(@PathVariable Long id) {
        try {
            storageService.deleteDeleteTarget(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/retention-policies")
    public ResponseEntity<List<StorageRetentionPolicyVO>> getRetentionPolicies() {
        return ResponseEntity.ok(storageService.getRetentionPolicies());
    }

    @PostMapping("/retention-policies")
    public ResponseEntity<StorageRetentionPolicyVO> createRetentionPolicy(@RequestBody StorageRetentionPolicyVO request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(storageService.createRetentionPolicy(request));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PutMapping("/retention-policies/{id}")
    public ResponseEntity<StorageRetentionPolicyVO> updateRetentionPolicy(@PathVariable Long id,
                                                                          @RequestBody StorageRetentionPolicyVO request) {
        try {
            return ResponseEntity.ok(storageService.updateRetentionPolicy(id, request));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/retention-policies/{id}")
    public ResponseEntity<Void> deleteRetentionPolicy(@PathVariable Long id) {
        try {
            storageService.deleteRetentionPolicy(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
