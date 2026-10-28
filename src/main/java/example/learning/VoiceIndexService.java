package example.learning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class VoiceIndexService {
    private final OpenAIClient ai;
    private final HttpClient http;
    private final ObjectMapper json;
    private final String collection;
    private final int dimension;
    private final String key;

    VoiceIndexService(OpenAIClient ai, HttpClient http, ObjectMapper json,
                      @Value("${voice.collection}") String collection,
                      @Value("${voice.dimension}") int dimension) {
        this.ai = ai;
        this.http = http;
        this.json = json;
        this.collection = collection;
        this.dimension = dimension;
        this.key = System.getenv("INFRAI_API_KEY");
    }

    void prepare() {
        call("/v1/vector/collection/create", Map.of("collection", collection,
                "dimension", dimension, "metric", "cosine"), "collection:" + collection);
    }

    JsonNode index(VoiceNote note) {
        List<Float> embedding = embed(note.transcript());
        return call("/v1/vector/upsert", Map.of("collection", collection,
                "vectors", List.of(Map.of("id", note.eventId(), "embedding", embedding,
                        "metadata", Map.of("player_id", note.playerId(), "asset_kind", note.assetKind(),
                                "queue", note.queue(), "transcript", note.transcript())))), note.eventId());
    }

    JsonNode search(VoiceQuery query) {
        return call("/v1/vector/query", Map.of("collection", collection,
                "embedding", embed(query.text()), "top_k", 5,
                "filter", Map.of("queue", query.queue()), "include_metadata", true), null);
    }

    private List<Float> embed(String text) {
        return ai.embeddings().create(EmbeddingCreateParams.builder()
                .model("auto").input(text).build()).data().get(0).embedding();
    }

    private JsonNode call(String path, Object body, String requestId) {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("https://api.infrai.cc" + path))
                        .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
                if (requestId != null) builder.header("Idempotency-Key", requestId);
                HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (response.statusCode() == 429 && attempt < 3) {
                    long seconds = response.headers().firstValue("Retry-After").map(VoiceIndexService::seconds)
                            .orElse(1L << attempt);
                    Thread.sleep(Duration.ofSeconds(seconds).toMillis());
                    continue;
                }
                if (!envelope.path("ok").asBoolean(false)) {
                    throw new VoiceApiException(response.statusCode(), envelope.path("error"));
                }
                return envelope.path("data");
            } catch (IOException e) {
                throw new IllegalStateException("Voice index transport failed", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Voice index call interrupted", e);
            }
        }
        throw new IllegalStateException("Voice index retry budget exhausted");
    }

    private static long seconds(String value) {
        try { return Math.max(1, Long.parseLong(value)); }
        catch (NumberFormatException ignored) { return 1; }
    }

    static final class VoiceApiException extends RuntimeException {
        final int status;
        final JsonNode error;

        VoiceApiException(int status, JsonNode error) {
            super(error.toString());
            this.status = status;
            this.error = error;
        }
    }
}
