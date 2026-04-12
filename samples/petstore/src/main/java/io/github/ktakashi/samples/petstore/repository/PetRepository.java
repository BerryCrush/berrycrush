package io.github.ktakashi.samples.petstore.repository;

import io.github.ktakashi.samples.petstore.entity.Pet;
import io.github.ktakashi.samples.petstore.entity.PetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
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

    /**
     * Insert a pet with a specific ID (for upsert operation).
     * Uses native SQL to bypass JPA's auto-generation.
     */
    @Modifying
    @Query(value = "INSERT INTO pets (id, name, status, category, price, created_at, updated_at) " +
                   "VALUES (:id, :name, :status, :category, :price, :createdAt, :updatedAt)",
           nativeQuery = true)
    void insertWithId(
        @Param("id") Long id,
        @Param("name") String name,
        @Param("status") String status,
        @Param("category") String category,
        @Param("price") BigDecimal price,
        @Param("createdAt") Instant createdAt,
        @Param("updatedAt") Instant updatedAt
    );
}
