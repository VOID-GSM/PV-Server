package pv.domain.home.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.home.dto.DashboardResponse;
import pv.domain.home.dto.HomeTodosResponse;
import pv.domain.home.service.HomeService;

@RestController
@RequestMapping("/home")
public class HomeController {
    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/todos")
    public HomeTodosResponse getTodos(@AuthenticationPrincipal  Long memberId) {
        return homeService.getTodos(memberId);
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard(@AuthenticationPrincipal Long memberId) {
        return homeService.getDashboard(memberId);
    }
}
