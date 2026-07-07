package com.itheima.controller;

import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import com.itheima.utils.CurrentHolder;
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
public class EmpController {
    
    @Autowired
    private EmpService empService;
    
    @GetMapping("/list")
    public Result listAll() {
        return Result.success(empService.findAll());
    }

    @GetMapping
    public Result page(EmpQueryParam empQueryParam) {
        log.info("员工分页查询: page={}, pageSize={}", 
                empQueryParam.getPage(), empQueryParam.getPageSize());
        PageResult<Emp> pageResult = empService.page(empQueryParam);
        return Result.success(pageResult);
    }
    
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
    
    @DeleteMapping
    public Result delete(@RequestParam List<Integer> ids) {
        log.info("删除员工: count={}", ids.size());
        empService.delete(ids);
        return Result.success();
    }
    
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id) {
        log.info("查询员工: id={}", id);
        Emp emp = empService.getInfo(id);
        return Result.success(emp);
    }
    
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
