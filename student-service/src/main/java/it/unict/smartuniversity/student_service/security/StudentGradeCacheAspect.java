package it.unict.smartuniversity.student_service.security;

import it.unict.smartuniversity.student_service.dto.GradeDTO;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class StudentGradeCacheAspect {

    // Cache thread-safe in-memory: associa a ciascuno username la lista dei suoi voti (libretto)
    private static final Map<String, List<GradeDTO>> gradeCache = new ConcurrentHashMap<>();

    /**
     * Intercetta l'esecuzione del recupero voti per memorizzarli in cache.
     * Pattern applicato: Cache Proxy / Aspect-Oriented Caching.
     */
    @Around("execution(* it.unict.smartuniversity.student_service.service.StudentService.getLibrettoByUsername(String)) && args(username)")
    public Object cacheGrades(ProceedingJoinPoint joinPoint, String username) throws Throwable {
        if (gradeCache.containsKey(username)) {
            System.out.println("⚡ [AspectJ Cache] Hit! Restituisco il libretto dello studente '" + username + "' direttamente dalla cache.");
            return gradeCache.get(username);
        }

        System.out.println("💾 [AspectJ Cache] Miss. Interrogo PostgreSQL per lo studente '" + username + "' e memorizzo il risultato.");
        Object result = joinPoint.proceed();
        
        if (result instanceof List) {
            gradeCache.put(username, (List<GradeDTO>) result);
        }
        return result;
    }

    /**
     * Metodo pubblico per invalidare (svuotare) la cache di uno specifico studente.
     * Invocato dal Listener di RabbitMQ quando arriva un nuovo voto!
     */
    public static void evictCacheForUser(String username) {
        if (gradeCache.containsKey(username)) {
            gradeCache.remove(username);
            System.out.println("🗑️ [AspectJ Cache] Invalidazione (Eviction) della cache eseguita con successo per lo studente '" + username + "'.");
        }
    }
}