package com.huixuetong.controller;

import com.huixuetong.pojo.Emp;
import com.huixuetong.pojo.EmpQueryParam;
import com.huixuetong.pojo.PageResult;
import com.huixuetong.pojo.Result;
import com.huixuetong.service.EmpService;
import com.huixuetong.utils.CurrentHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 员工管理控制器
 */
@Slf4j
@RequestMapping("/emps")
@RestController
@Validated
@Tag(name = "员工管理", description = "员工的增删改查接口")
public class EmpController {
    
    @Autowired
    private EmpService empService;
    
    @Operation(summary = "查询所有员工")
    @GetMapping("/list")
    public Result listAll() {
        return Result.success(empService.findAll());
    }

    @Operation(summary = "员工分页查询")
    @GetMapping
    public Result page(EmpQueryParam empQueryParam) {
        log.info("员工分页查询: page={}, pageSize={}", 
                empQueryParam.getPage(), empQueryParam.getPageSize());
        PageResult<Emp> pageResult = empService.page(empQueryParam);
        return Result.success(pageResult);
    }
    
    @Operation(summary = "新增员工")
    @PostMapping
    public Result save(@RequestBody Emp emp) {
        if (emp.getUsername() == null || emp.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (emp.getName() == null || emp.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        log.info("新增员工: id={}, username={}", emp.getId(), emp.getUsername());
        empService.save(emp);
        return Result.success();
    }
    
    @Operation(summary = "批量删除员工")
    @DeleteMapping
    public Result delete(@RequestParam List<Integer> ids) {
        log.info("删除员工: count={}", ids.size());
        empService.delete(ids);
        return Result.success();
    }
    
    @Operation(summary = "查询员工详情")
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id) {
        log.info("查询员工: id={}", id);
        Emp emp = empService.getInfo(id);
        return Result.success(emp);
    }
    
    @Operation(summary = "修改员工")
    @PutMapping
    public Result update(@RequestBody Emp emp) {
        if (emp.getUsername() == null || emp.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (emp.getName() == null || emp.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        log.info("修改员工: id={}, username={}", emp.getId(), emp.getUsername());
        empService.update(emp);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result updatePassword(@RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error("请输入旧密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }
        Integer currentUserId = CurrentHolder.getId();
        log.info("修改密码: userId={}", currentUserId);
        try {
            empService.updatePassword(currentUserId, oldPassword, newPassword);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}