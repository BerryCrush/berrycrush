package org.berrycrush.samples.petstore.dto;

import org.berrycrush.samples.petstore.entity.Coordinate;
import org.berrycrush.samples.petstore.entity.Pet;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * DTO for pet responses.
 */
public record PetResponseV2(
    Long id,
    String name,
    String nickname,
    Integer age,
    Boolean vaccinated,
    LocalDate vaccinationDate,
    String status,
    Double[] coordinates
) {
    /**
     * Creates a PetResponse from a Pet entity.
     */
    public static PetResponseV2 from(Pet pet) {
        var coordinates = pet.getCoordinates();
        var vaccinationDate = pet.getVaccinationDate();
        return new PetResponseV2(
                pet.getId(),
                pet.getName(),
                pet.getNickname(),
                pet.getAge(),
                pet.getVaccinated(),
                vaccinationDate != null
                        ? pet.getVaccinationDate().atZone(ZoneId.systemDefault()).toLocalDate()
                        : null,
                pet.getStatus().name(),
                coordinates != null
                        ? new Double[] { coordinates.getLatitude(), coordinates.getLongitude() }
                        : null
        );
    }
}
