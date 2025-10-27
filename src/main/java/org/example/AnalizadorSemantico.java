package org.example;

import java.util.ArrayList;
import java.util.List;


public class AnalizadorSemantico {

    private final List<Sentencia> sentencias;
    private final TablaSimbolos tablaSimbolos;
    private final List<String> errores = new ArrayList<>();

    // Contador de bucles activos para validar 'break'
    private int profundidadBucle = 0;

    public AnalizadorSemantico(List<Sentencia> sentencias, TablaSimbolos tablaSimbolos) {
        this.sentencias = sentencias;
        this.tablaSimbolos = tablaSimbolos;
    }

    /**
     * Inicia el análisis semántico.
     */
    public void analizar() {
        for (Sentencia s : sentencias) {
            revisarSentencia(s);
        }
    }

    public List<String> getErrores() { return errores; }

   
    private enum TipoInterno { ENTERO, DECIMAL, CADENA, BOOLEANO, ERROR }

    // ===================================
    // REVISIÓN DE SENTENCIAS 
    // ===================================

    private void revisarSentencia(Sentencia s) {
        // Usamos 'instanceof' para determinar qué tipo de sentencia es
        
        if (s instanceof Sentencia.Lectura r) {
            requerirDeclarado(r.nombre, s.linea, s.columna);
            return;
        }
        if (s instanceof Sentencia.Escritura w) {
            tipoDe(w.expresion); // Valida la expresión interna
            return;
        }
        if (s instanceof Sentencia.Asignacion a) {
            Simbolo sym = requerirDeclarado(a.nombre, s.linea, s.columna);
            TipoInterno tipoDerecha = tipoDe(a.valor);
            
            if (sym != null) { // Si la variable existe
                if (!esAsignable(sym.tipo, tipoDerecha)) {
                    errores.add(reportarError(s.linea, s.columna, "Tipos incompatibles en asignacion: variable '" + sym.nombre +
                            "' es " + sym.tipo + " pero la expresion es " + tipoDerecha + "."));
                } else {
                    // Tipos válidos: actualizamos el "Valor" en la tabla
                    actualizarValorSimbolo(sym, a.valor);
                }
            }
            return;
        }
        if (s instanceof Sentencia.Bloque b) {
            for (Sentencia interna : b.sentencias) revisarSentencia(interna);
            return;
        }
        if (s instanceof Sentencia.Condicional c) {
            TipoInterno tipoCond = tipoDe(c.condicion);
            if (!esBooleano(tipoCond)) {
                errores.add(reportarError(c.linea, c.columna, "La condicion de 'if' debe ser booleana o una comparacion (obtuvo: " + tipoCond + ")."));
            }
            if (c.ramaEntonces != null) revisarSentencia(c.ramaEntonces);
            if (c.ramaSino != null) revisarSentencia(c.ramaSino);
            return;
        }
        if (s instanceof Sentencia.Mientras m) {
            TipoInterno tipoCond = tipoDe(m.condicion);
            if (!esBooleano(tipoCond)) {
                errores.add(reportarError(m.linea, m.columna, "La condicion de 'while' debe ser booleana o una comparacion (obtuvo: " + tipoCond + ")."));
            }
            // Entramos en un bucle
            profundidadBucle++;
            if (m.cuerpo != null) revisarSentencia(m.cuerpo);
            // Salimos del bucle
            profundidadBucle--;
            return;
        }
        if (s instanceof Sentencia.Interrumpir i) {
            if (profundidadBucle == 0) {
                errores.add(reportarError(i.linea, i.columna, "'break' solo puede usarse dentro de un 'while'."));
            }
            return;
        }
    }

    /**
     * Actualiza el "Valor" en la Tabla de Símbolos.
     * Solo funciona si la asignación es a un literal (ej. a = 10).
     * Si es (a = b + 5), el valor se marca como N/A (null).
     */
    private void actualizarValorSimbolo(Simbolo sym, Expresion expr) {
        // Desempaquetar si viene entre paréntesis: a = (10);
        if (expr instanceof Expresion.Agrupacion g) {
            expr = g.interna;
        }

        if (!(expr instanceof Expresion.Literal lit)) {
            sym.valor = null; // No es un literal, valor desconocido
            return;
        }

        switch (lit.tipoLiteral) {
            case INT_LITERAL:
                // Se permite: long = 10; y double = 10;
                if (sym.tipo == TipoSimbolo.LONG || sym.tipo == TipoSimbolo.DOUBLE) {
                    sym.valor = lit.valor;
                } else {
                    sym.valor = null;
                }
                break;

            case DOUBLE_LITERAL:
                // Se permite: double = 10.5;
                // (long = 10.5 ya fue bloqueado por 'esAsignable')
                if (sym.tipo == TipoSimbolo.DOUBLE) {
                    sym.valor = lit.valor;
                } else {
                    sym.valor = null;
                }
                break;

            default:
                // No manejamos variables string/boolean
                sym.valor = null;
                break;
        }
    }

    // ===================================
    // CHEQUEO DE TIPOS DE EXPRESIONES 
    // ===================================

    /**
     * Función recursiva que determina el tipo de cualquier expresión.
     */
    private TipoInterno tipoDe(Expresion e) {
        if (e instanceof Expresion.Literal lit) {
            switch (lit.tipoLiteral) {
                case INT_LITERAL: return TipoInterno.ENTERO;
                case DOUBLE_LITERAL: return TipoInterno.DECIMAL;
                case STRING_LITERAL: return TipoInterno.CADENA;
                case KW_TRUE:
                case KW_FALSE: return TipoInterno.BOOLEANO;
                default: return TipoInterno.ERROR;
            }
        }
        if (e instanceof Expresion.Variable v) {
            Simbolo sym = requerirDeclarado(v.nombre, e.linea, e.columna);
            if (sym == null) return TipoInterno.ERROR;
            // Mapea el tipo de la tabla (LONG/DOUBLE) al tipo interno
            return (sym.tipo == TipoSimbolo.LONG) ? TipoInterno.ENTERO : TipoInterno.DECIMAL;
        }
        if (e instanceof Expresion.Agrupacion g) {
            return tipoDe(g.interna); // El tipo es el de la expresión interna
        }
        if (e instanceof Expresion.Unaria u) {
            TipoInterno tipoDerecha = tipoDe(u.derecha);
            
            if (u.op == TokenType.BANG) {
                if (tipoDerecha == TipoInterno.BOOLEANO || tipoDerecha == TipoInterno.ENTERO || tipoDerecha == TipoInterno.DECIMAL) {
                    return TipoInterno.BOOLEANO;
                }
                errores.add(reportarError(e.linea, e.columna, "Operador '!' invalido sobre tipo " + tipoDerecha + "."));
                return TipoInterno.ERROR;
            

            } else if (u.op == TokenType.MINUS) {
                if (esNumerico(tipoDerecha)) return tipoDerecha;
                errores.add(reportarError(e.linea, e.columna, "Operador unario '-' requiere numerico (obtuvo " + tipoDerecha + ")."));
                return TipoInterno.ERROR;
            }
            return TipoInterno.ERROR;
        }
        
        if (e instanceof Expresion.Binaria b) {
            TipoInterno tipoIzq = tipoDe(b.izquierda);
            TipoInterno tipoDer = tipoDe(b.derecha);

            // Lógicos: && ||
            
            if (b.op == TokenType.ANDAND || b.op == TokenType.OROR) {
                if (tipoIzq == TipoInterno.BOOLEANO && tipoDer == TipoInterno.BOOLEANO) return TipoInterno.BOOLEANO;
                // También se corrigió 'b.op.lexema' a solo 'b.op'
                errores.add(reportarError(e.linea, e.columna, "Operador logico requiere booleanos: " + tipoIzq + " " + b.op + " " + tipoDer + "."));
                return TipoInterno.ERROR;
            }

            // Comparaciones: > < >= <= == != <>
            
            if (b.op == TokenType.GT || b.op == TokenType.LT || b.op == TokenType.GTE || b.op == TokenType.LTE
                    || b.op == TokenType.EQEQ || b.op == TokenType.NEQ || b.op == TokenType.NEQ_ALT) {
                
                if (esNumerico(tipoIzq) && esNumerico(tipoDer)) return TipoInterno.BOOLEANO;
                if (tipoIzq == tipoDer && tipoIzq != TipoInterno.ERROR) return TipoInterno.BOOLEANO;
                
                errores.add(reportarError(e.linea, e.columna, "Comparacion entre tipos incompatibles: " + tipoIzq + " y " + tipoDer + "."));
                return TipoInterno.ERROR;
            }

            // Aritméticos: + - * /
            
            if (b.op == TokenType.PLUS || b.op == TokenType.MINUS || b.op == TokenType.STAR || b.op == TokenType.SLASH) {
                if (esNumerico(tipoIzq) && esNumerico(tipoDer)) {
                    if (tipoIzq == TipoInterno.DECIMAL || tipoDer == TipoInterno.DECIMAL) return TipoInterno.DECIMAL;
                    return TipoInterno.ENTERO;
                }
                errores.add(reportarError(e.linea, e.columna, "Operacion aritmetica con tipos no numericos: " + tipoIzq + " y " + tipoDer + "."));
                return TipoInterno.ERROR;
            }

            errores.add(reportarError(e.linea, e.columna, "Operador binario no reconocido."));
            return TipoInterno.ERROR;
        }
        
        return TipoInterno.ERROR;
    }

    // ===================================
    // MÉTODOS AYUDANTES
    // ===================================

    private boolean esNumerico(TipoInterno t) { 
        return t == TipoInterno.ENTERO || t == TipoInterno.DECIMAL;
    }
    
    private boolean esBooleano(TipoInterno t) {
        // El modelo es estricto y solo permite BOOLEAN 
        return t == TipoInterno.BOOLEANO; 
    }

    /**
     * Comprueba si un valor (tipoDerecha) puede ser asignado a una variable (tipoVar).
     */
    private boolean esAsignable(TipoSimbolo tipoVar, TipoInterno tipoDerecha) {
        // Permitimos: long = ENTERO
        if (tipoVar == TipoSimbolo.LONG) return tipoDerecha == TipoInterno.ENTERO;
        // Permitimos: double = ENTERO (promoción) o double = DECIMAL
        if (tipoVar == TipoSimbolo.DOUBLE) return tipoDerecha == TipoInterno.ENTERO || tipoDerecha == TipoInterno.DECIMAL;
        return false;
    }

    /**
     * Comprueba que una variable exista en la tabla.
     * Si no existe, reporta un error y devuelve null.
     */
    private Simbolo requerirDeclarado(String nombre, int linea, int col) {
        Simbolo s = tablaSimbolos.obtener(nombre);
        if (s == null) {
            errores.add(reportarError(linea, col, "Identificador no declarado: '" + nombre + "'."));
        }
        return s;
    }

    private String reportarError(int linea, int col, String msg) {
        return "Error semantico [linea " + linea + ", col " + col + "]: " + msg;
    }
}