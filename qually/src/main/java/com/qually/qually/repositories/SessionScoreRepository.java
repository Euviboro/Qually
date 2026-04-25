package com.qually.qually.repositories;

import com.qually.qually.models.SessionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionScoreRepository extends JpaRepository<SessionScore, Integer> {

    /** Returns all score rows for a single session (both original and post-dispute). */
    List<SessionScore> findByAuditSession_SessionId(Long sessionId);

    /**
     * Bulk-fetches all score rows for a set of session IDs in a single query.
     * Used by {@link com.qually.qually.services.ResultsService} to avoid the
     * N+1 problem that occurred when scores were fetched per session in a loop.
     */
    List<SessionScore> findByAuditSession_SessionIdIn(List<Long> sessionIds);
}
