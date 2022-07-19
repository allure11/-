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
    public String test2(String name) {
        return "hello " + name;
    }

}
