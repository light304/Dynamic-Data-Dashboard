/**
 * UserSession  –  Singleton holding the currently authenticated user.
 *
 * After a successful login AuthService calls session.login(...).
 * Every other part of the application can query it with
 * UserSession.getInstance() to find out who is logged in and their role.
 */
public class UserSession {

    // Singleton 
    private static UserSession instance;

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    private UserSession() {}

    // Fields 
    private int    userId;
    private String username;
    private String fullName;
    private String email;
    private String roleName;       // "Admin" | "Manager" | "Viewer"

    // Lifecycle 

    /** Called by AuthService on successful authentication. */
    public void login(int userId, String username, String fullName,
                      String email, String roleName) {
        this.userId   = userId;
        this.username = username;
        this.fullName = fullName;
        this.email    = email;
        this.roleName = roleName;
    }

    /** Clears all session data (call on logout / app close). */
    public void logout() {
        userId   = 0;
        username = null;
        fullName = null;
        email    = null;
        roleName = null;
    }

    // Getters 
    public int    getUserId()   { return userId;   }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail()    { return email;    }
    public String getRoleName() { return roleName; }

    /** @return true if a user has successfully logged in this session. */
    public boolean isLoggedIn() { return username != null; }

    // Role helpers 
    public boolean isAdmin()   { return "Admin".equalsIgnoreCase(roleName);   }
    public boolean isManager() { return "Manager".equalsIgnoreCase(roleName); }
    public boolean isViewer()  { return "Viewer".equalsIgnoreCase(roleName);  }

    @Override
    public String toString() {
        return "UserSession{username='" + username + "', role='" + roleName + "'}";
    }
}
