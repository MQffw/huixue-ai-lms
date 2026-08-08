package com.itheima.controller;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.NoticeMapper;
import com.itheima.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RequestMapping("/notices")
@RestController
public class NoticeController {

    @Autowired private NoticeMapper noticeMapper;
    @Autowired private AiAnswerCache aiAnswerCache;

    /** 分页+搜索 */
    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer type) {
        List<Notice> all;
        if (keyword != null && !keyword.isEmpty()) {
            all = noticeMapper.searchByKeyword(keyword);
        } else if (type != null) {
            all = noticeMapper.findByType(type);
        } else {
            all = noticeMapper.findLatest(1000);
        }
        if (keyword != null && !keyword.isEmpty() && type != null) {
            all = all.stream().filter(n -> n.getType().equals(type)).toList();
        }
        long total = all.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        return Result.success(new PageResult<>(total, start < total ? all.subList(start, end) : List.of()));
    }

    @GetMapping("/latest")
    public Result latest(@RequestParam(defaultValue = "10") int limit) { return Result.success(noticeMapper.findLatest(limit)); }

    @GetMapping("/type/{type}")
    public Result getByType(@PathVariable Integer type) { return Result.success(noticeMapper.findByType(type)); }

    @GetMapping("/search")
    public Result search(@RequestParam String keyword) { return Result.success(noticeMapper.searchByKeyword(keyword)); }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Notice n = noticeMapper.getById(id);
        return n != null ? Result.success(n) : Result.error(404, "公告不存在");
    }

    @PostMapping
    public Result add(@RequestBody Notice n) {
        if (n.getTitle() == null || n.getTitle().trim().isEmpty()) return Result.error(400, "标题不能为空");
        n.setPublishTime(null); n.setCreateTime(null);
        noticeMapper.insert(n);
        aiAnswerCache.clear();
        return Result.success();
    }

    @PutMapping
    public Result update(@RequestBody Notice n) {
        if (n.getId() == null) return Result.error(400, "ID不能为空");
        noticeMapper.update(n);
        aiAnswerCache.clear();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        noticeMapper.deleteById(id);
        aiAnswerCache.clear();
        return Result.success();
    }

    @DeleteMapping("/batch/{ids}")
    public Result deleteBatch(@PathVariable String ids) {
        noticeMapper.deleteByIds(Arrays.stream(ids.split(",")).map(Integer::parseInt).toList());
        aiAnswerCache.clear();
        return Result.success();
    }
}
