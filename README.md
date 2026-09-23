# Kven II Android

Minimal Android transport for the existing Kven II relationship.

## Debug provisioning

Keep device credentials outside Git. Add these local-only values to `local.properties`:

```properties
kven.baseUrl=https://kven-android.kuris.kiev.ua
kven.apiKey=<native-client-token>
```

`local.properties` is ignored by Git. Release builds do not receive the debug gateway URL or credential. The Android client reaches Kven through the public HTTPS reverse-proxy endpoint; it does not address the private VM192 interface directly.

The client uses the existing OpenAI-style gateway endpoints:

- `GET /v1/models`
- `POST /v1/chat/completions`

The Android app never supplies an email, canonical person ID, or caller-selected Kven provenance. Identity is established server-side from the native client credential and the owner-managed transport-principal registry.
