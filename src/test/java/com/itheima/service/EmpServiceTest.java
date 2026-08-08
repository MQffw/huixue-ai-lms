package com.itheima.service;

import com.itheima.ai.cache.AiAnswerCache;
import com.itheima.mapper.EmpMapper;
import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.impl.EmpServiceImpl;
import com.itheima.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 员工 Service 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("员工服务测试")
class EmpServiceTest {

    @InjectMocks
    private EmpServiceImpl empService;

    @Mock
    private EmpMapper empMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AiAnswerCache aiAnswerCache;

    private Emp testEmp;

    @BeforeEach
    void setUp() {
        testEmp = new Emp();
        testEmp.setId(1);
        testEmp.setUsername("zhangsan");
        testEmp.setPassword("123456");
        testEmp.setName("张三");
        testEmp.setGender(1);
        testEmp.setPhone("13800138000");
        testEmp.setJob(1);
        testEmp.setSalary(8000);
        testEmp.setDeptId(1);
        testEmp.setEntryDate(LocalDate.of(2024, 1, 1));
        testEmp.setCreateTime(LocalDateTime.now());
        testEmp.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("登录 - 用户名密码正确")
    void login_success() {
        when(empMapper.findByUsername("zhangsan")).thenReturn(testEmp);
        when(jwtUtils.generateToken(any())).thenReturn("mock-token");

        var result = empService.login(testEmp);

        assertNotNull(result);
        assertEquals("zhangsan", result.getUsername());
        verify(empMapper).findByUsername("zhangsan");
    }

    @Test
    @DisplayName("登录 - 用户不存在")
    void login_userNotFound() {
        when(empMapper.findByUsername("zhangsan")).thenReturn(null);

        var result = empService.login(testEmp);

        assertNull(result);
    }

    @Test
    @DisplayName("登录 - 密码错误")
    void login_wrongPassword() {
        when(empMapper.findByUsername("zhangsan")).thenReturn(testEmp);

        Emp wrongPwdEmp = new Emp();
        wrongPwdEmp.setUsername("zhangsan");
        wrongPwdEmp.setPassword("wrong");

        var result = empService.login(wrongPwdEmp);

        assertNull(result);
    }

    @Test
    @DisplayName("分页查询")
    void page() {
        List<Emp> empList = Arrays.asList(testEmp);
        when(empMapper.count(any())).thenReturn(1L);
        when(empMapper.pageList(any())).thenReturn(empList);

        EmpQueryParam param = new EmpQueryParam();
        param.setPage(1);
        param.setPageSize(10);

        PageResult<Emp> result = empService.page(param);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRows().size());
    }

    @Test
    @DisplayName("新增员工")
    void save() {
        when(empMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> empService.save(testEmp));
        verify(empMapper).insert(any());
        assertNotNull(testEmp.getCreateTime());
        assertNotNull(testEmp.getUpdateTime());
    }

    @Test
    @DisplayName("批量删除员工")
    void delete() {
        when(empMapper.deleteByIds(any())).thenReturn(2);

        assertDoesNotThrow(() -> empService.delete(Arrays.asList(1, 2)));
        verify(empMapper).deleteByIds(Arrays.asList(1, 2));
    }
}
