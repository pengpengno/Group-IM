package com.github.im.server.repository;

import com.github.im.server.model.UserDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDepartmentRepository extends JpaRepository<UserDepartment, Long> {
    
    List<UserDepartment> findByUserId(Long userId);
    
    List<UserDepartment> findByDepartmentId(Long departmentId);
    
    List<UserDepartment> findByDepartmentIdIn(List<Long> departmentIds);
    
    Optional<UserDepartment> findByUserIdAndDepartmentId(Long userId, Long departmentId);
    
    void deleteByUserIdAndDepartmentId(Long userId, Long departmentId);
    
    @Query("SELECT ud.departmentId FROM UserDepartment ud WHERE ud.userId = :userId")
    List<Long> findDepartmentIdsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ud.userId FROM UserDepartment ud WHERE ud.departmentId = :departmentId")
    List<Long> findUserIdsByDepartmentId(@Param("departmentId") Long departmentId);
}