# Al-Mahalla - Cloudflare Worker API & Real-time Backend

This worker runs on Cloudflare Workers and provides:
1. **D1 SQL Database** for Users, Friend Requests, Friendships, Messages, and Conversations.
2. **KV Namespace** for Sessions, Presence, and Typing indicators.
3. **R2 Bucket** for Media (images, videos, files, and voice notes).
4. **Durable Objects / WebSockets** for real-time messaging and chat events.
5. **LiveKit Token Generation** on the edge (no API secret inside APK).

## Deployment Instructions

### 1. Install Wrangler
```bash
npm install -g wrangler
```

### 2. Login to Cloudflare
```bash
wrangler login
```

### 3. Create D1 Database & Initialize Tables
```bash
# Create D1 database
wrangler d1 create ps1-d1

# Run the schema migration
wrangler d1 execute ps1-d1 --file=./schema.sql
```
*(Copy the generated `database_id` into `wrangler.toml`)*

### 4. Create R2 Bucket
```bash
wrangler r2 bucket create ps1-media
```

### 5. Set Worker Secrets (LiveKit & JWT)
```bash
wrangler secret put JWT_SECRET
# Enter a secure random string

wrangler secret put LIVEKIT_API_KEY
# Enter your LiveKit Cloud API key

wrangler secret put LIVEKIT_API_SECRET
# Enter your LiveKit Cloud Secret key
```

### 6. Deploy to Cloudflare
```bash
wrangler deploy
```

The worker will be live at `https://api.ps1-netplay.workers.dev` or your assigned custom domain!
