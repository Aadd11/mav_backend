package com.mav.alpha.repository;

import com.mav.alpha.entity.LabelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabelRepository extends JpaRepository<LabelEntity, Long> {
    LabelEntity findByName(String name); // Метод для поиска метки по имени
}
