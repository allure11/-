package com.zym.service.impl;

import com.zym.service.UserService;
import com.zym.spring.annotation.MyService;

/**
 * 业务类实现
 * @author zym
 */
@MyService
public class UserServiceImpl implements UserService {

    @Override
    public void test() {
        System.out.println("hello world");
    }

    @Override
    public void test2() {
        System.out.println("哈哈哈哈哈哈哈，成了！！！！！");
    }

}
