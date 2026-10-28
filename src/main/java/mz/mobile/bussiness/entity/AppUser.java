package mz.mobile.bussiness.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_users")
@Setter
@Getter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<AppRoles> roles = new HashSet<>();

    @Column(nullable = false)
    private int failedAttempts = 0;

    private LocalDateTime lockedUntil;

    @Column(nullable = false)
    private int tokenVersion = 0;

    // for mobile OTP adding code
    @Column(unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(unique = true, length = 15)        // stored as +91XXXXXXXXXX
    private String mobile;

    private boolean mobileVerified = false;

    private boolean enabled = true;
}