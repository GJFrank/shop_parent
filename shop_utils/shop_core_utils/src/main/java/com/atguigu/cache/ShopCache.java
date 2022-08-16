package com.atguigu.cache;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ShopCache {
    String prefix() default "cache";
}
