package com.cloudrangers.cloudpilot.repository.pkg;

import com.cloudrangers.cloudpilot.domain.pkg.PkgRequest;
import com.cloudrangers.cloudpilot.enums.PkgRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PkgRequestRepository extends JpaRepository<PkgRequest, Long> {

    // 1) 내가 올린 요청들 (정렬 없이)
    List<PkgRequest> findByRequestedBy_Id(Long userId);

    // 2) 내가 올린 요청들 (최신순)
    List<PkgRequest> findByRequestedBy_IdOrderByRequestedAtDesc(Long userId);

    // 3) 상태로만 필터
    List<PkgRequest> findByStatus(PkgRequestStatus status);

    List<PkgRequest> findByStatusOrderByRequestedAtDesc(PkgRequestStatus status);

    /**
     * 팀장용 To-do 목록
     *  - 내 팀(teamId)의 구성원들이 올린 pending 요청만 조회
     */
    @Query("""
           SELECT pr
           FROM PkgRequest pr
           WHERE pr.status = :status
             AND pr.requestedBy.id IN (
                SELECT ur.user.id
                FROM UserRole ur
                WHERE ur.team.id = :teamId
           )
           ORDER BY pr.requestedAt DESC
           """)
    List<PkgRequest> findTeamPendingRequests(
            @Param("teamId") Long teamId,
            @Param("status") PkgRequestStatus status
    );
    @Modifying
    @Query("""
       UPDATE PkgRequest p
          SET p.status = :newStatus,
              p.decidedAt = :decidedAt
        WHERE p.id = :id
          AND p.status = :expectedStatus
       """)
    int updateStatusIfMatches(
            @Param("id") Long id,
            @Param("expectedStatus") PkgRequestStatus expectedStatus,
            @Param("newStatus") PkgRequestStatus newStatus,
            @Param("decidedAt") Instant decidedAt
    );



    /**
     * 부장(HEAD)용 To-do 목록
     *  - 상태 여러 개로 필터 (예: [PENDING, L1_APPROVED])
     */
    List<PkgRequest> findByStatusInOrderByRequestedAtDesc(List<PkgRequestStatus> statuses);
}
