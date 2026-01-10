package com.univ.memoir.core.aop;

import com.univ.memoir.core.util.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final int MAX_STRING_LENGTH = 100;
    private static final long SLOW_METHOD_THRESHOLD_MS = 1000;

    /**
     * @Loggable 어노테이션이 붙은 메서드만 상세 로깅
     * - 파라미터, 반환값, 실행시간 모두 로깅
     * - 민감 정보 마스킹
     */
    @Around("@annotation(com.univ.memoir.core.aop.Loggable)")
    public Object logLoggableMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        Object[] args = joinPoint.getArgs();
        String parameters = formatParameters(args);
        log.info("→ Enter: {}.{} [{}]", className, methodName, parameters);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            long executionTime = stopWatch.getLastTaskTimeMillis();

            String returnValue = formatReturnValue(result);
            log.info("← Exit: {}.{} [{}ms] Return: {}",
                    className, methodName, executionTime, returnValue);

            return result;
        } catch (Exception ex) {
            stopWatch.stop();
            long executionTime = stopWatch.getLastTaskTimeMillis();

            log.error("✗ Error: {}.{} [{}ms] Exception: {}",
                    className, methodName, executionTime, ex.getMessage());

            throw ex;
        }
    }

    /**
     * Service/Repository 메서드 로깅
     * - @Loggable이 붙은 메서드는 제외 (중복 방지)
     * - 느린 메서드(1초 이상)만 경고 로깅
     * - 에러는 항상 로깅
     * - 정상 동작은 로깅하지 않음
     */
    @Around("(@within(org.springframework.stereotype.Service) || " +
            "@within(org.springframework.stereotype.Repository)) && " +
            "!@annotation(com.univ.memoir.core.aop.Loggable)")  // ← 이 부분 추가!
    public Object logServiceAndRepository(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            long executionTime = stopWatch.getLastTaskTimeMillis();

            // 느린 메서드만 경고 로깅
            if (executionTime > SLOW_METHOD_THRESHOLD_MS) {
                log.warn("⚠ SLOW: {}.{} [{}ms]", className, methodName, executionTime);
            }

            return result;
        } catch (Exception ex) {
            stopWatch.stop();
            long executionTime = stopWatch.getLastTaskTimeMillis();

            log.error("✗ Error: {}.{} [{}ms] {}",
                    className, methodName, executionTime, ex.getMessage());

            throw ex;
        }
    }

    /**
     * 메서드 매개변수 포맷팅
     * - 민감한 정보 마스킹
     * - 길이 제한
     */
    private String formatParameters(Object[] args) {
        if (args == null || args.length == 0) {
            return "no parameters";
        }

        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) {
                        return "null";
                    }

                    // String 타입만 상세 로깅 + 마스킹
                    String stringArg;
                    if (arg instanceof String) {
                        stringArg = SensitiveDataMasker.mask((String) arg);
                    } else {
                        // 객체는 클래스명만 로깅 (toString() 호출 방지)
                        stringArg = arg.getClass().getSimpleName();
                    }

                    // 길이 제한
                    if (stringArg.length() > MAX_STRING_LENGTH) {
                        stringArg = stringArg.substring(0, MAX_STRING_LENGTH) + "...";
                    }

                    return stringArg;
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * 반환값 포맷팅
     * - 민감한 정보 마스킹
     * - 길이 제한
     */
    private String formatReturnValue(Object result) {
        if (result == null) {
            return "null";
        }

        // String 타입만 상세 로깅 + 마스킹
        String stringResult;
        if (result instanceof String) {
            stringResult = SensitiveDataMasker.mask((String) result);
        } else {
            // 객체는 클래스명만 로깅 (toString() 호출 방지)
            stringResult = result.getClass().getSimpleName();
        }

        // 길이 제한
        if (stringResult.length() > MAX_STRING_LENGTH) {
            stringResult = stringResult.substring(0, MAX_STRING_LENGTH) + "...";
        }

        return stringResult;
    }
}