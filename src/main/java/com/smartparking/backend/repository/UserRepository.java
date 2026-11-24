
package com.smartparking.backend.repository;

import com.smartparking.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm người dùng theo mã tài khoản A, B, C, D
    Optional<User> findByUserCode(String userCode);

    // Tìm người dùng theo biển số xe
    Optional<User> findByLicensePlate(String licensePlate);

}
