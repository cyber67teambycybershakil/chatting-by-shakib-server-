# Chatting by Shakib — Android LAN Server Prototype

**This is a LAN-only development prototype, NOT safe for public internet or production.**

- Install debug APK on a spare Android phone, connect both phones to same Wi-Fi, open app, press Start server.
- App displays `http://PHONE_IP:8080`. Use an HTTP API client on the other phone.
- API requires JSON request bodies and Bearer token except login/register/health.
- Build from GitHub Actions > Build Android Server APK > Run workflow > download artifact.

## API

- `GET /api/health`
- `POST /api/auth/register` `{ "username":"alice", "password":"password123" }` returns `token`
- `POST /api/auth/login` same body returns `token`
- `POST /api/uploads` `{ "mime":"image/jpeg", "base64":"..." }` returns `fileId`; JPEG, PNG, WebP up to 1 MB.
- `POST /api/posts` `{ "text":"Hello", "fileId":"optional" }`
- `GET /api/posts`
- `POST /api/messages` `{ "receiverUsername":"bob", "text":"Hi", "fileId":"optional" }`
- `GET /api/messages/bob`
- `GET /api/files/FILE_ID` (Authorization required)

Authenticated calls require header `Authorization: Bearer TOKEN`.

Messages expire 24 hours after server insertion. Expired message records are deleted on message reads; media files are **not** automatically deleted in this prototype. Images can currently be read by any authenticated user who knows the file ID. No realtime push, image access ACL, TLS, reliable background service, rate limiting, failover, backups, message deduplication, or replication is implemented. Do not upload private data. Do not port-forward this server to the internet.
