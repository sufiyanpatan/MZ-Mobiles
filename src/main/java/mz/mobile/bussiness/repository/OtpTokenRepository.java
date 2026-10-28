package mz.mobile.bussiness.repository;

import jakarta.transaction.Transactional;
import mz.mobile.bussiness.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByMobileOrderByIdDesc(String mobile);
    long countByMobileAndCreatedAtAfter(String mobile, LocalDateTime since);
    long countByCreatedAtAfter(LocalDateTime since);

    // Atomic: succeeds only while attempts remain, so parallel guesses can't exceed the limit.
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OtpToken t set t.attempts = t.attempts + 1 " +
            "where t.id = :id and t.used = false and t.attempts < :max")
    int tryConsumeAttempt(@Param("id") Long id, @Param("max") int max);

    // Atomic: an OTP can be used exactly once, even with two simultaneous requests.
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OtpToken t set t.used = true where t.id = :id and t.used = false")
    int markUsed(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("delete from OtpToken t where t.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
