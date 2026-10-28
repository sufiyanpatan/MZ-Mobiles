package mz.mobile.bussiness.repository;

import mz.mobile.bussiness.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Mobile OTP

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = "roles")
    Optional<AppUser> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<AppUser> findByMobile(String mobile);

    @EntityGraph(attributePaths = "roles")
    @Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> findByIdWithRoles(@Param("id") Long id);

    boolean existsByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AppUser u set u.failedAttempts = u.failedAttempts + 1 where u.email = :email")
    int incrementFailedAttempts(@Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AppUser u set u.failedAttempts = 0, u.lockedUntil = null where u.email = :email")
    int resetFailedAttempts(@Param("email") String email);

    // now by ID, because mobile-only users have no email
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AppUser u set u.tokenVersion = u.tokenVersion + 1 where u.id = :id")
    int incrementTokenVersion(@Param("id") Long id);
}