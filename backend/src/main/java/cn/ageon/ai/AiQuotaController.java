package cn.ageon.ai;

import cn.ageon.ai.dto.AiQuotaResponse;
import cn.ageon.auth.AuthenticatedUser;
import cn.ageon.auth.SiteUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/quota")
public class AiQuotaController {
    private final AiQuotaService quotaService;

    public AiQuotaController(AiQuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @GetMapping
    public AiQuotaResponse current(Authentication authentication) {
        SiteUser user = AuthenticatedUser.requireApprovedUser(authentication);
        return quotaService.current(user.getId());
    }
}
