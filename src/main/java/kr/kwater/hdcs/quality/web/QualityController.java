package kr.kwater.hdcs.quality.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.quality.service.QualityService;
import kr.kwater.hdcs.quality.vo.QualityVO;

import java.util.List;

@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;

    @GetMapping("/status")
    public ResponseEntity<List<QualityVO>> getStatus(QualityVO vo) throws Exception {
        return ResponseEntity.ok(qualityService.getQualityList(vo));
    }
}
