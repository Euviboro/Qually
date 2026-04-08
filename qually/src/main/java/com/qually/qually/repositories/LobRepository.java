package com.qually.qually.repositories;

import com.qually.qually.models.Client;
import com.qually.qually.models.Lob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LobRepository extends JpaRepository<Lob, Integer> {
    List<Lob> findByClient(Client client);

    List<Lob> findByTeamLeader_UserEmail(String userEmail);

    Optional<Lob> findByLobNameAndClient_ClientId(String lobName, Integer clientId);
}