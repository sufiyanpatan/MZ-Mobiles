package mz.mobile.bussiness.repository;

import mz.mobile.bussiness.entity.AppRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<AppRoles, Long> {

    Optional<AppRoles> findByRole(String user);
}
