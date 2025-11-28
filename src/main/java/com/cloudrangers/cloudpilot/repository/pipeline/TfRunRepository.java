package com.cloudrangers.cloudpilot.repository.pipeline;

import com.cloudrangers.cloudpilot.domain.pipeline.TfRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TfRunRepository extends JpaRepository<TfRun, Long> {

    /**
     * 특정 provision_job_id에 대해
     * action = 'apply' AND status = 'succeeded' 인 가장 마지막 TfRun 1건 조회
     *
     * ⚠️ enum / 문자열이 DB에서 대소문자 다르면 여기 문자열도 맞게 바꿔줘야 함
     */
    @Query(
            value = """
            SELECT *
            FROM tf_run
            WHERE provision_job_id = :provisionJobId
              AND action = 'apply'
              AND status = 'succeeded'
            ORDER BY id DESC
            LIMIT 1
        """,
            nativeQuery = true
    )
    Optional<TfRun> findLatestSucceededApply(@Param("provisionJobId") Long provisionJobId);

    List<TfRun> findTop5ByOrderByIdDesc();
}
