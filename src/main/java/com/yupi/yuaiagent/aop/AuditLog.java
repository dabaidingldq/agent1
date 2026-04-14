package com.yupi.yuaiagent.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    String operationName();

    String operationModule() default "default";
}