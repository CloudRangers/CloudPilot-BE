package com.cloudrangers.cloudpilot.controller;

import com.cloudrangers.cloudpilot.common.ApiResponse;
import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.dto.user.HeadMyPageResponse;
import com.cloudrangers.cloudpilot.dto.user.MyPageResponse;
import com.cloudrangers.cloudpilot.dto.user.TeamLeaderMyPageResponse;
import com.cloudrangers.cloudpilot.repository.user.UserRepository;
import com.cloudrangers.cloudpilot.service.user.MyPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;
    private final UserRepository userRepository;

    // 1) 멤버 마이페이지
    @GetMapping("/me")
    public ApiResponse<MyPageResponse> getMyPage() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("인증 정보가 없습니다. (authentication null)");
        }

        String username = authentication.getName();   // ★ "member", "head" 같은 값
        log.info("[MyPageController] /mypage/me 요청, username={}", username);

        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. username=" + username));

        MyPageResponse response = myPageService.getMyPage(user.getId());
        return ApiResponse.success(response);
    }

    // 2) 부장(HEAD) 마이페이지
    @GetMapping("/head")
    public ApiResponse<HeadMyPageResponse> getHeadMyPage() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("인증 정보가 없습니다. (authentication null)");
        }

        String username = authentication.getName();   // ★ "head"
        log.info("[MyPageController] /mypage/head 요청, username={}", username);

        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. username=" + username));

        HeadMyPageResponse response = myPageService.getHeadMyPage(user.getId());
        return ApiResponse.success(response);
    }

    // 3) 팀장(LEADER) 마이페이지
    @GetMapping("/leader")
    public ApiResponse<TeamLeaderMyPageResponse> getLeaderMyPage() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("인증 정보가 없습니다. (authentication null)");
        }

        String username = authentication.getName();   // ★ 팀장 username
        log.info("[MyPageController] /mypage/leader 요청, username={}", username);

        User user = userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. username=" + username));

        TeamLeaderMyPageResponse response = myPageService.getTeamLeaderMyPage(user.getId());
        return ApiResponse.success(response);
    }
}
