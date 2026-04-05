package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.roles.Role;
import io.mpruy.gor_gemilangcondet.backend_api.entities.roles.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatus;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ForbiddenException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.NotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks UserService userService;
    @Mock UserRepository userRepository;
    @Mock UserStatusRepository userStatusRepository;
    @Mock UserMapper userMapper;

    private Role adminRole() {
        return Role.builder().id(1L).name(RoleName.ROLE_ADMIN).build();
    }

    private UserStatus pendingStatus() {
        return UserStatus.builder().id(1L).name(UserStatusName.PENDING).build();
    }

    private UserStatus aktifStatus() {
        return UserStatus.builder().id(2L).name(UserStatusName.AKTIF).build();
    }

    private User pendingAdminUser(String id) {
        return User.builder().id(id).username("user-" + id).email(id + "@test.com")
                .password("pw").role(adminRole()).status(pendingStatus()).build();
    }

    @Test
    void listPendingAdminUsers_shouldReturnCombinedList() {
        var u1 = pendingAdminUser("1");
        var u2 = pendingAdminUser("2");
        when(userRepository.findByStatus_NameAndRole_Name(UserStatusName.PENDING, RoleName.ROLE_ADMIN))
                .thenReturn(List.of(u1));
        when(userRepository.findByStatus_NameAndRole_Name(UserStatusName.PENDING, RoleName.ROLE_PEGAWAI))
                .thenReturn(List.of(u2));
        when(userMapper.toUserDto(any())).thenReturn(new UserDto());

        List<UserDto> result = userService.listPendingAdminUsers();

        assertThat(result).hasSize(2);
    }

    @Test
    void approveUser_shouldChangeStatusToAktif() {
        var user = pendingAdminUser("1");
        when(userRepository.findById("1")).thenReturn(Optional.of(user));
        when(userStatusRepository.findByName(UserStatusName.AKTIF)).thenReturn(Optional.of(aktifStatus()));
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toUserDto(any())).thenReturn(new UserDto());

        userService.approveUser("1");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus().getName()).isEqualTo(UserStatusName.AKTIF);
    }

    @Test
    void rejectUser_shouldDeleteUser() {
        var user = pendingAdminUser("1");
        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        userService.rejectUser("1");

        verify(userRepository).delete(user);
    }

    @Test
    void approveUser_notFound_shouldThrowNotFoundException() {
        when(userRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.approveUser("999"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void approveUser_notPending_shouldThrowForbiddenException() {
        var user = User.builder().id("1").username("u").email("u@test.com")
                .password("pw").role(adminRole()).status(aktifStatus()).build();
        when(userRepository.findById("1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.approveUser("1"))
                .isInstanceOf(ForbiddenException.class);
    }
}
