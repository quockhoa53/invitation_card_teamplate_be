package com.invitation.backend.service.impl;

import com.invitation.backend.dto.request.TemplateSchemaKeyRequest;
import com.invitation.backend.dto.response.TemplateSchemaKeyResponse;
import com.invitation.backend.entity.TemplateSchemaKey;
import com.invitation.backend.repository.TemplateSchemaKeyRepository;
import com.invitation.backend.service.TemplateSchemaKeyService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateSchemaKeyServiceImpl implements TemplateSchemaKeyService {

    private final TemplateSchemaKeyRepository repository;

    @PostConstruct
    public void init() {
        try {
            if (repository.count() == 0) {
                log.info("Khởi tạo danh sách Template Schema Keys chuẩn mặc định...");
                seedDefaultSchemaKeys();
            }
        } catch (Exception e) {
            log.warn("Không thể tự động seed schema keys (bảng có thể chưa tạo): {}", e.getMessage());
        }
    }

    @Override
    public List<TemplateSchemaKeyResponse> getActiveSchemaKeys() {
        return repository.findByIsActiveTrueOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TemplateSchemaKeyResponse> getAllSchemaKeysForAdmin() {
        return repository.findAllByOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TemplateSchemaKeyResponse getSchemaKeyById(UUID id) {
        TemplateSchemaKey key = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Schema Key"));
        return mapToResponse(key);
    }

    @Override
    @Transactional
    public TemplateSchemaKeyResponse createSchemaKey(TemplateSchemaKeyRequest request) {
        String keyName = request.getKeyName().trim();
        if (repository.existsByKeyName(keyName)) {
            throw new IllegalArgumentException("Mã Key '" + keyName + "' đã tồn tại trong hệ thống");
        }

        TemplateSchemaKey key = TemplateSchemaKey.builder()
                .keyName(keyName)
                .label(request.getLabel().trim())
                .fieldType(request.getFieldType().trim())
                .sectionName(request.getSectionName().trim())
                .placeholder(request.getPlaceholder())
                .description(request.getDescription())
                .defaultValue(request.getDefaultValue())
                .isRequired(request.getIsRequired() != null ? request.getIsRequired() : false)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return mapToResponse(repository.save(key));
    }

    @Override
    @Transactional
    public TemplateSchemaKeyResponse updateSchemaKey(UUID id, TemplateSchemaKeyRequest request) {
        TemplateSchemaKey key = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Schema Key với ID: " + id));

        String keyName = request.getKeyName().trim();
        if (repository.existsByKeyNameAndIdNot(keyName, id)) {
            throw new IllegalArgumentException("Mã Key '" + keyName + "' đã được dùng bởi mục khác");
        }

        key.setKeyName(keyName);
        key.setLabel(request.getLabel().trim());
        key.setFieldType(request.getFieldType().trim());
        key.setSectionName(request.getSectionName().trim());
        key.setPlaceholder(request.getPlaceholder());
        key.setDescription(request.getDescription());
        key.setDefaultValue(request.getDefaultValue());
        if (request.getIsRequired() != null) key.setIsRequired(request.getIsRequired());
        if (request.getDisplayOrder() != null) key.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsActive() != null) key.setIsActive(request.getIsActive());

        return mapToResponse(repository.save(key));
    }

    @Override
    @Transactional
    public TemplateSchemaKeyResponse toggleSchemaKeyStatus(UUID id) {
        TemplateSchemaKey key = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Schema Key với ID: " + id));
        key.setIsActive(!key.getIsActive());
        return mapToResponse(repository.save(key));
    }

    @Override
    @Transactional
    public void deleteSchemaKey(UUID id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy Schema Key để xóa");
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public List<TemplateSchemaKeyResponse> seedDefaultSchemaKeys() {
        List<TemplateSchemaKey> defaults = new ArrayList<>();

        // Nội Dung Lời Chúc
        defaults.add(createKey("recipientName", "Tên Người Nhận", "text", "Nội Dung Lời Chúc", "Ví dụ: Em Yêu, Bạn Thân...", 1));
        defaults.add(createKey("senderName", "Tên Người Gửi", "text", "Nội Dung Lời Chúc", "Ví dụ: Anh Yêu, Bạn Khoa...", 2));
        defaults.add(createKey("greetingTitle", "Tiêu Đề Lời Chúc", "text", "Nội Dung Lời Chúc", "Ví dụ: Chúc Mừng Sinh Nhật! 🎉", 3));
        defaults.add(createKey("greetingMessage", "Nội Dung Lời Chúc", "textarea", "Nội Dung Lời Chúc", "Nội dung lời nhắn chúc mừng...", 4));

        // Thời Gian & Địa Điểm
        defaults.add(createKey("eventDate", "Ngày Diễn Ra Sự Kiện", "date", "Thời Gian & Địa Điểm", "YYYY-MM-DD", 5));
        defaults.add(createKey("eventTime", "Giờ Diễn Ra", "text", "Thời Gian & Địa Điểm", "18:30", 6));
        defaults.add(createKey("eventLocation", "Địa Điểm Tổ Chức", "text", "Thời Gian & Địa Điểm", "Trung Tâm Sự Kiện White Palace", 7));
        defaults.add(createKey("eventMapUrl", "Link Bản Đồ Google Maps", "text", "Thời Gian & Địa Điểm", "https://maps.google.com/...", 8));
        defaults.add(createKey("loveStartDate", "Ngày Bắt Đầu Tình Yêu", "date", "Thời Gian & Địa Điểm", "YYYY-MM-DD", 9));

        // Hình Ảnh & Đại Diện
        defaults.add(createKey("senderAvatar", "Ảnh Đại Diện", "image", "Hình Ảnh", "https://...", 10));
        defaults.add(createKey("senderNickname", "Biệt Danh / Nickname", "text", "Hình Ảnh", "Bé Bự, Mèo Con...", 11));

        // Album Ảnh
        defaults.add(createKey("photos", "Album Ảnh Kỷ Niệm", "gallery", "Album Ảnh Kỷ Niệm", "Danh sách ảnh kỷ niệm", 12));

        // Nhạc Nền
        defaults.add(createKey("musicUrl", "Nhạc Nền Thiệp Mời", "music", "Nhạc Nền Thiệp Mời", "Link bài hát mp3", 13));

        // Bản Đồ & Khoảng Cách
        defaults.add(createKey("distanceKm", "Khoảng Cách (km)", "number", "Bản Đồ & Khoảng Cách", "1500", 14));
        defaults.add(createKey("coordinates", "Tọa Độ Địa Lý", "text", "Bản Đồ & Khoảng Cách", "10.7769° N, 106.7009° E", 15));

        // Hiệu Ứng Từ Khóa Rơi
        defaults.add(createKey("keyword1", "Từ Khóa Rơi 1", "text", "Hiệu Ứng Từ Khóa Rơi", "Ví dụ: Hạnh Phúc", 16));
        defaults.add(createKey("keyword2", "Từ Khóa Rơi 2", "text", "Hiệu Ứng Từ Khóa Rơi", "Ví dụ: Yêu Thương", 17));
        defaults.add(createKey("keyword3", "Từ Khóa Rơi 3", "text", "Hiệu Ứng Từ Khóa Rơi", "Ví dụ: Bình Yên", 18));
        defaults.add(createKey("keyword4", "Từ Khóa Rơi 4", "text", "Hiệu Ứng Từ Khóa Rơi", "Ví dụ: Mãi Mãi", 19));
        defaults.add(createKey("fallingKeywords", "Danh Sách Từ Khóa Rơi", "keywords", "Hiệu Ứng Từ Khóa Rơi", "Danh sách từ khóa", 20));

        // Ngày Sinh Nhật & Đếm Ngược
        defaults.add(createKey("birthdayDate", "Ngày Sinh Nhật (Tự Tính Đếm Ngược)", "date", "Thời Gian & Địa Điểm", "YYYY-MM-DD", 21));

        // 5 Khoảnh Khắc Kỷ Niệm (Scrapbook KK1 -> KK5)
        defaults.add(createKey("moment1Photo", "Ảnh Khoảnh Khắc 1", "image", "5 Khoảnh Khắc Kỷ Niệm", "https://...", 22));
        defaults.add(createKey("moment1Text", "Nội Dung Khoảnh Khắc 1", "text", "5 Khoảnh Khắc Kỷ Niệm", "Ngày đầu tiên nắm tay nhau", 23));
        defaults.add(createKey("moment1Date", "Ngày Khoảnh Khắc 1", "text", "5 Khoảnh Khắc Kỷ Niệm", "14.02.2023", 24));

        defaults.add(createKey("moment2Photo", "Ảnh Khoảnh Khắc 2", "image", "5 Khoảnh Khắc Kỷ Niệm", "https://...", 25));
        defaults.add(createKey("moment2Text", "Nội Dung Khoảnh Khắc 2", "text", "5 Khoảnh Khắc Kỷ Niệm", "Chuyến đi Đà Lạt đầu tiên", 26));
        defaults.add(createKey("moment2Date", "Ngày Khoảnh Khắc 2", "text", "5 Khoảnh Khắc Kỷ Niệm", "08.05.2023", 27));

        defaults.add(createKey("moment3Photo", "Ảnh Khoảnh Khắc 3", "image", "5 Khoảnh Khắc Kỷ Niệm", "https://...", 28));
        defaults.add(createKey("moment3Text", "Nội Dung Khoảnh Khắc 3", "text", "5 Khoảnh Khắc Kỷ Niệm", "Sinh nhật năm ngoái", 29));
        defaults.add(createKey("moment3Date", "Ngày Khoảnh Khắc 3", "text", "5 Khoảnh Khắc Kỷ Niệm", "20.10.2023", 30));

        defaults.add(createKey("moment4Photo", "Ảnh Khoảnh Khắc 4", "image", "5 Khoảnh Khắc Kỷ Niệm", "https://...", 31));
        defaults.add(createKey("moment4Text", "Nội Dung Khoảnh Khắc 4", "text", "5 Khoảnh Khắc Kỷ Niệm", "Đón năm mới cùng nhau", 32));
        defaults.add(createKey("moment4Date", "Ngày Khoảnh Khắc 4", "text", "5 Khoảnh Khắc Kỷ Niệm", "01.01.2026", 33));

        defaults.add(createKey("moment5Photo", "Ảnh Khoảnh Khắc 5", "image", "5 Khoảnh Khắc Kỷ Niệm", "https://...", 34));
        defaults.add(createKey("moment5Text", "Nội Dung Khoảnh Khắc 5", "text", "5 Khoảnh Khắc Kỷ Niệm", "Hành trình dài phía trước", 35));
        defaults.add(createKey("moment5Date", "Ngày Khoảnh Khắc 5", "text", "5 Khoảnh Khắc Kỷ Niệm", "Mãi Mãi Về Sau", 36));

        // Thư Tay Khi Nhấn Mở Thư
        defaults.add(createKey("letterMessage", "Nội Dung Thư Tay Khi Mở", "textarea", "Thư Chúc Mừng", "Gửi người thương...", 37));
        defaults.add(createKey("footerNote", "Dòng Chữ Dưới Chân Trang", "text", "Thông Tin Khác", "made with love, mỗi ngày bên em", 38));

        for (TemplateSchemaKey item : defaults) {
            if (!repository.existsByKeyName(item.getKeyName())) {
                repository.save(item);
            }
        }

        return getAllSchemaKeysForAdmin();
    }

    private TemplateSchemaKey createKey(String keyName, String label, String fieldType, String sectionName, String placeholder, int order) {
        return TemplateSchemaKey.builder()
                .keyName(keyName)
                .label(label)
                .fieldType(fieldType)
                .sectionName(sectionName)
                .placeholder(placeholder)
                .displayOrder(order)
                .isActive(true)
                .isRequired(false)
                .build();
    }

    @Override
    public TemplateSchemaKeyResponse mapToResponse(TemplateSchemaKey entity) {
        return TemplateSchemaKeyResponse.builder()
                .id(entity.getId())
                .keyName(entity.getKeyName())
                .label(entity.getLabel())
                .fieldType(entity.getFieldType())
                .sectionName(entity.getSectionName())
                .placeholder(entity.getPlaceholder())
                .description(entity.getDescription())
                .defaultValue(entity.getDefaultValue())
                .isRequired(entity.getIsRequired())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
