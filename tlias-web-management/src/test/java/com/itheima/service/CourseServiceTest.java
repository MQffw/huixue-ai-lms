package com.itheima.service;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.CourseMapper;
import com.itheima.mapper.CourseScheduleMapper;
import com.itheima.pojo.Course;
import com.itheima.pojo.PageResult;
import com.itheima.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 课程 Service 单元测试（含级联删除）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("课程服务（级联删除）")
class CourseServiceTest {

    @InjectMocks
    private CourseServiceImpl courseService;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private CourseScheduleMapper courseScheduleMapper;

    @Mock
    private AiAnswerCache aiAnswerCache;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setName("Java基础");
        testCourse.setSubject(1);
        testCourse.setHours(80);
    }

    // ==================== CRUD ====================

    @Test @DisplayName("分页查询")
    void page() {
        when(courseMapper.count(null, null)).thenReturn(1L);
        when(courseMapper.pageList(null, null, 0, 10)).thenReturn(List.of(testCourse));

        PageResult<Course> result = courseService.page(null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRows().size());
    }

    @Test @DisplayName("新增课程")
    void add() {
        when(courseMapper.insert(any())).thenReturn(1);
        assertDoesNotThrow(() -> courseService.add(testCourse));
        verify(courseMapper).insert(any());
    }

    @Test @DisplayName("更新课程")
    void update() {
        when(courseMapper.update(any())).thenReturn(1);
        assertDoesNotThrow(() -> courseService.update(testCourse));
        verify(courseMapper).update(any());
    }

    // ==================== 级联删除 ====================

    @Test @DisplayName("删除单个课程 → 先删排课，再删课程")
    void deleteById_cascadeSchedule() {
        when(courseScheduleMapper.deleteByCourseId(1)).thenReturn(3); // 3条排课关联
        when(courseMapper.deleteById(1)).thenReturn(1);

        courseService.deleteById(1);

        verify(courseScheduleMapper).deleteByCourseId(1); // 先删排课
        verify(courseMapper).deleteById(1);                // 再删课程
    }

    @Test @DisplayName("批量删除课程 → 逐个级联删排课")
    void deleteByIds_cascadeSchedules() {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        when(courseScheduleMapper.deleteByCourseId(anyInt())).thenReturn(1);
        when(courseMapper.deleteByIds(ids)).thenReturn(3);

        courseService.deleteByIds(ids);

        verify(courseScheduleMapper, times(3)).deleteByCourseId(anyInt());
        verify(courseMapper).deleteByIds(ids);
    }

    // ==================== 查询 ====================

    @Test @DisplayName("按学科查询")
    void findBySubject() {
        when(courseMapper.findBySubject(1)).thenReturn(List.of(testCourse));
        List<Course> list = courseService.findBySubject(1);
        assertEquals(1, list.size());
    }

    @Test @DisplayName("列出全部")
    void listAll() {
        when(courseMapper.findAll()).thenReturn(List.of(testCourse));
        List<Course> list = courseService.listAll();
        assertEquals(1, list.size());
    }
}
