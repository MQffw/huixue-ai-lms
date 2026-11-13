package com.huixuetong.controller;

import com.huixuetong.ai.cache.AiAnswerCache;
import com.huixuetong.mapper.PaymentMapper;
import com.huixuetong.pojo.*;
import com.huixuetong.utils.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RequestMapping("/payments")
@RestController
@Tag(name = "缴费管理", description = "缴费记录的增删改查接口")
public class PaymentController {

    @Autowired private PaymentMapper paymentMapper;
    @Autowired private AiAnswerCache aiAnswerCache;

    /** 分页+搜索 */
    @Operation(summary = "缴费记录分页查询")
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String studentName) {
        PageQuery q = PageQuery.of(page, pageSize);
        List<Payment> rows = paymentMapper.findPage(status, studentName, q.offset(), q.limit());
        long total = paymentMapper.countPage(status, studentName);
        return Result.success(new PageResult<>(total, rows));
    }

    @Operation(summary = "查询学员缴费记录")
    @GetMapping("/student/{studentId}")
    public Result getByStudent(@PathVariable Integer studentId) { return Result.success(paymentMapper.findByStudentId(studentId)); }

    @Operation(summary = "按状态查询缴费记录")
    @GetMapping("/status/{status}")
    public Result getByStatus(@PathVariable Integer status) { return Result.success(paymentMapper.findByStatus(status)); }

    @Operation(summary = "新增缴费记录")
    @PostMapping
    public Result add(@RequestBody Payment p) {
        if (p.getStudentId() == null) return Result.error(400, "学员ID不能为空");
        paymentMapper.insert(p);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "修改缴费记录")
    @PutMapping
    public Result update(@RequestBody Payment p) {
        if (p.getId() == null) return Result.error(400, "ID不能为空");
        paymentMapper.update(p);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "删除缴费记录")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        paymentMapper.deleteById(id);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "批量删除缴费记录")
    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable String ids) {
        for (String id : ids.split(",")) paymentMapper.deleteById(Integer.parseInt(id));
        aiAnswerCache.clear();
        return Result.success();
    }
}