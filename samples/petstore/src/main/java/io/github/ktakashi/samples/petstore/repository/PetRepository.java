package io.github.ktakashi.samples.petstore.repository;

import io.github.ktakashi.samples.petstore.entity.Pet;
import io.github.ktakashi.samples.petstore.entity.PetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Pet entities.
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    /**
     * Find pets by status.
     */
    List<Pet> findByStatus(PetStatus status);

    /**
     * Find pets by status with limit.
     */
    List<Pet> findByStatusOrderByIdAsc(PetStatus status);
}
