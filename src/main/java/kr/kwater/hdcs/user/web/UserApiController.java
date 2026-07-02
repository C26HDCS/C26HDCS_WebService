package kr.kwater.hdcs.user.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping; 
import org.springframework.web.bind.annotation.RestController; 

import kr.kwater.hdcs.user.dao.UserDAO;
import kr.kwater.hdcs.user.vo.UserVO;

@RestController 
@RequestMapping("/api/user") 
public class UserApiController {
    
    private final UserDAO userDAO;

    public UserApiController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // 리스트 조회 API (최종 URL: /api/user/list)
    @GetMapping("/list")
    public List<UserVO> getUserList() {
        return userDAO.findAllUsers();
    }
}