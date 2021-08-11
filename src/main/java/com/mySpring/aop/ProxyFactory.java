package com.mySpring.aop;

import com.mySpring.aop.advice.Advice;
import com.mySpring.aop.annotation.PointCut;

import java.util.Map;

/**
 * Created by seven on 2018/5/12.
 * 对Bean进行AOP操作
 */
public class ProxyFactory {

    /**
     * 将bean生成代理
     *
     * @param map
     */
    public static void makeProxyBean(Map<String, Object> map) {
        for (String key : map.keySet()) {

            AopProxy aopProxy = new AopProxy();

            // 获取 每个 Class 类型，如 ClassService
            // 并判断 是否有 @PointCut 注解
            Object o = map.get(key);
            Class classes = o.getClass();
            //判断是否有类注解
            if (classes.isAnnotationPresent(PointCut.class)) {
                // 获取 PointCut 的具体类型
                // 如 @PointCut("com.mySpring.aop.aspect.AfterAspect")
                PointCut pointCut = (PointCut) classes.getAnnotation(PointCut.class);
                String classPath = pointCut.value();
                try {
                    // 加载 Advice 类 并获取 实例
                    Advice advice = (Advice) Class.forName(classPath).newInstance();
                    // 设置 通知 的 类型
                    aopProxy.setClassAdvice(advice);

                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
            // map 中的
            // key 是 @MyBean 注解中的 value 如 "classService"
            // value 是 cglib 创建的 代理类
            map.put(key, aopProxy.getProxy(classes));
        }
    }
}
