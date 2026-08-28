package cn.ageon.apply;

import cn.ageon.apply.dto.ApplicationResponse;
import cn.ageon.apply.dto.ChangeStatusRequest;
import cn.ageon.apply.dto.CreateApplicationRequest;
import cn.ageon.apply.dto.SourcePreviewRequest;
import cn.ageon.apply.dto.SourcePreviewResponse;
import cn.ageon.apply.dto.StatusOptionResponse;
import cn.ageon.apply.dto.UpdateApplicationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {
    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public List<ApplicationResponse> listApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String companyType,
            @RequestParam(required = false) Boolean hasDeadline,
            Authentication authentication) {
        return applicationService.list(status, keyword, companyType, hasDeadline, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createApplication(@Valid @RequestBody CreateApplicationRequest request,
                                                 Authentication authentication) {
        return applicationService.create(request, authentication);
    }

    @PostMapping("/source-preview")
    public SourcePreviewResponse previewSource(@Valid @RequestBody SourcePreviewRequest request) {
        return applicationService.previewSource(request);
    }

    /** 精确路径优先于 /{id} 模板匹配，不会冲突。 */
    @GetMapping("/meta/statuses")
    public List<StatusOptionResponse> listStatusMeta() {
        return applicationService.listStatusMeta();
    }

    @GetMapping("/{id}")
    public ApplicationResponse getApplication(@PathVariable Long id, Authentication authentication) {
        return applicationService.get(id, authentication);
    }

    @PatchMapping("/{id}")
    public ApplicationResponse updateApplication(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateApplicationRequest request,
                                                 Authentication authentication) {
        return applicationService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApplication(@PathVariable Long id, Authentication authentication) {
        applicationService.delete(id, authentication);
    }

    @PostMapping("/{id}/status")
    public ApplicationResponse changeStatus(@PathVariable Long id,
                                            @Valid @RequestBody ChangeStatusRequest request,
                                            Authentication authentication) {
        return applicationService.changeStatus(id, request, authentication);
    }
}
