package com.sentinel.api.repository;

import com.sentinel.api.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findAllByOwnerId(Long ownerId);

    default List<Application> findByOwnerId(Long ownerId) {
        return findAllByOwnerId(ownerId);
    }

    Optional<Application> findByIdAndOwnerId(Long id, Long ownerId);
}
