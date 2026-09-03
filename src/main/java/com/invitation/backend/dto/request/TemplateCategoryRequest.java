package com.invitation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateCategoryRequest {

    @NotBlank(message = "Category code is required")
    @Size(max = 50, message = "Code must be under 50 characters")
    private String code;

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Name must be under 100 characters")
    private String name;

    private String emoji;

    private String description;

    private Integer displayOrder;

    private Boolean isActive;
}

