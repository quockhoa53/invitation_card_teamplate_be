package com.invitation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportSchemaKeysResponse {
    private int totalSubmitted;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
    private List<TemplateSchemaKeyResponse> keys;
}
