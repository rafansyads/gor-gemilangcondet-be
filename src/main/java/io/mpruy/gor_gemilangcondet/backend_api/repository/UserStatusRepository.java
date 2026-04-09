package io.mpruy.gor_gemilangcondet.backend_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Integer> {

    Optional<UserStatus> findByName(UserStatusName name);
}
