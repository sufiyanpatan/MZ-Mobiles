package mz.mobile.bussiness.service;

import mz.mobile.bussiness.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    private final AppUserRepository userRepository;

    @Value("${app.security.max-login-attempts}")
    private int maxAttempts;

    @Value("${app.security.lock-minutes}")
    private long lockMinutes;

    public LoginAttemptService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String email) {
        userRepository.incrementFailedAttempts(email);

        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getFailedAttempts() >= maxAttempts) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                user.setFailedAttempts(0);   // fresh 5 attempts after the lock expires
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String email) {
        userRepository.resetFailedAttempts(email);
    }
}
