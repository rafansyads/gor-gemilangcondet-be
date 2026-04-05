package io.mpruy.gor_gemilangcondet.backend_api.service;

import io.mpruy.gor_gemilangcondet.backend_api.dto.users.UserDto;
import io.mpruy.gor_gemilangcondet.backend_api.entities.roles.RoleName;
import io.mpruy.gor_gemilangcondet.backend_api.entities.users.UserStatusName;
import io.mpruy.gor_gemilangcondet.backend_api.exception.ForbiddenException;
import io.mpruy.gor_gemilangcondet.backend_api.exception.NotFoundException;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserRepository;
import io.mpruy.gor_gemilangcondet.backend_api.repository.UserStatusRepository;
import io.mpruy.gor_gemilangcondet.backend_api.service.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private UserStatusRepository userStatusRepository;
    @Autowired private UserMapper userMapper;

    public List<UserDto> listPendingAdminUsers() {
        var adminUsers = userRepository.findByStatus_NameAndRole_Name(UserStatusName.PENDING, RoleName.ROLE_ADMIN);
        var pegawaiUsers = userRepository.findByStatus_NameAndRole_Name(UserStatusName.PENDING, RoleName.ROLE_PEGAWAI);
        return Stream.concat(adminUsers.stream(), pegawaiUsers.stream())
                .map(userMapper::toUserDto)
                .collect(Collectors.toList());
    }

    public UserDto approveUser(String userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User tidak ditemukan: " + userId));
        if (user.getStatus().getName() != UserStatusName.PENDING) {
            throw new ForbiddenException("User bukan dalam status PENDING");
        }
        var aktifStatus = userStatusRepository.findByName(UserStatusName.AKTIF)
                .orElseThrow(() -> new RuntimeException("Status AKTIF tidak ditemukan"));
        user.setStatus(aktifStatus);
        user = userRepository.save(user);
        return userMapper.toUserDto(user);
    }

    public void rejectUser(String userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User tidak ditemukan: " + userId));
        if (user.getStatus().getName() != UserStatusName.PENDING) {
            throw new ForbiddenException("User bukan dalam status PENDING");
        }
        userRepository.delete(user);
    }
}
