package com.byteelement.stockguard.controller;

import com.byteelement.stockguard.exception.SkuDuplicadoException;
import com.byteelement.stockguard.exception.StockInsuficienteException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(com.byteelement.stockguard.exception.ConflictoCatalogoException.class)
    public ProblemDetail conflictoCatalogo(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

        @ExceptionHandler(SkuDuplicadoException.class)
        public ProblemDetail skuDuplicado(SkuDuplicadoException exception) {
                return ProblemDetail.forStatusAndDetail(
                                HttpStatus.CONFLICT, exception.getMessage());
        }

        @ExceptionHandler(StockInsuficienteException.class)
        public ProblemDetail stockInsuficiente(StockInsuficienteException exception) {
                return ProblemDetail.forStatusAndDetail(
                                HttpStatus.CONFLICT, exception.getMessage());
        }

        @ExceptionHandler(NoSuchElementException.class)
        public ProblemDetail noEncontrado(NoSuchElementException exception) {
                return ProblemDetail.forStatusAndDetail(
                                HttpStatus.NOT_FOUND, exception.getMessage());
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ProblemDetail argumentoInvalido(IllegalArgumentException exception) {
                return ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ProblemDetail datosInvalidos(
                        MethodArgumentNotValidException exception) {

                ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                                HttpStatus.BAD_REQUEST,
                                "Revisa los campos enviados.");

                List<Map<String, String>> errores = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(error -> Map.of(
                                                "campo", error.getField(),
                                                "mensaje", String.valueOf(error.getDefaultMessage())))
                                .toList();

                problema.setProperty("errores", errores);
                return problema;
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ProblemDetail conflictoDeDatos() {
                return ProblemDetail.forStatusAndDetail(
                                HttpStatus.CONFLICT,
                                "La operación incumple una restricción de integridad; "
                                                + "comprueba, entre otras condiciones, que el SKU no exista.");
        }
}
