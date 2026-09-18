-- D1 Database Schema for Cloudflare ps1_db (Domain: ahmed1986y.com)
-- Tables: users, sessions, private_messages, friendships, friend_requests, conversations

CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE,
    password_hash TEXT NOT NULL,
    avatar_url TEXT DEFAULT '',
    status TEXT DEFAULT 'offline',
    created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
    last_seen INTEGER DEFAULT (strftime('%s', 'now') * 1000),
    livekit_identity TEXT
);

CREATE TABLE IF NOT EXISTS sessions (
    token TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    expiry INTEGER NOT NULL,
    created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS private_messages (
    id TEXT PRIMARY KEY,
    conversation_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    receiver_id TEXT NOT NULL,
    type TEXT DEFAULT 'text',
    content TEXT NOT NULL,
    media_url TEXT DEFAULT '',
    file_name TEXT DEFAULT '',
    file_size INTEGER DEFAULT 0,
    duration INTEGER DEFAULT 0,
    location_lat REAL DEFAULT 0.0,
    location_lng REAL DEFAULT 0.0,
    created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
    is_read INTEGER DEFAULT 0,
    is_delivered INTEGER DEFAULT 1,
    FOREIGN KEY(sender_id) REFERENCES users(id),
    FOREIGN KEY(receiver_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS friendships (
    id TEXT PRIMARY KEY,
    user1_id TEXT NOT NULL,
    user2_id TEXT NOT NULL,
    created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
    is_blocked INTEGER DEFAULT 0,
    blocked_by TEXT DEFAULT '',
    mute_until INTEGER DEFAULT 0,
    mute_type TEXT DEFAULT 'none',
    FOREIGN KEY(user1_id) REFERENCES users(id),
    FOREIGN KEY(user2_id) REFERENCES users(id),
    UNIQUE(user1_id, user2_id)
);

CREATE TABLE IF NOT EXISTS friend_requests (
    id TEXT PRIMARY KEY,
    from_user_id TEXT NOT NULL,
    to_user_id TEXT NOT NULL,
    status TEXT DEFAULT 'pending',
    created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
    updated_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
    FOREIGN KEY(from_user_id) REFERENCES users(id),
    FOREIGN KEY(to_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS conversations (
    id TEXT PRIMARY KEY,
    user1_id TEXT NOT NULL,
    user2_id TEXT NOT NULL,
    last_message_id TEXT DEFAULT '',
    last_message_text TEXT DEFAULT '',
    last_message_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
    unread_count_user1 INTEGER DEFAULT 0,
    unread_count_user2 INTEGER DEFAULT 0,
    UNIQUE(user1_id, user2_id)
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_messages_conv ON private_messages(conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_friendships_users ON friendships(user1_id, user2_id);
CREATE INDEX IF NOT EXISTS idx_requests_to ON friend_requests(to_user_id, status);
CREATE INDEX IF NOT EXISTS idx_requests_from ON friend_requests(from_user_id, status);

-- Seed Default Admin Account (User: ahmed | Pass: 123456)
INSERT OR IGNORE INTO users (id, username, email, password_hash, avatar_url, status, created_at, last_seen, livekit_identity)
VALUES (
    'u_ahmed_1986',
    'ahmed',
    'ahmed1986y5@gmail.com',
    'b5e7d733d525d44fa4b43d6f554a8d30eb575d3df05b7b8b8446bee2249d711c',
    'https://api.ahmed1986y.com/media/avatars/ahmed.jpg',
    'online',
    1741478400000,
    1741478400000,
    'u_ahmed_1986'
);

