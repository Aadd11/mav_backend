package com.mav.alpha.service.impl;

import com.mav.alpha.entity.ImageEntity;
import com.mav.alpha.entity.LabelEntity;
import com.mav.alpha.repository.ImageRepository;
import com.mav.alpha.repository.LabelRepository;
import com.mav.alpha.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ImageServiceImpl implements ImageService {
    @Autowired
    private LabelRepository labelRepository;

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
    @Transactional
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

            // Конвертируем изображение в формат JPEG
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", bos);
            byte[] jpegBytes = bos.toByteArray();

            // Создаем новый файл с конвертированным изображением
            String convertedFilename = "converted_" + filename;
            Path convertedFilePath = Paths.get(uploadDir + convertedFilename);
            Files.write(convertedFilePath, jpegBytes);

            // Создаем MultiValueMap для отправки файла
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.FileSystemResource(convertedFilePath.toFile()));

            // Отправляем файл на внешний API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);

            // Предполагаем, что ответ содержит метаданные в формате JSON
            String metadata = response.getBody();

            // Парсим метаданные
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(metadata);

            System.out.println(metadata);

            // Извлекаем данные из JSON
            String detailedCaption = jsonNode.get("caption").asText();
            JsonNode captionToPhraseGrounding = jsonNode.get("objects");
            JsonNode tags =  jsonNode.get("tags");
            String text = jsonNode.get("text").asText();
            // Извлекаем bboxes и labels
            List<String> bboxes = new ArrayList<>();
            if (captionToPhraseGrounding.has("bboxes")) {
                for (JsonNode bbox : captionToPhraseGrounding.get("bboxes")) {
                    bboxes.add(bbox.toString()); // Сохраняем bboxes как строки
                }
            }


            List<LabelEntity> labelEntities = new ArrayList<>();
            if (tags != null) {
                for (JsonNode label : jsonNode.get("tags")) {
                    String labelName = label.asText();
                    LabelEntity existingLabel = labelRepository.findByName(labelName);
                    if (existingLabel == null) {
                        // Если метка не существует, создаем новую
                        LabelEntity newLabel = new LabelEntity();
                        newLabel.setName(labelName);
                        existingLabel = labelRepository.save(newLabel);
                    }
                    // Добавляем метку в список
                    labelEntities.add(existingLabel);
                }
            }

            // Сохраняем метаданные в БД
            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setFilename(filename);
            imageEntity.setFilepath(filepath.toString());
            imageEntity.setContentType("image/jpeg");
            imageEntity.setDetailedCaption(detailedCaption);
            imageEntity.setLabels(labelEntities); // Устанавливаем уникальные метки
            imageEntity.setBboxes(bboxes);
            imageEntity.setText(text);

            String metadataString = "Detailed Caption: " + detailedCaption + "\n" +
                    "Bboxes: " + String.join(", ", bboxes) + "\n" +
                    "Labels: " + labelEntities.stream().map(LabelEntity::getName).collect(Collectors.joining(", ")) + "\n";

            // Создаем новый файл с метаданными
            Path metadataFilePath = Paths.get(uploadDir + "metadata_" + filename);
            Files.write(metadataFilePath, metadataString.getBytes(), StandardOpenOption.CREATE);

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