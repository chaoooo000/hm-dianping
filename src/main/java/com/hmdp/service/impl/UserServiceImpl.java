package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    // 1. ⚠️ 修改为 StringRedisTemplate
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }

        //2校验验证码
        String code = loginForm.getCode();
        // ⚠️ 统一使用 stringRedisTemplate
        String scode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if(scode == null || !scode.equals(code)){
            return  Result.fail("验证码格式错误");
        }

        //4一致，查询用户
        User user = query().eq("phone", phone).one();
        //5用户不存在，创建用户
        if(user == null){
            user = createuser(phone);
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setNickName(user.getNickName());
        userDTO.setIcon(user.getIcon());

        //随机生成token令牌
        String token  = UUID.randomUUID().toString();

        // 3. ⚠️ 解决 ClassCastException 隐患：将 UserDTO 里面的所有字段（如 Long 类型的 id）强制转为 String
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        // 保存用户到 redis
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        // ⚠️ 这里之前写的是 LOGIN_CODE_TTL，建议改为 Token 专属的过期时间 LOGIN_USER_TTL
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
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
        if(RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        String code  = RandomUtil.randomNumbers(6);

        // 4. ⚠️ 统一使用 LOGIN_CODE_KEY 常量，保持前后一致
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        log.debug("发送验证码成功");
        log.debug(code);
        return Result.ok();
    }

}