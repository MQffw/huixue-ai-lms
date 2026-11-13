package com.huixuetong.utils;

/**
 * 统一分页参数：把 page/pageSize 收敛成 offset/limit，并做边界钳制。
 *
 * 全项目分页约定（不引入 PageHelper，保证离线构建）：
 *   PageQuery q = PageQuery.of(page, pageSize);
 *   List<X> rows = mapper.findPage(..., q.offset(), q.limit());
 *   long total = mapper.countPage(...);
 *   return Result.success(new PageResult<>(total, rows));
 *
 * 禁止 findAll() + subList() 的内存分页。
 */
public record PageQuery(int offset, int limit) {

    /** 单页上限，防止一次拉爆数据库 */
    public static final int MAX_PAGE_SIZE = 100;

    public static PageQuery of(int page, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int current = Math.max(page, 1);
        return new PageQuery((current - 1) * size, size);
    }
}
