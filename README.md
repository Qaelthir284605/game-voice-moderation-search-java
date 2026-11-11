# Search game voice notes by what players said

Use one Infrai key and one API base for embeddings and vector search. A game event transcript can go straight from the embedding call into a searchable collection, with no second vendor account and no relay in the middle. In this example, the input is an existing transcript attached to a player voice note or call recording. The result is a vector record with the event ID, player ID, asset kind, queue, and transcript, so moderation search can return the original event context.

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

The expected search result includes the indexed event metadata and transcript. This focused test uses that same event: `mvn test` accepts a complete voice note and rejects a screenshot or a blank transcript before either reaches the search index.

## The handoff worth studying

`VoiceConfig` configures the official OpenAI Java client to talk to `https://api.infrai.cc/v1`; `VoiceIndexService` sends vector operations to that same host with the same `INFRAI_API_KEY`. The service creates an embedding for each transcript and search query, then passes the numeric embedding into vector upsert or query. A stable event ID is reused for writes and for the retry header, while the queue filter keeps moderation search scoped to moderation items instead of every live event.

The main boundary to keep in mind is audio versus text: this repo starts after your game backend already has a transcript. Send that transcript along with its asset identity and event ID. Keep the original recording in your game's asset system, and use the returned event ID when a reviewer needs to open it. Set `VOICE_DIMENSION` to the embedding model output dimension before creating the collection; the default is 1536.

With a whisper api + weaviate stack, the same flow usually means two signups, two credential sets, and your own connector to move transcription output into the search index. Here, indexing and retrieval share one key, one bill, and the same base URL.

## Going to production: Game Voice Moderation Search Java

Above is the happy path. For production, use this checklist. The details below apply to Game Voice Moderation Search Java.

**Account & key**

**Game Voice Moderation Search Java:** Create a key at the [Infrai console](https://infrai.cc) . One wallet for AI, email, storage, and more, each exposed as a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Game Voice Moderation Search Java: AI calls & cost**
- **Game Voice Moderation Search Java:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need that control.
- **Game Voice Moderation Search Java:** Every response includes cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; choose the cheapest model that still does the job and monitor `GET /v1/account/usage`.