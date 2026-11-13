package com.huixuetong.controller;

import com.huixuetong.ai.cache.AiAnswerCache;
import com.huixuetong.mapper.*;
import com.huixuetong.utils.PageQuery;
import com.huixuetong.pojo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RequestMapping("/scores")
@RestController
@Tag(name = "成绩管理", description = "成绩的增删改查与统计接口")
public class ScoreController {

    @Autowired private ScoreMapper scoreMapper;
    @Autowired private ExamMapper examMapper;
    @Autowired private AiAnswerCache aiAnswerCache;

    /** 分页（按考试筛选，无 examId 时查全部） */
    @Operation(summary = "成绩分页查询")
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) Integer examId,
                       @RequestParam(required = false) String studentName) {
        PageQuery q = PageQuery.of(page, pageSize);
        List<Score> rows = scoreMapper.findPage(examId, studentName, q.offset(), q.limit());
        long total = scoreMapper.countPage(examId, studentName);
        return Result.success(new PageResult<>(total, rows));
    }

    @Operation(summary = "查询学员成绩")
    @GetMapping("/student/{studentId}")
    public Result getByStudent(@PathVariable Integer studentId) { return Result.success(scoreMapper.findByStudentId(studentId)); }

    @Operation(summary = "考试成绩统计")
    @GetMapping("/exam/{examId}/stats")
    public Result stats(@PathVariable Integer examId) {
        Map<String, Object> s = scoreMapper.getExamStats(examId);
        if (s == null) return Result.error(404, "无数据");
        Object avg = s.get("avg_score");
        return Result.success(Map.of("totalCount", s.getOrDefault("total_count", 0),
                "avgScore", avg != null ? String.format("%.1f", ((Number) avg).doubleValue()) : "0",
                "maxScore", s.getOrDefault("max_score", 0), "minScore", s.getOrDefault("min_score", 0),
                "failCount", s.getOrDefault("fail_count", 0)));
    }

    @Operation(summary = "考试成绩排名")
    @GetMapping("/exam/{examId}/ranking")
    public Result ranking(@PathVariable Integer examId) { return Result.success(scoreMapper.findByExamId(examId)); }

    /** 新增成绩 */
    @Operation(summary = "新增成绩")
    @PostMapping
    public Result add(@RequestBody Score score) {
        if (score.getExamId() == null || score.getStudentId() == null) return Result.error(400, "考试ID和学员ID不能为空");
        scoreMapper.insert(score);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "修改成绩")
    @PutMapping
    public Result update(@RequestBody Score score) {
        if (score.getId() == null) return Result.error(400, "ID不能为空");
        scoreMapper.update(score);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "删除成绩")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        scoreMapper.deleteById(id);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "批量删除成绩")
    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable String ids) {
        List<Integer> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
        if (!idList.isEmpty()) scoreMapper.deleteByIds(idList);
        aiAnswerCache.clear();
        return Result.success();
    }
}