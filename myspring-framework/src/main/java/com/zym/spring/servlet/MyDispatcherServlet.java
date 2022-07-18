package com.zym.spring.servlet;

import com.zym.spring.annotation.MyAutowired;
import com.zym.spring.annotation.MyController;
import com.zym.spring.annotation.MyRequestMapping;
import com.zym.spring.annotation.MyService;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 自定义 DispatcherServlet
 * @author zym
 */
public class MyDispatcherServlet extends HttpServlet {

    /**
     * 需要扫描的根路径
     */
    private String realPath = "";
    /**
     * 需要扫描的包路径
     */
    private String packagePath = "";

    /**
     * 需要被 ioc 管理的 bean 类的类名
     */
    private ArrayList<Class> beanList = new ArrayList<Class>();

    /**
     * 处理器映射器，存放请求路径和对应的控制器方法
     */
    private HashMap<String, Method> handlerMapping = new HashMap<>();

    /**
     * ioc 容器
     */
    private HashMap ioc = new HashMap<String, Object>(8);


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp);
    }

    public void dispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 获取请求路径
        String url = req.getRequestURL().toString().split(req.getContextPath())[1];
        // 在处理器映射器中查找对应的处理器方法
        Method method = handlerMapping.get(url);
        if (method == null) {
            resp.getWriter().write("404 not found");
            throw new RuntimeException("404 not found");
        }
        try {
            method.invoke(ioc.get(firstLetterLowercase(method.getDeclaringClass().getSimpleName())));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化
     * @throws ServletException
     */
    @Override
    public void init() throws ServletException {
        System.out.println("aaa");
        getConfiguration();
        getFile(packagePath);
        initIoc();
        initAutowired();
        initHandlerMapping();

    }

    /**
     * 属性赋值
     */
    public void initAutowired(){
        // 循环获取 ioc 中的所有 bean
        for (Object o : ioc.entrySet()) {
            Map.Entry<String, Object> entry = (Map.Entry) o;
            Class<?> clazz = entry.getValue().getClass();

            // 循环获取 bean 中的所有属性
            for (Field field : clazz.getDeclaredFields()) {
                // 看是否有 MyAutowired 注解
                if (field.isAnnotationPresent(MyAutowired.class)){
                    field.setAccessible(true);
                    try {
                        // 获取 MyAutowired 注解的 value 值
                        String value = field.getAnnotation(MyAutowired.class).value();
                        // 没有指定 value 值，则根据类型自动注入
                        if ("".equals(value) || value == null){
                            value = firstLetterLowercase(field.getType().getSimpleName());
                        }
                        // 在容器中查找对应的名称的 bean 赋值
                        field.set(entry.getValue(), ioc.get(value));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * 初始化处理器映射器
     */
    public void initHandlerMapping(){
        for (Object o : ioc.entrySet()) {
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) o;
            Class clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(MyController.class)){
                continue;
            }
            String baseUrl = "";
            /** 查看类上是否有 MyRequestMapping 指定公共路径  **/
            if (clazz.isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping myRequestMapping = (MyRequestMapping) clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = myRequestMapping.value();
            }
            /** 获取方法，看方法上是否有 MyRequestMapping 注解，有就将此方法对应存入处理器映射器 **/
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)){
                    continue;
                }
                MyRequestMapping myRequestMapping = method.getAnnotation(MyRequestMapping.class);
                String url = myRequestMapping.value();
                handlerMapping.put((baseUrl+"/"+url).replaceAll("/+", "/"), method);
            }
        }
    }

    /**
     * 实例化，方进 ioc
     */
    public void initIoc() {
        for (Class clazz : beanList) {
            String value = null;
            // 获取自定义 beanName
            if (clazz.isAnnotationPresent(MyController.class)) {
                // 获取自定义注解的value
                value = ((MyController) clazz.getAnnotation(MyController.class)).value();
            } else if (clazz.isAnnotationPresent(MyService.class)) {
                value =((MyService) clazz.getAnnotation(MyService.class)).value();
            }
            // 没有自定义 beanName 默认类名首字母小写
            if (value == null || "".equals(value)) {
                value = firstLetterLowercase(clazz.getSimpleName());
            }
            try {
                ioc.put(value, clazz.newInstance());
                // TODO 将类对应的接口也加入到 ioc 中,(感觉不够完善)
                for (Class anInterface : clazz.getInterfaces()) {
                    ioc.put(firstLetterLowercase(anInterface.getSimpleName()), clazz.newInstance());
                }
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 将需要被管理的 bean 加入 beanList
     */
    public void getFile(String packageUrl){
        String url = realPath + packageUrl;
        // 循环扫描包及其子包下的所有类
        for (File file : new File(url).listFiles()) {
            if (file.isFile()){
                String classUrl = packageUrl + "/" + file.getName();
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(classUrl.replace(".class", "").replace("/", "."));
                    // 将需要被管理的 bean 加入 beanList
                    if (clazz.isAnnotationPresent(MyController.class)){
                        beanList.add(clazz);
                    } else if (clazz.isAnnotationPresent(MyService.class)){
                        beanList.add(clazz);
                    }// TODO 其他类型的 bean
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

            } else {
                getFile(packageUrl + "/" + file.getName());
            }
        }
    }

    /**
     * 获取配置文件信息
     */
    public void getConfiguration(){
        try {
            // 获取配置文件路径
            realPath = this.getClass().getResource("/").getPath();
            // 读取配置文件
            FileInputStream fileInputStream = new FileInputStream(new File(realPath + "applicationContext.properties"));
            Properties properties = new Properties();
            properties.load(fileInputStream);
            String basePackage = properties.getProperty("basePackage");
            packagePath = basePackage.replace(".", "/");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 字符串首字母小写
     */
    public String firstLetterLowercase(String str){
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
