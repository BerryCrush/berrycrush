package org.berrycrush.samples.petstore.controller;

import jakarta.validation.Valid;
import org.berrycrush.samples.petstore.dto.ErrorResponse;
import org.berrycrush.samples.petstore.dto.NewPet;
import org.berrycrush.samples.petstore.dto.PetResponse;
import org.berrycrush.samples.petstore.service.PetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for pet operations.
 * <p>
 * This controller is mounted at /api/v0/pets to demonstrate multi-host API testing.
 * The petstore API uses /api/v0 while auth API uses /auth/api/v1, allowing us to
 * test with different base URLs.
 */
@RestController
@RequestMapping("/api/v0/pets")
public class V0PetController {

    private final PetService petService;

    public V0PetController(PetService petService) {
        this.petService = petService;
    }

    /**
     * List all pets.
     * GET /pets?limit=20&status=available
     */
    @GetMapping
    public ResponseEntity<?> listPets(
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false) String status) {
        
        // Validate limit
        if (limit != null && (limit < 1 || limit > 100)) {
            return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Invalid limit value", 
                    List.of("limit must be between 1 and 100")));
        }

        // Validate status
        if (status != null && !status.isBlank()) {
            if (!isValidStatus(status)) {
                return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(400, "Invalid status value",
                        List.of("status must be one of: available, pending, sold")));
            }
        }

        List<PetResponse> pets = petService.listPets(limit, status);
        long total = petService.countPets(status);

        return ResponseEntity.ok(Map.of(
            "pets", pets,
            "total", total
        ));
    }

    /**
     * Get a pet by ID.
     * GET /pets/{petId}
     */
    @GetMapping("/{petId}")
    public ResponseEntity<?> getPetById(@PathVariable Long petId) {
        return petService.getPetById(petId)
            .map(pet -> ResponseEntity.ok((Object) pet))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Pet not found",
                    List.of("No pet exists with id: " + petId))));
    }

    /**
     * Create a new pet.
     * POST /pets
     */
    @PostMapping
    public ResponseEntity<?> createPet(@Valid @RequestBody NewPet newPet) {
        // Validate status if provided
        if (newPet.status() != null && !newPet.status().isBlank() && !isValidStatus(newPet.status())) {
            return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Validation failed",
                    List.of("status must be one of: available, pending, sold")));
        }

        PetResponse created = petService.createPet(newPet);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing pet, or create if not exists (upsert).
     * PUT /pets/{petId}
     * Returns 200 if updated, 201 if created.
     */
    @PutMapping("/{petId}")
    public ResponseEntity<?> updatePet(
            @PathVariable Long petId,
            @Valid @RequestBody NewPet newPet) {
        
        // Validate status if provided
        if (newPet.status() != null && !newPet.status().isBlank() && !isValidStatus(newPet.status())) {
            return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Validation failed",
                    List.of("status must be one of: available, pending, sold")));
        }

        PetService.UpsertResult result = petService.upsertPet(petId, newPet);
        
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result.pet());
        }
        return ResponseEntity.ok(result.pet());
    }

    /**
     * Delete a pet.
     * DELETE /pets/{petId}
     */
    @DeleteMapping("/{petId}")
    public ResponseEntity<?> deletePet(@PathVariable Long petId) {
        if (petService.deletePet(petId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(404, "Pet not found",
                List.of("No pet exists with id: " + petId)));
    }

    private boolean isValidStatus(String status) {
        return status.equalsIgnoreCase("available") ||
               status.equalsIgnoreCase("pending") ||
               status.equalsIgnoreCase("sold");
    }
}
