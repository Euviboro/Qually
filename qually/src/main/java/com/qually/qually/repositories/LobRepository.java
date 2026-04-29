package com.qually.qually.repositories;

import com.qually.qually.models.Client;
import com.qually.qually.models.Lob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Lob}.
 *
 * <p>{@code findByTeamLeader_UserEmail} removed — the {@code lobs} table no
 * longer has a team leader column.</p>
 */
@Repository
public interface LobRepository extends JpaRepository<Lob, Integer> {
    List<Lob> findByClient(Client client);
    Optional<Lob> findByLobNameAndClient_ClientId(String lobName, Integer clientId);

    List<Lob> findByClient_ClientId(Integer clientId);
}
