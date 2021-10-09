package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired(required = false)
    private UserMapper userMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    //rabbitmq需要发送短信 引入模版
    @Autowired(required = false)
    private AmqpTemplate amqpTemplate;

    //前缀用来区分不同的验证码
    static final String KEY_PREFIX = "user:verify:";

    static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /*
        校验数据是否可用
     */
    public Boolean checkUser(String data, Integer type) {
        User record = new User();
        //要校验的数据类型：1，用户名；2，手机；
        if(type == 1) {
            record.setUsername(data);
        }else if(type == 2) {
            record.setPhone(data);
        }else {
            return null;
        }
        return this.userMapper.selectCount(record) == 0;
    }

    public void sendVerifyCode(String phone) {
        //验证码为空直接返回
        if(StringUtils.isBlank(phone)) {
            return;
        }
        //生成验证码
        String code = NumberUtils.generateCode(6);

        //发送消息到rabbitMQ
        // 要与监听的交换机相同 监听的code在sms/listener/SmsListener.java
        //找到交换机的名称 exchange=leyou.sms.exchange  key(routingkey)
        Map<String, String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        this.amqpTemplate.convertAndSend("verifycode.sms","leyou.sms.exchange", msg);

        //验证码保存到redis中
        //设置前缀来区分不同的验证码
        this.redisTemplate.opsForValue().set(KEY_PREFIX + phone, code, 5, TimeUnit.MINUTES);
    }

    public void register(User user, String code) {
        //redis中存在验证码，查询redis中的验证码，上面怎么set 就怎么get
        String redisCode = this.redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());

        //校验验证码
        if(!StringUtils.equals(code, redisCode)) {
            return ;
        }

        //生成盐 CodeUtils.java
        String salt = CodecUtils.generateSalt();
        //盐要保存到数据库中
        user.setSalt(salt);

        //加盐加密  加密完成后重新设置给user
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));

        //新增用户 通用mapper的insert方法
        user.setId(null);
        user.setCreated(new Date());
        this.userMapper.insertSelective(user);
    }

    public User queryUser(String username, String password) {
        // 查询
        User record = new User();
        record.setUsername(username);
        User user = this.userMapper.selectOne(record);
        // 校验用户名
        if (user == null) {
            return null;
        }
        // 校验密码
        if (!user.getPassword().equals(CodecUtils.md5Hex(password, user.getSalt()))) {
            return null;
        }
        // 用户名密码都正确
        return user;

    }
}
