package example.learning;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class VoiceRuleTest {
    @Test
    void onlyIndexIdentifiedAudioEventsWithTranscripts() {
        assertTrue(VoiceRule.accept(new VoiceNote("match-28:voice-4", "player-7", "voice_note", "moderation", "Help at the gate")));
        assertFalse(VoiceRule.accept(new VoiceNote("match-28:voice-4", "player-7", "screenshot", "moderation", "Help at the gate")));
        assertFalse(VoiceRule.accept(new VoiceNote("match-28:voice-4", "player-7", "voice_note", "moderation", " ")));
    }
}
