package com.qually.qually.repositories;

import com.qually.qually.models.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {
    Optional<Client> findByClientName(String clientName);

    boolean existsByClientName(String clientName);
}