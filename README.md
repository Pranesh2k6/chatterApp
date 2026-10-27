# ChatterApp

A **one-to-one real-time chat application** built with Spring Boot, Thymeleaf, MySQL, and pure HTML/CSS.  
**Zero JavaScript.** Real-time updates use `<meta http-equiv="refresh">` inside `<iframe>` elements.

---

## Prerequisites

- **Java 17+**
- **Maven 3.8+**
- **MySQL 8**

## MySQL Setup

```sql
-- Connect as root (or a privileged user)
CREATE DATABASE IF NOT EXISTS chatter_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'chatter_user'@'localhost' IDENTIFIED BY 'changeme';
GRANT ALL PRIVILEGES ON chatter_db.* TO 'chatter_user'@'localhost';
FLUSH PRIVILEGES;
```

The schema (`users`, `conversations`, `messages` tables) is created automatically on startup via `schema.sql` using `CREATE TABLE IF NOT EXISTS`, so your data is preserved across restarts.

## Environment Variables

| Variable      | Default         | Description                  |
|---------------|-----------------|------------------------------|
| `DB_HOST`     | `localhost`     | MySQL host                   |
| `DB_PORT`     | `3306`          | MySQL port                   |
| `DB_NAME`     | `chatter_db`    | Database name                |
| `DB_USER`     | `chatter_user`  | Database username            |
| `DB_PASSWORD` | `changeme`      | Database password            |
| `SERVER_PORT` | `8080`          | Application port             |

## Run

```bash
# With defaults
mvn spring-boot:run

# Or override DB password
DB_PASSWORD=my_secret mvn spring-boot:run
```

Then open **http://localhost:8080** in your browser.

---

## How the No-JavaScript Refresh Works

Since there is **no JavaScript** anywhere in the project, real-time updates use a server-side approach:

1. **The chat page** has three parts:
   - A **sidebar** `<iframe>` → points to `/iframe/sidebar`
   - A **messages pane** `<iframe>` → points to `/iframe/messages/{convId}`
   - A **send form** → standard `<form method="post">`

2. Each iframe page includes a `<meta http-equiv="refresh" content="N">` tag:
   - **Sidebar** refreshes every **3 seconds** → picks up new conversations, unread counts, online status changes.
   - **Messages pane** refreshes every **2 seconds** → picks up new messages from the other user.

3. The send form **POSTs** to the server, which saves the message and redirects back to the chat page. No AJAX needed.

4. **Online status** is maintained by a heartbeat interceptor (updates `last_seen` on every request, throttled to once per 10 s) and a scheduled job that marks users offline if their `last_seen` is older than 30 seconds.

5. Sidebar links use `<base target="_top">` to navigate the parent window instead of the iframe.

6. The messages pane uses `display: flex; flex-direction: column-reverse; overflow-y: auto` so scroll starts at the bottom (newest messages visible).

---

## Notification Sound

The sidebar checks for un-notified messages on each refresh. If found, it renders a hidden `<audio autoplay>` element pointing to `/sounds/notify.mp3`. **Note:** Most browsers block autoplay audio by default until the user has interacted with the page. The sound file is not included in this repository — to enable it, place an `.mp3` file at:

```
src/main/resources/static/sounds/notify.mp3
```

---

## Features

- **Registration & Login** – BCrypt password hashing, Spring Security sessions, CSRF protection.
- **User Search** – Case-insensitive username search with status dots.
- **One-to-one Chat** – Private conversations with access control (403 for non-participants).
- **Message History** – Persistent in MySQL. "Load older messages" pagination in steps of 50.
- **Online/Offline Status** – Green/grey dots, heartbeat, scheduled cleanup, "Last seen" display.
- **Unread Badges** – Count and highlight in sidebar, cleared on opening the conversation.

---

## Manual Test List

- [ ] **Registration:** Register two accounts. Duplicate username/email is rejected with a clear message. Passwords in the DB are BCrypt hashes (start with `$2a$`).
- [ ] **Login/Logout:** Login keeps the session. Logout ends it. Visiting `/chat` after logout redirects to `/login`.
- [ ] **Search:** User A searches for User B. Results show status dot. Clicking "Chat" creates a conversation.
- [ ] **Messaging:** User A sends a message. User B sees it in ~2–3 seconds without manual reload. Messages show sender name and time.
- [ ] **Message sides:** My messages appear on the right (purple). Their messages on the left (dark).
- [ ] **Persistence:** Reopening a chat, logging out and back in, and restarting the server all preserve message history.
- [ ] **Load older:** With >50 messages, "Load older messages" link appears and works. Auto-refresh preserves the increased limit.
- [ ] **Online status:** Green dot when logged in. Goes grey within ~30 seconds after closing the tab or logging out. "Last seen" shows the correct time.
- [ ] **Unread badges:** Sidebar shows unread count. Conversation row is highlighted. Both clear after opening the chat.
- [ ] **No JavaScript:** `grep -r '<script' src/` and `find . -name '*.js'` return nothing.
- [ ] **No forbidden deps:** `pom.xml` contains only spring-boot-starter-web, -security, -jdbc, -validation, thymeleaf, thymeleaf-extras-springsecurity6, and mysql-connector-j.

---

## Tech Stack

| Layer       | Technology                               |
|-------------|------------------------------------------|
| Backend     | Java 17, Spring Boot 3.2, Spring MVC     |
| Security    | Spring Security (form login, BCrypt)      |
| Database    | MySQL 8 with JdbcTemplate (no ORM)       |
| Templates   | Thymeleaf                                |
| Styling     | Hand-written CSS (dark theme)            |
| Real-time   | `<meta http-equiv="refresh">` in iframes |
