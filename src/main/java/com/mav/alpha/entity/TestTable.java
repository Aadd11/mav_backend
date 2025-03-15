package com.mav.alpha.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "test_table")
public class TestTable {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    public TestTable() {
    }

    public TestTable(String name) {
        this.name = name;
    }
}