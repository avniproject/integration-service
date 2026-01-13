package org.avni_integration_service.web;

import org.avni_integration_service.exception.IntegratorAPIException;
import org.avni_integration_service.goonj.exceptions.GoonjAdhocException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(GoonjAdhocException.class)
    public ResponseEntity<IntegratorAPIException> handleGoonjAdhocException(GoonjAdhocException goonjAdhocException, HttpServletRequest request){
        IntegratorAPIException integratorAPIException = new IntegratorAPIException();
        integratorAPIException.setHttpStatus(goonjAdhocException.getHttpStatus());
        integratorAPIException.setMessages(goonjAdhocException.getMessage());
        integratorAPIException.setPath(request.getRequestURI());
        return new ResponseEntity<>(integratorAPIException,integratorAPIException.getHttpStatus());
    }
}
