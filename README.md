# Kven II Android

Minimal Android transport for the existing Kven II relationship.

## Connection

The debug client uses the public HTTPS gateway:

```properties
kven.baseUrl=https://kven-android.kuris.kiev.ua
```

`local.properties` is ignored by Git. No Android username or password is compiled into the APK.

At first use, the user enters the issued Kven username and password in the app. The app stores them in its private preferences and sends them to the HTTPS gateway with HTTP Basic authentication.

The client uses the existing OpenAI-style gateway endpoints:

- `GET /v1/models`
- `POST /v1/chat/completions`

The Android app never supplies an email, canonical person ID, or caller-selected Kven provenance. The server resolves the authenticated Android username through the same small owner-managed user registry that maps accepted Open WebUI email identities and Telegram numeric identities to one canonical Kven person.
