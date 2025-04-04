package kr.cocoh.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import kr.cocoh.api.model.auth.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndProvider(String email, String provider);
}
