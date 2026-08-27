package com.sentinel.api.repository;

import com.sentinel.api.model.ApiPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiPolicyRepository extends JpaRepository<ApiPolicy, Long> {

    Optional<ApiPolicy> findByApplicationIdAndApiEndpointIdIsNull(Long applicationId);

    Optional<ApiPolicy> findByApplicationIdAndApiEndpointId(Long applicationId, Long apiEndpointId);

    List<ApiPolicy> findAllByApplicationId(Long applicationId);

    void deleteAllByApplicationId(Long applicationId);
}
