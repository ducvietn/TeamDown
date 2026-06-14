package com.teamup.repository;

import com.teamup.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByClassId(String classId);

    @Query("""
        SELECT DISTINCT g FROM Group g
        JOIN FETCH g.leader
        LEFT JOIN FETCH g.tasks t
        LEFT JOIN FETCH t.assignedTo
        WHERE g.groupId = :groupId
        """)
    Group findByIdWithLeaderAndTasks(@Param("groupId") Long groupId);

    @Query("""
        SELECT g FROM Group g
        JOIN FETCH g.leader
        WHERE g.leader.userId = :leaderId
        """)
    List<Group> findByLeaderId(@Param("leaderId") Long leaderId);
}
