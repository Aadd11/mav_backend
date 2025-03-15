package com.mav.alpha.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "labels")
public class LabelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;
}
