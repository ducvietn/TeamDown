package com.teamup.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "peer_reviews",
       uniqueConstraints = {
           // A reviewer can only review a given reviewee once per group
           @UniqueConstraint(name = "uk_peer_review_pair",
                             columnNames = {"group_id", "reviewer_id", "reviewee_id"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PeerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @Min(value = 1, message = "Score must be at least 1")
    @Max(value = 5, message = "Score must not exceed 5")
    @Column(nullable = false)
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    // --- Relationships ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_review_group"))
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_review_reviewer"))
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_review_reviewee"))
    private User reviewee;

    // --- Lifecycle Callbacks ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
