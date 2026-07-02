package com.els.javatheorytrainer.controller;

import com.els.javatheorytrainer.dto.AiChatRequest;
import com.els.javatheorytrainer.dto.AiChatResponse;
import com.els.javatheorytrainer.dto.AudioTranscriptionResponse;
import com.els.javatheorytrainer.dto.PracticeSessionSettings;
import com.els.javatheorytrainer.dto.TextToSpeechRequest;
import com.els.javatheorytrainer.entity.PracticeAttempt;
import com.els.javatheorytrainer.entity.Question;
import com.els.javatheorytrainer.entity.QuestionImage;
import com.els.javatheorytrainer.enums.ImageRole;
import com.els.javatheorytrainer.enums.PracticeGrade;
import com.els.javatheorytrainer.enums.PracticeQuestionFilter;
import com.els.javatheorytrainer.enums.PracticeScope;
import com.els.javatheorytrainer.repository.SectionRepository;
import com.els.javatheorytrainer.repository.VolumeRepository;
import com.els.javatheorytrainer.service.AiConversationMemoryService;
import com.els.javatheorytrainer.service.AiEvaluationService;
import com.els.javatheorytrainer.service.AiTextToSpeechService;
import com.els.javatheorytrainer.service.AiTranscriptionService;
import com.els.javatheorytrainer.service.MarkdownService;
import com.els.javatheorytrainer.service.PracticeService;
import com.els.javatheorytrainer.service.PracticeStatsService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/practice")
@RequiredArgsConstructor
public class PracticeController {

    private static final String PRACTICE_SETTINGS_SESSION_KEY = "practiceSettings";

    private final PracticeService practiceService;
    private final VolumeRepository volumeRepository;
    private final SectionRepository sectionRepository;
    private final MarkdownService markdownService;
    private final AiEvaluationService aiEvaluationService;
    private final AiConversationMemoryService aiConversationMemoryService;
    private final AiTranscriptionService aiTranscriptionService;
    private final AiTextToSpeechService aiTextToSpeechService;
    private final PracticeStatsService practiceStatsService;

    @GetMapping({"", "/start"})
    public String startPage(Model model) {
        addStartPageData(model);
        return "practice/start";
    }

    @PostMapping("/start")
    public String startPractice(@RequestParam PracticeScope scope,
                                @RequestParam Long volumeId,
                                @RequestParam(required = false) Long sectionId,
                                @RequestParam(defaultValue = "ALL_ACTIVE") PracticeQuestionFilter filter,
                                @RequestParam(defaultValue = "false") boolean randomOrder,
                                Model model,
                                HttpSession session) {
        try {
            PracticeSessionSettings settings = buildPracticeSettings(scope, volumeId, sectionId, filter, randomOrder);
            aiConversationMemoryService.clearSession(session.getId());
            session.setAttribute(PRACTICE_SETTINGS_SESSION_KEY, settings);

            Question question = practiceService.pickNextQuestion(settings, null);
            return "redirect:/practice/questions/" + question.getId();
        } catch (IllegalStateException e) {
            addStartPageData(model);
            model.addAttribute("errorMessage", "No ACTIVE questions match selected practice options.");
            return "practice/start";
        } catch (IllegalArgumentException e) {
            addStartPageData(model);
            model.addAttribute("errorMessage", e.getMessage());
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
            practiceService.evaluateAnswerWithAi(attempt.getId());
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
                              @RequestParam PracticeGrade grade,
                              HttpSession session,
                              Model model) {

        PracticeAttempt attempt = practiceService.submitGrade(attemptId, grade);
        aiConversationMemoryService.clearAttempt(session.getId(), attemptId);
        Question answeredQuestion = attempt.getQuestion();

        Question nextQuestion;
        try {
            nextQuestion = practiceService.pickNextQuestion(
                    practiceSettings(session, answeredQuestion),
                    answeredQuestion.getId()
            );
        } catch (IllegalStateException e) {
            addStartPageData(model);
            model.addAttribute("errorMessage", "No more questions match selected practice options.");
            return "practice/start";
        }

        return "redirect:/practice/questions/" + nextQuestion.getId();
    }

    @PostMapping("/attempts/{attemptId}/ai-chat")
    @ResponseBody
    public ResponseEntity<?> chatWithAi(@PathVariable Long attemptId,
                                        @Valid @RequestBody AiChatRequest request,
                                        HttpSession session) {

        PracticeAttempt attempt = practiceService.findAttempt(attemptId);
        String message = request.message().trim();

        try {
            String reply = aiEvaluationService.chat(
                    attempt,
                    aiConversationMemoryService.getHistory(session.getId(), attemptId),
                    message
            );

            aiConversationMemoryService.addExchange(session.getId(), attemptId, message, reply);

            return ResponseEntity.ok(new AiChatResponse(reply, markdownService.toHtml(reply)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage() == null ? "AI chat failed" : e.getMessage()));
        }
    }

    @PostMapping("/attempts/{attemptId}/ai-chat/clear")
    @ResponseBody
    public ResponseEntity<Void> clearAiChat(@PathVariable Long attemptId, HttpSession session) {
        aiConversationMemoryService.clearAttempt(session.getId(), attemptId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/audio/transcribe")
    @ResponseBody
    public ResponseEntity<?> transcribeAudio(@RequestParam("audio") MultipartFile audio) {
        try {
            return ResponseEntity.ok(new AudioTranscriptionResponse(aiTranscriptionService.transcribe(audio)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage() == null ? "Audio transcription failed" : e.getMessage()));
        }
    }

    @PostMapping("/audio/speech")
    @ResponseBody
    public ResponseEntity<?> createSpeech(@Valid @RequestBody TextToSpeechRequest request) {
        try {
            byte[] audio = aiTextToSpeechService.createSpeech(request.text());

            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("audio/mpeg"))
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(audio);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", e.getMessage() == null ? "Text-to-speech failed" : e.getMessage()));
        }
    }

    private void addStartPageData(Model model) {
        model.addAttribute("volumes", volumeRepository.findAllByOrderBySortOrderAscTitleAsc());
        model.addAttribute("sections", sectionRepository.findAllByOrderByVolumeSortOrderAscSortOrderAscTitleAsc());
        model.addAttribute("scopes", PracticeScope.values());
        model.addAttribute("filters", PracticeQuestionFilter.values());
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
        model.addAttribute("sectionStats", practiceStatsService.sectionStatsById().get(question.getSection().getId()));
        model.addAttribute("volumeStats", practiceStatsService.volumeStatsById().get(question.getSection().getVolume().getId()));
    }

    private List<QuestionImage> imagesByRole(Question question, ImageRole role) {
        return question.getImages().stream()
                .filter(image -> image.getRole() == role)
                .toList();
    }

    private PracticeSessionSettings buildPracticeSettings(
            PracticeScope scope,
            Long volumeId,
            Long sectionId,
            PracticeQuestionFilter filter,
            boolean randomOrder
    ) {
        if (scope == null) {
            throw new IllegalArgumentException("Practice scope is required");
        }
        if (volumeId == null) {
            throw new IllegalArgumentException("Volume is required");
        }
        if (scope == PracticeScope.SECTION && sectionId == null) {
            throw new IllegalArgumentException("Section is required for section practice");
        }

        if (scope == PracticeScope.SECTION) {
            sectionRepository.findById(sectionId)
                    .filter(section -> section.getVolume().getId().equals(volumeId))
                    .orElseThrow(() -> new IllegalArgumentException("Selected section does not belong to selected volume"));
        } else {
            volumeRepository.findById(volumeId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected volume does not exist"));
        }

        return new PracticeSessionSettings(
                scope,
                volumeId,
                scope == PracticeScope.SECTION ? sectionId : null,
                filter == null ? PracticeQuestionFilter.ALL_ACTIVE : filter,
                randomOrder
        );
    }

    private PracticeSessionSettings practiceSettings(HttpSession session, Question answeredQuestion) {
        Object value = session.getAttribute(PRACTICE_SETTINGS_SESSION_KEY);
        if (value instanceof PracticeSessionSettings settings) {
            return settings;
        }

        return PracticeSessionSettings.section(answeredQuestion.getSection().getId());
    }
}
