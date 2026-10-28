package mz.mobile.bussiness.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

//public class AuthUser extends User {
//
//    private final int tokenVersion;
//
//    public AuthUser(String username, String password, boolean enabled, boolean accountNonLocked,
//                    Collection<? extends GrantedAuthority> authorities, int tokenVersion) {
//        super(username, password, enabled, true, true, accountNonLocked, authorities);
//        this.tokenVersion = tokenVersion;
//    }
//
//    public int getTokenVersion() { return tokenVersion; }
//
//    /** ["ADMIN"], ["USER"] ... (without the ROLE_ prefix), for the frontend. */
//    public List<String> getRoleNames() {
//        return getAuthorities().stream()
//                .map(GrantedAuthority::getAuthority)
//                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
//                .toList();
//    }
//}

// Mobile OTP

public class AuthUser extends User {

    private final Long id;
    private final String email;
    private final String mobile;
    private final int tokenVersion;

    public AuthUser(Long id, String email, String mobile, String passwordHash,
                    boolean enabled, boolean accountNonLocked,
                    Collection<? extends GrantedAuthority> authorities, int tokenVersion) {
        super(email != null ? email : mobile,                 // Spring needs a non-empty username
                passwordHash != null ? passwordHash : "",       // and a non-null password
                enabled, true, true, accountNonLocked, authorities);
        this.id = id;
        this.email = email;
        this.mobile = mobile;
        this.tokenVersion = tokenVersion;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public int getTokenVersion() { return tokenVersion; }

    public List<String> getRoleNames() {
        return getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .toList();
    }
}
