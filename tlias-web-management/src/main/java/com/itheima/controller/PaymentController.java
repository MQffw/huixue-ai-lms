package com.itheima.controller;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.PaymentMapper;
import com.itheima.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RequestMapping("/payments")
@RestController
public class PaymentController {

    @Autowired private PaymentMapper paymentMapper;
    @Autowired private AiAnswerCache aiAnswerCache;

    /** 分页+搜索 */
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String studentName) {
        List<Payment> all = new ArrayList<>();
        if (status != null) {
            all = paymentMapper.findByStatus(status);
        } else {
            for (int s = 1; s <= 3; s++) all.addAll(paymentMapper.findByStatus(s));
        }
        all.sort((a, b) -> b.getPaymentDate().compareTo(a.getPaymentDate()));
        if (studentName != null && !studentName.isEmpty()) {
            all = all.stream().filter(p -> p.getStudentName() != null && p.getStudentName().contains(studentName)).toList();
        }
        long total = all.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        return Result.success(new PageResult<>(total, start < total ? all.subList(start, end) : List.of()));
    }

    @GetMapping("/student/{studentId}")
    public Result getByStudent(@PathVariable Integer studentId) { return Result.success(paymentMapper.findByStudentId(studentId)); }

    @GetMapping("/status/{status}")
    public Result getByStatus(@PathVariable Integer status) { return Result.success(paymentMapper.findByStatus(status)); }

    @PostMapping
    public Result add(@RequestBody Payment p) {
        if (p.getStudentId() == null) return Result.error(400, "学员ID不能为空");
        paymentMapper.insert(p);
        aiAnswerCache.clear();
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody Payment p) {
        if (p.getId() == null) return Result.error(400, "ID不能为空");
        paymentMapper.update(p);
        aiAnswerCache.clear();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        paymentMapper.deleteById(id);
        aiAnswerCache.clear();
        return Result.success();
    }

    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable String ids) {
        for (String id : ids.split(",")) paymentMapper.deleteById(Integer.parseInt(id));
        aiAnswerCache.clear();
        return Result.success();
    }
}
