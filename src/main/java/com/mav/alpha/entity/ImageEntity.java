package com.mav.alpha.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "images")
public class ImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String filepath;
    private String contentType;

    @Column(name = "detailed_caption")
    private String detailedCaption;

    @Lob
    @Column(name = "text")
    private String text;

    @ManyToMany
    @JoinTable(
            name = "image_labels",
            joinColumns = @JoinColumn(name = "image_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    private List<LabelEntity> labels;

    @ElementCollection
    @CollectionTable(name = "image_bboxes", joinColumns = @JoinColumn(name = "image_id"))
    @Column(name = "bbox")
    private List<String> bboxes;
}
