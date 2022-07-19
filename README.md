# myspring-framework
spring 框架的学习，以 spring-webmvc 为例，文档只对流程作简单描述，具体实现看代码注释

myspring-demo 作为一个测试项目，添加 myspring-framework 用作测试

## 第一步：完成基本的 ioc 和 di 功能
创建框架主体，普通 java 项目

首先定义基本的自定义注解，和创建 DispatcherServlet，框架目录如下：

![img.png](https://zym-notes.oss-cn-shenzhen.aliyuncs.com/img/img.png)

注意：在自定义注解中，注意添加 `@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)` ，否者后面无法通过 `isAnnotationPresent` 等方法查找到这个注解的存在

MyDispatcherSerclet 主要有如下几个步骤：

1. 加载配置文件，找到配置里面的包扫描路径
2. 找到所有需要 ioc 管理的类，以 `com.zym.controller.UserController` 的形式保存
3. 实例化
4. 属性赋值（依赖注入）
5. 将请求路径和其方法对应，保存到处理器映射器中（handlerMapping）

编写 doDispatcher 方法，处理请求

## 第二步：controller 的基本类型参数处理，Handler 类，基本类型返回值

先创建 `MyRequestParam` 参数注解,只能放到方法形参上，表示该位置需要传递参数，使用方法如下

```java
@MyRequestMapping("/user2")
public String test2(@MyRequestParam("name") String name){
    return userServiceImpl.test2(name);
}
```

创建 Handler 类，构造方法中需要调用封装参数和其位置的方法

```java
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
```

将 handlerMapping 修改为 ArrayList 类型，用于保存 Handler

```java
/**
     * 处理器映射器，存放请求路径和对应的控制器方法
     */
private ArrayList<Handler> handlerMapping = new ArrayList<>();
```

doDispatcher 中需要对参数进行封装，执行目标方法需要传递参数，并得到返回值

如果返回值不为 null，则通过 response 输出到前端页面

```java
Object invoke = handler.method.invoke(handler.controller, parameters);
if (invoke != null) {
    resp.setCharacterEncoding("UTF-8");
    resp.setHeader("content-type","text/html;charset=UTF-8");
    resp.getWriter().write(invoke.toString());
}
```



