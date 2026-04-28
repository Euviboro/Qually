package com.qually.qually.services;

import com.qually.qually.dto.request.SubattributeOptionRequestDTO;
import com.qually.qually.dto.response.SubattributeOptionResponseDTO;
import com.qually.qually.mappers.SubattributeOptionMapper;
import com.qually.qually.models.Subattribute;
import com.qually.qually.models.SubattributeOption;
import com.qually.qually.repositories.SubattributeOptionRepository;
import com.qually.qually.repositories.SubattributeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service for standalone creation of individual {@link SubattributeOption} entities.
 *
 * <p>This service is used when an option is created in isolation — i.e. via a direct
 * API call that supplies an existing {@code subattributeId}. It is <em>not</em> involved
 * in the nested question-create/update flow, where options are built by
 * {@code SubattributeOptionMapper.toEntity(dto, parent)} using the parent passed
 * directly from the object graph.</p>
 */
@Service
public class SubattributeOptionService {

    private final SubattributeOptionRepository subattributeOptionRepository;
    private final SubattributeRepository subattributeRepository;
    private final SubattributeOptionMapper subattributeOptionMapper;

    public SubattributeOptionService(SubattributeOptionRepository subattributeOptionRepository,
                                     SubattributeRepository subattributeRepository, SubattributeOptionMapper subattributeOptionMapper) {
        this.subattributeOptionRepository = subattributeOptionRepository;
        this.subattributeRepository = subattributeRepository;
        this.subattributeOptionMapper = subattributeOptionMapper;
    }

    /**
     * Creates a new {@link SubattributeOption} linked to an existing subattribute.
     *
     * <p>{@code dto.getSubattributeId()} must be non-null. This check was previously
     * delegated to Bean Validation via {@code @NotNull(groups = OnIndividualSave.class)}
     * on the DTO. That annotation was removed because it caused the nested
     * question-create flow — where {@code subattributeId} is intentionally null — to
     * fail validation. The guard is now enforced here instead, keeping both flows
     * working correctly.</p>
     *
     * @param dto The option payload. {@code subattributeId} and {@code optionLabel}
     *            are both required.
     * @return The persisted option as a response DTO.
     * @throws IllegalArgumentException if {@code subattributeId} is null.
     * @throws EntityNotFoundException  if no subattribute with the given ID exists.
     */
    public SubattributeOptionResponseDTO createSubattributeOption(SubattributeOptionRequestDTO dto) {
        // Explicit null guard — replaces the removed @NotNull(groups = OnIndividualSave.class)
        // annotation on SubattributeOptionRequestDTO.subattributeId. Bean Validation cannot
        // distinguish the standalone flow from the nested flow at the DTO level, so the
        // check lives here instead.
        if (dto.getSubattributeId() == null) {
            throw new IllegalArgumentException("Subattribute ID is required");
        }

        Subattribute subattribute = subattributeRepository.findById(dto.getSubattributeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Subattribute with id %d not found".formatted(dto.getSubattributeId())));

        SubattributeOption saved = subattributeOptionRepository.save(subattributeOptionMapper.toEntity(dto, subattribute));

        return subattributeOptionMapper.toDTO(saved);
    }
}