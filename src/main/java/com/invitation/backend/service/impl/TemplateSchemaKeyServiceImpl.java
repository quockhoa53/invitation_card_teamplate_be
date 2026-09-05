package com.invitation.backend.service.impl;

import com.invitation.backend.dto.request.ImportSchemaKeysRequest;
import com.invitation.backend.dto.request.TemplateSchemaKeyRequest;
import com.invitation.backend.dto.response.ImportSchemaKeysResponse;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
            log.info("Đồng bộ Template Schema Keys chuẩn hệ thống...");
            seedDefaultSchemaKeys();
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

    private List<String> sanitizeLabels(String primaryLabel, List<String> labels) {
        List<String> result = new ArrayList<>();
        if (primaryLabel != null && !primaryLabel.trim().isEmpty()) {
            result.add(primaryLabel.trim());
        }
        if (labels != null) {
            for (String l : labels) {
                if (l != null && !l.trim().isEmpty() && result.stream().noneMatch(x -> x.equalsIgnoreCase(l.trim()))) {
                    result.add(l.trim());
                }
            }
        }
        return result;
    }

    @Override
    @Transactional
    public TemplateSchemaKeyResponse createSchemaKey(TemplateSchemaKeyRequest request) {
        String keyName = request.getKeyName().trim();
        if (repository.existsByKeyName(keyName)) {
            throw new IllegalArgumentException("Mã Key '" + keyName + "' đã tồn tại trong hệ thống");
        }

        String label = request.getLabel().trim();
        List<String> labels = sanitizeLabels(label, request.getLabels());

        TemplateSchemaKey key = TemplateSchemaKey.builder()
                .keyName(keyName)
                .label(label)
                .labels(labels)
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

        String label = request.getLabel().trim();
        List<String> labels = sanitizeLabels(label, request.getLabels());

        key.setKeyName(keyName);
        key.setLabel(label);
        key.setLabels(labels);
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

        // 1. Nội Dung Lời Chúc
        defaults.add(createKeyWithLabels("recipientName", "Tên Người Nhận", Arrays.asList("Tên Người Nhận", "Tên Cô Dâu", "Tên Bạn Gái", "Tên Bạn Thân", "Tên Người Thương"), "text", "Nội Dung Lời Chúc", "Ví dụ: Em Yêu, Bạn Thân...", 1));
        defaults.add(createKeyWithLabels("senderName", "Tên Người Gửi", Arrays.asList("Tên Người Gửi", "Tên Chú Rể", "Tên Bạn Trai", "Tên Người Thương", "Tên Tác Giả"), "text", "Nội Dung Lời Chúc", "Ví dụ: Anh Yêu, Bạn Khoa...", 2));
        defaults.add(createKeyWithLabels("greetingTitle", "Tiêu Đề Lời Chúc", Arrays.asList("Tiêu Đề Lời Chúc", "Tiêu Đề Thiệp Mời", "Thông Điệp Chính", "Dòng Tít Nổi Bật"), "text", "Nội Dung Lời Chúc", "Ví dụ: Chúc Mừng Sinh Nhật! 🎉", 3));
        defaults.add(createKeyWithLabels("greetingMessage", "Nội Dung Lời Chúc", Arrays.asList("Nội Dung Lời Chúc", "Lời Nhắn Yêu Thương", "Tâm Thư Gửi Bạn", "Lời Chúc Mừng"), "textarea", "Nội Dung Lời Chúc", "Nội dung lời nhắn chúc mừng...", 4));

        // 2. Thời Gian & Địa Điểm
        defaults.add(createKeyWithLabels("eventDate", "Ngày Diễn Ra Sự Kiện", Arrays.asList("Ngày Diễn Ra Sự Kiện", "Ngày Cưới", "Ngày Sinh Nhật", "Thời Gian Tổ Chức"), "date", "Thời Gian & Địa Điểm", "YYYY-MM-DD", 5));
        defaults.add(createKeyWithLabels("eventTime", "Giờ Diễn Ra", Arrays.asList("Giờ Diễn Ra", "Thời Gian Bắt Đầu", "Khung Giờ Khai Tiệc"), "text", "Thời Gian & Địa Điểm", "18:30", 6));
        defaults.add(createKeyWithLabels("eventLocation", "Địa Điểm Tổ Chức", Arrays.asList("Địa Điểm Tổ Chức", "Nơi Diễn Ra Sự Kiện", "Trung Tâm Tiệc Cưới", "Địa Chỉ"), "text", "Thời Gian & Địa Điểm", "Trung Tâm Sự Kiện White Palace", 7));
        defaults.add(createKeyWithLabels("eventMapUrl", "Link Bản Đồ Google Maps", Arrays.asList("Link Bản Đồ Google Maps", "Bản Đồ Chỉ Đường", "Tọa Độ Google Maps"), "text", "Thời Gian & Địa Điểm", "https://maps.google.com/...", 8));
        defaults.add(createKeyWithLabels("loveStartDate", "Ngày Bắt Đầu Tình Yêu", Arrays.asList("Ngày Bắt Đầu Tình Yêu", "Ngày Kỷ Niệm", "Ngày Đầu Tiên Quen Nhau", "Ngày Bắt Đầu Hẹn Hò"), "date", "Thời Gian & Địa Điểm", "YYYY-MM-DD", 9));

        // 3. Hình Ảnh & Đại Diện
        defaults.add(createKeyWithLabels("senderAvatar", "Ảnh Đại Diện Người Gửi", Arrays.asList("Ảnh Đại Diện Người Gửi", "Ảnh Chú Rể", "Ảnh Bạn Trai", "Ảnh Đại Diện"), "image", "Hình Ảnh", "https://...", 10));
        defaults.add(createKeyWithLabels("recipientAvatar", "Ảnh Đại Diện Người Nhận", Arrays.asList("Ảnh Đại Diện Người Nhận", "Ảnh Cô Dâu", "Ảnh Bạn Gái", "Ảnh Nhân Vật Chính"), "image", "Hình Ảnh", "https://...", 11));
        defaults.add(createKeyWithLabels("senderNickname", "Biệt Danh Người Gửi", Arrays.asList("Biệt Danh Người Gửi", "Nickname", "Tên Thân Mật"), "text", "Hình Ảnh", "Bé Bự, Mèo Con...", 12));

        // 4. Địa Điểm Cặp Đôi & Bản Đồ
        defaults.add(createKeyWithLabels("senderLocation", "Vị Trí Người Gửi", Arrays.asList("Vị Trí Người Gửi", "Nơi Ở Người Gửi", "Thành Phố Người Gửi"), "text", "Bản Đồ & Khoảng Cách", "Hà Nội (21.0285° N)", 13));
        defaults.add(createKeyWithLabels("recipientLocation", "Vị Trí Người Nhận", Arrays.asList("Vị Trí Người Nhận", "Nơi Ở Người Nhận", "Thành Phố Người Nhận"), "text", "Bản Đồ & Khoảng Cách", "TP. Hồ Chí Minh (10.8231° N)", 14));
        defaults.add(createKeyWithLabels("distanceKm", "Khoảng Cách (km)", Arrays.asList("Khoảng Cách (km)", "Cự Ly Địa Lý", "Khoảng Cách Hai Trái Tim"), "number", "Bản Đồ & Khoảng Cách", "1720", 15));
        defaults.add(createKeyWithLabels("coordinates", "Tọa Độ Địa Lý", Arrays.asList("Tọa Độ Địa Lý", "Tọa Độ GPS"), "text", "Bản Đồ & Khoảng Cách", "10.7769° N, 106.7009° E", 16));

        // 5. Album Ảnh & Kỷ Niệm
        defaults.add(createKeyWithLabels("photos", "Album Ảnh Kỷ Niệm", Arrays.asList("Album Ảnh Kỷ Niệm", "Ảnh Tái Ngộ", "Bộ Sưu Tập Khoảnh Khắc"), "gallery", "Album Ảnh Kỷ Niệm", "Danh sách ảnh kỷ niệm", 17));

        // 6. Nhạc Nền
        defaults.add(createKeyWithLabels("musicUrl", "Nhạc Nền Thiệp Mời", Arrays.asList("Nhạc Nền Thiệp Mời", "Giai Điệu Tình Yêu", "Bài Hát Kỷ Niệm"), "music", "Nhạc Nền Thiệp Mời", "Link bài hát mp3", 18));

        // 7. Hiệu Ứng Từ Khóa Rơi & Mốc Thời Gian
        defaults.add(createKeyWithLabels("keyword1", "Từ Khóa Rơi 1", Arrays.asList("Từ Khóa Rơi 1", "Từ Khóa Bay 1"), "text", "Hiệu Ứng Từ Khóa Rơi", "Ví dụ: Hạnh Phúc", 19));
        defaults.add(createKeyWithLabels("keyword2", "Từ Khóa Rơi 2", Arrays.asList("Từ Khóa Rơi 2", "Từ Khóa Bay 2"), "text", "Hiệu Ứng Từ Khóa Rơi", "Ví dụ: Yêu Thương", 20));
        defaults.add(createKeyWithLabels("keyword3", "Từ Khóa Rơi 3", Arrays.asList("Từ Khóa Rơi 3", "Từ Khóa Bay 3"), "text", "Hiệu Ứng Từ Khóa Rơi", "Ví dụ: Bình Yên", 21));
        defaults.add(createKeyWithLabels("keyword4", "Từ Khóa Rơi 4", Arrays.asList("Từ Khóa Rơi 4", "Từ Khóa Bay 4"), "text", "Hiệu Ứng Từ Khóa Rơi", "Ví dụ: Mãi Mãi", 22));
        defaults.add(createKeyWithLabels("keyword5", "Từ Khóa Rơi 5", Arrays.asList("Từ Khóa Rơi 5", "Từ Khóa Bay 5"), "text", "Hiệu Ứng Từ Khóa Rơi", "Ví dụ: Trọn Vẹn", 23));
        defaults.add(createKeyWithLabels("fallingKeywords", "Danh Sách Từ Khóa Rơi", Arrays.asList("Danh Sách Từ Khóa Rơi", "Mưa Chữ Neon", "Từ Khóa Tình Yêu"), "keywords", "Hiệu Ứng Từ Khóa Rơi", "Danh sách từ khóa", 24));
        defaults.add(createKeyWithLabels("milestoneUnit", "Đơn Vị Mốc Thời Gian", Arrays.asList("Đơn Vị Mốc Thời Gian", "Đơn Vị Đếm (DAYS, YEARS, NGÀY)"), "text", "Hiệu Ứng Từ Khóa Rơi", "NGÀY, THÁNG, NĂM", 25));
        defaults.add(createKeyWithLabels("milestoneText", "Dòng Chữ Mốc Kỷ Niệm", Arrays.asList("Dòng Chữ Mốc Kỷ Niệm", "Nội Dung Huy Hiệu Kỷ Niệm"), "text", "Hiệu Ứng Từ Khóa Rơi", "BÊN NHAU, YÊU NHAU", 26));

        // 8. Ngày Sinh Nhật & Đếm Ngược
        defaults.add(createKeyWithLabels("birthdayDate", "Ngày Sinh Nhật", Arrays.asList("Ngày Sinh Nhật", "Ngày Sinh (Tự Tính Đếm Ngược)"), "date", "Thời Gian & Địa Điểm", "YYYY-MM-DD", 27));

        // 9. 5 Khoảnh Khắc Kỷ Niệm (Scrapbook KK1 -> KK5)
        defaults.add(createKeyWithLabels("moment1Photo", "Ảnh Khoảnh Khắc 1", List.of("Ảnh Khoảnh Khắc 1"), "image", "5 Khoảnh Khắc Kỷ Niệm", "https://...", 28));
        defaults.add(createKeyWithLabels("moment1Text", "Nội Dung Khoảnh Khắc 1", List.of("Nội Dung Khoảnh Khắc 1"), "text", "5 Khoảnh Khắc Kỷ Niệm", "Ngày đầu tiên nắm tay nhau", 29));
        defaults.add(createKeyWithLabels("moment1Date", "Ngày Khoảnh Khắc 1", List.of("Ngày Khoảnh Khắc 1"), "text", "5 Khoảnh Khắc Kỷ Niệm", "14.02.2023", 30));

        defaults.add(createKeyWithLabels("moment2Photo", "Ảnh Khoảnh Khắc 2", List.of("Ảnh Khoảnh Khắc 2"), "image", "5 Khoảnh Khắc Kỷ Niệm", "https://...", 31));
        defaults.add(createKeyWithLabels("moment2Text", "Nội Dung Khoảnh Khắc 2", List.of("Nội Dung Khoảnh Khắc 2"), "text", "5 Khoảnh Khắc Kỷ Niệm", "Chuyến đi Đà Lạt đầu tiên", 32));
        defaults.add(createKeyWithLabels("moment2Date", "Ngày Khoảnh Khắc 2", List.of("Ngày Khoảnh Khắc 2"), "text", "5 Khoảnh Khắc Kỷ Niệm", "08.05.2023", 33));

        defaults.add(createKeyWithLabels("moment3Photo", "Ảnh Khoảnh Khắc 3", List.of("Ảnh Khoảnh Khắc 3"), "image", "5 Khoảnh Khắc Kỷ Niệm", "https://...", 34));
        defaults.add(createKeyWithLabels("moment3Text", "Nội Dung Khoảnh Khắc 3", List.of("Nội Dung Khoảnh Khắc 3"), "text", "5 Khoảnh Khắc Kỷ Niệm", "Sinh nhật năm ngoái", 35));
        defaults.add(createKeyWithLabels("moment3Date", "Ngày Khoảnh Khắc 3", List.of("Ngày Khoảnh Khắc 3"), "text", "5 Khoảnh Khắc Kỷ Niệm", "20.10.2023", 36));

        defaults.add(createKeyWithLabels("moment4Photo", "Ảnh Khoảnh Khắc 4", List.of("Ảnh Khoảnh Khắc 4"), "image", "5 Khoảnh Khắc Kỷ Niệm", "https://...", 37));
        defaults.add(createKeyWithLabels("moment4Text", "Nội Dung Khoảnh Khắc 4", List.of("Nội Dung Khoảnh Khắc 4"), "text", "5 Khoảnh Khắc Kỷ Niệm", "Đón năm mới cùng nhau", 38));
        defaults.add(createKeyWithLabels("moment4Date", "Ngày Khoảnh Khắc 4", List.of("Ngày Khoảnh Khắc 4"), "text", "5 Khoảnh Khắc Kỷ Niệm", "01.01.2026", 39));

        defaults.add(createKeyWithLabels("moment5Photo", "Ảnh Khoảnh Khắc 5", List.of("Ảnh Khoảnh Khắc 5"), "image", "5 Khoảnh Khắc Kỷ Niệm", "https://...", 40));
        defaults.add(createKeyWithLabels("moment5Text", "Nội Dung Khoảnh Khắc 5", List.of("Nội Dung Khoảnh Khắc 5"), "text", "5 Khoảnh Khắc Kỷ Niệm", "Hành trình dài phía trước", 41));
        defaults.add(createKeyWithLabels("moment5Date", "Ngày Khoảnh Khắc 5", List.of("Ngày Khoảnh Khắc 5"), "text", "5 Khoảnh Khắc Kỷ Niệm", "Mãi Mãi Về Sau", 42));

        // 10. Thư Tay Khi Nhấn Mở Thư
        defaults.add(createKeyWithLabels("letterMessage", "Nội Dung Thư Tay Khi Mở", Arrays.asList("Nội Dung Thư Tay Khi Mở", "Bức Thư Tình", "Lời Nhắn Bí Mật"), "textarea", "Thư Chúc Mừng", "Gửi người thương...", 43));
        defaults.add(createKeyWithLabels("footerNote", "Dòng Chữ Dưới Chân Trang", Arrays.asList("Dòng Chữ Dưới Chân Trang", "Lời Kết", "Chữ Ký Chân Trang"), "text", "Thông Tin Khác", "made with love, mỗi ngày bên em", 44));

        for (TemplateSchemaKey item : defaults) {
            Optional<TemplateSchemaKey> existingOpt = repository.findByKeyName(item.getKeyName());
            if (existingOpt.isPresent()) {
                TemplateSchemaKey existing = existingOpt.get();
                if (existing.getLabels() == null || existing.getLabels().isEmpty()) {
                    existing.setLabels(item.getLabels());
                    repository.save(existing);
                }
            } else {
                repository.save(item);
            }
        }

        return getAllSchemaKeysForAdmin();
    }

    @Override
    @Transactional
    public ImportSchemaKeysResponse importSchemaKeys(ImportSchemaKeysRequest request) {
        if (request == null || request.getKeys() == null || request.getKeys().isEmpty()) {
            throw new IllegalArgumentException("Danh sách keys không được để trống");
        }

        boolean overwrite = Boolean.TRUE.equals(request.getOverwrite());
        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        for (TemplateSchemaKeyRequest req : request.getKeys()) {
            if (req.getKeyName() == null || req.getKeyName().trim().isEmpty()) {
                continue;
            }

            String keyName = req.getKeyName().trim();
            Optional<TemplateSchemaKey> existingOpt = repository.findByKeyName(keyName);

            String label = req.getLabel() != null && !req.getLabel().trim().isEmpty()
                    ? req.getLabel().trim()
                    : keyName;
            List<String> labels = sanitizeLabels(label, req.getLabels());

            String fieldType = req.getFieldType() != null && !req.getFieldType().trim().isEmpty()
                    ? req.getFieldType().trim()
                    : "text";
            String sectionName = req.getSectionName() != null && !req.getSectionName().trim().isEmpty()
                    ? req.getSectionName().trim()
                    : "Tùy Chỉnh Nội Dung";

            if (existingOpt.isPresent()) {
                if (overwrite) {
                    TemplateSchemaKey existing = existingOpt.get();
                    existing.setLabel(label);
                    existing.setLabels(labels);
                    existing.setFieldType(fieldType);
                    existing.setSectionName(sectionName);
                    if (req.getPlaceholder() != null) existing.setPlaceholder(req.getPlaceholder());
                    if (req.getDescription() != null) existing.setDescription(req.getDescription());
                    if (req.getDefaultValue() != null) existing.setDefaultValue(req.getDefaultValue());
                    if (req.getIsRequired() != null) existing.setIsRequired(req.getIsRequired());
                    if (req.getDisplayOrder() != null) existing.setDisplayOrder(req.getDisplayOrder());
                    if (req.getIsActive() != null) existing.setIsActive(req.getIsActive());
                    repository.save(existing);
                    updatedCount++;
                } else {
                    skippedCount++;
                }
            } else {
                TemplateSchemaKey newKey = TemplateSchemaKey.builder()
                        .keyName(keyName)
                        .label(label)
                        .labels(labels)
                        .fieldType(fieldType)
                        .sectionName(sectionName)
                        .placeholder(req.getPlaceholder())
                        .description(req.getDescription())
                        .defaultValue(req.getDefaultValue())
                        .isRequired(req.getIsRequired() != null ? req.getIsRequired() : false)
                        .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                        .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                        .build();
                repository.save(newKey);
                createdCount++;
            }
        }

        List<TemplateSchemaKeyResponse> allKeys = getAllSchemaKeysForAdmin();
        return ImportSchemaKeysResponse.builder()
                .totalSubmitted(request.getKeys().size())
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .skippedCount(skippedCount)
                .keys(allKeys)
                .build();
    }

    private TemplateSchemaKey createKeyWithLabels(String keyName, String label, List<String> labels, String fieldType, String sectionName, String placeholder, int order) {
        return TemplateSchemaKey.builder()
                .keyName(keyName)
                .label(label)
                .labels(new ArrayList<>(labels))
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
                .labels(entity.getLabels() != null && !entity.getLabels().isEmpty()
                        ? new ArrayList<>(entity.getLabels())
                        : new ArrayList<>(List.of(entity.getLabel())))
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
