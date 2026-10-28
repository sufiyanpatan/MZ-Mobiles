package mz.mobile.bussiness.service;

import mz.mobile.bussiness.dto.AuthResponseDTO;
import mz.mobile.bussiness.dto.OtpSentResponseDTO;
import mz.mobile.bussiness.entity.AppRoles;
import mz.mobile.bussiness.entity.AppUser;
import mz.mobile.bussiness.repository.AppUserRepository;
import mz.mobile.bussiness.repository.RoleRepository;
import mz.mobile.bussiness.security.AuthUser;
import mz.mobile.bussiness.security.SlidingWindowRateLimiter;
import mz.mobile.bussiness.utils.MobileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Set;

@Service
public class MobileAuthService {

    private static final Set<String> BLOCKED_ROLES = Set.of("ADMIN", "STAFF");

    private final AppUserRepository users;
    private final RoleRepository roles;
    private final OtpService otpService;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    private final SlidingWindowRateLimiter sendByIp;
    private final SlidingWindowRateLimiter verifyByIp;

    public MobileAuthService(AppUserRepository users, RoleRepository roles, OtpService otpService,
                             CustomUserDetailsService userDetailsService, JwtService jwtService,
                             @Value("${app.otp.ip-send-limit}") int ipSendLimit,
                             @Value("${app.otp.ip-verify-limit}") int ipVerifyLimit) {
        this.users = users;
        this.roles = roles;
        this.otpService = otpService;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.sendByIp = new SlidingWindowRateLimiter(ipSendLimit, Duration.ofHours(1).toMillis());
        this.verifyByIp = new SlidingWindowRateLimiter(ipVerifyLimit, Duration.ofMinutes(15).toMillis());
    }

    public OtpSentResponseDTO requestOtp(String rawMobile, String ip) {
        if (!sendByIp.tryAcquire(ip)) throw tooMany();
        String mobile = MobileUtils.normalize(rawMobile);

        // Admin/staff/disabled accounts never receive an OTP, but the reply looks identical.
        boolean allowed = users.findByMobile(mobile).map(this::canUseOtpLogin).orElse(true);
        if (allowed) otpService.sendNew(mobile);

        return new OtpSentResponseDTO("If the number is valid, an OTP has been sent.",
                otpService.getCooldownSeconds(), otpService.getExpirySeconds());
    }

    public AuthResponseDTO verifyOtp(String rawMobile, String code, String ip) {
        if (!verifyByIp.tryAcquire(ip)) throw tooMany();
        String mobile = MobileUtils.normalize(rawMobile);

        if (!otpService.verify(mobile, code)) throw invalid();

        AppUser user = findOrCreate(mobile);
        if (!canUseOtpLogin(user)) throw invalid();       // second line of defence for admin/staff

        AuthUser authUser = userDetailsService.toAuthUser(user);
        return new AuthResponseDTO(jwtService.generateToken(authUser), "Bearer",
                jwtService.getExpirationMs(), authUser.getEmail(), authUser.getMobile(),
                authUser.getRoleNames());
    }

    private boolean canUseOtpLogin(AppUser u) {
        return u.isEnabled() && u.getRoles().stream().noneMatch(r -> BLOCKED_ROLES.contains(r.getRole()));
    }

    // First successful OTP creates the account, a plain USER with no email and no password.
    private AppUser findOrCreate(String mobile) {
        return users.findByMobile(mobile).orElseGet(() -> {
            AppRoles role = roles.findByRole("USER")
                    .orElseThrow(() -> new IllegalStateException("Default USER role not found"));
            AppUser u = new AppUser();
            u.setMobile(mobile);
            u.setMobileVerified(true);
            u.setEnabled(true);
            u.getRoles().add(role);
            try {
                return users.saveAndFlush(u);
            } catch (DataIntegrityViolationException e) {      // two first-logins at once
                return users.findByMobile(mobile)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Please try again"));
            }
        });
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
    }

    private ResponseStatusException tooMany() {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.");
    }

    @Scheduled(fixedDelay = 600_000)
    public void cleanupLimiters() {
        sendByIp.cleanup();
        verifyByIp.cleanup();
    }
}
