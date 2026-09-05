package com.invitation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSchemaKeyRequest {

    @NotBlank(message = "Tên Key không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Key chỉ được chứa chữ cái, số và dấu gạch dưới")
    @Size(max = 100, message = "Key tối đa 100 ký tự")
    private String keyName;

    @NotBlank(message = "Nhãn hiển thị không được để trống")
    @Size(max = 150, message = "Nhãn tối đa 150 ký tự")
    private String label;

    private List<String> labels;

    @NotBlank(message = "Kiểu dữ liệu không được để trống")
    private String fieldType;

    @NotBlank(message = "Nhóm mục không được để trống")
    private String sectionName;

    private String placeholder;
    private String description;
    private String defaultValue;
    private Boolean isRequired;
    private Integer displayOrder;
    private Boolean isActive;
}
