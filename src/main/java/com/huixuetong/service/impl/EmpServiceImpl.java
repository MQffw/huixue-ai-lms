package com.huixuetong.service.impl;

import com.huixuetong.ai.cache.AiAnswerCache;
import com.huixuetong.mapper.EmpMapper;
import com.huixuetong.pojo.*;
import com.huixuetong.service.EmpService;
import com.huixuetong.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmpServiceImpl implements EmpService {

    /** 手机号格式：1 开头，第二位 3-9，共 11 位数字 */
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
    
    @Autowired
    private EmpMapper empMapper;

    @Autowired
    private AiAnswerCache aiAnswerCache;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Override
    public LoginInfo login(Emp emp) {
        Emp dbEmp = empMapper.findByUsername(emp.getUsername());
        
        if (dbEmp == null || !dbEmp.getPassword().equals(emp.getPassword())) {
            return null;
        }
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", dbEmp.getId());
        claims.put("username", dbEmp.getUsername());
        claims.put("role", "ADMIN");
        
        String token = jwtUtils.generateToken(claims);
        
        return new LoginInfo(dbEmp.getId(), dbEmp.getUsername(), dbEmp.getName(), token);
    }
    
    @Override
    public PageResult<Emp> page(EmpQueryParam param) {
        Long count = empMapper.count(param);
        List<Emp> rows = empMapper.pageList(param);
        return new PageResult<>(count, rows);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Emp emp) {
        checkPhone(emp.getPhone(), null);
        try {
            emp.setCreateTime(LocalDateTime.now());
            emp.setUpdateTime(LocalDateTime.now());
            empMapper.insert(emp);
            log.info("新增员工成功: id={}, username={}", emp.getId(), emp.getUsername());
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("新增员工失败: username={}", emp.getUsername(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Integer> ids) {
        try {
            int count = empMapper.deleteByIds(ids);
            log.info("批量删除员工成功: count={}", count);
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("批量删除员工失败: count={}", ids.size(), e);
            throw e;
        }
    }
    
    @Override
    public Emp getInfo(Integer id) {
        return empMapper.getById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Emp emp) {
        //前端编辑表单回显的是脱敏号码（138****8001），原样提交回来时直接忽略，避免把星号写进库
        if (emp.getPhone() != null && emp.getPhone().contains("*")) {
            log.warn("修改员工收到脱敏手机号，已忽略该字段: id={}, phone={}", emp.getId(), emp.getPhone());
            emp.setPhone(null);
        } else if (emp.getPhone() != null) {
            checkPhone(emp.getPhone(), emp.getId());
        }
        try {
            emp.setUpdateTime(LocalDateTime.now());
            empMapper.updateById(emp);
            log.info("修改员工成功: id={}, username={}", emp.getId(), emp.getUsername());
            aiAnswerCache.clear();
        } catch (Exception e) {
            log.error("修改员工失败: id={}", emp.getId(), e);
            throw e;
        }
    }
    
    @Override
    public List<Emp> findAll() {
        return empMapper.findAll();
    }

    @Override
    public void updatePassword(Integer id, String oldPassword, String newPassword) {
        Emp emp = empMapper.getById(id);
        if (emp == null) {
            throw new RuntimeException("\u7528\u6237\u4e0d\u5b58\u5728");
        }
        if (!emp.getPassword().equals(oldPassword)) {
            throw new RuntimeException("\u65e7\u5bc6\u7801\u9519\u8bef");
        }
        empMapper.updatePassword(id, newPassword);
        log.info("\u4fee\u6539\u5bc6\u7801\u6210\u529f: id={}", id);
    }

    /**
     * 手机号校验：格式 + 唯一性
     * 查重用数据库里的真实号码，脱敏只发生在 JSON 输出阶段，不会进到这里
     *
     * @param selfId 修改时传自己的 id（查重排除自己），新增传 null
     */
    private void checkPhone(String phone, Integer selfId) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (phone.contains("*")) {
            throw new IllegalArgumentException("手机号不能使用脱敏后的号码，请填写完整的11位手机号");
        }
        if (!phone.matches(PHONE_REGEX)) {
            throw new IllegalArgumentException("手机号格式不正确，应为1开头的11位数字");
        }
        Emp exist = empMapper.findByPhone(phone);
        if (exist != null && !exist.getId().equals(selfId)) {
            throw new IllegalArgumentException("手机号已被其他员工使用，请更换后重试");
        }
    }
}
