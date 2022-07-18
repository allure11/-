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

