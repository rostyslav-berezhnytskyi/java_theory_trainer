package com.els.javatheorytrainer.controller;

import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.enums.PracticeGrade;
import com.els.javatheorytrainer.repository.SectionRepository;
import com.els.javatheorytrainer.repository.VolumeRepository;
import com.els.javatheorytrainer.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for practice mode.
 */
@Controller
@RequestMapping("/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;
    private final VolumeRepository volumeRepository;
    private final SectionRepository sectionRepository;

    @GetMapping({"", "/start"})
    public String startPage(Model model) {
        addStartPageData(model);
        return "practice/start";
    }

    @PostMapping("/start")
    public String startPractice(@RequestParam Long sectionId, Model model) {
        try {
            Question question = practiceService.pickNextQuestion(sectionId, null);
            return "redirect:/practice/questions/" + question.getId();
        } catch (IllegalStateException e) {
            addStartPageData(model);
            model.addAttribute("errorMessage", "У цьому розділі ще немає ACTIVE питань.");
            return "practice/start";
        }
    }

    @GetMapping("/questions/{id}")
    public String questionPage(@PathVariable Long id,
                               @RequestParam(defaultValue = "false") boolean answer,
                               Model model) {

        Question question = practiceService.findQuestionForPractice(id);

        model.addAttribute("question", question);
        model.addAttribute("showAnswer", answer);
        model.addAttribute("grades", PracticeGrade.values());

        return "practice/question";
    }

    @PostMapping("/questions/{id}/grade")
    public String submitGrade(@PathVariable Long id,
                              @RequestParam PracticeGrade grade) {

        Question answeredQuestion = practiceService.submitGrade(id, grade);

        Question nextQuestion = practiceService.pickNextQuestion(
                answeredQuestion.getSection().getId(),
                answeredQuestion.getId()
        );

        return "redirect:/practice/questions/" + nextQuestion.getId();
    }

    private void addStartPageData(Model model) {
        model.addAttribute("volumes", volumeRepository.findAllByOrderBySortOrderAscTitleAsc());
        model.addAttribute("sections", sectionRepository.findAllByOrderByVolumeSortOrderAscSortOrderAscTitleAsc());
    }
}
