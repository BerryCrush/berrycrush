package org.berrycrush.samples.petstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO for creating or updating a pet.
 */
public record NewPet(
    @NotBlank(message = "name is required")
    @Size(min = 1, max = 100, message = "name must be between 1 and 100 characters")
    String name,

    String status,

    @Size(max = 100, message = "category must be at most 100 characters")
    String category,

    List<String> tags,

    @PositiveOrZero(message = "price must be greater than or equal to 0")
    Double price
) {
    /**
     * Creates a NewPet with default status if not provided.
     */
    public NewPet {
        if (status == null || status.isBlank()) {
            status = "available";
        }
        if (tags == null) {
            tags = List.of();
        }
    }
}
