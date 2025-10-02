package org.example.aspect;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.example.aspect.annotation.Cached;
import org.example.aspect.dto.CacheEntry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
public class CachedAspect {

    private static final Logger logger = LoggerFactory.getLogger(CachedAspect.class);

    @Value("${cache.default-ttl:300000}") // 5 минут по умолчанию
    private long defaultTtl;

    // Хранилище кэшей: Map<CacheName, Map<Key, CacheEntry>>
    private final Map<String, Map<Object, CacheEntry>> cacheStore = new ConcurrentHashMap<>();

    // Планировщик для очистки просроченных записей
    private final ScheduledExecutorService cleanupScheduler = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void init() {
        // Запускаем очистку каждые 30 секунд
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 30, 30, TimeUnit.SECONDS);
        logger.info("CachedAspect initialized with default TTL: {} ms", defaultTtl);
    }

    @PreDestroy
    public void destroy() {
        cleanupScheduler.shutdown();
        logger.info("CachedAspect cleanup scheduler stopped");
    }

    @Pointcut("@annotation(org.example.aspect.annotation.Cached)")
    public void annotatedMethod() {}

    @Around("annotatedMethod() && @annotation(cached)")
    public Object cacheMethodResult(ProceedingJoinPoint joinPoint, Cached cached) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String cacheName = getCacheName(cached, joinPoint);
        Object cacheKey = generateCacheKey(joinPoint);
        long ttl = cached.ttl() > 0 ? cached.ttl() : defaultTtl;

        logger.debug("Checking cache for method: {}, key: {}", methodName, cacheKey);

        // Проверяем кэш
        CacheEntry cachedResult = getFromCache(cacheName, cacheKey);
        if (cachedResult != null) {
            logger.info("CACHE HIT - Method: {}, Key: {}", methodName, cacheKey);
            return cachedResult.getValue();
        }

        logger.debug("CACHE MISS - Method: {}, Key: {}", methodName, cacheKey);

        // Выполняем оригинальный метод
        Object result = joinPoint.proceed();

        // Сохраняем результат в кэш
        if (result != null) {
            saveToCache(cacheName, cacheKey, result, ttl);
            logger.info("CACHE SAVED - Method: {}, Key: {}, TTL: {} ms", methodName, cacheKey, ttl);
        }

        return result;
    }

    private String getCacheName(Cached cached, ProceedingJoinPoint joinPoint) {
        if (!cached.cacheName().isEmpty()) {
            return cached.cacheName();
        }
        // Генерируем имя кэша на основе класса и метода
        return joinPoint.getSignature().getDeclaringType().getSimpleName() +
                "." + joinPoint.getSignature().getName();
    }

    private Object generateCacheKey(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        // Если метод принимает один аргумент - используем его как ключ
        if (args.length == 1) {
            Object arg = args[0];

            // Если это примитив или строка - используем как есть
            if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                return arg;
            }

            // Если у объекта есть ID поле - используем его
            try {
                var idField = arg.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                Object idValue = idField.get(arg);
                if (idValue != null) {
                    return idValue;
                }
            } catch (Exception e) {
                // Если нет ID поля, используем hashCode
            }

            // Используем hashCode объекта
            return arg.hashCode();
        }

        // Если несколько аргументов - используем их хэш
        return Arrays.hashCode(args);
    }

    private CacheEntry getFromCache(String cacheName, Object key) {
        Map<Object, CacheEntry> cache = cacheStore.get(cacheName);
        if (cache == null) {
            return null;
        }

        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        // Проверяем не истекло ли время
        if (entry.isExpired()) {
            cache.remove(key);
            logger.debug("Expired cache entry removed: {}/{}", cacheName, key);
            return null;
        }

        return entry;
    }

    private void saveToCache(String cacheName, Object key, Object value, long ttl) {
        Map<Object, CacheEntry> cache = cacheStore.computeIfAbsent(cacheName, k -> new ConcurrentHashMap<>());

        Instant expirationTime = Instant.now().plusMillis(ttl);
        CacheEntry entry = new CacheEntry(value, expirationTime, key.toString());

        cache.put(key, entry);
    }

    private void cleanupExpiredEntries() {
        int totalRemoved = 0;

        for (Map.Entry<String, Map<Object, CacheEntry>> cacheEntry : cacheStore.entrySet()) {
            String cacheName = cacheEntry.getKey();
            Map<Object, CacheEntry> cache = cacheEntry.getValue();

            int removed = 0;
            var iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    iterator.remove();
                    removed++;
                }
            }
            totalRemoved += removed;

            if (removed > 0) {
                logger.debug("Cleaned {} expired entries from cache: {}", removed, cacheName);
            }
        }

        if (totalRemoved > 0) {
            logger.info("Total expired cache entries removed: {}", totalRemoved);
        }
    }

    // Методы для управления кэшем
    public void evictCache(String cacheName, Object key) {
        Map<Object, CacheEntry> cache = cacheStore.get(cacheName);
        if (cache != null) {
            cache.remove(key);
            logger.info("Cache evicted: {}/{}", cacheName, key);
        }
    }

    public void clearCache(String cacheName) {
        Map<Object, CacheEntry> cache = cacheStore.remove(cacheName);
        if (cache != null) {
            logger.info("Cache cleared: {}", cacheName);
        }
    }

    public void clearAllCaches() {
        int size = cacheStore.size();
        cacheStore.clear();
        logger.info("All caches cleared ({} cache(s) removed)", size);
    }
}