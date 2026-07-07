package com.itheima.controller;

import com.itheima.pojo.Emp;
import com.itheima.pojo.LoginInfo;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录Controller
 */
@Tag(name = "登录认证", description = "用户登录接口")
@Slf4j
@RestController
@Validated
public class LoginController {

    @Autowired
    private EmpService empService;
    /**
     * 登录方法
     * @return
     */
    @Operation(summary = "登录提示", description = "提示使用POST请求登录")
    @GetMapping("/login")
    public Result loginGet(){
        return Result.error("请使用POST请求登录");
    }

    @Operation(summary = "用户登录", description = "用户名密码登录，返回JWT Token")
    @PostMapping("/login")
    public Result login(@RequestBody Emp emp){
        log.info("登录:{}",emp);
        LoginInfo info = empService.login(emp);
        if (info != null){
            return Result.success(info);
        }
        return Result.error("用户名或密码错误");
    }
}
