
package org.example;


import java.util.ArrayList;
import java.util.List;


/**
 * Implementa un Analizador de Descenso Recursivo.
 * Su trabajo es tomar la lista de tokens y construir un
 * Árbol de Sintaxis Abstracta (AST) de Sentencias y Expresiones.
 */
public class AnalizadorSintactico {

    private final List<Token> tokens;
    private int posicion = 0; // 'current' en el modelo
    private final List<String> errores = new ArrayList<>();

    public AnalizadorSintactico(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Punto de entrada. Parsea una secuencia de sentencias hasta el Fin de Archivo (EOF).
     */
    public List<Sentencia> analizar() {
        List<Sentencia> sentencias = new ArrayList<>();
        while (!esFinDeArchivo()) {

            // FASE 3 ignora las declaraciones de FASE 2
            if (verificar(TokenType.KW_LONG) || verificar(TokenType.KW_DOUBLE)) {
                saltarDeclaracion();
                continue;
            }

            Sentencia s = sentencia();
            if (s != null) {
                sentencias.add(s);
            }
            // A diferencia de otros parsers, no sincronizamos aquí.
            // Cada método de sentencia es responsable de avanzar.
        }
        return sentencias;
    }

    /**
     * Esta función avanza rápidamente sobre las declaraciones de variables
     * (long ...; / double ...;) que ya fueron procesadas por el
     * RecolectorDeclaraciones en la Fase 2.
     */
    private void saltarDeclaracion() {
        avanzar(); // Consumir el 'long' o 'double'

        boolean esperarIdent = true;
        while (!esFinDeArchivo()) {
            if (esperarIdent) {
                if (verificar(TokenType.IDENTIFIER)) {
                    avanzar(); // Consumir el ID
                    esperarIdent = false;
                } else {
                    // Declaración mal formada, pero FASE 2 ya reportó el error.
                    // Salimos y dejamos que el parser intente recuperarse.
                    return;
                }
            } else {
                if (coincidir(TokenType.COMMA)) {
                    esperarIdent = true; // Viene otro ID
                } else if (coincidir(TokenType.SEMICOLON)) {
                    return; // Fin de la declaración
                } else {
                    // Hay "basura" (ej. long a b;), FASE 2 ya lo reportó.
                    // Avanzamos hasta el ';' para re-sincronizar.
                    avanzar();
                }
            }
        }
    }

    public List<String> getErrores() { return errores; }

    // ======================================================
    // MÉTODOS DE SENTENCIAS 
    // ======================================================

    /**
     * Método "router" principal para las sentencias.
     * Identifica qué tipo de sentencia comienza.
     */
    private Sentencia sentencia() {
        // if / while
        if (coincidir(TokenType.KW_IF)) return sentenciaSi(verAnterior());
        if (coincidir(TokenType.KW_WHILE)) return sentenciaMientras(verAnterior());

        // break;
        if (coincidir(TokenType.KW_BREAK)) return sentenciaInterrumpir(verAnterior());

        // bloque { ... }
        if (coincidir(TokenType.LBRACE)) return sentenciaBloque();

        // read / write
        if (coincidir(TokenType.KW_READ)) return sentenciaLectura(verAnterior());
        if (coincidir(TokenType.KW_WRITE)) return sentenciaEscritura(verAnterior());

        // Asignación: id = ...; o id += ...;
        // Usamos "lookahead" (mirar 2 tokens adelante)
        if (verificar(TokenType.IDENTIFIER) && (
                verificarSiguiente(TokenType.EQUAL) ||
                verificarSiguiente(TokenType.PLUS_EQ) ||
                verificarSiguiente(TokenType.MINUS_EQ) ||
                verificarSiguiente(TokenType.STAR_EQ) ||
                verificarSiguiente(TokenType.SLASH_EQ)
        )) {
            return sentenciaAsignacion();
        }

        // Si no se reconoce nada, es un error.
        Token t = verActual();
        reportarError(t, "Se esperaba una sentencia: 'if', 'while', 'break', '{', 'read', 'write' o asignación 'id = expr;'.");
        avanzar(); // Avanzar un token para no entrar en bucle infinito
        return null;
    }

    private Sentencia sentenciaSi(Token kwSi) {
        consumir(TokenType.LPAREN, "Se esperaba '(' después de 'if'.");
        Expresion condicion = expresion();
        consumir(TokenType.RPAREN, "Se esperaba ')' después de la condicion de 'if'.");

        // La especificación exige 'then'
        Token t = verActual();
        if (!coincidir(TokenType.KW_THEN)) {
            reportarError(t, "Se esperaba 'then' despues de 'if (cond)'.");
        }

        Sentencia ramaEntonces = sentencia();

        Sentencia ramaSino = null;
        if (coincidir(TokenType.KW_ELSE)) {
            ramaSino = sentencia();
        }

        return new Sentencia.Condicional(condicion, ramaEntonces, ramaSino, kwSi.linea, kwSi.columna);
    }

    private Sentencia sentenciaMientras(Token kwMientras) {
        consumir(TokenType.LPAREN, "Se esperaba '(' despues de 'while'.");
        Expresion condicion = expresion();
        consumir(TokenType.RPAREN, "Se esperaba ')' despues de la condicion de 'while'.");
        Sentencia cuerpo = sentencia();
        return new Sentencia.Mientras(condicion, cuerpo, kwMientras.linea, kwMientras.columna);
    }
    
    private Sentencia sentenciaInterrumpir(Token kw) {
        consumir(TokenType.SEMICOLON, "Se esperaba ';' despues de 'break'.");
        return new Sentencia.Interrumpir(kw.linea, kw.columna);
    }

    private Sentencia sentenciaBloque() {
        int linea = verAnterior().linea, col = verAnterior().columna;
        List<Sentencia> sentencias = new ArrayList<>();
        
        while (!esFinDeArchivo() && !verificar(TokenType.RBRACE)) {
            Sentencia s = sentencia();
            if (s != null) sentencias.add(s);
        }
        
        consumir(TokenType.RBRACE, "Se esperaba '}' para cerrar el bloque.");
        return new Sentencia.Bloque(sentencias, linea, col);
    }

    private Sentencia sentenciaLectura(Token kw) {
        consumir(TokenType.LPAREN, "Se esperaba '(' despues de 'read'.");
        Token id = consumir(TokenType.IDENTIFIER, "Se esperaba identificador dentro de read(...).");
        consumir(TokenType.RPAREN, "Se esperaba ')' despues de identificador en read(...).");
        consumir(TokenType.SEMICOLON, "Se esperaba ';' despues de read(...).");
        return new Sentencia.Lectura(id.lexema, kw.linea, kw.columna);
    }

    private Sentencia sentenciaEscritura(Token kw) {
        consumir(TokenType.LPAREN, "Se esperaba '(' después de 'write'.");
        Expresion e = expresion();
        consumir(TokenType.RPAREN, "Se esperaba ')' después de la expresion en write(...).");
        consumir(TokenType.SEMICOLON, "Se esperaba ';' despues de write(...).");
        return new Sentencia.Escritura(e, kw.linea, kw.columna);
    }

    private Sentencia sentenciaAsignacion() {
        Token idTok = consumir(TokenType.IDENTIFIER, "Se esperaba identificador al inicio de la asignacion.");

        if (coincidir(TokenType.EQUAL)) {
            // Asignación simple: id = expr;
            Expresion valor = expresion();
            consumir(TokenType.SEMICOLON, "Se esperaba ';' al final de la asignacion.");
            return new Sentencia.Asignacion(idTok.lexema, valor, idTok.linea, idTok.columna);
            
        } else if (coincidir(TokenType.PLUS_EQ, TokenType.MINUS_EQ, TokenType.STAR_EQ, TokenType.SLASH_EQ)) {
            // Asignación compuesta: id += expr;
            Token op = verAnterior();
            Expresion derecha = expresion();
            consumir(TokenType.SEMICOLON, "Se esperaba ';' al final de la asignacion compuesta.");

            // "Desugar" (Convertir): a += 5   ->   a = a + 5
            
            // 1. Mapear (+=) a (+)
            TokenType opBase;
            switch (op.tipo) {
                case PLUS_EQ:  opBase = TokenType.PLUS;  break;
                case MINUS_EQ: opBase = TokenType.MINUS; break;
                case STAR_EQ:  opBase = TokenType.STAR;  break;
                case SLASH_EQ: opBase = TokenType.SLASH; break;
                default: opBase = TokenType.PLUS; // No debería pasar
            }

            // 2. Crear la expresión 'a + 5'
            Expresion izquierdaVar = new Expresion.Variable(idTok.lexema, idTok.linea, idTok.columna);
            Expresion binaria = new Expresion.Binaria(izquierdaVar, opBase, derecha, op.linea, op.columna);
            
            // 3. Crear la asignación 'a = (a + 5)'
            return new Sentencia.Asignacion(idTok.lexema, binaria, idTok.linea, idTok.columna);
        }

        // Error: Esto no debería pasar si el 'lookahead' de 'sentencia()' funcionó
        reportarError(verActual(), "Se esperaba '=' o un operador de asignacion compuesta ('+=', '-=', '*=', '/=').");
        while (!esFinDeArchivo() && !verificar(TokenType.SEMICOLON)) avanzar();
        if (coincidir(TokenType.SEMICOLON)) { /* consumir ';' si está */ }
        return null;
    }


    // ======================================================
    // MÉTODOS DE EXPRESIONES (CON PRECEDENCIA)
    // ======================================================
    
    // El orden de llamado implementa la precedencia (de menor a mayor)
    // expresion -> O_logico -> Y_logico -> igualdad -> comparacion -> termino -> factor -> unaria -> primaria

    private Expresion expresion() {
        return expresionLogicaO();
    }
    
    // or -> and ( '||' and )*
    private Expresion expresionLogicaO() {
        Expresion expr = expresionLogicaY();
        while (coincidir(TokenType.OROR)) {
            Token op = verAnterior();
            Expresion derecha = expresionLogicaY();
            expr = new Expresion.Binaria(expr, op.tipo, derecha, op.linea, op.columna);
        }
        return expr;
    }

    // and -> equality ( '&&' equality )*
    private Expresion expresionLogicaY() {
        Expresion expr = igualdad();
        while (coincidir(TokenType.ANDAND)) {
            Token op = verAnterior();
            Expresion derecha = igualdad();
            expr = new Expresion.Binaria(expr, op.tipo, derecha, op.linea, op.columna);
        }
        return expr;
    }

    // equality -> comparison ( (== | != | <>) comparison )*
    private Expresion igualdad() {
        Expresion expr = comparacion();
        while (coincidir(TokenType.EQEQ, TokenType.NEQ, TokenType.NEQ_ALT)) {
            Token op = verAnterior();
            Expresion derecha = comparacion();
            expr = new Expresion.Binaria(expr, op.tipo, derecha, op.linea, op.columna);
        }
        return expr;
    }

    // comparison -> term ( ( > | < | >= | <= ) term )*
    private Expresion comparacion() {
        Expresion expr = termino();
        while (coincidir(TokenType.GT, TokenType.LT, TokenType.GTE, TokenType.LTE)) {
            Token op = verAnterior();
            Expresion derecha = termino();
            expr = new Expresion.Binaria(expr, op.tipo, derecha, op.linea, op.columna);
        }
        return expr;
    }

    // term -> factor ( ( + | - ) factor )*
    private Expresion termino() {
        Expresion expr = factor();
        while (coincidir(TokenType.PLUS, TokenType.MINUS)) {
            Token op = verAnterior();
            Expresion derecha = factor();
            expr = new Expresion.Binaria(expr, op.tipo, derecha, op.linea, op.columna);
        }
        return expr;
    }

    // factor -> unary ( ( * | / ) unary )*
    private Expresion factor() {
        Expresion expr = unaria();
        while (coincidir(TokenType.STAR, TokenType.SLASH)) {
            Token op = verAnterior();
            Expresion derecha = unaria();
            expr = new Expresion.Binaria(expr, op.tipo, derecha, op.linea, op.columna);
        }
        return expr;
    }

    // unary -> ( ! | - ) unary | primary
    private Expresion unaria() {
        if (coincidir(TokenType.BANG, TokenType.MINUS)) {
            Token op = verAnterior();
            Expresion derecha = unaria();
            return new Expresion.Unaria(op.tipo, derecha, op.linea, op.columna);
        }
        return primaria();
    }

    // primary -> NUMBER | STRING | true | false | IDENT | '(' expression ')'
    private Expresion primaria() {
        if (coincidir(TokenType.INT_LITERAL, TokenType.DOUBLE_LITERAL, TokenType.STRING_LITERAL)) {
            Token lit = verAnterior();
            return new Expresion.Literal(lit.lexema, lit.tipo, lit.linea, lit.columna);
        }
        if (coincidir(TokenType.KW_TRUE, TokenType.KW_FALSE)) {
            Token lit = verAnterior();
            return new Expresion.Literal(lit.lexema, lit.tipo, lit.linea, lit.columna);
        }
        if (coincidir(TokenType.IDENTIFIER)) {
            Token id = verAnterior();
            return new Expresion.Variable(id.lexema, id.linea, id.columna);
        }
        if (coincidir(TokenType.LPAREN)) {
            Token lp = verAnterior();
            Expresion e = expresion();
            consumir(TokenType.RPAREN, "Se esperaba ')' para cerrar la expresion.");
            return new Expresion.Agrupacion(e, lp.linea, lp.columna);
        }

        // Error
        Token t = verActual();
        reportarError(t, "Expresión invalida.");
        // Devolvemos un literal "dummy" para evitar NullPointerExceptions
        return new Expresion.Literal("", TokenType.STRING_LITERAL, t.linea, t.columna);
    }


    // ======================================================
    // MÉTODOS DE UTILIDAD DEL PARSER
    // ======================================================

    /**
     * Comprueba si el token actual es de alguno de los tipos dados.
     * Si es así, lo consume (avanza) y devuelve true.
     */
    private boolean coincidir(TokenType... tipos) {
        for (TokenType tt : tipos) {
            if (verificar(tt)) {
                avanzar();
                return true;
            }
        }
        return false;
    }

    /**
     * Consume un token del tipo esperado.
     * Si no es de ese tipo, reporta un error.
     */
    private Token consumir(TokenType tipo, String msgError) {
        if (verificar(tipo)) return avanzar();
        reportarError(verActual(), msgError);
        // Devolvemos un "token fantasma" para que el parser no falle
        return new Token(tipo, "", verActual().linea, verActual().columna);
    }

    /**
     * Comprueba (sin consumir) si el token actual es del tipo dado.
     */
    private boolean verificar(TokenType tipo) {
        if (esFinDeArchivo()) return false;
        return verActual().tipo == tipo;
    }

    /**
     * Comprueba (sin consumir) el token que SIGUE al actual.
     * Esencial para el "lookahead" de las asignaciones (id = ...).
     */
    private boolean verificarSiguiente(TokenType tipo) {
        if (posicion + 1 >= tokens.size()) return false;
        return tokens.get(posicion + 1).tipo == tipo;
    }

    /**
     * Consume el token actual y avanza el cursor.
     */
    private Token avanzar() {
        if (!esFinDeArchivo()) posicion++;
        return verAnterior();
    }

    private boolean esFinDeArchivo() {
        return verActual().tipo == TokenType.EOF;
    }

    /**
     * Devuelve el token actual (en el que estamos parados).
     */
    private Token verActual() {
        return tokens.get(posicion);
    }

    /**
     * Devuelve el token que acabamos de consumir.
     */
    private Token verAnterior() {
        return tokens.get(posicion - 1);
    }

    /**
     * Añade un error a la lista.
     */
    private void reportarError(Token t, String msg) {
        errores.add("Error sintactico [linea " + t.linea + ", col " + t.columna + "]: " + msg);
    }
}
