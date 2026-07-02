package com.els.javatheorytrainer.service;

import com.els.javatheorytrainer.dto.PracticeProgressStats;
import com.els.javatheorytrainer.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PracticeStatsService {

    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public Map<Long, PracticeProgressStats> sectionStatsById() {
        return questionRepository.findSectionProgressStats().stream()
                .collect(Collectors.toMap(PracticeProgressStats::ownerId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public Map<Long, PracticeProgressStats> volumeStatsById() {
        return questionRepository.findVolumeProgressStats().stream()
                .collect(Collectors.toMap(PracticeProgressStats::ownerId, Function.identity()));
    }
}
