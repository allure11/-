package com.zym.spring.servlet;

import com.zym.spring.annotation.*;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义 DispatcherServlet
 * 设计模式：注册式单例模式，策略模式，工厂模式
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
    private ArrayList<Handler> handlerMapping = new ArrayList<>();

    /**
     * ioc 容器
     */
    private HashMap ioc = new HashMap<String, Object>(8);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    public void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        /** 在处理器映射器中查找对应的处理器方法 **/
        Handler handler = getHandler(req);
        // 没找到对应的处理器，报错 404
        if (handler == null) {
            resp.getWriter().write("404 not found");
            throw new RuntimeException("404 not found");
        }

        /** 参数处理 **/
        // 获取前端传进的所有参数，这个值是字符串数组类型的，后面需要转成字符串使用
        Map<String, String[]> parameterMap = req.getParameterMap();
        // 获取 handler 方法的参数位置
        HashMap<String, Integer> parametersIndexMapping = handler.parametersIndexMapping;
        // 获取 handler 方法的参数类型
        Class<?>[] parameterTypes = handler.method.getParameterTypes();
        // 封装参数
        Object[] parameters = new Object[parametersIndexMapping.size()];
        // 循环接收到的参数，封装到对应的形参中
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {

            /** 获取参数值，将之前的字符串数组转换成字符串 **/
            String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
            // 判断是否需要名为 entry.getKey() 的参数
            if (!parametersIndexMapping.containsKey(entry.getKey())){continue;}
            // 获取参数位置
            int size = parametersIndexMapping.get(entry.getKey());
            // 参数类型转换
            parameters[size]= convert(parameterTypes[size], value);
        }
        // 参数中添加 request 和 response 对象
        if (parametersIndexMapping.containsKey(HttpServletRequest.class.getName())){
            parameters[parametersIndexMapping.get(HttpServletRequest.class.getName())] = req;
        }
        if (parametersIndexMapping.containsKey(HttpServletResponse.class.getName())){
            parameters[parametersIndexMapping.get(HttpServletResponse.class.getName())] = req;
        }

        /** 执行目标方法 **/
        try {
            Object invoke = handler.method.invoke(handler.controller, parameters);
            if (invoke != null) {
                resp.setCharacterEncoding("UTF-8");
                resp.setHeader("content-type","text/html;charset=UTF-8");
                resp.getWriter().write(invoke.toString());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据请求路径，获取对应的处理器方法
     * @param req
     * @return
     */
    private Handler getHandler(HttpServletRequest req) {
        // 获取请求路径
        String url = req.getRequestURL().toString().split(req.getContextPath())[1];
        if (handlerMapping.size() == 0 || handlerMapping == null) {
            return null;
        }
        // 循环 handlerMapping，找到对应的处理器方法
        for (Handler handler : handlerMapping) {
            // url 的正则表达式匹配
            Matcher matcher = handler.pattern.matcher(url);
            // 匹配不成功，继续下一个
            if (!matcher.matches()) {continue;}
            // 匹配成功，返回 handler
            return handler;
        }
        // 没有，返回 null
        return null;
    }

    /**
     * 初始化
     * @throws ServletException
     */
    @Override
    public void init() throws ServletException {
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
                // 请求路径
                String url = baseUrl + "/" + myRequestMapping.value();
                Handler handler = new Handler(entry.getValue(), method, Pattern.compile(url.replaceAll("/+","/")));
                // 将 handler 存入 handlerMapping
                handlerMapping.add(handler);
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
     * 值类型：String （ com.zym.controller.UserController ）
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
                    }// TODO 其他类型的 bean，以后补充
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
            // 获取配置文件中的包扫描路径
            String basePackage = properties.getProperty("basePackage");
            packagePath = basePackage.replace(".", "/");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 参数类型装换
     */
    private Object convert(Class<?> type, String value) {
        if (type == String.class){
            return value;
        } else if (type == int.class || type == Integer.class){
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class){
            return Long.parseLong(value);
        } else if (type == double.class || type == Double.class){
            return Double.parseDouble(value);
        } else if (type == boolean.class || type == Boolean.class){
            return Boolean.parseBoolean(value);
        } else {
            throw new RuntimeException("参数类型转换异常");
        }
    }

    /**
     * 字符串首字母小写
     */
    public String firstLetterLowercase(String str){
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
     * Handler 处理器适配器
     */
    private class Handler{
        /** 保存控制器实例 **/
        protected Object controller;
        /** 保存方法 **/
        protected Method method;
        /** 保存请求路径 **/
        protected Pattern pattern;
        /** 保存方法参数名称和其位置 **/
        protected HashMap<String, Integer> parametersIndexMapping;

        protected Handler(Object controller, Method method, Pattern pattern) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            parametersIndexMapping = new HashMap<String, Integer>();
            putParametersIndexMapping(method);
        }

        /**
         * 将方法参数名称和其位置存入 parametersIndexMapping
         */
        private void putParametersIndexMapping(Method method){
            // 获取所有参数及其注解信息
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            // 循环保存参数名称和位置
            for (int i = 0; i < parameterAnnotations.length; i++) {
                for (Annotation annotation : parameterAnnotations[i]) {
                    // 判断是否是 MyRequestParam 注解
                    if (annotation instanceof MyRequestParam){
                        String name = ((MyRequestParam) annotation).value();
                        parametersIndexMapping.put(name, i);
                    }
                }
            }
            // 获取所有参数类型信息
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                // 判断是否是 HttpServletRequest 或 HttpServletResponse 类型
                if (parameterType == HttpServletRequest.class){
                    parametersIndexMapping.put(HttpServletRequest.class.getName(), i);
                } else if (parameterType == HttpServletResponse.class){
                    parametersIndexMapping.put(HttpServletResponse.class.getName(), i);
                }
            }
        }
    }
}
