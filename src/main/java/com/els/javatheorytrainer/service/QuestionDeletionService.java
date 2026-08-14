package com.els.javatheorytrainer.service;

import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.enums.QuestionStatus;
import com.els.javatheorytrainer.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionDeletionService {

    private final QuestionRepository questionRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public void delete(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        if (question.getStatus() == QuestionStatus.ACTIVE) {
            throw new IllegalStateException("Active questions cannot be deleted");
        }

        List<String> imageUrls = question.getImages().stream()
                .map(image -> image.getImageUrl())
                .toList();

        questionRepository.delete(question);
        questionRepository.flush();
        imageUrls.forEach(fileStorageService::deleteByPublicUrl);
    }
}
