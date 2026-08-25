package com.assessment.booking.service;

import com.assessment.booking.dto.request.ResourceCreateRequest;
import com.assessment.booking.dto.request.ResourceUpdateRequest;
import com.assessment.booking.dto.response.PagedResponse;
import com.assessment.booking.dto.response.ResourceResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ResourceService {

    ResourceResponse createResource(ResourceCreateRequest request);

    ResourceResponse getResourceById(Long id);

    PagedResponse<ResourceResponse> getAllResources(Pageable pageable, Boolean activeOnly);

    List<ResourceResponse> getAllActiveResources();

    ResourceResponse updateResource(Long id, ResourceUpdateRequest request);

    void deleteResource(Long id);
}
