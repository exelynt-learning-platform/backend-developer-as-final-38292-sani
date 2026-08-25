package com.assessment.booking.controller;

import com.assessment.booking.dto.request.ResourceCreateRequest;
import com.assessment.booking.dto.request.ResourceUpdateRequest;
import com.assessment.booking.dto.response.ApiResponse;
import com.assessment.booking.dto.response.PagedResponse;
import com.assessment.booking.dto.response.ResourceResponse;
import com.assessment.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@Tag(name = "2. Resources", description = "Endpoints for managing bookable resources (ADMIN CRUD, USER Read-Only)")
@SecurityRequirement(name = "BearerAuth")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "Get all resources (Paginated & Filterable)",
               description = "Returns paginated list of resources. Both USER and ADMIN roles can access this endpoint.")
    public ResponseEntity<ApiResponse<PagedResponse<ResourceResponse>>> getAllResources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<ResourceResponse> response = resourceService.getAllResources(pageable, activeOnly);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource by ID",
               description = "Returns details of a specific resource. Both USER and ADMIN roles can access this endpoint.")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResourceById(@PathVariable Long id) {
        ResourceResponse response = resourceService.getResourceById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new resource (ADMIN Only)",
               description = "Allows ADMIN to create a new resource with hourly rate, capacity, location, and type.")
    public ResponseEntity<ApiResponse<ResourceResponse>> createResource(
            @Valid @RequestBody ResourceCreateRequest request
    ) {
        ResourceResponse response = resourceService.createResource(request);
        return new ResponseEntity<>(ApiResponse.success("Resource created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing resource (ADMIN Only)",
               description = "Allows ADMIN to update resource details like name, hourly rate, capacity, and active status.")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceUpdateRequest request
    ) {
        ResourceResponse response = resourceService.updateResource(id, request);
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a resource (ADMIN Only)",
               description = "Allows ADMIN to remove a resource from the catalog.")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.ok(ApiResponse.success("Resource deleted successfully", null));
    }
}
