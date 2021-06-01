package de.jdellert.iwsa.tokenize;

public class UnknownIpaSymbolException extends Exception {
    String symbol;
    String context;

    public UnknownIpaSymbolException(String symbol, String context) {
        this.symbol = symbol;
        this.context = context;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getContext() {
        return context;
    }
}
