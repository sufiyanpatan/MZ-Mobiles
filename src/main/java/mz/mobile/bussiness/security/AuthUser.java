package mz.mobile.bussiness.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;

public class AuthUser extends User {

    private final int tokenVersion;

    public AuthUser(String username, String password, boolean enabled, boolean accountNonLocked,
                    Collection<? extends GrantedAuthority> authorities, int tokenVersion) {
        super(username, password, enabled, true, true, accountNonLocked, authorities);
        this.tokenVersion = tokenVersion;
    }

    public int getTokenVersion() { return tokenVersion; }

    /** ["ADMIN"], ["USER"] ... (without the ROLE_ prefix), for the frontend. */
    public List<String> getRoleNames() {
        return getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .toList();
    }
}
