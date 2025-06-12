package com.tskrypko.encoding.model;

import lombok.Getter;

@Getter
public enum VideoQuality {
    HIGH_1080P("1080p", 1920, 1080, 4000),
    MEDIUM_720P("720p", 1280, 720, 2500),
    LOW_480P("480p", 854, 480, 1000);

    private final String label;
    private final int width;
    private final int height;
    private final int bitrateKbps;

    VideoQuality(String label, int width, int height, int bitrateKbps) {
        this.label = label;
        this.width = width;
        this.height = height;
        this.bitrateKbps = bitrateKbps;
    }

    public String getFolder() {
        return label;
    }
} 