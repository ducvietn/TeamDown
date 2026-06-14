package com.teamup.repository;

import com.teamup.model.PeerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeerReviewRepository extends JpaRepository<PeerReview, Long> {

    List<PeerReview> findByGroupGroupId(Long groupId);

    List<PeerReview> findByReviewerUserId(Long reviewerId);

    List<PeerReview> findByRevieweeUserId(Long revieweeId);

    boolean existsByGroupGroupIdAndReviewerUserIdAndRevieweeUserId(
        Long groupId, Long reviewerId, Long revieweeId
    );

    @Query("""
        SELECT AVG(pr.score) FROM PeerReview pr
        WHERE pr.reviewee.userId = :userId
          AND pr.group.groupId = :groupId
        """)
    Double findAverageScoreByRevieweeAndGroup(
        @Param("userId") Long userId,
        @Param("groupId") Long groupId
    );
}
