package com.els.javatheorytrainer.controller;

import com.els.javatheorytrainer.entity.PracticeAttempt;
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
                               @RequestParam(required = false) Long attemptId,
                               Model model) {

        Question question = practiceService.findQuestionForPractice(id);
        PracticeAttempt attempt = attemptId == null ? null : practiceService.findAttempt(attemptId);
        if (attempt != null && !attempt.getQuestion().getId().equals(question.getId())) {
            throw new IllegalArgumentException("Practice attempt does not belong to question: " + id);
        }

        addQuestionPageData(model, question, attempt);

        return "practice/question";
    }

    @PostMapping("/questions/{id}/answer")
    public String submitAnswer(@PathVariable Long id,
                               @RequestParam String userAnswer,
                               Model model) {
        try {
            PracticeAttempt attempt = practiceService.submitAnswer(id, userAnswer);
            return "redirect:/practice/questions/" + id + "?attemptId=" + attempt.getId();
        } catch (IllegalArgumentException e) {
            Question question = practiceService.findQuestionForPractice(id);
            addQuestionPageData(model, question, null);
            model.addAttribute("answerError", "Відповідь не може бути порожньою.");
            model.addAttribute("userAnswer", userAnswer);
            return "practice/question";
        }
    }

    @PostMapping("/attempts/{attemptId}/grade")
    public String submitGrade(@PathVariable Long attemptId,
                              @RequestParam PracticeGrade grade) {

        PracticeAttempt attempt = practiceService.submitGrade(attemptId, grade);
        Question answeredQuestion = attempt.getQuestion();

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

    private void addQuestionPageData(Model model, Question question, PracticeAttempt attempt) {
        model.addAttribute("question", question);
        model.addAttribute("attempt", attempt);
        model.addAttribute("showAnswer", attempt != null);
        model.addAttribute("grades", PracticeGrade.values());

        model.addAttribute("shortAnswerHtml", markdownService.toHtml(question.getShortAnswer()));
        model.addAttribute("fullAnswerHtml", markdownService.toHtml(question.getFullAnswer()));
        model.addAttribute("hintHtml", markdownService.toHtml(question.getHint()));
        model.addAttribute("theoryNotesHtml", markdownService.toHtml(question.getTheoryNotes()));

        model.addAttribute("questionImages", imagesByRole(question, ImageRole.QUESTION));
        model.addAttribute("answerImages", imagesByRole(question, ImageRole.ANSWER));
    }

    private List<QuestionImage> imagesByRole(Question question, ImageRole role) {
        return question.getImages().stream()
                .filter(image -> image.getRole() == role)
                .toList();
    }
}
