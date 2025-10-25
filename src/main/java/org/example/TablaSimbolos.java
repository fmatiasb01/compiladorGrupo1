package org.example;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Administra la colección de Símbolos (variables).
 * Usa LinkedHashMap para mantener el orden de inserción.
 */
public class TablaSimbolos {
    
    // Un mapa que va del nombre del símbolo (String) al objeto Simbolo
    private final Map<String, Simbolo> tabla = new LinkedHashMap<>();

    /**
     * Intenta agregar un nuevo símbolo a la tabla.
     * @param s El Simbolo a agregar.
     * @return true si se agregó, false si ya existía (redeclaración).
     */
    public boolean agregar(Simbolo s) {
        if (tabla.containsKey(s.nombre)) {
            return false; // Error: Redeclaración
        }
        tabla.put(s.nombre, s);
        return true;
    }

    /**
     * Obtiene un símbolo de la tabla por su nombre.
     * @param nombre El nombre del símbolo a buscar.
     * @return El Simbolo, o null si no se encuentra.
     */
    public Simbolo obtener(String nombre) {
        return tabla.get(nombre);
    }

    /**
     * Devuelve el mapa completo de todos los símbolos.
     * @return Un Map con todos los símbolos.
     */
    public Map<String, Simbolo> obtenerTodos() {
        return tabla;
    }
}