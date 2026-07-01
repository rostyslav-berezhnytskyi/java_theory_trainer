package com.els.javatheorytrainer.controller;

import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.entity.QuestionImage;
import com.els.javatheorytrainer.enums.ImageRole;
import com.els.javatheorytrainer.enums.PracticeGrade;
import com.els.javatheorytrainer.repository.SectionRepository;
import com.els.javatheorytrainer.repository.VolumeRepository;
import com.els.javatheorytrainer.service.MarkdownService;
import com.els.javatheorytrainer.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final MarkdownService markdownService;

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

        model.addAttribute("shortAnswerHtml", markdownService.toHtml(question.getShortAnswer()));
        model.addAttribute("fullAnswerHtml", markdownService.toHtml(question.getFullAnswer()));
        model.addAttribute("hintHtml", markdownService.toHtml(question.getHint()));
        model.addAttribute("theoryNotesHtml", markdownService.toHtml(question.getTheoryNotes()));

        model.addAttribute("questionImages", imagesByRole(question, ImageRole.QUESTION));
        model.addAttribute("answerImages", imagesByRole(question, ImageRole.ANSWER));

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

    private List<QuestionImage> imagesByRole(Question question, ImageRole role) {
        return question.getImages().stream()
                .filter(image -> image.getRole() == role)
                .toList();
    }
}
