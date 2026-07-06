package com.els.javatheorytrainer.controller;

import com.els.javatheorytrainer.entity.Section;
import com.els.javatheorytrainer.entity.Volume;
import com.els.javatheorytrainer.repository.QuestionRepository;
import com.els.javatheorytrainer.repository.SectionRepository;
import com.els.javatheorytrainer.repository.VolumeRepository;
import com.els.javatheorytrainer.service.MarkdownService;
import com.els.javatheorytrainer.service.PracticeService;
import com.els.javatheorytrainer.service.PracticeStatsService;
import com.els.javatheorytrainer.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

/**
 * Admin controller for managing sections.
 *
 * Section is a smaller topic inside a volume.
 *
 * Example:
 * Volume  = Java Core
 * Section = JVM, Memory
 */
@Controller
@RequestMapping("/admin/sections")
@RequiredArgsConstructor
public class AdminSectionController {

    private static final int SECTIONS_PAGE_SIZE = 20;

    private final QuestionRepository questionRepository;
    private final SectionRepository sectionRepository;
    private final VolumeRepository volumeRepository;
    private final MarkdownService markdownService;
    private final PracticeService practiceService;
    private final PracticeStatsService practiceStatsService;

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + id));

        model.addAttribute("section", section);
        model.addAttribute("questions", questionRepository.findBySectionIdOrderBySortOrderAscIdAsc(id));
        model.addAttribute("descriptionHtml", markdownService.toHtml(section.getDescription()));
        model.addAttribute("sectionStats", practiceStatsService.sectionStatsById().get(id));

        return "admin/sections/view";
    }

    /**
     * Shows all sections.
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) Long volumeId,
                       @RequestParam(required = false) Boolean active,
                       Model model) {
        Page<Section> sectionsPage = sectionRepository.findAdminPage(
                blankToNull(search),
                volumeId,
                active,
                PageRequest.of(normalizePage(page), SECTIONS_PAGE_SIZE)
        );

        model.addAttribute("sectionsPage", sectionsPage);
        model.addAttribute("sections", sectionsPage.getContent());
        model.addAttribute("currentPage", sectionsPage.getNumber() + 1);
        model.addAttribute("totalPages", sectionsPage.getTotalPages());
        model.addAttribute("pageSize", SECTIONS_PAGE_SIZE);
        model.addAttribute("search", search);
        model.addAttribute("selectedVolumeId", volumeId);
        model.addAttribute("selectedActive", active);
        model.addAttribute("volumes", volumeRepository.findAllByOrderBySortOrderAscTitleAsc());
        model.addAttribute("sectionStatsById", practiceStatsService.sectionStatsById());
        return "admin/sections/list";
    }

    /**
     * Shows form for creating a new section.
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        Section section = new Section();
        section.setActive(true);

        model.addAttribute("section", section);
        model.addAttribute("volumes", volumeRepository.findAllByOrderBySortOrderAscTitleAsc());
        model.addAttribute("pageTitle", "Новий розділ");

        return "admin/sections/form";
    }

    /**
     * Saves a new section.
     *
     * volumeId is passed separately because binding nested entities directly
     * from HTML forms is less clear and can create unnecessary complexity.
     */
    @PostMapping
    public String create(@ModelAttribute Section section,
                         @RequestParam Long volumeId) {

        Volume volume = volumeRepository.findById(volumeId)
                .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + volumeId));

        section.setVolume(volume);
        prepareSectionBeforeSave(section);

        if (section.getSortOrder() <= 0) {
            section.setSortOrder(sectionRepository.findMaxSortOrderByVolumeId(volumeId) + 10);
        }

        sectionRepository.save(section);

        return "redirect:/admin/sections";
    }

    /**
     * Shows form for editing an existing section.
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + id));

        model.addAttribute("section", section);
        model.addAttribute("volumes", volumeRepository.findAllByOrderBySortOrderAscTitleAsc());
        model.addAttribute("pageTitle", "Редагувати розділ");

        return "admin/sections/form";
    }

    /**
     * Updates existing section.
     */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute Section formSection,
                         @RequestParam Long volumeId) {

        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + id));

        Volume volume = volumeRepository.findById(volumeId)
                .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + volumeId));

        section.setVolume(volume);
        section.setTitle(formSection.getTitle());
        section.setSlug(formSection.getSlug());
        section.setDescription(formSection.getDescription());
        section.setActive(formSection.isActive());

        prepareSectionBeforeSave(section);

        sectionRepository.save(section);

        return "redirect:/admin/sections";
    }

    /**
     * Soft-hides section.
     * We do not delete it from database.
     */
    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + id));

        section.setActive(false);
        sectionRepository.save(section);

        return "redirect:/admin/sections";
    }

    /**
     * Makes archived section active again.
     */
    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + id));

        section.setActive(true);
        sectionRepository.save(section);

        return "redirect:/admin/sections";
    }

    @PostMapping("/{id}/reset-practice")
    public String resetPractice(@PathVariable Long id, @RequestHeader(value = "Referer", required = false) String referer) {
        practiceService.resetSectionPracticeStats(id);
        return redirectBack(referer, "/admin/sections/" + id);
    }

    /**
     * Generates slug from title if slug is empty.
     */
    private void prepareSectionBeforeSave(Section section) {
        if (section.getSlug() == null || section.getSlug().isBlank()) {
            section.setSlug(SlugUtils.toSlug(section.getTitle()));
        }
    }

    private String redirectBack(String referer, String fallback) {
        return "redirect:" + (referer == null || referer.isBlank() ? fallback : referer);
    }

    private int normalizePage(int page) {
        return Math.max(page, 1) - 1;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
