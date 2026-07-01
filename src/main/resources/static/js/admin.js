/**
 * Admin UI scripts.
 *
 * Features:
 * - searchable select inputs via Tom Select;
 * - cascading Volume -> Section selects for question form;
 * - clickable table rows;
 * - image lightbox previews.
 */

document.addEventListener('DOMContentLoaded', function () {
    initBasicSelects();
    initQuestionCascadeSelects();
    initClickableRows();
    initImageLightbox();
});

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
