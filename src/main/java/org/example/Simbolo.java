
package org.example;

/**
 * Representa una entrada en la Tabla de Símbolos (una variable).
 */
public class Simbolo {

    public final String nombre;
    public final TipoSimbolo tipo;
    public final String ambito;
    public final int lineaDeclaracion;
    public String valor; 

    public Simbolo(String nombre, TipoSimbolo tipo, String ambito, int linea) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.ambito = ambito;
        this.lineaDeclaracion = linea;
        this.valor = null; // Se inicializa como nulo
    }

    @Override
    public String toString() {
        // Formato para que se alinee bien en la consola
        String valorStr = (valor == null) ? "N/A" : valor;
        return String.format("%-14s | %-8s | %-8s | %-5d | %s",
                nombre, tipo, ambito, lineaDeclaracion, valorStr);
    }
}
