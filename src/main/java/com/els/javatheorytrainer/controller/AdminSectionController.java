package com.els.javatheorytrainer.controller;

import com.els.javatheorytrainer.entity.Section;
import com.els.javatheorytrainer.entity.Volume;
import com.els.javatheorytrainer.repository.SectionRepository;
import com.els.javatheorytrainer.repository.VolumeRepository;
import com.els.javatheorytrainer.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    private final SectionRepository sectionRepository;
    private final VolumeRepository volumeRepository;

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + id));

        model.addAttribute("section", section);

        return "admin/sections/view";
    }

    /**
     * Shows all sections.
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("sections", sectionRepository.findAllByOrderByVolumeSortOrderAscSortOrderAscTitleAsc());
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

    /**
     * Generates slug from title if slug is empty.
     */
    private void prepareSectionBeforeSave(Section section) {
        if (section.getSlug() == null || section.getSlug().isBlank()) {
            section.setSlug(SlugUtils.toSlug(section.getTitle()));
        }
    }
}
