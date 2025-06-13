package com.tskrypko.streaming.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "video_qualities")
@NoArgsConstructor
public class VideoQuality extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "video_id", insertable = false, updatable = false)
    private Long videoId;

    @NotBlank(message = "Quality name is required")
    @Column(name = "quality_name", nullable = false)
    private String qualityName; // 480p, 720p, 1080p

    @NotNull(message = "Width is required")
    @Column(nullable = false)
    private Integer width;

    @NotNull(message = "Height is required")
    @Column(nullable = false)
    private Integer height;

    @NotNull(message = "Bitrate is required")
    @Column(nullable = false)
    private Integer bitrate; // in kbps

    @Column(name = "file_size")
    private Long fileSize; // Size of the encoded file

    @Column(name = "s3_key")
    private String s3Key; // S3 key for this quality

    @Column(name = "hls_playlist_url")
    private String hlsPlaylistUrl; // HLS m3u8 playlist for this quality

    @Column(name = "dash_representation_id")
    private String dashRepresentationId; // DASH representation ID

    @Enumerated(EnumType.STRING)
    @Column(name = "encoding_status", nullable = false)
    private EncodingStatus encodingStatus = EncodingStatus.PENDING;

    @Column(name = "encoding_progress")
    private Integer encodingProgress = 0; // 0-100

    @Column(name = "error_message")
    private String errorMessage;

    public VideoQuality(String qualityName, Integer width, Integer height, Integer bitrate) {
        this.qualityName = qualityName;
        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
    }

    // Helper method to set video and videoId
    public void setVideo(Video video) {
        this.video = video;
        this.videoId = video != null ? video.getId() : null;
    }

    @Override
    public String toString() {
        return "VideoQuality{" +
                "id=" + getId() +
                ", videoId=" + videoId +
                ", qualityName='" + qualityName + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", bitrate=" + bitrate +
                ", encodingStatus=" + encodingStatus +
                ", encodingProgress=" + encodingProgress +
                '}';
    }
} 