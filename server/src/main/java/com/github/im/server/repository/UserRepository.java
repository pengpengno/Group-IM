package com.github.im.server.repository;

import com.github.im.server.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<List<User>> findByUserIdIn(Collection<Long> userId);

    Optional<User> findByUsernameOrEmail(String username,String email);

    @Query("SELECT u FROM User u WHERE u.username ILIKE ?1 OR u.email ILIKE ?1")
//    @Query("SELECT u FROM User u WHERE u.username LIKE %:query% OR u.email LIKE %:query%")
    Optional<List<User>> findByNameOrEmail(@Param("query") String query, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.username = ?1 OR u.email = ?1")
    Optional<User> findByUsernameOrEmail(@Param("loginAccount") String loginAccount);


    Page<User>  findByUsernameLikeOrEmailLike(String username,String email ,  Pageable pageable);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByRefreshToken(String refreshToken);
    
    @Query("SELECT u FROM User u WHERE u.username IN ?1 OR u.email IN ?2")
    List<User> findByUsernameInOrEmailIn(List<String> usernames, List<String> emails);
}