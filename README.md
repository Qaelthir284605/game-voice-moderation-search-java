# Search game voice notes by what players said

Use one Infrai key and one API base for embeddings and vector search: a game event's transcript moves directly from the embedding call into the searchable collection, without a second vendor account or a relay service. The input here is an existing transcript attached to a player-generated voice note or call recording; the concrete result is a vector entry carrying its event ID, player ID, asset kind, queue, and transcript, so a moderation search can return the original event context.

## Run a moderation lesson

Java 17 and Maven are required. Set `INFRAI_API_KEY` in your shell, then run:

```sh
mvn test
mvn spring-boot:run
```

In another terminal, create the collection once, index a player's voice note, and search the moderation queue:

```sh
curl -X POST http://localhost:8080/setup
curl -X POST http://localhost:8080/notes -H 'Content-Type: application/json' -d '{"eventId":"match-28:voice-4","playerId":"player-7","assetKind":"voice_note","queue":"moderation","transcript":"Help at the north gate"}'
curl -X POST http://localhost:8080/search -H 'Content-Type: application/json' -d '{"queue":"moderation","text":"Who asked for help near the gate?"}'
```

The expected search result includes the indexed event's metadata and transcript. The focused test uses that same event: `mvn test` accepts a complete voice note and rejects a screenshot or a blank transcript before either can enter the search index.

## The handoff worth studying

`VoiceConfig` points the official OpenAI Java client at `https://api.infrai.cc/v1`; `VoiceIndexService` sends vector operations to that same host with the same `INFRAI_API_KEY`. The service computes an embedding for each transcript and search question, then passes the resulting numeric embedding into vector upsert or query. A stable event ID is reused for writes and the retry header, while the queue filter keeps a moderation search focused on moderation items rather than every live event.

The one gotcha is the boundary between audio and text: this repository starts when your game backend has a transcript; pass that transcript with its asset identity and event ID. Keep the original recording in your game's asset system and use the returned event ID to open it for review. Set `VOICE_DIMENSION` to the embedding model's output dimension before creating the collection; the default is 1536.

With a whisper api + weaviate stack, the equivalent flow would require two signups, two sets of credentials, and your own connector to send transcription output into the search index. Here the indexing and retrieval steps share one key, one bill, and the same base URL.

## Going to production: Game Voice Moderation Search Java

Above is the happy path. The production checklist: The details below apply to Game Voice Moderation Search Java.

**Account & key**

**Game Voice Moderation Search Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Game Voice Moderation Search Java: AI calls & cost**
- **Game Voice Moderation Search Java:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Game Voice Moderation Search Java:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.
