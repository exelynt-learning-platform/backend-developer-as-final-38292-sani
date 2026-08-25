package com.assessment.booking.service.impl;

import com.assessment.booking.dto.request.ResourceCreateRequest;
import com.assessment.booking.dto.request.ResourceUpdateRequest;
import com.assessment.booking.dto.response.PagedResponse;
import com.assessment.booking.dto.response.ResourceResponse;
import com.assessment.booking.entity.Resource;
import com.assessment.booking.exception.ConflictException;
import com.assessment.booking.exception.ResourceNotFoundException;
import com.assessment.booking.repository.ResourceRepository;
import com.assessment.booking.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;

    @Override
    @Transactional
    public ResourceResponse createResource(ResourceCreateRequest request) {
        log.info("Creating new resource with name: {}", request.getName());

        if (resourceRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new ConflictException("Resource with name '" + request.getName() + "' already exists");
        }

        Resource resource = Resource.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .type(request.getType() != null ? request.getType().trim().toUpperCase() : "GENERAL")
                .capacity(request.getCapacity())
                .location(request.getLocation())
                .pricePerHour(request.getPricePerHour())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Resource savedResource = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(savedResource);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        log.info("Fetching resource by id: {}", id);
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));
        return ResourceResponse.fromEntity(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ResourceResponse> getAllResources(Pageable pageable, Boolean activeOnly) {
        log.info("Fetching all resources - activeOnly: {}, pageable: {}", activeOnly, pageable);

        Page<Resource> page;
        if (Boolean.TRUE.equals(activeOnly)) {
            page = resourceRepository.findByActiveTrue(pageable);
        } else {
            page = resourceRepository.findAll(pageable);
        }

        List<ResourceResponse> responses = page.getContent()
                .stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());

        return PagedResponse.of(page, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceResponse> getAllActiveResources() {
        return resourceRepository.findByActiveTrue()
                .stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResourceResponse updateResource(Long id, ResourceUpdateRequest request) {
        log.info("Updating resource with id: {}", id);

        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));

        if (resourceRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new ConflictException("Another resource with name '" + request.getName() + "' already exists");
        }

        resource.setName(request.getName().trim());
        resource.setDescription(request.getDescription());
        if (request.getType() != null) {
            resource.setType(request.getType().trim().toUpperCase());
        }
        resource.setCapacity(request.getCapacity());
        resource.setLocation(request.getLocation());
        resource.setPricePerHour(request.getPricePerHour());
        if (request.getActive() != null) {
            resource.setActive(request.getActive());
        }

        Resource updatedResource = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(updatedResource);
    }

    @Override
    @Transactional
    public void deleteResource(Long id) {
        log.info("Deleting resource with id: {}", id);
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));
        resourceRepository.delete(resource);
    }
}
