package io.mpruy.gor_gemilangcondet.backend_api.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    List<User> findByStatus_NameAndRole_RoleNameIn(UserStatusName statusName, Collection<RoleName> roleNames);

    List<User> findByStatus_NameAndBannedAtLessThanEqual(UserStatusName statusName, LocalDateTime threshold);

    Optional<User> findByIdAndStatus_NameAndRole_RoleNameIn(UUID id, UserStatusName statusName,
            Collection<RoleName> roleNames);

    Optional<User> findByIdAndStatus_NameInAndRole_RoleNameIn(UUID id, Collection<UserStatusName> statusNames,
            Collection<RoleName> roleNames);
}