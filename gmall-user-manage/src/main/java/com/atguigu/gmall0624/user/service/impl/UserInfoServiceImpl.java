package com.atguigu.gmall0624.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0624.bean.UserAddress;
import com.atguigu.gmall0624.bean.UserInfo;
import com.atguigu.gmall0624.config.RedistUtil;
import com.atguigu.gmall0624.service.UserInfoService;
import com.atguigu.gmall0624.user.mapper.UserAddressMapper;
import com.atguigu.gmall0624.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedistUtil redistUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24*7;

    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserInfo> findUserInfo(UserInfo userInfo) {
        return null;
    }

    @Override
    public List<UserInfo> findByNickName(String nickName) {
        return null;
    }

    @Override
    public void addUser(UserInfo userInfo) {

    }

    @Override
    public void updateUser(UserInfo userInfo) {

    }

    @Override
    public void delUser(UserInfo userInfo) {

    }

    //根据用户id去查询用户地址列表
    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        //select * from userAddress where userId = ?
        //查询那张表就用哪张表的mapper
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return userAddressMapper.select(userAddress);
    }

    //根据用户去查询用户地址列表
    @Override
    public List<UserAddress> findUserAddressListByUserId(UserAddress userAddress) {
        return userAddressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //select * from userInfo where loginName = atguigu and passwd = 123
        //将密码进行加密
        String passwd = userInfo.getPasswd();
        String newPassword = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(newPassword);

        UserInfo info = userInfoMapper.selectOne(userInfo);
        if (info!=null){

            //获取Jedis
            Jedis jedis = redistUtil.getJedis();
            //定义key user:userId:info
            String userKey = userKey_prefix+info.getId()+userinfoKey_suffix;
            //考虑到数据类型
            jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(info));

            jedis.close();
            return info;
        }
        return null;

    }

    @Override
    public UserInfo verify(String userId) {
        // 获取Jedis
        Jedis jedis = redistUtil.getJedis();
        // 定义key user:userId:info
        String userKey = userKey_prefix+userId+userinfoKey_suffix;
        // 获取缓存数据
        String userJson = jedis.get(userKey);
        if (!StringUtils.isEmpty(userJson)){
            // userJson 转换成对象
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
            return userInfo;
        }
        jedis.close();

        return null;
    }
}
