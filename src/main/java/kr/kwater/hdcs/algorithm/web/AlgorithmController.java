package kr.kwater.hdcs.algorithm.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.kwater.hdcs.algorithm.service.AlgorithmService;
import kr.kwater.hdcs.algorithm.vo.AlgorithmVO;

import java.util.List;

@RestController
@RequestMapping("/api/algorithm")
@RequiredArgsConstructor
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    @GetMapping("/list")
    public ResponseEntity<List<AlgorithmVO>> getList(AlgorithmVO vo) throws Exception {
        return ResponseEntity.ok(algorithmService.getAlgorithmList(vo));
    }

    @PutMapping("/{id}/enabled")
    public ResponseEntity<Integer> updateEnabled(@PathVariable Long id,
                                                 @RequestBody AlgorithmVO vo) throws Exception {
        vo.setId(id);
        return ResponseEntity.ok(algorithmService.updateEnabled(vo));
    }
}
