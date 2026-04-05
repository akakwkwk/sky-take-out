package com.sky.annotation;


import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)// 注解所修饰的元素为方法
@Retention(RetentionPolicy.RUNTIME)// 注解所修饰的元素在运行时保留
public @interface AutoFill {

    OperationType value();// 数据库操作类型 UPDATE INSERT

}
