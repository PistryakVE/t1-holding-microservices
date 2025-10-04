//package org.example.aspect.annotation;
//
//import java.lang.annotation.ElementType;
//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;
//import java.lang.annotation.Target;
//
//@Target(ElementType.METHOD)
//@Retention(RetentionPolicy.RUNTIME)
//public @interface Cached {
//    String cacheName() default ""; // Имя кэша (опционально)
//    long ttl() default -1; // Время жизни в ms (переопределяет значение по умолчанию)
//    CacheType type() default CacheType.LOCAL; // Тип кэша
//}
//
//enum CacheType {
//    LOCAL, // Локальный in-memory кэш
//    REDIS  // Для будущего расширения
//}