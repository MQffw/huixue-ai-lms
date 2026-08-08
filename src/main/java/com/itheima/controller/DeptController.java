package com.itheima.controller;

import com.itheima.pojo.Dept;
import com.itheima.pojo.Result;
import com.itheima.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 */
@Tag(name = "部门管理", description = "部门的增删改查接口")
@Slf4j
@RestController
@RequestMapping("/depts")
@Validated
public class DeptController {

    @Autowired
    private DeptService deptService;

    @Operation(summary = "查询全部部门")
    @GetMapping
    public Result list() {
        log.info("查询全部部门");
        List<Dept> deptList = deptService.finAll();
        return Result.success(deptList);
    }
    
    @Operation(summary = "删除部门")
    @DeleteMapping
    public Result delete(@Parameter(description = "部门ID") Integer id) {
        log.info("删除部门: id={}", id);
        deptService.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "批量删除部门")
    @DeleteMapping("/{ids}")
    public Result deleteByIds(@Parameter(description = "部门ID列表") @PathVariable List<Integer> ids) {
        log.info("批量删除部门: ids={}", ids);
        deptService.deleteByIds(ids);
        return Result.success();
    }
    
    @Operation(summary = "新增部门")
    @PostMapping
    public Result add(@RequestBody Dept dept) {
        if (dept.getName() == null || dept.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("部门名称不能为空");
        }
        log.info("新增部门: name={}", dept.getName());
        deptService.add(dept);
        return Result.success();
    }
    
    @Operation(summary = "根据ID查询部门")
    @GetMapping("/{id}")
    public Result getInfo(@Parameter(description = "部门ID") @PathVariable Integer id) {
        log.info("查询部门: id={}", id);
        Dept dept = deptService.getById(id);
        return Result.success(dept);
    }
    
    @Operation(summary = "修改部门")
    @PutMapping
    public Result update(@RequestBody Dept dept) {
        if (dept.getName() == null || dept.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("部门名称不能为空");
        }
        log.info("修改部门: id={}, name={}", dept.getId(), dept.getName());
        deptService.update(dept);
        return Result.success();
    }
}
