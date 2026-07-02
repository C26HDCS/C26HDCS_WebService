package kr.kwater.hdcs.user.config;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import kr.kwater.hdcs.user.dao.UserDAO;
import kr.kwater.hdcs.user.vo.UserVO;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDAO userDAO;

    // 생성자 주입 방식으로 프로젝트의 UserDAO 장착
    public CustomUserDetailsService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("로그인 시도 아이디: " + username); 
        UserVO user = userDAO.findByUserId(username);
        System.out.println("DB 조회 결과: " + user);
        if (user == null) {
            System.out.println("에러: DB에 해당 사용자가 없습니다!"); 
            throw new UsernameNotFoundException("사용할 수 없거나 존재하지 않는 계정입니다: " + username);
        }

        return User.builder()
                .username(user.getUserId())
                .password(user.getPassword())           
                .roles(user.getRoleType() != null ? user.getRoleType().trim() : "USER") 
                .build();
    }
}