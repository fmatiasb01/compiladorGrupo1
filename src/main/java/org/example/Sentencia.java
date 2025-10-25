
package org.example;

import java.util.List;

/**
 * Clase base abstracta para todas las Sentencias (AST).
 * Cada subclase representa una construcción que realiza una acción.
 */
abstract class Sentencia {
    public final int linea;
    public final int columna;

    protected Sentencia(int linea, int columna) {
        this.linea = linea;
        this.columna = columna;
    }

    /**
     * Sentencia de asignación: a = 5; (o a += 5;)
     */
    static class Asignacion extends Sentencia {
        public final String nombre; 
        public final Expresion valor;
        Asignacion(String nombre, Expresion valor, int linea, int columna) {
            super(linea, columna);
            this.nombre = nombre;
            this.valor = valor;
        }
        public String toString() { return "Asignacion(" + nombre + " = " + valor + ")"; }
    }

    /**
     * Sentencia read(a);
     */
    static class Lectura extends Sentencia {
        public final String nombre; // Variable donde se guarda
        Lectura(String nombre, int linea, int columna) {
            super(linea, columna);
            this.nombre = nombre;
        }
        public String toString() { return "Lectura(" + nombre + ")"; }
    }

    /**
     * Sentencia write(a + 5);
     */
    static class Escritura extends Sentencia {
        public final Expresion expresion; // Expresión a imprimir
        Escritura(Expresion expresion, int linea, int columna) {
            super(linea, columna);
            this.expresion = expresion;
        }
        public String toString() { return "Escritura(" + expresion + ")"; }
    }

    /**
     * Un bloque de código: { ... }
     */
    static class Bloque extends Sentencia {
        public final List<Sentencia> sentencias;
        Bloque(List<Sentencia> sentencias, int linea, int columna) {
            super(linea, columna);
            this.sentencias = sentencias;
        }
        public String toString() { return "Bloque" + sentencias; }
    }
    
    /**
     * Sentencia if (cond) then ... else ...
     */
    static class Condicional extends Sentencia {
        public final Expresion condicion;
        public final Sentencia ramaEntonces;
        public final Sentencia ramaSino; // Puede ser null

        Condicional(Expresion condicion, Sentencia ramaEntonces, Sentencia ramaSino, int linea, int columna) {
            super(linea, columna);
            this.condicion = condicion;
            this.ramaEntonces = ramaEntonces;
            this.ramaSino = ramaSino;
        }

        public String toString() {
            return "Si(" + condicion + ", entonces=" + ramaEntonces + ", sino=" + ramaSino + ")";
        }
    }

    /**
     * Sentencia while (cond) { ... }
     */
    static class Mientras extends Sentencia {
        public final Expresion condicion;
        public final Sentencia cuerpo;

        Mientras(Expresion condicion, Sentencia cuerpo, int linea, int columna) {
            super(linea, columna);
            this.condicion = condicion;
            this.cuerpo = cuerpo;
        }

        public String toString() {
            return "Mientras(" + condicion + ", " + cuerpo + ")";
        }
    }

    /**
     * Sentencia break;
     */
    static class Interrumpir extends Sentencia {
        Interrumpir(int linea, int columna) { super(linea, columna); }
        public String toString() { return "Interrumpir"; }
    }
}
