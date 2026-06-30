/**
 * Admin UI scripts.
 *
 * Features:
 * - searchable select inputs via Tom Select;
 * - cascading Volume -> Section selects for question form;
 * - clickable table rows;
 * - mobile-friendly behavior.
 */

document.addEventListener('DOMContentLoaded', function () {
    initBasicSelects();
    initQuestionCascadeSelects();
    initClickableRows();
    initImageLightbox();
});

/**
 * Initializes image lightbox.
 *
 * Any link with class "image-lightbox" will open image in full preview mode.
 */
function initImageLightbox() {
    if (typeof GLightbox === 'undefined') {
        return;
    }

    GLightbox({
        selector: '.image-lightbox',
        touchNavigation: true,
        loop: true,
        zoomable: true,
        draggable: true
    });
}

/**
 * Initializes simple searchable selects.
 *
 * Used for selects that do not depend on another select.
 */
function initBasicSelects() {
    document.querySelectorAll('select[data-basic-select]').forEach(function (select) {
        if (select.tomselect) {
            return;
        }

        new TomSelect(select, {
            create: false,
            allowEmptyOption: true,
            maxOptions: 1000,
            searchField: ['text'],
            sortField: {
                field: 'text',
                direction: 'asc'
            },
            placeholder: select.dataset.placeholder || 'Почни вводити...'
        });
    });
}

/**
 * Initializes cascading searchable selects:
 * Volume -> Section.
 *
 * Used on question create/edit form.
 */
function initQuestionCascadeSelects() {
    const volumeSelect = document.querySelector('select[data-volume-select]');
    const sectionSelect = document.querySelector('select[data-section-select]');

    if (!volumeSelect || !sectionSelect) {
        return;
    }

    const initialVolumeId = volumeSelect.value;
    const initialSectionId = sectionSelect.value;

    const allSectionOptions = Array.from(sectionSelect.querySelectorAll('option'))
        .filter(option => option.value !== '')
        .map(option => ({
            value: option.value,
            text: option.textContent.trim(),
            volumeId: option.dataset.volumeId
        }));

    const volumeTom = new TomSelect(volumeSelect, {
        create: false,
        allowEmptyOption: true,
        maxOptions: 1000,
        searchField: ['text'],
        sortField: {
            field: 'text',
            direction: 'asc'
        },
        placeholder: volumeSelect.dataset.placeholder || 'Почни вводити назву тому...'
    });

    const sectionTom = new TomSelect(sectionSelect, {
        create: false,
        allowEmptyOption: true,
        maxOptions: 1000,
        searchField: ['text'],
        sortField: {
            field: 'text',
            direction: 'asc'
        },
        placeholder: sectionSelect.dataset.placeholder || 'Спочатку обери том...'
    });

    function rebuildSections(volumeId, selectedSectionId) {
        sectionTom.clear(true);
        sectionTom.clearOptions();

        if (!volumeId) {
            sectionTom.disable();
            return;
        }

        const filteredSections = allSectionOptions
            .filter(option => String(option.volumeId) === String(volumeId));

        filteredSections.forEach(option => {
            sectionTom.addOption({
                value: option.value,
                text: option.text
            });
        });

        sectionTom.enable();
        sectionTom.refreshOptions(false);

        if (selectedSectionId && filteredSections.some(option => String(option.value) === String(selectedSectionId))) {
            sectionTom.setValue(String(selectedSectionId), true);
        }
    }

    volumeTom.on('change', function (value) {
        rebuildSections(value, null);
    });

    rebuildSections(initialVolumeId, initialSectionId);
}

/**
 * Makes table rows clickable.
 *
 * Important:
 * clicks on buttons, links and forms are ignored,
 * so edit/archive buttons still work normally.
 */
function initClickableRows() {
    document.querySelectorAll('[data-row-href]').forEach(function (row) {
        row.setAttribute('tabindex', '0');

        row.addEventListener('click', function (event) {
            if (shouldIgnoreRowClick(event.target)) {
                return;
            }

            window.location.href = row.dataset.rowHref;
        });

        row.addEventListener('keydown', function (event) {
            if (event.key !== 'Enter' && event.key !== ' ') {
                return;
            }

            if (shouldIgnoreRowClick(event.target)) {
                return;
            }

            event.preventDefault();
            window.location.href = row.dataset.rowHref;
        });
    });
}

function shouldIgnoreRowClick(target) {
    return Boolean(target.closest(
        'a, button, input, select, textarea, label, form, .ts-wrapper, .no-row-click'
    ));
}