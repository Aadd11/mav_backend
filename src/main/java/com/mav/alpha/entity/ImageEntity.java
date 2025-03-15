package com.mav.alpha.entity;

import jakarta.persistence.*;
import lombok.Data;

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

    @Lob
    private String metadata; // Поле для хранения метаданных в формате JSON
}
