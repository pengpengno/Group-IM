package com.github.im.server.service;

import com.github.im.server.config.FileUploadProperties;
import com.github.im.server.constants.CacheKeyConstants;
import com.github.im.server.model.SystemConfigItem;
import com.github.im.server.repository.SystemConfigItemRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.IntPredicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigService {

    public static final String GROUP_MEDIA = "media";

    public static final String KEY_MEDIA_PREVIEW_DEFAULT_WIDTH = "media.preview.defaultWidth";
    public static final String KEY_MEDIA_PREVIEW_MIN_WIDTH = "media.preview.minWidth";
    public static final String KEY_MEDIA_PREVIEW_MAX_WIDTH = "media.preview.maxWidth";
    public static final String KEY_MEDIA_PREVIEW_DEFAULT_QUALITY = "media.preview.defaultQuality";
    public static final String KEY_MEDIA_PREVIEW_MIN_QUALITY = "media.preview.minQuality";
    public static final String KEY_MEDIA_PREVIEW_MAX_QUALITY = "media.preview.maxQuality";
    public static final String KEY_MEDIA_THUMBNAIL_ENABLED = "media.thumbnail.enabled";
    public static final String KEY_MEDIA_THUMBNAIL_WIDTH = "media.thumbnail.width";
    public static final String KEY_MEDIA_THUMBNAIL_HEIGHT = "media.thumbnail.height";
    public static final String KEY_MEDIA_THUMBNAIL_QUALITY = "media.thumbnail.quality";
    public static final String KEY_MEDIA_UPLOAD_COMPRESSION_ENABLED = "media.upload.compressionEnabled";
    public static final String KEY_MEDIA_UPLOAD_COMPRESS_MIN_SIZE_KB = "media.upload.compressMinSizeKb";
    public static final String KEY_MEDIA_UPLOAD_MAX_IMAGE_EDGE = "media.upload.maxImageEdge";
    public static final String KEY_MEDIA_UPLOAD_JPEG_QUALITY = "media.upload.jpegQuality";

    private final SystemConfigItemRepository repository;
    private final FileUploadProperties fileUploadProperties;

    @Getter
    @RequiredArgsConstructor
    public enum ValueType {
        BOOLEAN("BOOLEAN"),
        INTEGER("INTEGER");

        private final String code;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ConfigDefinition {
        private final String key;
        private final String group;
        private final ValueType valueType;
        private final String label;
        private final String description;
        private final String defaultValue;
        private final boolean publicReadable;
        private final IntPredicate validator;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ConfigFieldView {
        private final String key;
        private final String label;
        private final String description;
        private final String valueType;
        private final String defaultValue;
        private final boolean publicReadable;
        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ConfigGroupView {
        private final String group;
        private final String title;
        private final List<ConfigFieldView> fields;
    }

    @Getter
    @RequiredArgsConstructor
    public static class MediaRuntimePolicy {
        private final int previewDefaultWidth;
        private final int previewMinWidth;
        private final int previewMaxWidth;
        private final int previewDefaultQuality;
        private final int previewMinQuality;
        private final int previewMaxQuality;
        private final boolean thumbnailEnabled;
        private final int thumbnailWidth;
        private final int thumbnailHeight;
        private final int thumbnailQuality;
        private final boolean uploadCompressionEnabled;
        private final int uploadCompressMinSizeKb;
        private final int uploadMaxImageEdge;
        private final int uploadJpegQuality;
    }

    private Map<String, ConfigDefinition> definitions;

    @PostConstruct
    @Transactional
    public void initializeDefaults() {
        definitions = buildDefinitions();
        for (ConfigDefinition definition : definitions.values()) {
            repository.findByConfigKey(definition.getKey()).orElseGet(() -> {
                SystemConfigItem item = new SystemConfigItem();
                item.setConfigKey(definition.getKey());
                item.setConfigGroup(definition.getGroup());
                item.setConfigValue(definition.getDefaultValue());
                item.setValueType(definition.getValueType().getCode());
                item.setDescription(definition.getDescription());
                item.setDefaultValue(definition.getDefaultValue());
                item.setPublicReadable(definition.isPublicReadable());
                return repository.save(item);
            });
        }
        log.info("Initialized {} system config definitions", definitions.size());
    }

    public List<ConfigGroupView> getAdminConfigGroups() {
        List<ConfigFieldView> mediaFields = new ArrayList<>();
        for (ConfigDefinition definition : definitions.values()) {
            if (!GROUP_MEDIA.equals(definition.getGroup())) {
                continue;
            }
            mediaFields.add(new ConfigFieldView(
                definition.getKey(),
                definition.getLabel(),
                definition.getDescription(),
                definition.getValueType().getCode(),
                definition.getDefaultValue(),
                definition.isPublicReadable(),
                getConfigValue(definition.getKey())
            ));
        }
        return List.of(new ConfigGroupView(GROUP_MEDIA, "Media Delivery Policy", mediaFields));
    }

    @Transactional
    @CacheEvict(value = CacheKeyConstants.SystemConfig.PREFIX, allEntries = true)
    public void updateMediaConfig(Map<String, String> updates) {
        Map<String, String> mergedValues = new HashMap<>();
        for (ConfigDefinition definition : definitions.values()) {
            if (GROUP_MEDIA.equals(definition.getGroup())) {
                mergedValues.put(definition.getKey(), getConfigValue(definition.getKey()));
            }
        }

        for (Map.Entry<String, String> entry : updates.entrySet()) {
            ConfigDefinition definition = definitions.get(entry.getKey());
            if (definition == null || !GROUP_MEDIA.equals(definition.getGroup())) {
                throw new IllegalArgumentException("Unsupported config key: " + entry.getKey());
            }
            String normalizedValue = normalizeAndValidate(definition, entry.getValue());
            mergedValues.put(entry.getKey(), normalizedValue);
        }

        validateMediaPolicy(mergedValues);

        for (Map.Entry<String, String> entry : mergedValues.entrySet()) {
            ConfigDefinition definition = definitions.get(entry.getKey());
            if (definition != null && GROUP_MEDIA.equals(definition.getGroup())) {
                saveConfigValue(definition, entry.getValue());
            }
        }
    }

    public MediaRuntimePolicy getMediaRuntimePolicy() {
        return new MediaRuntimePolicy(
            getInt(KEY_MEDIA_PREVIEW_DEFAULT_WIDTH),
            getInt(KEY_MEDIA_PREVIEW_MIN_WIDTH),
            getInt(KEY_MEDIA_PREVIEW_MAX_WIDTH),
            getInt(KEY_MEDIA_PREVIEW_DEFAULT_QUALITY),
            getInt(KEY_MEDIA_PREVIEW_MIN_QUALITY),
            getInt(KEY_MEDIA_PREVIEW_MAX_QUALITY),
            getBoolean(KEY_MEDIA_THUMBNAIL_ENABLED),
            getInt(KEY_MEDIA_THUMBNAIL_WIDTH),
            getInt(KEY_MEDIA_THUMBNAIL_HEIGHT),
            getInt(KEY_MEDIA_THUMBNAIL_QUALITY),
            getBoolean(KEY_MEDIA_UPLOAD_COMPRESSION_ENABLED),
            getInt(KEY_MEDIA_UPLOAD_COMPRESS_MIN_SIZE_KB),
            getInt(KEY_MEDIA_UPLOAD_MAX_IMAGE_EDGE),
            getInt(KEY_MEDIA_UPLOAD_JPEG_QUALITY)
        );
    }

    private int getInt(String key) {
        return Integer.parseInt(getConfigValue(key));
    }

    private boolean getBoolean(String key) {
        return Boolean.parseBoolean(getConfigValue(key));
    }

    @Cacheable(value = CacheKeyConstants.SystemConfig.PREFIX, key = "'system:config:key:' + #configKey")
    public String getConfigValue(String configKey) {
        ConfigDefinition definition = definitions.get(configKey);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown config key: " + configKey);
        }
        return repository.findByConfigKey(configKey)
            .map(SystemConfigItem::getConfigValue)
            .orElse(definition.getDefaultValue());
    }

    protected void saveConfigValue(ConfigDefinition definition, String normalizedValue) {
        SystemConfigItem item = repository.findByConfigKey(definition.getKey()).orElseGet(SystemConfigItem::new);
        item.setConfigKey(definition.getKey());
        item.setConfigGroup(definition.getGroup());
        item.setConfigValue(normalizedValue);
        item.setValueType(definition.getValueType().getCode());
        item.setDescription(definition.getDescription());
        item.setDefaultValue(definition.getDefaultValue());
        item.setPublicReadable(definition.isPublicReadable());
        repository.save(item);
    }

    private String normalizeAndValidate(ConfigDefinition definition, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Config value cannot be empty: " + definition.getKey());
        }
        if (definition.getValueType() == ValueType.BOOLEAN) {
            if (!"true".equalsIgnoreCase(rawValue) && !"false".equalsIgnoreCase(rawValue)) {
                throw new IllegalArgumentException("Boolean config must be true or false: " + definition.getKey());
            }
            return String.valueOf(Boolean.parseBoolean(rawValue));
        }

        int value;
        try {
            value = Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Integer config is invalid: " + definition.getKey(), ex);
        }
        if (definition.getValidator() != null && !definition.getValidator().test(value)) {
            throw new IllegalArgumentException("Config value out of range: " + definition.getKey());
        }
        return String.valueOf(value);
    }

    private void validateMediaPolicy(Map<String, String> values) {
        int minWidth = Integer.parseInt(values.get(KEY_MEDIA_PREVIEW_MIN_WIDTH));
        int defaultWidth = Integer.parseInt(values.get(KEY_MEDIA_PREVIEW_DEFAULT_WIDTH));
        int maxWidth = Integer.parseInt(values.get(KEY_MEDIA_PREVIEW_MAX_WIDTH));
        if (!(minWidth <= defaultWidth && defaultWidth <= maxWidth)) {
            throw new IllegalArgumentException("Preview width policy must satisfy min <= default <= max");
        }

        int minQuality = Integer.parseInt(values.get(KEY_MEDIA_PREVIEW_MIN_QUALITY));
        int defaultQuality = Integer.parseInt(values.get(KEY_MEDIA_PREVIEW_DEFAULT_QUALITY));
        int maxQuality = Integer.parseInt(values.get(KEY_MEDIA_PREVIEW_MAX_QUALITY));
        if (!(minQuality <= defaultQuality && defaultQuality <= maxQuality)) {
            throw new IllegalArgumentException("Preview quality policy must satisfy min <= default <= max");
        }

        validateRange(
            Integer.parseInt(values.get(KEY_MEDIA_UPLOAD_COMPRESS_MIN_SIZE_KB)),
            32,
            20 * 1024,
            "Upload compression min size KB must stay between 32 and 20480"
        );
        validateRange(
            Integer.parseInt(values.get(KEY_MEDIA_UPLOAD_MAX_IMAGE_EDGE)),
            320,
            4096,
            "Upload max image edge must stay between 320 and 4096"
        );
        validateRange(
            Integer.parseInt(values.get(KEY_MEDIA_UPLOAD_JPEG_QUALITY)),
            30,
            100,
            "Upload JPEG quality must stay between 30 and 100"
        );
    }

    private void validateRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(message);
        }
    }

    private Map<String, ConfigDefinition> buildDefinitions() {
        FileUploadProperties.Preview preview = fileUploadProperties.getPreview();
        FileUploadProperties.Thumbnail thumbnail = fileUploadProperties.getThumbnail();
        FileUploadProperties.Upload upload = fileUploadProperties.getUpload();

        Map<String, ConfigDefinition> map = new LinkedHashMap<>();
        map.put(KEY_MEDIA_PREVIEW_DEFAULT_WIDTH, new ConfigDefinition(
            KEY_MEDIA_PREVIEW_DEFAULT_WIDTH, GROUP_MEDIA, ValueType.INTEGER, "Preview default width",
            "Default preview width returned when clients do not provide a width.",
            String.valueOf(preview.getDefaultWidth()), true, value -> value >= 120 && value <= 2400
        ));
        map.put(KEY_MEDIA_PREVIEW_MIN_WIDTH, new ConfigDefinition(
            KEY_MEDIA_PREVIEW_MIN_WIDTH, GROUP_MEDIA, ValueType.INTEGER, "Preview minimum width",
            "Smallest width accepted for preview image requests.",
            String.valueOf(preview.getMinWidth()), true, value -> value >= 80 && value <= 2400
        ));
        map.put(KEY_MEDIA_PREVIEW_MAX_WIDTH, new ConfigDefinition(
            KEY_MEDIA_PREVIEW_MAX_WIDTH, GROUP_MEDIA, ValueType.INTEGER, "Preview maximum width",
            "Largest width accepted for preview image requests.",
            String.valueOf(preview.getMaxWidth()), true, value -> value >= 120 && value <= 4000
        ));
        map.put(KEY_MEDIA_PREVIEW_DEFAULT_QUALITY, new ConfigDefinition(
            KEY_MEDIA_PREVIEW_DEFAULT_QUALITY, GROUP_MEDIA, ValueType.INTEGER, "Preview default quality",
            "Default quality used for generated preview images.",
            String.valueOf(preview.getDefaultQuality()), true, value -> value >= 30 && value <= 100
        ));
        map.put(KEY_MEDIA_PREVIEW_MIN_QUALITY, new ConfigDefinition(
            KEY_MEDIA_PREVIEW_MIN_QUALITY, GROUP_MEDIA, ValueType.INTEGER, "Preview minimum quality",
            "Smallest quality value accepted for preview requests.",
            String.valueOf(preview.getMinQuality()), true, value -> value >= 1 && value <= 100
        ));
        map.put(KEY_MEDIA_PREVIEW_MAX_QUALITY, new ConfigDefinition(
            KEY_MEDIA_PREVIEW_MAX_QUALITY, GROUP_MEDIA, ValueType.INTEGER, "Preview maximum quality",
            "Largest quality value accepted for preview requests.",
            String.valueOf(preview.getMaxQuality()), true, value -> value >= 1 && value <= 100
        ));
        map.put(KEY_MEDIA_THUMBNAIL_ENABLED, new ConfigDefinition(
            KEY_MEDIA_THUMBNAIL_ENABLED, GROUP_MEDIA, ValueType.BOOLEAN, "Thumbnail generation enabled",
            "Whether the server should persist poster thumbnails for audio and video uploads.",
            String.valueOf(thumbnail.isEnabled()), true, null
        ));
        map.put(KEY_MEDIA_THUMBNAIL_WIDTH, new ConfigDefinition(
            KEY_MEDIA_THUMBNAIL_WIDTH, GROUP_MEDIA, ValueType.INTEGER, "Thumbnail width",
            "Target width used for generated audio and video poster thumbnails.",
            String.valueOf(thumbnail.getWidth()), true, value -> value >= 120 && value <= 2400
        ));
        map.put(KEY_MEDIA_THUMBNAIL_HEIGHT, new ConfigDefinition(
            KEY_MEDIA_THUMBNAIL_HEIGHT, GROUP_MEDIA, ValueType.INTEGER, "Thumbnail height",
            "Target height used for generated audio and video poster thumbnails.",
            String.valueOf(thumbnail.getHeight()), true, value -> value >= 90 && value <= 2400
        ));
        map.put(KEY_MEDIA_THUMBNAIL_QUALITY, new ConfigDefinition(
            KEY_MEDIA_THUMBNAIL_QUALITY, GROUP_MEDIA, ValueType.INTEGER, "Thumbnail quality",
            "Encoding quality used when the server saves generated poster thumbnails.",
            String.valueOf(thumbnail.getQuality()), true, value -> value >= 30 && value <= 100
        ));
        map.put(KEY_MEDIA_UPLOAD_COMPRESSION_ENABLED, new ConfigDefinition(
            KEY_MEDIA_UPLOAD_COMPRESSION_ENABLED, GROUP_MEDIA, ValueType.BOOLEAN, "Upload compression enabled",
            "Whether clients should compress oversized image uploads before sending them.",
            String.valueOf(upload.isCompressionEnabled()), true, null
        ));
        map.put(KEY_MEDIA_UPLOAD_COMPRESS_MIN_SIZE_KB, new ConfigDefinition(
            KEY_MEDIA_UPLOAD_COMPRESS_MIN_SIZE_KB, GROUP_MEDIA, ValueType.INTEGER, "Upload compression threshold (KB)",
            "Clients start compressing images when the source file exceeds this size in KB.",
            String.valueOf(upload.getCompressMinSizeKb()), true, value -> value >= 32 && value <= 20 * 1024
        ));
        map.put(KEY_MEDIA_UPLOAD_MAX_IMAGE_EDGE, new ConfigDefinition(
            KEY_MEDIA_UPLOAD_MAX_IMAGE_EDGE, GROUP_MEDIA, ValueType.INTEGER, "Upload image max edge",
            "Clients resize large images so the longest edge stays within this limit before upload.",
            String.valueOf(upload.getMaxImageEdge()), true, value -> value >= 320 && value <= 4096
        ));
        map.put(KEY_MEDIA_UPLOAD_JPEG_QUALITY, new ConfigDefinition(
            KEY_MEDIA_UPLOAD_JPEG_QUALITY, GROUP_MEDIA, ValueType.INTEGER, "Upload JPEG quality",
            "JPEG quality hint clients use after resizing upload images.",
            String.valueOf(upload.getJpegQuality()), true, value -> value >= 30 && value <= 100
        ));
        return map;
    }
}
