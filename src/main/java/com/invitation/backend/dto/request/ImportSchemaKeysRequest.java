package com.invitation.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportSchemaKeysRequest {

    @NotEmpty(message = "Danh sách keys không được để trống")
    @Valid
    private List<TemplateSchemaKeyRequest> keys;

    @Builder.Default
    private Boolean overwrite = false;
}
