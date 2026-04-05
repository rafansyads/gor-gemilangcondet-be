package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.*;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserStatusRepository userStatusRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private Role adminRole;
    private UserStatus pendingStatus;
    private UserStatus aktifStatus;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder().id(1L).name(RoleName.ROLE_ADMIN).build();
        pendingStatus = UserStatus.builder().id(1L).name(UserStatusName.PENDING).build();
        aktifStatus = UserStatus.builder().id(2L).name(UserStatusName.AKTIF).build();
    }

    @Test
    void listPendingAdmins_shouldReturnAllPendingUsers() {
        User pendingUser1 = User.builder().id(1L).username("admin1").role(adminRole).status(pendingStatus).build();
        User pendingUser2 = User.builder().id(2L).username("admin2").role(adminRole).status(pendingStatus).build();
        when(userRepository.findAllByStatus_Name(UserStatusName.PENDING))
                .thenReturn(List.of(pendingUser1, pendingUser2));

        UserDto dto1 = UserDto.builder().id(1L).username("admin1").status("PENDING").build();
        UserDto dto2 = UserDto.builder().id(2L).username("admin2").status("PENDING").build();
        when(userMapper.toUserDto(pendingUser1)).thenReturn(dto1);
        when(userMapper.toUserDto(pendingUser2)).thenReturn(dto2);

        List<UserDto> result = userService.listPendingAdmins();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserDto::getStatus).containsOnly("PENDING");
        verify(userRepository).findAllByStatus_Name(UserStatusName.PENDING);
    }

    @Test
    void listPendingAdmins_shouldReturnEmptyList_whenNoPendingUsers() {
        when(userRepository.findAllByStatus_Name(UserStatusName.PENDING)).thenReturn(List.of());

        List<UserDto> result = userService.listPendingAdmins();

        assertThat(result).isEmpty();
    }

    @Test
    void approveUser_shouldChangeStatusToAktif() {
        User pendingUser = User.builder().id(1L).username("admin1").role(adminRole).status(pendingStatus).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(pendingUser));
        when(userStatusRepository.findByName(UserStatusName.AKTIF)).thenReturn(Optional.of(aktifStatus));

        User savedUser = User.builder().id(1L).username("admin1").role(adminRole).status(aktifStatus).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDto expectedDto = UserDto.builder().id(1L).username("admin1").status("AKTIF").build();
        when(userMapper.toUserDto(savedUser)).thenReturn(expectedDto);

        UserDto result = userService.approveUser(1L);

        assertThat(result.getStatus()).isEqualTo("AKTIF");
        verify(userRepository).save(pendingUser);
        assertThat(pendingUser.getStatus()).isEqualTo(aktifStatus);
    }

    @Test
    void approveUser_shouldThrowResourceNotFoundException_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.approveUser(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void approveUser_shouldThrowIllegalArgumentException_whenUserNotPending() {
        User aktifUser = User.builder().id(1L).username("admin1").role(adminRole).status(aktifStatus).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(aktifUser));

        assertThatThrownBy(() -> userService.approveUser(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bukan dalam status PENDING");
    }

    @Test
    void rejectUser_shouldDeleteUser() {
        User pendingUser = User.builder().id(1L).username("admin1").role(adminRole).status(pendingStatus).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(pendingUser));

        userService.rejectUser(1L);

        verify(userRepository).delete(pendingUser);
    }

    @Test
    void rejectUser_shouldThrowResourceNotFoundException_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.rejectUser(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void rejectUser_shouldThrowIllegalArgumentException_whenUserNotPending() {
        User aktifUser = User.builder().id(1L).username("admin1").role(adminRole).status(aktifStatus).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(aktifUser));

        assertThatThrownBy(() -> userService.rejectUser(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bukan dalam status PENDING");
    }
}
