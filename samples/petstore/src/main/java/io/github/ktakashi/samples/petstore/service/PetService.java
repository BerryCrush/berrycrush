package io.github.ktakashi.samples.petstore.service;

import io.github.ktakashi.samples.petstore.dto.NewPet;
import io.github.ktakashi.samples.petstore.dto.PetResponse;
import io.github.ktakashi.samples.petstore.entity.Pet;
import io.github.ktakashi.samples.petstore.entity.PetStatus;
import io.github.ktakashi.samples.petstore.repository.PetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service for pet CRUD operations.
 */
@Service
@Transactional
public class PetService {

    private final PetRepository petRepository;

    public PetService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    /**
     * List all pets with optional filtering.
     */
    @Transactional(readOnly = true)
    public List<PetResponse> listPets(Integer limit, String status) {
        List<Pet> pets;
        
        if (status != null && !status.isBlank()) {
            PetStatus petStatus = PetStatus.valueOf(status.toUpperCase());
            pets = petRepository.findByStatus(petStatus);
        } else {
            pets = petRepository.findAll();
        }

        // Apply limit
        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;
        if (pets.size() > effectiveLimit) {
            pets = pets.subList(0, effectiveLimit);
        }

        return pets.stream()
            .map(PetResponse::from)
            .toList();
    }

    /**
     * Get total count of pets.
     */
    @Transactional(readOnly = true)
    public long countPets(String status) {
        if (status != null && !status.isBlank()) {
            PetStatus petStatus = PetStatus.valueOf(status.toUpperCase());
            return petRepository.findByStatus(petStatus).size();
        }
        return petRepository.count();
    }

    /**
     * Get a pet by ID.
     */
    @Transactional(readOnly = true)
    public Optional<PetResponse> getPetById(Long id) {
        return petRepository.findById(id)
            .map(PetResponse::from);
    }

    /**
     * Create a new pet.
     */
    public PetResponse createPet(NewPet newPet) {
        Pet pet = new Pet();
        pet.setName(newPet.name());
        pet.setStatus(parseStatus(newPet.status()));
        pet.setCategory(newPet.category());
        pet.setTags(newPet.tags() != null ? newPet.tags() : List.of());
        if (newPet.price() != null) {
            pet.setPrice(BigDecimal.valueOf(newPet.price()));
        }

        Pet saved = petRepository.save(pet);
        return PetResponse.from(saved);
    }

    /**
     * Update an existing pet.
     */
    public Optional<PetResponse> updatePet(Long id, NewPet newPet) {
        return petRepository.findById(id)
            .map(pet -> {
                pet.setName(newPet.name());
                pet.setStatus(parseStatus(newPet.status()));
                pet.setCategory(newPet.category());
                pet.setTags(newPet.tags() != null ? newPet.tags() : List.of());
                if (newPet.price() != null) {
                    pet.setPrice(BigDecimal.valueOf(newPet.price()));
                }
                Pet saved = petRepository.save(pet);
                return PetResponse.from(saved);
            });
    }

    /**
     * Delete a pet.
     */
    public boolean deletePet(Long id) {
        if (petRepository.existsById(id)) {
            petRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private PetStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return PetStatus.AVAILABLE;
        }
        return PetStatus.valueOf(status.toUpperCase());
    }
}
