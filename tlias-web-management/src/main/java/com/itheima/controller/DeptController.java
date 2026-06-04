package com.itheima.controller;

import com.itheima.pojo.Dept;
import com.itheima.pojo.Result;
import com.itheima.service.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
//先写controller层
@RestController
public class DeptController {

    //private static final Logger log = LoggerFactory.getLogger(DeptController.class);//固定的，除了字节码对象

    @Autowired
    private DeptService deptService;

    //@RequestMapping(value = "/depts",method = RequestMethod.GET)//指定请求方式
    @GetMapping("/depts")//衍生注解，按住ctrl查看源码
    public Result list(){
        log.info("查询全部部门数据");
        //System.out.println("查询全部部门数据");
        List<Dept> deptList = deptService.finAll();
        return Result.success(deptList);
    }
    //当前端传递请求参数名与服务端方法形参名一致，可以省@RequestParam
    @DeleteMapping("/depts")
    public Result delete(Integer id){
        log.info("根据ID删除部门:"+id);
        //System.out.println("根据ID删除部门:"+id);
        deptService.deleteById(id);//alt+enter直接自动创建
        return Result.success();
    }

    @PostMapping("/depts")
    public Result add(@RequestBody Dept dept){
        log.info("新增部门:"+dept);
        //System.out.println("新增部门:"+dept);
        deptService.add(dept);
        return Result.success();
    }

    @GetMapping("/depts/{id}")
    public Result getInfo(@PathVariable Integer id){
        log.info("根据ID查询部门:"+id);
        //System.out.println("根据ID查询部门:"+id);
        Dept dept = deptService.getById(id);
        return Result.success(dept);
    }
    @PutMapping("/depts")
    public Result update(@RequestBody Dept dept){
        log.info("修改部门:"+dept);
        //System.out.println("修改部门:"+dept);
        deptService.update(dept);
        return Result.success();
    }
}
