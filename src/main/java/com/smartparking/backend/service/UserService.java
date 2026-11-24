
package com.smartparking.backend.service;

import com.smartparking.backend.entity.User;
import com.smartparking.backend.repository.UserRepository;
import com.smartparking.backend.response.UserResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    // Lấy danh sách tất cả người dùng
    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return repository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    // Tìm người dùng theo mã tài khoản
    @Transactional(readOnly = true)
    public UserResponse getByUserCode(String userCode) {

        User user = repository.findByUserCode(
                userCode.trim().toUpperCase()
        ).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + userCode
                )
        );

        return UserResponse.from(user);
    }
}
