package com.github.im.server.repository;

import com.github.im.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username,String email);

    @Query("SELECT u FROM User u WHERE u.username ILIKE ?1 OR u.email ILIKE ?1")
//    @Query("SELECT u FROM User u WHERE u.username LIKE %:query% OR u.email LIKE %:query%")
    Optional<User> findByNameOrEmail(@Param("query") String query);

    @Query("SELECT u FROM User u WHERE u.username = ?1 OR u.email = ?1")
    Optional<List<User>> findByUsernameOrEmail(@Param("loginAccount") String loginAccount);


    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);
}
