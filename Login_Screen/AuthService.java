import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AuthService
 *
 * Handles:
 *   • login.db initialisation (creates schema + default users on first run)
 *   • credential verification against the users table
 *   • login_audit logging (success and failure)
 *   • last_login timestamp update
 *   • brute-force lock-out (MAX_ATTEMPTS per session)
 *
 * Database file: login_database.sql
 * Default credentials created at first run:
 *   admin   / Admin@123
 *   manager / Manager@123
 *   viewer  / Viewer@123
 */
public class AuthService {

    // Configuration 
    private static final String DB_URL      = "jdbc:sqlite:login.db";
    private static final int    MAX_ATTEMPTS = 5;
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int failedAttempts = 0;

    // Database connection
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found on classpath.", e);
        }
        return DriverManager.getConnection(DB_URL);
    }

    // Schema initialisation 

    /**
     * Creates all tables, inserts default roles and users if they don't
     * already exist.  Safe to call every time the app starts.
     *
     * @return true on success, false if a DB error occurred
     */
    public boolean initializeDatabase() {
        try (Connection conn = getConnection();
             Statement  stmt = conn.createStatement()) {

            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA foreign_keys = ON;");

            // 1 roles
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS roles (
                    role_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    role_name   TEXT    NOT NULL UNIQUE,
                    description TEXT
                )""");

            // 2 users
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT    NOT NULL UNIQUE,
                    password_hash TEXT    NOT NULL,
                    role_id       INTEGER NOT NULL REFERENCES roles(role_id),
                    full_name     TEXT,
                    email         TEXT    UNIQUE,
                    is_active     INTEGER NOT NULL DEFAULT 1,
                    created_at    TEXT    NOT NULL DEFAULT (datetime('now')),
                    last_login    TEXT
                )""");

            // 3 audit log
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS login_audit (
                    audit_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    username   TEXT    NOT NULL,
                    attempt_at TEXT    NOT NULL DEFAULT (datetime('now')),
                    success    INTEGER NOT NULL,
                    notes      TEXT
                )""");

            // 4 permissions view
            stmt.execute("""
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
                JOIN roles r ON u.role_id = r.role_id""");

            // 5 seed roles
            stmt.execute("""
                INSERT OR IGNORE INTO roles (role_name, description) VALUES
                    ('Admin',   'Full access: manage users, view all data, edit settings'),
                    ('Manager', 'View all dashboard data and reports; cannot manage users'),
                    ('Viewer',  'Read-only access to dashboard; no admin functions')""");

            // 6 – seed default users (hashes computed at runtime)
            insertDefaultUserIfAbsent(conn, "admin",
                    PasswordUtil.sha256("Admin@123"),
                    "Admin", "System Administrator", "admin@dashboard.local");

            insertDefaultUserIfAbsent(conn, "manager",
                    PasswordUtil.sha256("Manager@123"),
                    "Manager", "Dashboard Manager", "manager@dashboard.local");

            insertDefaultUserIfAbsent(conn, "viewer",
                    PasswordUtil.sha256("Viewer@123"),
                    "Viewer", "Dashboard Viewer", "viewer@dashboard.local");

            return true;

        } catch (SQLException e) {
            System.err.println("[AuthService] DB init failed: " + e.getMessage());
            return false;
        }
    }

    private void insertDefaultUserIfAbsent(Connection conn,
            String username, String hash, String roleName,
            String fullName, String email) throws SQLException {

        String sql = """
            INSERT OR IGNORE INTO users
                (username, password_hash, role_id, full_name, email)
            VALUES (?, ?,
                (SELECT role_id FROM roles WHERE role_name = ?),
                ?, ?)""";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, roleName);
            ps.setString(4, fullName);
            ps.setString(5, email);
            ps.executeUpdate();
        }
    }

    // Authentication 
    /**
     * Validates credentials, records the audit trail, and populates
     * {@link UserSession} on success.
     *
     * @param username plaintext username
     * @param password plaintext password (hashed internally)
     * @return {@link AuthResult} describing outcome
     */
    public AuthResult authenticate(String username, String password) {

        // Brute-force guard
        if (failedAttempts >= MAX_ATTEMPTS) {
            return fail(null, "Account locked – too many failed attempts. "
                    + "Restart the application to try again.", null);
        }

        String hash = PasswordUtil.sha256(password);
        String now  = LocalDateTime.now().format(DT_FMT);

        try (Connection conn = getConnection()) {

            // look up active user with matching hash 
            String sql = """
                SELECT u.user_id, u.username, u.full_name, u.email,
                       u.is_active, r.role_name
                FROM   users u
                JOIN   roles r ON u.role_id = r.role_id
                WHERE  u.username = ? AND u.password_hash = ?""";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username.trim());
                ps.setString(2, hash);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        // check active flag
                        if (rs.getInt("is_active") == 0) {
                            logAttempt(conn, username, false,
                                    "Disabled account", now);
                            return fail(username,
                                    "Your account has been disabled. "
                                    + "Contact an administrator.", conn);
                        }

                        // Success – populate session
                        UserSession session = UserSession.getInstance();
                        session.login(
                                rs.getInt("user_id"),
                                rs.getString("username"),
                                rs.getString("full_name"),
                                rs.getString("email"),
                                rs.getString("role_name"));

                        updateLastLogin(conn, username.trim(), now);
                        logAttempt(conn, username, true, "Login successful", now);
                        failedAttempts = 0;
                        return new AuthResult(true, session, null);
                    }
                }
            }

            // no matching row: wrong password or unknown username 
            failedAttempts++;
            int remaining = MAX_ATTEMPTS - failedAttempts;
            String lockMsg = remaining > 0
                    ? remaining + " attempt(s) remaining before lockout."
                    : "Account locked. Restart the application to try again.";

            // distinguish wrong-password from unknown username for the audit note
            if (usernameExists(conn, username.trim())) {
                logAttempt(conn, username, false, "Wrong password", now);
                return fail(username, "Incorrect password.  " + lockMsg, conn);
            } else {
                logAttempt(conn, username, false, "Unknown username", now);
                return fail(username, "Username not found.  " + lockMsg, conn);
            }

        } catch (SQLException e) {
            return fail(username, "Database error: " + e.getMessage(), null);
        }
    }

    // Helpers 

    private boolean usernameExists(Connection conn, String username)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void updateLastLogin(Connection conn, String username, String now)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET last_login = ? WHERE username = ?")) {
            ps.setString(1, now);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    private void logAttempt(Connection conn, String username,
            boolean success, String notes, String now) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO login_audit (username, attempt_at, success, notes) "
                + "VALUES (?, ?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, now);
            ps.setInt(3, success ? 1 : 0);
            ps.setString(4, notes);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuthService] Audit-log error: " + e.getMessage());
        }
    }

    private AuthResult fail(String username, String msg, Connection conn) {
        return new AuthResult(false, null, msg);
    }

    // Inner result class

    public static final class AuthResult {
        public final boolean     success;
        public final UserSession session;
        public final String      errorMessage;

        AuthResult(boolean success, UserSession session, String errorMessage) {
            this.success      = success;
            this.session      = session;
            this.errorMessage = errorMessage;
        }
    }
}
