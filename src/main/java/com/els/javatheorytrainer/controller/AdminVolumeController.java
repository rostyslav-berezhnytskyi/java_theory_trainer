package com.els.javatheorytrainer.controller;

import com.els.javatheorytrainer.entity.Volume;
import com.els.javatheorytrainer.repository.VolumeRepository;
import com.els.javatheorytrainer.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for managing study volumes.
 *
 * Volume examples:
 * - Java Core
 * - Hibernate
 * - Spring Web
 * - Algorithms
 */
@Controller
@RequestMapping("/admin/volumes")
@RequiredArgsConstructor
public class AdminVolumeController {

    private final VolumeRepository volumeRepository;

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Volume volume = volumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + id));

        model.addAttribute("volume", volume);

        return "admin/volumes/view";
    }

    /**
     * Shows all volumes.
     */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("volumes", volumeRepository.findAllByOrderBySortOrderAscTitleAsc());
        return "admin/volumes/list";
    }

    /**
     * Shows form for creating a new volume.
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        Volume volume = new Volume();
        volume.setActive(true);

        model.addAttribute("volume", volume);
        model.addAttribute("pageTitle", "Новий том");

        return "admin/volumes/form";
    }

    /**
     * Saves new volume.
     */
    @PostMapping
    public String create(@ModelAttribute Volume volume) {
        prepareVolumeBeforeSave(volume);
        volumeRepository.save(volume);

        return "redirect:/admin/volumes";
    }

    /**
     * Shows form for editing existing volume.
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Volume volume = volumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + id));

        model.addAttribute("volume", volume);
        model.addAttribute("pageTitle", "Редагувати том");

        return "admin/volumes/form";
    }

    /**
     * Updates existing volume.
     */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Volume formVolume) {
        Volume volume = volumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + id));

        volume.setTitle(formVolume.getTitle());
        volume.setSlug(formVolume.getSlug());
        volume.setDescription(formVolume.getDescription());
        volume.setSortOrder(formVolume.getSortOrder());
        volume.setActive(formVolume.isActive());

        prepareVolumeBeforeSave(volume);
        volumeRepository.save(volume);

        return "redirect:/admin/volumes";
    }

    /**
     * Soft-hides volume.
     * We do not delete it from database.
     */
    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id) {
        Volume volume = volumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + id));

        volume.setActive(false);
        volumeRepository.save(volume);

        return "redirect:/admin/volumes";
    }

    /**
     * Makes archived volume active again.
     */
    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id) {
        Volume volume = volumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + id));

        volume.setActive(true);
        volumeRepository.save(volume);

        return "redirect:/admin/volumes";
    }

    /**
     * If slug is empty, generate it from title.
     */
    private void prepareVolumeBeforeSave(Volume volume) {
        if (volume.getSlug() == null || volume.getSlug().isBlank()) {
            volume.setSlug(SlugUtils.toSlug(volume.getTitle()));
        }
    }
}
