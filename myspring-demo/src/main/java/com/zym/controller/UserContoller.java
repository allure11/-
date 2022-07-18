package com.zym.controller;

import com.zym.service.UserService;
import com.zym.spring.annotation.MyAutowired;
import com.zym.spring.annotation.MyController;
import com.zym.spring.annotation.MyRequestMapping;

/**
 * 控制器
 * @author zym
 */
@MyController
@MyRequestMapping("/users")
public class UserContoller {

    @MyAutowired
    private UserService userServiceImpl;

    @MyRequestMapping("/user")
    public void test() {
        userServiceImpl.test();
    }

    @MyRequestMapping("/user2")
    public void test2(){
        userServiceImpl.test2();
    }

}
