package com.huixuetong.controller;

import com.huixuetong.ai.cache.AiAnswerCache;
import com.huixuetong.mapper.ExamMapper;
import com.huixuetong.pojo.Exam;
import com.huixuetong.pojo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/exams")
@RestController
@Tag(name = "考试管理", description = "考试的查询接口")
public class ExamController {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private AiAnswerCache aiAnswerCache;

    @Operation(summary = "新增考试")
    @PostMapping
    public Result add(@RequestBody Exam exam) {
        validate(exam);
        examMapper.insert(exam);
        aiAnswerCache.clear();
        log.info("新增考试: id={}, name={}, clazzId={}", exam.getId(), exam.getName(), exam.getClazzId());
        return Result.success();
    }

    @Operation(summary = "修改考试")
    @PutMapping
    public Result update(@RequestBody Exam exam) {
        if (exam.getId() == null) throw new IllegalArgumentException("考试ID不能为空");
        validate(exam);
        examMapper.update(exam);
        aiAnswerCache.clear();
        log.info("修改考试: id={}", exam.getId());
        return Result.success();
    }

    @Operation(summary = "删除考试")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        examMapper.deleteById(id);
        aiAnswerCache.clear();
        log.info("删除考试: id={}", id);
        return Result.success();
    }

    private void validate(Exam exam) {
        if (exam.getName() == null || exam.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("考试名称不能为空");
        }
        if (exam.getClazzId() == null) {
            throw new IllegalArgumentException("班级不能为空");
        }
        if (exam.getExamDate() == null) {
            throw new IllegalArgumentException("考试日期不能为空");
        }
        if (exam.getFullScore() == null || exam.getFullScore() <= 0) {
            throw new IllegalArgumentException("满分必须大于 0");
        }
        if (exam.getPassScore() == null) exam.setPassScore(60);
        if (exam.getPassScore() < 0 || exam.getPassScore() > exam.getFullScore()) {
            throw new IllegalArgumentException("及格分必须在 0 到满分之间");
        }
    }

    @Operation(summary = "查询所有考试")
    @GetMapping
    public Result listAll() {
        List<Exam> list = examMapper.findAll();
        return Result.success(list);
    }

    @Operation(summary = "查询考试详情")
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Exam exam = examMapper.getById(id);
        return exam != null ? Result.success(exam) : Result.error(404, "考试不存在");
    }

    @Operation(summary = "按班级查询考试")
    @GetMapping("/clazz/{clazzId}")
    public Result getByClazz(@PathVariable Integer clazzId) {
        List<Exam> list = examMapper.findByClazzId(clazzId);
        return Result.success(list);
    }
}