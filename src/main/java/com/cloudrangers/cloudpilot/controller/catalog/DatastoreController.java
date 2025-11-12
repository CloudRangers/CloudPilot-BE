package com.cloudrangers.cloudpilot.controller.catalog;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.dto.common.PageResponse;
import com.cloudrangers.cloudpilot.dto.response.DatastoreResponse;
import com.cloudrangers.cloudpilot.service.catalog.DatastoreQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalog/datastores")
@RequiredArgsConstructor
public class DatastoreController {

    private final DatastoreQueryService service;

    @GetMapping
    public ApiResponse<PageResponse<DatastoreResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer minFreeGiB,
            @RequestParam(required = false) Integer requestedCapacityGiB,
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.ok(
                service.getDatastores(page, size, providerId, zoneId, q, type, minFreeGiB, requestedCapacityGiB, sort)
        );
    }
}
