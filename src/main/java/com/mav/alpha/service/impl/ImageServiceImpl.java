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
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        // Проверка на то, что файл является изображением
        if (file.isEmpty() || !isImageFile(file)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Загружаемый файл должен быть изображением.");
        }

        try {
            // Сохраняем файл на диск
            String filename = file.getOriginalFilename();
            Path filepath = Paths.get(uploadDir + filename);
            Files.write(filepath, file.getBytes());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.FileSystemResource(filepath.toFile()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);

            String metadata = response.getBody();

            // Сохраняем метаданные в БД
            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setFilename(filename);
            imageEntity.setFilepath(filepath.toString());
            imageEntity.setContentType(file.getContentType());
            imageEntity.setMetadata(metadata); // Сохраняем метаданные в формате JSON

            return imageRepository.save(imageEntity);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении изображения или отправке на API", e);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при отправке изображения на API", e);
        }
    }
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (contentType.startsWith("image/"));
    }
}