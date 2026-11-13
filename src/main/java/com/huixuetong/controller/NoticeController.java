package com.huixuetong.controller;

import com.huixuetong.ai.cache.AiAnswerCache;
import com.huixuetong.mapper.NoticeMapper;
import com.huixuetong.pojo.*;
import com.huixuetong.utils.CurrentHolder;
import com.huixuetong.utils.PageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RequestMapping("/notices")
@RestController
@Tag(name = "公告管理", description = "公告的增删改查接口")
public class NoticeController {

    @Autowired private NoticeMapper noticeMapper;
    @Autowired private AiAnswerCache aiAnswerCache;

    /** 分页+搜索+定向过滤（SQL 层分页） */
    @Operation(summary = "公告分页查询")
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer type,
                       @RequestParam(required = false) String targetAudience) {
        PageQuery q = PageQuery.of(page, pageSize);
        List<Notice> rows = noticeMapper.findPage(keyword, type, targetAudience, q.offset(), q.limit());
        long total = noticeMapper.countPage(keyword, type, targetAudience);
        return Result.success(new PageResult<>(total, rows));
    }

    @Operation(summary = "查询最新公告")
    @GetMapping("/latest")
    public Result latest(@RequestParam(defaultValue = "10") int limit) { return Result.success(noticeMapper.findLatest(limit)); }

    @Operation(summary = "按类型查询公告")
    @GetMapping("/type/{type}")
    public Result getByType(@PathVariable Integer type) { return Result.success(noticeMapper.findByType(type)); }

    @Operation(summary = "搜索公告")
    @GetMapping("/search")
    public Result search(@RequestParam String keyword) { return Result.success(noticeMapper.searchByKeyword(keyword)); }

    @Operation(summary = "查询公告详情")
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Notice n = noticeMapper.getById(id);
        return n != null ? Result.success(n) : Result.error(404, "公告不存在");
    }

    @Operation(summary = "新增公告")
    @PostMapping
    public Result add(@RequestBody Notice n) {
        if (n.getTitle() == null || n.getTitle().trim().isEmpty()) return Result.error(400, "标题不能为空");
        if (n.getTargetAudience() == null || n.getTargetAudience().isBlank()) n.setTargetAudience("全体");
        n.setPublishEmpId(CurrentHolder.getId());
        n.setPublishTime(null); n.setCreateTime(null);
        noticeMapper.insert(n);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "修改公告")
    @PutMapping
    public Result update(@RequestBody Notice n) {
        if (n.getId() == null) return Result.error(400, "ID不能为空");
        if (n.getTitle() == null || n.getTitle().trim().isEmpty()) return Result.error(400, "标题不能为空");
        // 发布人以首次创建为准，编辑不改变
        Notice old = noticeMapper.getById(n.getId());
        if (old != null) n.setPublishEmpId(old.getPublishEmpId());
        if (n.getTargetAudience() == null || n.getTargetAudience().isBlank()) n.setTargetAudience("全体");
        noticeMapper.update(n);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        noticeMapper.deleteById(id);
        aiAnswerCache.clear();
        return Result.success();
    }

    @Operation(summary = "批量删除公告")
    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable String ids) {
        noticeMapper.deleteByIds(Arrays.stream(ids.split(",")).map(Integer::parseInt).toList());
        aiAnswerCache.clear();
        return Result.success();
    }
}