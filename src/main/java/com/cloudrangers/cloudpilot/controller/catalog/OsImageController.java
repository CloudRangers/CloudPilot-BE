package com.cloudrangers.cloudpilot.controller.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.OsImageResponse;
import com.cloudrangers.cloudpilot.service.catalog.OsImageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog/os-images")
@RequiredArgsConstructor
public class OsImageController {

    private final OsImageQueryService osImageQueryService;

    /**
     * OS Image 목록 조회 API
     *
     * 예시 호출:
     *   GET /catalog/os-images?page=0&size=50&zoneId=1&sort=name,asc
     *
     * @param page       페이지 번호 (0-base)
     * @param size       페이지 크기
     * @param providerId (옵션) 프로바이더 ID
     * @param zoneId     (옵션) 존 ID
     * @param q          (옵션) 이름/패밀리 검색어
     * @param sort       (옵션) 정렬 (예: "name,asc")
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<PageResponse<OsImageResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort
    ) {
        PageResponse<OsImageResponse> result =
                osImageQueryService.getOsImages(page, size, providerId, zoneId, q, sort);

        return ApiResponse.success(result);
    }
}
