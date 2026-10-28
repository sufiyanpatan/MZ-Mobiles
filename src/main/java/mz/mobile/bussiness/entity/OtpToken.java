package mz.mobile.bussiness.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens", indexes = {
        @Index(name = "idx_otp_mobile_created", columnList = "mobile, created_at"),
        @Index(name = "idx_otp_created", columnList = "created_at")})
@Getter
@Setter
@NoArgsConstructor
public class OtpToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 15)
    private String mobile;

    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}

