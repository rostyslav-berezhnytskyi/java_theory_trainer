package com.els.javatheorytrainer.controller;

import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.entity.QuestionImage;
import com.els.javatheorytrainer.enums.ImageRole;
import com.els.javatheorytrainer.repository.QuestionImageRepository;
import com.els.javatheorytrainer.repository.QuestionRepository;
import com.els.javatheorytrainer.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin controller for uploading and deleting question images.
 */
@Controller
@RequestMapping("/admin/questions/{questionId}/images")
@RequiredArgsConstructor
public class AdminQuestionImageController {

    private final QuestionRepository questionRepository;
    private final QuestionImageRepository questionImageRepository;
    private final FileStorageService fileStorageService;

    @PostMapping
    public String uploadImage(@PathVariable Long questionId,
                              @RequestParam MultipartFile file,
                              @RequestParam ImageRole role,
                              @RequestParam(required = false) String altText,
                              @RequestParam(required = false) String caption,
                              @RequestParam(defaultValue = "0") int sortOrder) {

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        String imageUrl = fileStorageService.storeQuestionImage(question, file);

        QuestionImage image = new QuestionImage();
        image.setQuestion(question);
        image.setRole(role);
        image.setImageUrl(imageUrl);
        image.setAltText(altText);
        image.setCaption(caption);
        image.setSortOrder(sortOrder);

        questionImageRepository.save(image);

        return "redirect:/admin/questions/" + questionId + "/edit";
    }

    @PostMapping("/{imageId}/delete")
    public String deleteImage(@PathVariable Long questionId,
                              @PathVariable Long imageId) {

        QuestionImage image = questionImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found: " + imageId));

        fileStorageService.deleteByPublicUrl(image.getImageUrl());
        questionImageRepository.delete(image);

        return "redirect:/admin/questions/" + questionId + "/edit";
    }
}