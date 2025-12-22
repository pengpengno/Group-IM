package com.github.im.server.repository;

import com.github.im.server.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * 根据公司ID查找顶级部门（没有父级的部门）
     * @param companyId 公司ID
     * @return 顶级部门列表
     */
    @Query("SELECT d FROM Department d WHERE d.companyId = ?1 AND d.parentId IS NULL AND d.status = true ORDER BY d.orderNum")
    List<Department> findTopDepartmentsByCompanyId(Long companyId);

    /**
     * 根据父级部门ID查找子部门
     * @param parentId 父级部门ID
     * @return 子部门列表
     */
    @Query("SELECT d FROM Department d WHERE d.parentId = ?1 AND d.status = true ORDER BY d.orderNum")
    List<Department> findByParentId(Long parentId);

    /**
     * 根据公司ID查找所有有效部门
     * @param companyId 公司ID
     * @return 部门列表
     */
    @Query("SELECT d FROM Department d WHERE d.companyId = ?1 AND d.status = true ORDER BY d.orderNum")
    List<Department> findByCompanyIdAndStatusTrue(Long companyId);

    /**
     * 根据公司ID删除所有部门
     * @param companyId 公司ID
     */
    @Modifying
    @Query("DELETE FROM Department d WHERE d.companyId = ?1")
    void deleteByCompanyId(Long companyId);

    /**
     * 根据部门ID查找部门及子部门
     * @param departmentId 部门ID
     * @return 部门列表
     */
    @Query("SELECT d FROM Department d WHERE d.id IN (?1) OR d.parentId IN (?1)")
    List<Department> findDepartmentAndChildren(List<Long> departmentId);


}