package com.itheima.controller;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.*;
import com.itheima.pojo.*;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RequestMapping("/scores")
@RestController
public class ScoreController {

    @Autowired private ScoreMapper scoreMapper;
    @Autowired private ExamMapper examMapper;
    @Autowired private AiAnswerCache aiAnswerCache;

    /** 分页（按考试筛选，无 examId 时查全部） */
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) Integer examId,
                       @RequestParam(required = false) String studentName) {
        List<Score> all;
        if (examId != null) {
            all = scoreMapper.findByExamId(examId);
        } else {
            all = scoreMapper.findAllWithNames();
        }
        if (StringUtils.hasText(studentName)) {
            all = all.stream().filter(s -> s.getStudentName() != null && s.getStudentName().contains(studentName)).toList();
        }
        long total = all.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        return Result.success(new PageResult<>(total, start < total ? all.subList(start, end) : List.of()));
    }

    @GetMapping("/student/{studentId}")
    public Result getByStudent(@PathVariable Integer studentId) { return Result.success(scoreMapper.findByStudentId(studentId)); }

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

    @GetMapping("/exam/{examId}/ranking")
    public Result ranking(@PathVariable Integer examId) { return Result.success(scoreMapper.findByExamId(examId)); }

    /** 新增成绩 */
    @PostMapping
    public Result add(@RequestBody Score score) {
        if (score.getExamId() == null || score.getStudentId() == null) return Result.error(400, "考试ID和学员ID不能为空");
        scoreMapper.insert(score);
        aiAnswerCache.clear();
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody Score score) {
        if (score.getId() == null) return Result.error(400, "ID不能为空");
        scoreMapper.update(score);
        aiAnswerCache.clear();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        scoreMapper.deleteById(id);
        aiAnswerCache.clear();
        return Result.success();
    }

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
