
package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AnalizadorLexico {

    
    private final String codigoFuente;
    private int posicion = 0; 
    private int lineaActual = 1;
    private int columnaActual = 1;

    private final List<Token> flujoDeTokens = new ArrayList<>();
    private final List<String> erroresLexicos = new ArrayList<>();

    private static final int LONGITUD_MAX_IDENT = 32;

    private static final Map<String, TokenType> PALABRAS_RESERVADAS = new HashMap<>();
    static {
        // Tipos
        PALABRAS_RESERVADAS.put("long", TokenType.KW_LONG);
        PALABRAS_RESERVADAS.put("double", TokenType.KW_DOUBLE);
        // Control
        PALABRAS_RESERVADAS.put("if", TokenType.KW_IF);
        PALABRAS_RESERVADAS.put("then", TokenType.KW_THEN);
        PALABRAS_RESERVADAS.put("else", TokenType.KW_ELSE);
        PALABRAS_RESERVADAS.put("while", TokenType.KW_WHILE);
        PALABRAS_RESERVADAS.put("break", TokenType.KW_BREAK);
        // E/S
        PALABRAS_RESERVADAS.put("read", TokenType.KW_READ);
        PALABRAS_RESERVADAS.put("write", TokenType.KW_WRITE);
        // Booleanas 
        PALABRAS_RESERVADAS.put("true", TokenType.KW_TRUE);
        PALABRAS_RESERVADAS.put("false", TokenType.KW_FALSE);
    }

    public AnalizadorLexico(String codigoFuente) {
        this.codigoFuente = codigoFuente != null ? codigoFuente : "";
    }

    /**
     * Método principal que genera la lista de tokens a partir del código fuente.
     */
    public List<Token> analizarTokens() { 
        while (!esFinDeArchivo()) {
            consumirEspaciosYComentarios();
            
            if (esFinDeArchivo()) break;

            // La lógica principal se mueve a 'escanearSiguienteToken'
            escanearSiguienteToken();
        }

        flujoDeTokens.add(new Token(TokenType.EOF, "", lineaActual, columnaActual));
        return flujoDeTokens;
    }

    /**
     * Escanea el siguiente token y lo añade a la lista.
     */
    private void escanearSiguienteToken() {
        int lineaInicio = lineaActual;
        int columnaInicio = columnaActual;
        char c = consumir();

        // --- Identificadores y Palabras Clave ---
        if (Character.isLetter(c) || c == '_') {
            String ident = procesarIdentificador(c);
            TokenType tipo = PALABRAS_RESERVADAS.getOrDefault(ident, TokenType.IDENTIFIER);
            
            if (tipo == TokenType.IDENTIFIER && ident.length() > LONGITUD_MAX_IDENT) {
                erroresLexicos.add(formatearError(lineaInicio, columnaInicio, "Identificador excede longitud maxima (" + LONGITUD_MAX_IDENT + ")."));
                ident = ident.substring(0, LONGITUD_MAX_IDENT);
            }
            emitirToken(tipo, ident, lineaInicio, columnaInicio);
            return;
        }

        // --- Literales Numéricos ---
        if (Character.isDigit(c)) {
            String num = procesarLiteralNumerico(c);
            if (num.contains(".")) {
                emitirToken(TokenType.DOUBLE_LITERAL, num, lineaInicio, columnaInicio);
            } else {
                emitirToken(TokenType.INT_LITERAL, num, lineaInicio, columnaInicio);
            }
            return;
        }

        // --- Operadores, Puntuación y Cadenas ---
        switch (c) {
            case '"':
                procesarLiteralCadena(lineaInicio, columnaInicio);
                break;

            // Agrupación y puntuación
            case '(': emitirToken(TokenType.LPAREN, "(", lineaInicio, columnaInicio); break;
            case ')': emitirToken(TokenType.RPAREN, ")", lineaInicio, columnaInicio); break;
            case '{': emitirToken(TokenType.LBRACE, "{", lineaInicio, columnaInicio); break;
            case '}': emitirToken(TokenType.RBRACE, "}", lineaInicio, columnaInicio); break;
            case ';': emitirToken(TokenType.SEMICOLON, ";", lineaInicio, columnaInicio); break;
            case ',': emitirToken(TokenType.COMMA, ",", lineaInicio, columnaInicio); break;

            // Operadores simples y compuestos
            case '+':
                emitirToken(verificarYConsumir('=') ? TokenType.PLUS_EQ : TokenType.PLUS, "+", lineaInicio, columnaInicio);
                break;
            case '-':
                emitirToken(verificarYConsumir('=') ? TokenType.MINUS_EQ : TokenType.MINUS, "-", lineaInicio, columnaInicio);
                break;
            case '*':
                emitirToken(verificarYConsumir('=') ? TokenType.STAR_EQ : TokenType.STAR, "*", lineaInicio, columnaInicio);
                break;
            case '/':
                emitirToken(verificarYConsumir('=') ? TokenType.SLASH_EQ : TokenType.SLASH, "/", lineaInicio, columnaInicio);
                break;
            case '=':
                emitirToken(verificarYConsumir('=') ? TokenType.EQEQ : TokenType.EQUAL, "=", lineaInicio, columnaInicio);
                break;
            case '!':
                emitirToken(verificarYConsumir('=') ? TokenType.NEQ : TokenType.BANG, "!", lineaInicio, columnaInicio);
                break;
            case '<':
                if (verificarYConsumir('=')) emitirToken(TokenType.LTE, "<=", lineaInicio, columnaInicio);
                else if (verificarYConsumir('>')) emitirToken(TokenType.NEQ_ALT, "<>", lineaInicio, columnaInicio);
                else emitirToken(TokenType.LT, "<", lineaInicio, columnaInicio);
                break;
            case '>':
                emitirToken(verificarYConsumir('=') ? TokenType.GTE : TokenType.GTE, ">", lineaInicio, columnaInicio);
                break;
            
            case '&':
                if (verificarYConsumir('&')) emitirToken(TokenType.ANDAND, "&&", lineaInicio, columnaInicio);
                else erroresLexicos.add(formatearError(lineaInicio, columnaInicio, "Carácter inesperado '&' (¿querías '&&'?)."));
                break;
            case '|':
                if (verificarYConsumir('|')) emitirToken(TokenType.OROR, "||", lineaInicio, columnaInicio);
                else erroresLexicos.add(formatearError(lineaInicio, columnaInicio, "Carácter inesperado '|' (¿querías '||'?)."));
                break;

            default:
                erroresLexicos.add(formatearError(lineaInicio, columnaInicio, "Carácter inválido: '" + caracterImprimible(c) + "'."));
                break;
        }
    }


    public List<String> getErrores() {
        return erroresLexicos;
    }

    // ==================================================
    //    PRIMITIVAS DE PROCESAMIENTO
    // ==================================================

    private boolean esFinDeArchivo() {
        return posicion >= codigoFuente.length();
    }

    /**
     * Consume el carácter actual y avanza la posicion.
     * Actualiza línea y columna.
     */
    private char consumir() {
        char c = codigoFuente.charAt(posicion++);
        if (c == '\n') {
            lineaActual++;
            columnaActual = 1;
        } else {
            columnaActual++;
        }
        return c;
    }

    /**
     * Comprueba si el carácter actual coincide con 'esperado'.
     * Si coincide, lo consume y retorna true.
     * Si no, retorna false.
     */
    private boolean verificarYConsumir(char esperado) {
        if (esFinDeArchivo()) return false;
        if (codigoFuente.charAt(posicion) != esperado) return false;
        
        posicion++;
        columnaActual++; 
        return true;
    }

    /**
     * Mira el carácter actual sin consumirlo.
     */
    private char verSiguiente() {
        if (esFinDeArchivo()) return '\0';
        return codigoFuente.charAt(posicion);
    }

    /**
     * Mira el carácter siguiente al actual sin consumirlo.
     */
    private char verSiguienteDelSiguiente() {
        if (posicion + 1 >= codigoFuente.length()) return '\0';
        return codigoFuente.charAt(posicion + 1);
    }

    /**
     * Añade un token a la lista de tokens.
     */
    private void emitirToken(TokenType tipo, String lexema, int l, int c) {
        flujoDeTokens.add(new Token(tipo, lexema, l, c));
    }

    // ==================================================
    //    LÓGICA DE RECONOCIMIENTO DE TOKENS
    // ==================================================

    private void consumirEspaciosYComentarios() {
        while (!esFinDeArchivo()) {
            char c = verSiguiente();

            // 1. Espacios en blanco
            if (c == ' ' || c == '\r' || c == '\t' || c == '\n') {
                consumir();
                continue;
            }

            // 2. Comentarios
            if (c == '/') {
                char n = verSiguienteDelSiguiente();
                if (n == '/') {
                    // Comentario de línea
                    while (!esFinDeArchivo() && verSiguiente() != '\n') consumir();
                    continue;
                } else if (n == '*') {
                    // Comentario multilínea
                    consumir(); // Consume '/'
                    consumir(); // Consume '*'
                    int lineaComentario = lineaActual, colComentario = columnaActual;
                    boolean cerrado = false;
                    while (!esFinDeArchivo()) {
                        char actual = consumir();
                        if (actual == '*' && verSiguiente() == '/') {
                            consumir(); // consume '/'
                            cerrado = true;
                            break;
                        }
                    }
                    if (!cerrado) {
                        erroresLexicos.add(formatearError(lineaComentario, colComentario, "Comentario multilinea sin cierre."));
                    }
                    continue;
                }
            }
            
            break;
        }
    }

    private String procesarIdentificador(char primero) {
        StringBuilder sb = new StringBuilder();
        sb.append(primero);
        
        while (!esFinDeArchivo() && (Character.isLetterOrDigit(verSiguiente()) || verSiguiente() == '_')) {
            sb.append(consumir());
        }
        return sb.toString();
    }

    private String procesarLiteralNumerico(char primero) {
        StringBuilder sb = new StringBuilder();
        sb.append(primero);

        while (Character.isDigit(verSiguiente())) {
            sb.append(consumir());
        }

        // Parte decimal
        if (verSiguiente() == '.' && Character.isDigit(verSiguienteDelSiguiente())) {
            sb.append(consumir()); // punto
            while (Character.isDigit(verSiguiente())) {
                sb.append(consumir());
            }
        }

        return sb.toString();
    }

    private void procesarLiteralCadena(int lineaInicio, int columnaInicio) {
        StringBuilder sb = new StringBuilder();
        
        while (!esFinDeArchivo() && verSiguiente() != '"') {
            char c = consumir();
            if (c == '\n') {
                erroresLexicos.add(formatearError(lineaInicio, columnaInicio, "Cadena sin cierre en la misma linea."));
            }
            sb.append(c);
        }

        if (esFinDeArchivo()) {
            erroresLexicos.add(formatearError(lineaInicio, columnaInicio, "Cadena sin cierre de comillas."));
            return;
        }

        consumir(); // consume la comilla de cierre
        emitirToken(TokenType.STRING_LITERAL, sb.toString(), lineaInicio, columnaInicio);
    }

    // ==================================================
    //    UTILIDADES DE ERRORES
    // ==================================================

    private String formatearError(int l, int c, String desc) {
        return "Error lexico [linea " + l + ", col " + c + "]: " + desc;
    }

    private String caracterImprimible(char c) {
        if (c == '\n') return "\\n";
        if (c == '\t') return "\\t";
        if (c == '\r') return "\\r";
        return String.valueOf(c);
    }
}
