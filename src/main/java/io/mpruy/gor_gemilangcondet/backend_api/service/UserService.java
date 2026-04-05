package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.User;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ResourceNotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       UserStatusRepository userStatusRepository,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userStatusRepository = userStatusRepository;
        this.userMapper = userMapper;
    }

    public List<UserDto> listPendingAdmins() {
        return userRepository.findAllByStatus_Name(UserStatusName.PENDING)
                .stream()
                .map(userMapper::toUserDto)
                .toList();
    }

    @Transactional
    public UserDto approveUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan dengan id: " + id));

        if (!UserStatusName.PENDING.equals(user.getStatus().getName())) {
            throw new IllegalArgumentException("User bukan dalam status PENDING");
        }

        var aktifStatus = userStatusRepository.findByName(UserStatusName.AKTIF)
                .orElseThrow(() -> new IllegalStateException("Status AKTIF tidak ditemukan"));

        user.setStatus(aktifStatus);
        User savedUser = userRepository.save(user);
        return userMapper.toUserDto(savedUser);
    }

    @Transactional
    public void rejectUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan dengan id: " + id));

        if (!UserStatusName.PENDING.equals(user.getStatus().getName())) {
            throw new IllegalArgumentException("User bukan dalam status PENDING");
        }

        userRepository.delete(user);
    }
}
