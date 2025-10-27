
package org.example;

public class Token {
    
    public final TokenType tipo;
    public final String lexema;
    public final int linea;
    public final int columna;

    public Token(TokenType type, String lexeme, int line, int column) {
        this.tipo = type;
        this.lexema = lexeme;
        this.linea = line;
        this.columna = column;
    }

    @Override
    public String toString() {
        return String.format("%s('%s') @ %d:%d", tipo, lexema, linea, columna);
    }
}
