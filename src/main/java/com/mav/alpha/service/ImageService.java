package com.mav.alpha.service;

import com.mav.alpha.entity.ImageEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {
    ImageEntity saveImage(MultipartFile file);
    List<ImageEntity> getAllImages();
    ImageEntity getImageById(Long id);
    ImageEntity uploadImageToApi(MultipartFile file, String apiUrl); // Новый метод
}
