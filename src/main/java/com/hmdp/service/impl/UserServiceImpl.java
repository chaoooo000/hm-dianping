package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Random;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        //2校验验证码
        String code = loginForm.getCode();//获取前端传来校验码
        //获取后端校验码
        Object scode = session.getAttribute("code");
        if(scode == null || !scode.toString().equals(code)){
            return  Result.fail("验证码格式错误");
        }
        //3不一致报错

        //4一致，查询用户
        User user = query().eq("phone", phone).one();
        //5用户不存在，创建用户，保存session
        if(user == null){
            user = createuser(phone);
        }
        // 将 User 对象转换为 UserDTO 对象
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setNickName(user.getNickName());
        userDTO.setIcon(user.getIcon());

// 6.保存 UserDTO 到 session
        session.setAttribute("user", userDTO);

        return Result.ok(user.getId());
    }

    private User createuser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user_"+RandomUtil.randomString(10));
        save(user);
        return user;
    }

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1校验手机号

        //3不符合返回错误信息
        if(RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        //2符合生成校验码
        String code  = RandomUtil.randomNumbers(6);
        //保存验证码到session
        session.setAttribute("code", code);

        log.debug("发送验证码成功");
        log.debug(code);
        //返回验证码
        return Result.ok();
    }



}
