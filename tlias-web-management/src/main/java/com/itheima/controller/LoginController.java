package com.itheima.controller;

import com.itheima.pojo.Emp;
import com.itheima.pojo.LoginInfo;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录Controller
 */

@Slf4j
@RestController
public class LoginController {

    @Autowired
    private EmpService empService;
    /**
     * 登录方法
     * @return
     */
    @GetMapping("/login")
    public Result loginGet(){
        return Result.error("请使用POST请求登录");
    }

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
