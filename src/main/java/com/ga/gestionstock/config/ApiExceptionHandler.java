package com.ga.gestionstock.config;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Verifiez les champs transmis.");
        var champs = new LinkedHashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(e -> champs.putIfAbsent(e.getField(), e.getDefaultMessage()));
        detail.setProperty("champs", champs);
        return handleExceptionInternal(ex, detail, headers, status, request);
    }
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail validation(ConstraintViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Parametres invalides.");
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflit(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Donnees en conflit : la reference de l'article ou l'identifiant de l'utilisateur doit etre unique.");
    }
    @ExceptionHandler({PessimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    ProblemDetail concurrence(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Une autre operation a modifie ces donnees. Reessayez.");
    }
}
