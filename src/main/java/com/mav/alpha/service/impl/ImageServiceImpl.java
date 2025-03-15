package com.mav.alpha.service.impl;

import com.mav.alpha.entity.ImageEntity;
import com.mav.alpha.repository.ImageRepository;
import com.mav.alpha.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;
    private final String uploadDir = "uploads/"; // Папка для сохранения изображений
    private final RestTemplate restTemplate;

    @Autowired
    public ImageServiceImpl(ImageRepository imageRepository, RestTemplate restTemplate) {
        this.imageRepository = imageRepository;
        this.restTemplate = restTemplate;
        // Создаем папку, если она не существует
        new File(uploadDir).mkdirs();
    }

    @Override
    public ImageEntity saveImage(MultipartFile file) {
        try {
            // Сохраняем файл на диск
            String filename = file.getOriginalFilename();
            Path filepath = Paths.get(uploadDir + filename);
            Files.write(filepath, file.getBytes());

            // Сохраняем метаданные в БД
            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setFilename(filename);
            imageEntity.setFilepath(filepath.toString());
            imageEntity.setContentType(file.getContentType());

            return imageRepository.save(imageEntity);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении изображения", e);
        }
    }

    @Override
    public List<ImageEntity> getAllImages() {
        return imageRepository.findAll();
    }

    @Override
    public ImageEntity getImageById(Long id) {
        Optional<ImageEntity> imageEntity = imageRepository.findById(id);
        if (imageEntity.isPresent()) {
            return imageEntity.get();
        } else {
            throw new RuntimeException("Изображение не найдено с ID: " + id);
        }
    }

    @Override
    public ImageEntity uploadImageToApi(MultipartFile file, String apiUrl) {
        try {
            // Сохраняем файл на диск
            String filename = file.getOriginalFilename();
            Path filepath = Paths.get(uploadDir + filename);
            Files.write(filepath, file.getBytes());

            // Отправляем файл на внешний API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultipartFile> requestEntity = new HttpEntity<>(file, headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);

            // Предполагаем, что ответ содержит метаданные в формате JSON
            String metadata = response.getBody();

            // Сохраняем метаданные в БД
            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setFilename(filename);
            imageEntity.setFilepath(filepath.toString());
            imageEntity.setContentType(file.getContentType());
            imageEntity.setMetadata(metadata); // Сохраняем метаданные в формате JSON

            return imageRepository.save(imageEntity);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении изображения", e);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при отправке изображения на API", e);
        }
    }
}