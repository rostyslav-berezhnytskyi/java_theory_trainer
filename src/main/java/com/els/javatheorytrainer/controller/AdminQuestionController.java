package com.els.javatheorytrainer.controller;

import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.entity.Section;
import com.els.javatheorytrainer.enums.Difficulty;
import com.els.javatheorytrainer.enums.QuestionStatus;
import com.els.javatheorytrainer.form.QuestionForm;
import com.els.javatheorytrainer.repository.QuestionRepository;
import com.els.javatheorytrainer.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Admin controller for managing questions.
 *
 * A question belongs to one section and contains:
 * - question text;
 * - short answer;
 * - full answer;
 * - hint;
 * - must-have points;
 * - common mistakes;
 * - tags and metadata.
 */
@Controller
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final QuestionRepository questionRepository;
    private final SectionRepository sectionRepository;

    /**
     * Shows all questions.
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("questions", questionRepository.findAllByOrderBySectionVolumeSortOrderAscSectionSortOrderAscSortOrderAscIdAsc());
        return "admin/questions/list";
    }

    /**
     * Shows form for creating a new question.
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        QuestionForm form = new QuestionForm();

        model.addAttribute("questionForm", form);
        addFormOptions(model);
        model.addAttribute("pageTitle", "Нове питання");

        return "admin/questions/form";
    }

    /**
     * Saves a new question.
     */
    @PostMapping
    public String create(@ModelAttribute QuestionForm form) {
        Section section = findSection(form.getSectionId());

        Question question = new Question();
        applyFormToQuestion(form, question, section);

        questionRepository.save(question);

        return "redirect:/admin/questions";
    }

    /**
     * Shows form for editing an existing question.
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Question question = findQuestion(id);

        QuestionForm form = toForm(question);

        model.addAttribute("questionForm", form);
        addFormOptions(model);
        model.addAttribute("pageTitle", "Редагувати питання");

        return "admin/questions/form";
    }

    /**
     * Updates an existing question.
     */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute QuestionForm form) {

        Question question = findQuestion(id);
        Section section = findSection(form.getSectionId());

        applyFormToQuestion(form, question, section);

        questionRepository.save(question);

        return "redirect:/admin/questions";
    }

    /**
     * Soft-archives question.
     */
    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id) {
        Question question = findQuestion(id);
        question.setStatus(QuestionStatus.ARCHIVED);
        questionRepository.save(question);

        return "redirect:/admin/questions";
    }

    /**
     * Makes question active again.
     */
    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id) {
        Question question = findQuestion(id);
        question.setStatus(QuestionStatus.ACTIVE);
        questionRepository.save(question);

        return "redirect:/admin/questions";
    }

    private Question findQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + id));
    }

    private Section findSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + id));
    }

    /**
     * Adds common select options to the form page.
     */
    private void addFormOptions(Model model) {
        model.addAttribute("sections", sectionRepository.findAllByOrderByVolumeSortOrderAscSortOrderAscTitleAsc());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("statuses", QuestionStatus.values());
    }

    /**
     * Copies form data into Question entity.
     */
    private void applyFormToQuestion(QuestionForm form, Question question, Section section) {
        question.setSection(section);
        question.setQuestionText(form.getQuestionText());
        question.setShortAnswer(form.getShortAnswer());
        question.setFullAnswer(form.getFullAnswer());
        question.setHint(form.getHint());
        question.setTags(form.getTags());
        question.setSourceReference(form.getSourceReference());
        question.setDifficulty(form.getDifficulty());
        question.setStatus(form.getStatus());
        question.setSortOrder(form.getSortOrder());

        question.getMustHavePoints().clear();
        question.getMustHavePoints().addAll(splitMultilineText(form.getMustHavePointsText()));

        question.getCommonMistakes().clear();
        question.getCommonMistakes().addAll(splitMultilineText(form.getCommonMistakesText()));
    }

    /**
     * Converts Question entity to QuestionForm for editing.
     */
    private QuestionForm toForm(Question question) {
        QuestionForm form = new QuestionForm();

        form.setId(question.getId());
        form.setSectionId(question.getSection().getId());
        form.setQuestionText(question.getQuestionText());
        form.setShortAnswer(question.getShortAnswer());
        form.setFullAnswer(question.getFullAnswer());
        form.setHint(question.getHint());
        form.setTags(question.getTags());
        form.setSourceReference(question.getSourceReference());
        form.setDifficulty(question.getDifficulty());
        form.setStatus(question.getStatus());
        form.setSortOrder(question.getSortOrder());

        form.setMustHavePointsText(joinLines(question.getMustHavePoints()));
        form.setCommonMistakesText(joinLines(question.getCommonMistakes()));

        return form;
    }

    /**
     * Splits textarea text into list.
     * Empty lines are ignored.
     */
    private List<String> splitMultilineText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    /**
     * Converts list items back to textarea text.
     */
    private String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        return String.join(System.lineSeparator(), lines);
    }
}
