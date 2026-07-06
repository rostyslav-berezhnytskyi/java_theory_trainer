package com.els.javatheorytrainer;

import com.els.javatheorytrainer.repository.QuestionRepository;
import com.els.javatheorytrainer.repository.SectionRepository;
import com.els.javatheorytrainer.repository.VolumeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class JavaTheoryPracticeApplicationTests {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private VolumeRepository volumeRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void adminListQueriesHandleEmptyFilters() {
        PageRequest firstPage = PageRequest.of(0, 20);

        volumeRepository.findAdminPage(null, null, firstPage);
        sectionRepository.findAdminPage(null, null, null, firstPage);
        questionRepository.findAdminPage(null, null, null, null, null, null, firstPage);
    }
}
