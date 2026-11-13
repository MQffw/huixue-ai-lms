package com.huixuetong.controller;

import com.huixuetong.pojo.Clazz;
import com.huixuetong.pojo.ClazzQueryParam;
import com.huixuetong.pojo.PageResult;
import com.huixuetong.pojo.Result;
import com.huixuetong.service.ClazzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/clazzs")
@RestController
@Validated
@Tag(name = "班级管理", description = "班级的增删改查接口")
public class ClazzController {
    @Autowired
    private ClazzService clazzService;

    // 班级分页条件查询
    @Operation(summary = "班级分页查询")
    @GetMapping
    public Result page(ClazzQueryParam clazzQueryParam) {
        log.info("班级分页查询：{}", clazzQueryParam);
        PageResult<Clazz> pageResult = clazzService.page(clazzQueryParam);
        return Result.success(pageResult);
    }

    // 根据ID删除班级
    @Operation(summary = "删除班级")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("根据ID删除班级：{}", id);
        clazzService.deleteById(id);
        return Result.success();
    }

    // 批量删除班级
    @Operation(summary = "批量删除班级")
    @DeleteMapping("/batch/{ids}")
    public Result deleteByIds(@PathVariable List<Integer> ids) {
        log.info("批量删除班级: ids={}", ids);
        clazzService.deleteByIds(ids);
        return Result.success();
    }

    // 新增班级
    @Operation(summary = "新增班级")
    @PostMapping
    public Result add(@RequestBody Clazz clazz) {
        log.info("新增班级：{}", clazz);
        clazzService.add(clazz);
        return Result.success();
    }

    // 根据ID查询班级
    @Operation(summary = "查询班级详情")
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        log.info("根据ID查询班级：{}", id);
        Clazz clazz = clazzService.getById(id);
        return Result.success(clazz);
    }

    // 修改班级
    @Operation(summary = "修改班级")
    @PutMapping
    public Result update(@RequestBody Clazz clazz) {
        log.info("修改班级：{}", clazz);
        clazzService.update(clazz);
        return Result.success();
    }

    // 查询所有班级（用于下拉选择）
    @Operation(summary = "查询所有班级")
    @GetMapping("/list")
    public Result list() {
        log.info("查询所有班级");
        List<Clazz> clazzList = clazzService.listAll();
        return Result.success(clazzList);
    }
}