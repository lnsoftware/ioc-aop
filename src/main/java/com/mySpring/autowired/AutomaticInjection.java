package com.mySpring.autowired;


import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by seven on 2018/5/12.
 * 自动注入类
 */
public class AutomaticInjection {


    /**
     * 将bean实例化，并且填充到map中
     * @param key
     * @param mmp
     */
    public static void automaticInjection(String key, Map mmp) {
        try {
            // 获取 com.mySpring 下所有 class 文件的 list 集合
            List<Class> list = GetClass.getClassList(key);

            for (Class classes : list) {
                // judgeMap 用于判断是否有循环依赖
                Map<String, Object> judgeMap = new HashMap();
                injection(mmp, classes, judgeMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 注入并判断是否循环依赖
     *
     * @param mmp       存放实例化后的 bean 的 map
     * @param classes   需要实例化的类的class
     * @param judgeMap  一个flag，因为是递归调用此方法的，判断是否有循环依赖，在外层for循环初始化为空
     *                  在 处理 第一个 class 的时候，会向里面添加，后面时递归调用，出来第二个 class 的时候 看是否存在
     * @throws Exception
     */
    private static void injection(Map mmp, Class classes, Map judgeMap)
            throws Exception {
        // 只有在 class 类上有 @MyBean 注解才会 去操作 和 解决 循环依赖
        boolean isExist = classes.isAnnotationPresent(MyBean.class);
        //如果该注解存在
        if (isExist) {
            MyBean myBean = (MyBean) classes.getAnnotation(MyBean.class);
            // 这里是 bean 的名称
            // @MyBean("classesService") 里面的 classService
            String beanName = myBean.value();
            if (null == judgeMap.get(beanName))
                judgeMap.put(beanName, true);
            else { //又返回依赖他
                throw new Exception("循环依赖");
            }

            // 还没有被注入
            if (null == mmp.get(beanName)) {
                // 获得 bean 实例， 比如 ClassService 类的 实例
                Object beanObj = classes.newInstance();

                Field[] fields = classes.getDeclaredFields();
                boolean fieldExist;
                for (Field field : fields) {
                    // 检查 该 属性 field 上是否有 @MyAutowired 注解
                    // 如果有 说明需要 被装配
                    fieldExist = field.isAnnotationPresent(MyAutowired.class);
                    // 存在需要注入的属性
                    if (fieldExist) {
                        String classtype = field.getGenericType().toString();
                        String filedClassName = classtype.substring(6);
                        // 需要被依赖注入的 类型 如 TeacherService
                        Class fieldClass = Class.forName(filedClassName);

                        //强制设置值 破坏了封装性
                        field.setAccessible(true);

                        // 判断 需要 被注入的 字段 的类型 的 class 上面是否有 @MyBean 注解
                        // 如果 有 @MyBean 注解 说明 要依赖 其他的 Bean
                        // 否则 不依赖其他 的 Bean, 直接实例化 就 可以
                        if (fieldClass.isAnnotationPresent(MyBean.class)) {
                            //该属性依赖其它Bean，递归实例化被依赖的bean
                            MyBean tbean = (MyBean) fieldClass.getAnnotation(MyBean.class);
                            injection(mmp, fieldClass, judgeMap);
                            // 设置 属性的 值， 是从 beanMap 中的值
                            field.set(beanObj, mmp.get(tbean.value()));

                        } else {
                            //该属性不依赖其它Bean，直接实例化自己
                            Object object = fieldClass.newInstance();
                            field.set(beanObj, object);
                        }
                    }
                }
                mmp.put(beanName, beanObj);
            }

        }
    }

    /**
     * 重新填充被cglib代理生成属性
     *
     * @param mmp
     * @param classes
     * @param obj
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static void reinjection(Map mmp, Class classes, Object obj) throws ClassNotFoundException,
            IllegalAccessException, InstantiationException {
        Field[] fields = classes.getDeclaredFields();
        boolean fieldExist;
        for (Field field : fields) {
            fieldExist = field.isAnnotationPresent(MyAutowired.class);

            if (fieldExist) {
                String classtype = field.getGenericType().toString();
                String filedClassName = classtype.substring(6);
                Class fieldClass = Class.forName(filedClassName);
                //强制设置值 破坏了封装性
                field.setAccessible(true);

                if (fieldClass.isAnnotationPresent(MyBean.class)) {//该属性依赖其它Bean
                    MyBean tbean = (MyBean) fieldClass.getAnnotation(MyBean.class);
                    field.set(obj, mmp.get(tbean.value()));

                } else { //该属性不依赖其它Bean
                    Object object = fieldClass.newInstance();
                    field.set(obj, object);
                }
            }
        }
    }

}
