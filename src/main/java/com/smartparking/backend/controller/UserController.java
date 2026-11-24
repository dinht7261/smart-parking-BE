
package com.smartparking.backend.controller;

import com.smartparking.backend.response.UserResponse;
import com.smartparking.backend.service.UserService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    // Lấy danh sách người dùng
    @GetMapping
    public List<UserResponse> list() {
        return service.list();
    }

    // Lấy thông tin người dùng theo mã
    @GetMapping("/{userCode}")
    public UserResponse getByUserCode(
            @PathVariable String userCode
    ) {
        return service.getByUserCode(userCode);
    }
}
