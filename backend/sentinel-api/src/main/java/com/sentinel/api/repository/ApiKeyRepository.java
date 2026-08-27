package com.sentinel.api.repository;

import com.sentinel.api.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByApplicationId(Long applicationId);

    Optional<ApiKey> findByIdAndApplicationId(Long id, Long applicationId);
}
