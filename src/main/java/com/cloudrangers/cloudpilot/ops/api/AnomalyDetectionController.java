//package com.cloudrangers.cloudpilot.ops.api;
//
//@RestController
//@RequestMapping("/ops/v1")
//public class AnomalyDetectionController {
//
//    @PostMapping("/anomaly-detection")
//    public ApiResponse<AnomalyDetectionResultDto> detect(
//            @RequestBody AnomalyDetectionRequestDto req
//    ) {
//        // 일단은 더미 응답
//        AnomalyDetectionResultDto dummy = new AnomalyDetectionResultDto(
//                req.getVmId(),
//                false,
//                0.0,
//                "이상 징후 없음 (stub)",
//                null,
//                "LOW"
//        );
//        return ApiResponse.ok(dummy);
//    }
//}
