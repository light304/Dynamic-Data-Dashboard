-- ============================================================
--  STANDALONE LOGIN DATABASE
--  Database : login.db   (separate from retail_dashboard.db)
--  File     : login_database.sql
--  Roles    : Admin | Manager | Viewer
-- ============================================================

-- ── 1. ROLES TABLE ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS roles (
    role_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    role_name   TEXT    NOT NULL UNIQUE,
    description TEXT
);

INSERT OR IGNORE INTO roles (role_name, description) VALUES
    ('Admin',   'Full access: manage users, view all data, edit settings'),
    ('Manager', 'View all dashboard data and reports; cannot manage users'),
    ('Viewer',  'Read-only access to dashboard; no admin functions');


-- ── 2. USERS TABLE ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    role_id       INTEGER NOT NULL REFERENCES roles(role_id),
    full_name     TEXT,
    email         TEXT    UNIQUE,
    is_active     INTEGER NOT NULL DEFAULT 1,     -- 1 = active, 0 = disabled
    created_at    TEXT    NOT NULL DEFAULT (datetime('now')),
    last_login    TEXT
);

-- Default accounts (replace password_hash values before going live)
INSERT OR IGNORE INTO users (username, password_hash, role_id, full_name, email) VALUES
    ('admin',
     'REPLACE_WITH_HASH_OF_YOUR_ADMIN_PASSWORD',
     (SELECT role_id FROM roles WHERE role_name = 'Admin'),
     'System Administrator',
     'admin@dashboard.local'),

    ('manager',
     'REPLACE_WITH_HASH_OF_YOUR_MANAGER_PASSWORD',
     (SELECT role_id FROM roles WHERE role_name = 'Manager'),
     'Dashboard Manager',
     'manager@dashboard.local'),

    ('viewer',
     'REPLACE_WITH_HASH_OF_YOUR_VIEWER_PASSWORD',
     (SELECT role_id FROM roles WHERE role_name = 'Viewer'),
     'Dashboard Viewer',
     'viewer@dashboard.local');


-- ── 3. LOGIN AUDIT LOG ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS login_audit (
    audit_id    INTEGER PRIMARY KEY AUTOINCREMENT,
    username    TEXT    NOT NULL,
    attempt_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    success     INTEGER NOT NULL,                 -- 1 = success, 0 = failed
    notes       TEXT
);


-- ── 4. PERMISSIONS VIEW ────────────────────────────────────
CREATE VIEW IF NOT EXISTS v_user_permissions AS
SELECT
    u.user_id,
    u.username,
    u.full_name,
    u.email,
    u.is_active,
    r.role_name,
    r.description AS role_description,
    u.last_login,
    u.created_at
FROM users u
JOIN roles r ON u.role_id = r.role_id;


-- ── 5. SANITY CHECK ────────────────────────────────────────
-- Run this after loading to confirm it worked:
--
-- SELECT 'roles' t, COUNT(*) n FROM roles UNION ALL
-- SELECT 'users',  COUNT(*)   FROM users;
--
-- Expected:
--   roles|3
--   users|3
-- ============================================================
