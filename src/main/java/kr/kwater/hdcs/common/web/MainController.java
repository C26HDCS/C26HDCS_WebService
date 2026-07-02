package kr.kwater.hdcs.common.web;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class MainController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // HTML5 Mode: 브라우저 직접 접근 → index, AngularJS $http 요청 → partial view
    @GetMapping({"/dashboard", "/output", "/distribution", "/ftp", "/user", "/mypage",
                 "/alarm", "/log", "/config", "/storage", "/report", "/history", "/statistics", "/intro"})
    public String spa(HttpServletRequest request) {
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            String menu = request.getServletPath().substring(1);
            return menu + "/" + menu;
        }
        return "index";
    }

    @GetMapping("/register")
    public String register() { return "register"; }

    @GetMapping("/find-id")
    public String findId() { return "find-id"; }

    @GetMapping("/find-password")
    public String findPassword() { return "find-password"; }

    @GetMapping("/health")
    public String health(Model model) {
        model.addAttribute("status", "UP");
        return "health";
    }

}
