package com.zym.controller;

import com.zym.service.UserService;
import com.zym.spring.annotation.MyAutowired;
import com.zym.spring.annotation.MyController;
import com.zym.spring.annotation.MyRequestMapping;
import com.zym.spring.annotation.MyRequestParam;

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
    public String test2(@MyRequestParam("name") String name){
        return userServiceImpl.test2(name);
    }

    @MyRequestMapping("/user3")
    public String test3(){
        return "<h1>你好啊</h1>";
    }
}
