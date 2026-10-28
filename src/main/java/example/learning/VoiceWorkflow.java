package example.learning;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

record VoiceNote(String eventId, String playerId, String assetKind, String queue, String transcript) {}
record VoiceQuery(String queue, String text) {}

@RestController
class VoiceWorkflow {
    private final VoiceIndexService index;

    VoiceWorkflow(VoiceIndexService index) { this.index = index; }

    @PostMapping("/setup")
    void setup() { index.prepare(); }

    @PostMapping("/notes")
    JsonNode add(@RequestBody VoiceNote note) {
        if (!VoiceRule.accept(note)) throw new IllegalArgumentException("A complete voice event is required");
        return index.index(note);
    }

    @PostMapping("/search")
    JsonNode search(@RequestBody VoiceQuery query) { return index.search(query); }

    @ExceptionHandler(VoiceIndexService.VoiceApiException.class)
    ResponseEntity<JsonNode> rejected(VoiceIndexService.VoiceApiException exception) {
        return ResponseEntity.status(exception.status >= 500 ? 502 : exception.status).body(exception.error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<String> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}

final class VoiceRule {
    private VoiceRule() {}

    static boolean accept(VoiceNote note) {
        return note != null && filled(note.eventId()) && filled(note.playerId())
                && ("voice_note".equals(note.assetKind()) || "call_recording".equals(note.assetKind()))
                && ("live".equals(note.queue()) || "moderation".equals(note.queue()))
                && filled(note.transcript());
    }

    private static boolean filled(String text) { return text != null && !text.isBlank(); }
}
