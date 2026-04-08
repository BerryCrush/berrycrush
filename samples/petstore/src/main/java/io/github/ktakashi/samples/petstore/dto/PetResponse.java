package io.github.ktakashi.samples.petstore.dto;

import io.github.ktakashi.samples.petstore.entity.Pet;
import java.util.List;

/**
 * DTO for pet responses.
 */
public record PetResponse(
    Long id,
    String name,
    String status,
    String category,
    List<String> tags,
    Double price
) {
    /**
     * Creates a PetResponse from a Pet entity.
     */
    public static PetResponse from(Pet pet) {
        return new PetResponse(
            pet.getId(),
            pet.getName(),
            pet.getStatus().name().toLowerCase(),
            pet.getCategory(),
            pet.getTags(),
            pet.getPrice() != null ? pet.getPrice().doubleValue() : null
        );
    }
}
