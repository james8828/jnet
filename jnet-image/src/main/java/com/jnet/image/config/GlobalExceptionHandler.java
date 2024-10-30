package com.jnet.image.config;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.Map;

/**
 * @author mugw
 * @version 1.0
 * @description 全局异常处理器 此类用于统一处理应用程序中的所有异常，并返回合适的HTTP响应
 * @date 2024/9/19 15:13:32
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {IllegalArgumentException.class})
    public ResponseEntity<Object> handleIllegalArgumentException(@NonNull IllegalArgumentException ex, @NonNull WebRequest request) {
        // 自定义异常处理逻辑
        return handleExceptionInternal(ex, "Bad Request", new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    /*@Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers, @NonNull HttpStatus status, @NonNull WebRequest request) {
        Map<String, Object> body = null;
        body.put("errors", ex.getBindingResult().getFieldErrors());
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(@NonNull MethodArgumentTypeMismatchException ex, @NonNull HttpHeaders headers, @NonNull HttpStatus status, @NonNull WebRequest request) {
        // 自定义异常处理逻辑
        return handleExceptionInternal(ex, "Type Mismatch", headers, status, request);
    }*/

    @ExceptionHandler(value = {ConstraintViolationException.class})
    public ResponseEntity<Object> handleConstraintViolationException(@NonNull ConstraintViolationException ex, @NonNull WebRequest request) {
        // 自定义异常处理逻辑
        return handleExceptionInternal(ex, "Validation Error", new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }
}

