package com.cloudrangers.cloudpilot.controller.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.PlaybookResponse;
import com.cloudrangers.cloudpilot.service.catalog.PlaybookQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/playbooks")
@RequiredArgsConstructor
public class PlaybookController {

    private final PlaybookQueryService service;

    @GetMapping
    public ApiResponse<PageResponse<PlaybookResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String osFamily,
            @RequestParam(required = false) String arch,
            @RequestParam(required = false) String tag
    ) {
        var result = service.getPlaybooks(page, size, q, osFamily, arch, tag);
        return ApiResponse.ok(result);
    }
}
