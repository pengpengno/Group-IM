package com.github.im.server.repository;

import com.github.im.server.model.UserDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 用户 部门 Repository
 */
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, Long> {
    
    List<UserDepartment> findByUserId(Long userId);
    
    List<UserDepartment> findByDepartmentId(Long departmentId);
    
    List<UserDepartment> findByDepartmentIdIn(List<Long> departmentIds);
    
    Optional<UserDepartment> findByUserIdAndDepartmentId(Long userId, Long departmentId);



    /**
     * 根据用户ID获取该用户所有的部门ID
     * @param userId 用户ID
     * @return 部门ID列表
     */
    @Query("SELECT ud.departmentId FROM UserDepartment ud WHERE ud.userId = :userId")
    List<Long> findDepartmentIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据部门ID获取该部门的所有用户ID
     * @param departmentId 部门ID
     * @return 用户ID列表
     */
    @Query("SELECT ud.userId FROM UserDepartment ud WHERE ud.departmentId = :departmentId")
    List<Long> findUserIdsByDepartmentId(@Param("departmentId") Long departmentId);
    
    /**
     * 根据公司ID获取该公司的所有用户及其部门信息
     * @param companyId 公司ID
     * @return 用户ID列表
     */
    @Query("SELECT DISTINCT ud.userId FROM UserDepartment ud JOIN Department d ON " +
            "ud.departmentId = d.departmentId WHERE d.companyId = :companyId")
    List<Long> findUserIdsByCompanyId(@Param("companyId") Long companyId);



    /**
     * 删除用户部门关系
     * @param userId 用户ID
     * @param departmentId 部门ID
     */
    int deleteByUserIdAndDepartmentId(Long userId, Long departmentId);


    /**
     * 删除部门下的所有用户关系
     * @param departmentId 部门ID
     */
    int deleteByDepartmentId(Long departmentId);

    /**
     * 删除用户下的所有部门关系
     * @param userId 用户ID
     */
    int deleteByUserId(Long userId);

    /**
     * 删除部门下的所有用户关系
     * @param departmentIds 部门ID列表
     */
    int deleteByDepartmentIdIn(List<Long> departmentIds);

    /**
     * 删除用户下的所有部门关系
     * @param userIds 用户ID列表
     */
    int deleteByUserIdIn(List<Long> userIds);
}
