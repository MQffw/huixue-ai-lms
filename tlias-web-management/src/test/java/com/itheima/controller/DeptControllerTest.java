package com.itheima.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.pojo.Dept;
import com.itheima.service.DeptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 部门 Controller 接口测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("部门接口测试")
class DeptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeptService deptService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.itheima.utils.JwtUtils jwtUtils;

    private String getToken() {
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("id", 1);
        claims.put("username", "admin");
        claims.put("role", "ADMIN");
        return jwtUtils.generateToken(claims);
    }

    @Test
    @DisplayName("查询全部部门")
    void list() throws Exception {
        Dept dept = new Dept();
        dept.setId(1);
        dept.setName("学工部");
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());

        when(deptService.finAll()).thenReturn(Arrays.asList(dept));

        mockMvc.perform(get("/depts").header("token", getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].name").value("学工部"));

        verify(deptService).finAll();
    }

    @Test
    @DisplayName("根据ID查询部门")
    void getInfo() throws Exception {
        Dept dept = new Dept();
        dept.setId(1);
        dept.setName("教研部");

        when(deptService.getById(1)).thenReturn(dept);

        mockMvc.perform(get("/depts/1").header("token", getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("教研部"));
    }

    @Test
    @DisplayName("新增部门")
    void add() throws Exception {
        Dept dept = new Dept();
        dept.setName("财务部");

        doNothing().when(deptService).add(any());

        mockMvc.perform(post("/depts")
                        .header("token", getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        verify(deptService).add(any());
    }

    @Test
    @DisplayName("新增部门 - 名称为空")
    void add_emptyName() throws Exception {
        Dept dept = new Dept();
        dept.setName("");

        mockMvc.perform(post("/depts")
                        .header("token", getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dept)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("修改部门")
    void update() throws Exception {
        Dept dept = new Dept();
        dept.setId(1);
        dept.setName("人事部");

        doNothing().when(deptService).update(any());

        mockMvc.perform(put("/depts")
                        .header("token", getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    @DisplayName("删除部门")
    void testDelete() throws Exception {
        doNothing().when(deptService).deleteById(1);

        mockMvc.perform(MockMvcRequestBuilders.delete("/depts").header("token", getToken()).param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        verify(deptService).deleteById(1);
    }

    @Test
    @DisplayName("批量删除部门")
    void testDeleteByIds() throws Exception {
        doNothing().when(deptService).deleteByIds(any());

        mockMvc.perform(MockMvcRequestBuilders.delete("/depts/1,2,3").header("token", getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}
