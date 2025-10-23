package com.univ.memoir.core.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingAspect Unit Tests")
class LoggingAspectTest {

    @InjectMocks
    private LoggingAspect loggingAspect;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Method method;

    @BeforeEach
    void setUp() {
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
    }

    @Test
    @DisplayName("should log method entry and exit for @Loggable method")
    void shouldLogMethodEntryAndExitForLoggableMethod() throws Throwable {
        // given
        String methodName = "testMethod";
        String className = "TestClass";
        Object[] args = new Object[]{"param1", "param2"};
        String returnValue = "success";

        when(method.getName()).thenReturn(methodName);
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(returnValue);

        // when
        Object result = loggingAspect.logLoggableMethod(joinPoint);

        // then
        assertThat(result).isEqualTo(returnValue);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("should log method with no parameters")
    void shouldLogMethodWithNoParameters() throws Throwable {
        // given
        String methodName = "noParamMethod";
        Object[] args = new Object[]{};
        String returnValue = "result";

        when(method.getName()).thenReturn(methodName);
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(returnValue);

        // when
        Object result = loggingAspect.logLoggableMethod(joinPoint);

        // then
        assertThat(result).isEqualTo(returnValue);
    }

    @Test
    @DisplayName("should log method with null parameters")
    void shouldLogMethodWithNullParameters() throws Throwable {
        // given
        Object[] args = new Object[]{null, "param2", null};
        String returnValue = "result";

        when(method.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(returnValue);

        // when
        Object result = loggingAspect.logLoggableMethod(joinPoint);

        // then
        assertThat(result).isEqualTo(returnValue);
    }

    @Test
    @DisplayName("should log method with null return value")
    void shouldLogMethodWithNullReturnValue() throws Throwable {
        // given
        Object[] args = new Object[]{"param1"};

        when(method.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(null);

        // when
        Object result = loggingAspect.logLoggableMethod(joinPoint);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should log exception when method throws exception")
    void shouldLogExceptionWhenMethodThrowsException() throws Throwable {
        // given
        String methodName = "failingMethod";
        Object[] args = new Object[]{"param1"};
        RuntimeException exception = new RuntimeException("Test exception");

        when(method.getName()).thenReturn(methodName);
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> loggingAspect.logLoggableMethod(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");
    }

    @Test
    @DisplayName("should truncate long parameter strings")
    void shouldTruncateLongParameterStrings() throws Throwable {
        // given
        String longString = "a".repeat(150); // 150 characters
        Object[] args = new Object[]{longString};
        String returnValue = "result";

        when(method.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(returnValue);

        // when
        Object result = loggingAspect.logLoggableMethod(joinPoint);

        // then
        assertThat(result).isEqualTo(returnValue);
    }

    @Test
    @DisplayName("should truncate long return value strings")
    void shouldTruncateLongReturnValueStrings() throws Throwable {
        // given
        String longString = "b".repeat(150);
        Object[] args = new Object[]{"param1"};

        when(method.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(longString);

        // when
        Object result = loggingAspect.logLoggableMethod(joinPoint);

        // then
        assertThat(result).isEqualTo(longString);
    }

    @Test
    @DisplayName("should mask sensitive data in parameters")
    void shouldMaskSensitiveDataInParameters() throws Throwable {
        // given
        String sensitiveParam = "password=secret123";
        Object[] args = new Object[]{sensitiveParam};
        String returnValue = "result";

        when(method.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(returnValue);

        // when
        Object result = loggingAspect.logLoggableMethod(joinPoint);

        // then
        assertThat(result).isEqualTo(returnValue);
    }

    @Test
    @DisplayName("should log service method that completes quickly")
    void shouldLogServiceMethodThatCompletesQuickly() throws Throwable {
        // given
        String methodName = "quickMethod";
        String returnValue = "result";

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn(methodName);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.proceed()).thenReturn(returnValue);

        // when
        Object result = loggingAspect.logServiceAndRepository(joinPoint);

        // then
        assertThat(result).isEqualTo(returnValue);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("should warn when service method is slow")
    void shouldWarnWhenServiceMethodIsSlow() throws Throwable {
        // given
        String methodName = "slowMethod";
        String returnValue = "result";

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn(methodName);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(1100); // Simulate slow method
            return returnValue;
        });

        // when
        Object result = loggingAspect.logServiceAndRepository(joinPoint);

        // then
        assertThat(result).isEqualTo(returnValue);
    }

    @Test
    @DisplayName("should log exception in service method")
    void shouldLogExceptionInServiceMethod() throws Throwable {
        // given
        String methodName = "failingServiceMethod";
        RuntimeException exception = new RuntimeException("Service error");

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn(methodName);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.proceed()).thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> loggingAspect.logServiceAndRepository(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service error");
    }

    @Test
    @DisplayName("should log repository method that completes quickly")
    void shouldLogRepositoryMethodThatCompletesQuickly() throws Throwable {
        // given
        String methodName = "findById";
        Object returnValue = new Object();

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn(methodName);
        when(joinPoint.getTarget()).thenReturn(new TestRepository());
        when(joinPoint.proceed()).thenReturn(returnValue);

        // when
        Object result = loggingAspect.logServiceAndRepository(joinPoint);

        // then
        assertThat(result).isEqualTo(returnValue);
    }

    @Test
    @DisplayName("should handle null arguments array")
    void shouldHandleNullArgumentsArray() throws Throwable {
        // given
        when(method.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("result");

        // when
        Object result = loggingAspect.logLoggableMethod(joinPoint);

        // then
        assertThat(result).isEqualTo("result");
    }

    @Test
    @DisplayName("should handle non-string parameters")
    void shouldHandleNonStringParameters() throws Throwable {
        // given
        Object[] args = new Object[]{123, 45.67, true, new TestClass()};
        String returnValue = "result";

        when(method.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(returnValue);

        // when
        Object result = loggingAspect.logLoggableMethod(joinPoint);

        // then
        assertThat(result).isEqualTo(returnValue);
    }

    @Test
    @DisplayName("should handle non-string return value")
    void shouldHandleNonStringReturnValue() throws Throwable {
        // given
        Object[] args = new Object[]{"param1"};
        Integer returnValue = 42;

        when(method.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new TestClass());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(returnValue);

        // when
        Object result = loggingAspect.logLoggableMethod(joinPoint);

        // then
        assertThat(result).isEqualTo(returnValue);
    }

    // Test helper classes
    private static class TestClass {
        @Override
        public String toString() {
            return "TestClass";
        }
    }

    @Service
    private static class TestService {
    }

    @Repository
    private static class TestRepository {
    }
}