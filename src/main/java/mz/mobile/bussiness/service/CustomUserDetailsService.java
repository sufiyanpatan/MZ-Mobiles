package mz.mobile.bussiness.service;


import mz.mobile.bussiness.entity.AppUser;
import mz.mobile.bussiness.repository.AppUserRepository;
import mz.mobile.bussiness.security.AuthUser;
import mz.mobile.bussiness.utils.EmailUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public CustomUserDetailsService(
            AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String email) {
        AppUser appUser = appUserRepository.findByEmail(EmailUtils.normalize(email))
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Invalid email or password"));

        boolean locked = appUser.getLockedUntil() != null && appUser.getLockedUntil().isAfter(LocalDateTime.now());

        List<SimpleGrantedAuthority> authorities = appUser.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole()))
                .toList();

//        return User.withUsername(appUser.getEmail())
//                .password(appUser.getPassword())
//                .authorities(authorities)
//                .disabled(!appUser.isEnabled())
//                .accountLocked(locked)
//                .build();

        return new AuthUser(appUser.getEmail(), appUser.getPassword(), appUser.isEnabled(), !locked, authorities, appUser.getTokenVersion());
    }
}
