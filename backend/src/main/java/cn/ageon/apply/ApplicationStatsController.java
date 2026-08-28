package cn.ageon.apply;

import cn.ageon.apply.dto.StatsOverviewResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications/stats")
public class ApplicationStatsController {
    private final ApplicationStatsService applicationStatsService;

    public ApplicationStatsController(ApplicationStatsService applicationStatsService) {
        this.applicationStatsService = applicationStatsService;
    }

    @GetMapping("/overview")
    public StatsOverviewResponse overview(Authentication authentication) {
        return applicationStatsService.overview(authentication);
    }
}