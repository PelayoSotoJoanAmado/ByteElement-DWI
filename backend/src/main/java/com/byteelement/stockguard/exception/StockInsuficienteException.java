package com.byteelement.stockguard.exception;

public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException() {
        super("Stock insuficiente para registrar la salida.");
    }
}
