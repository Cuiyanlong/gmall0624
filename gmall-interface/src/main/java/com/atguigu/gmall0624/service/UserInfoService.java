package com.atguigu.gmall0624.service;

import com.atguigu.gmall0624.bean.UserAddress;
import com.atguigu.gmall0624.bean.UserInfo;

import java.util.List;

public interface UserInfoService {
    /**
     * 返回所有数据
     * @return
     */
    List<UserInfo> findAll();

    /**
     *
     * 根据任何条件查询UserInfo
     */
    List<UserInfo> findUserInfo(UserInfo userInfo);

    //模糊查询
    List<UserInfo> findByNickName(String nickName);

    //添加数据
    void addUser(UserInfo userInfo);

    /**
     * 修改数据
     * @return
     */
    void updateUser(UserInfo userInfo);

    /**
     * 删除数据
     * @return
     */
    void delUser(UserInfo userInfo);

    /**
     * 根据用户id去查询用户地址列表
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(String userId);
    /**
     * 根据用户去查询用户地址列表
     * @param userAddress
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(UserAddress userAddress);

}
