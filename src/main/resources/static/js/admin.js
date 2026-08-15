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
    initAiPracticeChat();
    initPracticeHintToggle();
    initPracticeVoiceControls();
    initAiAudioTranscription();
    initResponsiveEntityDescriptions();
});

function initResponsiveEntityDescriptions() {
    const descriptions = document.querySelectorAll('.entity-description');
    if (!descriptions.length) {
        return;
    }

    const mobileQuery = window.matchMedia('(max-width: 900px)');
    let wasMobile = mobileQuery.matches;

    descriptions.forEach(function (description) {
        description.open = !wasMobile;
    });

    mobileQuery.addEventListener('change', function (event) {
        if (event.matches === wasMobile) {
            return;
        }

        descriptions.forEach(function (description) {
            description.open = !event.matches;
        });
        wasMobile = event.matches;
    });
}

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

function initAiPracticeChat() {
    document.querySelectorAll('.ai-chat').forEach(function (chat) {
        const form = chat.querySelector('.ai-chat-form');
        const input = chat.querySelector('.ai-chat-input');
        const messages = chat.querySelector('.ai-chat-messages');

        if (!form || !input || !messages || !chat.dataset.chatUrl) {
            return;
        }

        form.addEventListener('submit', function (event) {
            event.preventDefault();

            const text = input.value.trim();
            if (!text) {
                return;
            }

            appendAiChatMessage(messages, 'user', text);
            input.value = '';
            setAiChatPending(form, true);

            fetch(chat.dataset.chatUrl, {
                method: 'POST',
                headers: aiChatHeaders(),
                body: JSON.stringify({message: text})
            })
                .then(function (response) {
                    return response.json().then(function (body) {
                        if (!response.ok) {
                            throw new Error(body.error || 'AI chat failed');
                        }

                        return body;
                    });
                })
                .then(function (body) {
                    appendAiChatMessage(messages, 'assistant', body.replyHtml || body.reply || '', Boolean(body.replyHtml));
                })
                .catch(function (error) {
                    appendAiChatMessage(messages, 'error', error.message || 'AI chat failed');
                })
                .finally(function () {
                    setAiChatPending(form, false);
                    input.focus();
                });
        });
    });
}

function aiChatHeaders() {
    const csrfInput = document.querySelector('input[name="_csrf"]');
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };

    if (csrfInput) {
        headers['X-CSRF-TOKEN'] = csrfInput.value;
    }

    return headers;
}

function appendAiChatMessage(container, role, text, html) {
    const message = document.createElement('div');
    message.className = 'ai-chat-message ai-chat-message-' + role;

    if (html) {
        message.classList.add('markdown-content');
        message.innerHTML = text;
    } else {
        message.textContent = text;
    }

    container.appendChild(message);
    container.scrollTop = container.scrollHeight;
}

function setAiChatPending(form, pending) {
    const button = form.querySelector('button[type="submit"]');
    const input = form.querySelector('.ai-chat-input');

    if (button) {
        button.disabled = pending;
        button.textContent = pending ? 'Очікування...' : 'Запитати';
    }

    if (input) {
        input.disabled = pending;
    }
}

function initPracticeHintToggle() {
    document.querySelectorAll('.js-toggle-hint').forEach(function (button) {
        const hint = document.getElementById(button.getAttribute('aria-controls'));

        if (!hint) {
            return;
        }

        button.addEventListener('click', function () {
            const isHidden = hint.hidden;

            hint.hidden = !isHidden;
            button.setAttribute('aria-expanded', String(isHidden));
            button.textContent = isHidden ? 'Hide hint' : 'Show hint';
        });
    });
}

function initPracticeVoiceControls() {
    initSpeechSynthesisControls();
    initSpeechRecognitionControls();
}

function initSpeechSynthesisControls() {
    const canSpeak = 'speechSynthesis' in window && typeof SpeechSynthesisUtterance !== 'undefined';
    const buttons = Array.from(document.querySelectorAll('.js-speak-target'));

    buttons.forEach(function (button) {
        if (!canSpeak && !button.dataset.aiSpeechUrl) {
            button.disabled = true;
            button.title = 'Speech synthesis is not supported in this browser';
            return;
        }

        button.dataset.originalText = button.textContent;
        button.disabled = true;
        button.textContent = 'Завантажую голос...';

        button.addEventListener('click', function () {
            if (button.classList.contains('voice-active')) {
                stopActiveAiSpeech();
                window.speechSynthesis.cancel();
                resetSpeechButtons();
                return;
            }

            const target = document.querySelector(button.dataset.speechTarget);
            if (!target) {
                return;
            }

            const text = target.innerText.trim();
            if (!text) {
                return;
            }

            if (button.dataset.aiSpeechUrl) {
                speakWithOpenAi(text, button);
                return;
            }

            speakUkrainian(text, button);
        });
    });

    if (!canSpeak && !buttons.some(function (button) { return button.dataset.aiSpeechUrl; })) {
        return;
    }

    function refreshVoiceButtons() {
        const ukrainianVoice = findUkrainianVoice();

        buttons.forEach(function (button) {
            button.textContent = button.dataset.originalText;
            button.disabled = false;
            button.title = ukrainianVoice
                    ? ''
                    : 'Ukrainian speech voice is not available; browser default voice will be used';
        });
    }

    refreshVoiceButtons();
    window.speechSynthesis.onvoiceschanged = refreshVoiceButtons;
    setTimeout(refreshVoiceButtons, 500);
}

function speakUkrainian(text, button) {
    window.speechSynthesis.cancel();

    const voice = findUkrainianVoice();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'uk-UA';
    utterance.rate = 0.95;
    utterance.pitch = 1;
    if (voice) {
        utterance.voice = voice;
    }

    resetSpeechButtons();
    button.classList.add('voice-active');
    button.textContent = 'Зупинити';

    utterance.onend = function () {
        resetSpeechButtons();
    };

    utterance.onerror = function () {
        resetSpeechButtons();
    };

    window.speechSynthesis.speak(utterance);
}

function findUkrainianVoice() {
    const voices = window.speechSynthesis.getVoices();

    return voices.find(function (voice) {
        return voice.lang && voice.lang.toLowerCase() === 'uk-ua';
    }) || voices.find(function (voice) {
        return voice.lang && voice.lang.toLowerCase().startsWith('uk');
    }) || null;
}

function resetSpeechButtons() {
    document.querySelectorAll('.js-speak-target').forEach(function (button) {
        button.classList.remove('voice-active');
        if (button.dataset.originalText) {
            button.textContent = button.dataset.originalText;
        }
    });
}

let activeAiSpeechAudio = null;
let activeAiSpeechUrl = null;

function speakWithOpenAi(text, button) {
    stopActiveAiSpeech();
    window.speechSynthesis.cancel();
    resetSpeechButtons();

    button.disabled = true;
    button.textContent = 'Готую аудіо...';

    fetch(button.dataset.aiSpeechUrl, {
        method: 'POST',
        headers: aiJsonHeaders(),
        body: JSON.stringify({text: text})
    })
        .then(function (response) {
            if (!response.ok) {
                throw new Error('Text-to-speech failed');
            }

            return response.blob();
        })
        .then(function (blob) {
            activeAiSpeechUrl = URL.createObjectURL(blob);
            activeAiSpeechAudio = new Audio(activeAiSpeechUrl);

            button.disabled = false;
            button.classList.add('voice-active');
            button.textContent = 'Зупинити';

            activeAiSpeechAudio.onended = resetActiveAiSpeech;
            activeAiSpeechAudio.onerror = resetActiveAiSpeech;
            activeAiSpeechAudio.play().catch(function () {
                resetActiveAiSpeech();
                speakUkrainian(text, button);
            });
        })
        .catch(function () {
            button.disabled = false;
            speakUkrainian(text, button);
        });
}

function stopActiveAiSpeech() {
    if (activeAiSpeechAudio) {
        activeAiSpeechAudio.pause();
        activeAiSpeechAudio.currentTime = 0;
    }

    resetActiveAiSpeech();
}

function resetActiveAiSpeech() {
    if (activeAiSpeechUrl) {
        URL.revokeObjectURL(activeAiSpeechUrl);
    }

    activeAiSpeechAudio = null;
    activeAiSpeechUrl = null;
    resetSpeechButtons();
}

function aiJsonHeaders() {
    const csrfInput = document.querySelector('input[name="_csrf"]');
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json, audio/mpeg'
    };

    if (csrfInput) {
        headers['X-CSRF-TOKEN'] = csrfInput.value;
    }

    return headers;
}

function initSpeechRecognitionControls() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

    document.querySelectorAll('.js-dictate-answer').forEach(function (button) {
        const input = document.querySelector(button.dataset.speechInput);

        if (!SpeechRecognition || !input) {
            button.disabled = true;
            button.title = 'Speech recognition is not supported in this browser';
            return;
        }

        let recognition = null;
        let listening = false;
        const originalText = button.textContent;

        button.addEventListener('click', function () {
            if (listening && recognition) {
                recognition.stop();
                return;
            }

            recognition = new SpeechRecognition();
            recognition.lang = 'uk-UA';
            recognition.continuous = true;
            recognition.interimResults = false;

            recognition.onstart = function () {
                listening = true;
                button.textContent = 'Зупинити запис';
                button.classList.add('voice-active');
            };

            recognition.onresult = function (event) {
                const transcript = Array.from(event.results)
                        .slice(event.resultIndex)
                        .map(function (result) {
                            return result[0].transcript;
                        })
                        .join(' ')
                        .trim();

                if (transcript) {
                    appendDictatedText(input, transcript);
                }
            };

            recognition.onend = function () {
                listening = false;
                button.textContent = originalText;
                button.classList.remove('voice-active');
                input.focus();
            };

            recognition.onerror = function () {
                listening = false;
                button.textContent = originalText;
                button.classList.remove('voice-active');
            };

            recognition.start();
        });
    });
}

function appendDictatedText(input, text) {
    const current = input.value.trim();
    input.value = current ? current + ' ' + text : text;
    input.dispatchEvent(new Event('input', {bubbles: true}));
}

function initAiAudioTranscription() {
    const canRecord = Boolean(navigator.mediaDevices && navigator.mediaDevices.getUserMedia && window.MediaRecorder);

    document.querySelectorAll('.js-ai-record-transcribe').forEach(function (button) {
        const input = document.querySelector(button.dataset.transcriptionInput);

        if (!canRecord || !input || !button.dataset.transcriptionUrl) {
            button.disabled = true;
            button.title = 'Audio recording is not supported in this browser';
            return;
        }

        let recorder = null;
        let chunks = [];
        let stream = null;
        const originalText = button.textContent;

        button.addEventListener('click', function () {
            if (recorder && recorder.state === 'recording') {
                recorder.stop();
                return;
            }

            button.textContent = 'Готую мікрофон...';
            button.classList.add('voice-active');

            navigator.mediaDevices.getUserMedia({audio: true})
                .then(function (mediaStream) {
                    stream = mediaStream;
                    chunks = [];

                    recorder = new MediaRecorder(stream, mediaRecorderOptions());
                    recorder.ondataavailable = function (event) {
                        if (event.data && event.data.size > 0) {
                            chunks.push(event.data);
                        }
                    };
                    recorder.onstart = function () {
                        button.textContent = 'Говоріть...';
                        button.classList.add('voice-active');
                        setTimeout(function () {
                            if (recorder && recorder.state === 'recording') {
                                button.textContent = 'Зупинити AI запис';
                            }
                        }, 900);
                    };
                    recorder.onstop = function () {
                        stopAudioStream(stream);
                        stream = null;
                        button.classList.remove('voice-active');
                        uploadAudioForTranscription(button, input, chunks, originalText);
                    };
                    recorder.onerror = function () {
                        stopAudioStream(stream);
                        stream = null;
                        button.textContent = originalText;
                        button.classList.remove('voice-active');
                    };

                    recorder.start(1000);
                })
                .catch(function () {
                    button.textContent = originalText;
                    button.classList.remove('voice-active');
                });
        });
    });
}

function mediaRecorderOptions() {
    if (MediaRecorder.isTypeSupported && MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
        return {mimeType: 'audio/webm;codecs=opus'};
    }
    if (MediaRecorder.isTypeSupported && MediaRecorder.isTypeSupported('audio/webm')) {
        return {mimeType: 'audio/webm'};
    }
    return {};
}

function uploadAudioForTranscription(button, input, chunks, originalText) {
    if (!chunks.length) {
        button.textContent = originalText;
        return;
    }

    button.disabled = true;
    button.textContent = 'Розпізнаю...';

    const blob = new Blob(chunks, {type: chunks[0].type || 'audio/webm'});
    const formData = new FormData();
    formData.append('audio', blob, 'practice-audio.webm');

    fetch(button.dataset.transcriptionUrl, {
        method: 'POST',
        headers: aiAudioHeaders(),
        body: formData
    })
        .then(function (response) {
            return response.json().then(function (body) {
                if (!response.ok) {
                    throw new Error(body.error || 'Audio transcription failed');
                }

                return body;
            });
        })
        .then(function (body) {
            if (body.text) {
                appendDictatedText(input, body.text);
            }
        })
        .catch(function (error) {
            appendDictatedText(input, '[' + (error.message || 'Audio transcription failed') + ']');
        })
        .finally(function () {
            button.disabled = false;
            button.textContent = originalText;
            input.focus();
        });
}

function aiAudioHeaders() {
    const csrfInput = document.querySelector('input[name="_csrf"]');

    return csrfInput ? {'X-CSRF-TOKEN': csrfInput.value} : {};
}

function stopAudioStream(stream) {
    if (!stream) {
        return;
    }

    stream.getTracks().forEach(function (track) {
        track.stop();
    });
}
