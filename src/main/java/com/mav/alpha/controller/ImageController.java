package com.mav.alpha.controller;

import com.mav.alpha.entity.ImageEntity;
import com.mav.alpha.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/images")
public class ImageController {

    private final ImageService imageService;

    @Autowired
    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ImageEntity> uploadImage(@RequestParam("file") MultipartFile file) {
        ImageEntity savedImage = imageService.saveImage(file);
        return ResponseEntity.ok(savedImage);
    }

    @GetMapping
    public ResponseEntity<List<ImageEntity>> getAllImages() {
        List<ImageEntity> images = imageService.getAllImages();
        return ResponseEntity.ok(images);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImageById(@PathVariable Long id) {
        ImageEntity imageEntity = imageService.getImageById(id);
        Path path = Paths.get(imageEntity.getFilepath());
        try {
            byte[] imageBytes = Files.readAllBytes(path);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, imageEntity.getContentType());
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PostMapping("/upload-to-api")
    public ResponseEntity<ImageEntity> uploadImageToApi(@RequestParam("file") MultipartFile file, @RequestParam("apiUrl") String apiUrl) {
        ImageEntity savedImage = imageService.uploadImageToApi(file, apiUrl);
        return ResponseEntity.ok(savedImage);
    }
}
