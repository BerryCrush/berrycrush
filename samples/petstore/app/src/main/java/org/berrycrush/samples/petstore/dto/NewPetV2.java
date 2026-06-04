package org.berrycrush.samples.petstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record NewPetV2(
        @NotBlank(message = "name is required")
        @Size(min = 1, max = 100, message = "name must be between 1 and 100 characters")
        String name,

        String nickname,

        Double[] coordinates,

        String status,

        String species,

        Integer age,

        Boolean vaccinated,

        LocalDate vaccinationDate
) {
}
