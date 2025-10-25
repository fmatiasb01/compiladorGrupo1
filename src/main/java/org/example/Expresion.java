
package org.example;

/**
 * Clase base abstracta para todas las Expresiones (AST).
 * Cada subclase representa una construcción gramatical que produce un valor.
 */
abstract class Expresion {
    public final int linea;
    public final int columna;

    protected Expresion(int linea, int columna) {
        this.linea = linea;
        this.columna = columna;
    }

    /**
     * Un valor literal: "hola", 123, 3.14, true, false
     */
    static class Literal extends Expresion {
        public final String valor;
        public final TokenType tipoLiteral; // INT_LITERAL, DOUBLE_LITERAL, etc.

        Literal(String valor, TokenType tipoLiteral, int linea, int columna) {
            super(linea, columna);
            this.valor = valor;
            this.tipoLiteral = tipoLiteral;
        }
        public String toString() { return "Literal(" + tipoLiteral + ":" + valor + ")"; }
    }

    /**
     * Una referencia a una variable: _x, contador
     */
    static class Variable extends Expresion {
        public final String nombre;
        Variable(String nombre, int linea, int columna) {
            super(linea, columna);
            this.nombre = nombre;
        }
        public String toString() { return "Var(" + nombre + ")"; }
    }

    /**
     * Una expresión entre paréntesis: ( ... )
     */
    static class Agrupacion extends Expresion {
        public final Expresion interna;
        Agrupacion(Expresion interna, int linea, int columna) {
            super(linea, columna);
            this.interna = interna;
        }
        public String toString() { return "Grupo(" + interna + ")"; }
    }

    /**
     * Un operador unario: -5, !true
     */
    static class Unaria extends Expresion {
        public final TokenType op; // MINUS o BANG
        public final Expresion derecha;
        Unaria(TokenType op, Expresion derecha, int linea, int columna) {
            super(linea, columna);
            this.op = op;
            this.derecha = derecha;
        }
        public String toString() { return "Unario(" + op + " " + derecha + ")"; }
    }

    /**
     * Un operador binario: a + b, 5 > 3
     */
    static class Binaria extends Expresion {
        public final Expresion izquierda;
        public final TokenType op; // +, -, *, /, >, <, ==, !=, &&, ||, etc.
        public final Expresion derecha;
        Binaria(Expresion izquierda, TokenType op, Expresion derecha, int linea, int columna) {
            super(linea, columna);
            this.izquierda = izquierda;
            this.op = op;
            this.derecha = derecha;
        }
        public String toString() { return "Binario(" + izquierda + " " + op + " " + derecha + ")"; }
    }
}
