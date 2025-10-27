
package org.example;

import java.util.ArrayList;
import java.util.List;


public class RecolectorDeDeclaraciones {

    private final List<Token> tokens;
    private final TablaSimbolos tablaSimbolos = new TablaSimbolos();
    private final List<String> errores = new ArrayList<>();
    
    // Mantenemos un índice 'i' en lugar de un iterador para poder avanzar
    private int i = 0; 

    public RecolectorDeDeclaraciones(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * proceso de recolección.
     */
    public void recolectar() {
        while (i < tokens.size()) {
            Token t = tokens.get(i);

            // 1. Buscar solo KW_LONG o KW_DOUBLE
            TipoSimbolo tipoActual = null;
            if (t.tipo == TokenType.KW_LONG) {
                tipoActual = TipoSimbolo.LONG;
            } else if (t.tipo == TokenType.KW_DOUBLE) {
                tipoActual = TipoSimbolo.DOUBLE;
            }

            if (tipoActual == null) {
                // Si no es una declaración, la ignoramos y pasamos al siguiente token
                i++;
                continue;
            }

            // 2. Si encontramos un tipo, consumimos el token (avanzamos 'i')
            i++; 

            // 3. Procesar la lista de identificadores (ej: a, b, c;)
            boolean esperarMas = true;
            boolean necesitaIdentificador = true;

            while (esperarMas) {
                if (i >= tokens.size()) {
                    errores.add(errorSintactico(t.linea, t.columna, "Declaración incompleta (falta ';')."));
                    break;
                }

                Token siguiente = tokens.get(i);

                if (necesitaIdentificador) {
                    if (siguiente.tipo != TokenType.IDENTIFIER) {
                        errores.add(errorSintactico(siguiente.linea, siguiente.columna, "Se esperaba un identificador."));
                        // Intentamos sincronizar: buscar el ';' para seguir analizando
                        i = saltarHastaPuntoYComa(i);
                        break; // Salir del bucle 'while (esperarMas)'
                    }

                    // Tenemos un identificador, lo agregamos a la tabla
                    String nombre = siguiente.lexema;
                    Simbolo s = new Simbolo(nombre, tipoActual, "global", siguiente.linea);
                    
                    if (!tablaSimbolos.agregar(s)) {
                        errores.add(errorSintactico(siguiente.linea, siguiente.columna, "Identificador redeclarado: '" + nombre + "'."));
                    }
                    
                    i++; // Consumimos el IDENTIFIER
                    necesitaIdentificador = false;
                    continue;
                }

                // 4. Después de un ID, esperamos ',' o ';'
                if (siguiente.tipo == TokenType.COMMA) {
                    i++; // Consumimos ','
                    necesitaIdentificador = true; // Esperamos otro ID
                } else if (siguiente.tipo == TokenType.SEMICOLON) {
                    i++; // Consumimos ';'
                    esperarMas = false; // Terminamos esta declaración
                } else {
                    errores.add(errorSintactico(siguiente.linea, siguiente.columna, "Se esperaba ',' o ';' despues del identificador."));
                    i = saltarHastaPuntoYComa(i);
                    esperarMas = false; // Terminamos esta declaración (con error)
                }
            }
        }
    }

    /**
     * Método de "sincronización". En caso de error, avanza
     * hasta encontrar un ';' para poder seguir analizando lo que sigue.
     */
    private int saltarHastaPuntoYComa(int indice) {
        while (indice < tokens.size() && 
               tokens.get(indice).tipo != TokenType.SEMICOLON &&
               tokens.get(indice).tipo != TokenType.EOF) {
            indice++;
        }
        // Si encontramos ';', lo consumimos también
        if (indice < tokens.size() && tokens.get(indice).tipo == TokenType.SEMICOLON) {
            indice++; 
        }
        return indice;
    }

    private String errorSintactico(int linea, int col, String msg) {
        return "Error sintactico [linea " + linea + ", col " + col + "]: " + msg;
    }

    public TablaSimbolos getTablaSimbolos() {
        return tablaSimbolos;
    }

    public List<String> getErrores() {
        return errores;
    }
}
