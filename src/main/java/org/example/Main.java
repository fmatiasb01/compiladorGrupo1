package org.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) { // <-- 'args' es la clave
        String codigoFuente;
        String nombreArchivo; // <-- Variable para guardar el nombre del archivo

        // --- MODIFICACIÓN CLAVE ---
        
        // 1. Verificar si el profesor pasó un argumento
        if (args.length > 0) {
            // Si lo hizo, ese es el archivo que compilaremos
            nombreArchivo = args[0];
        } else {
            // Si no pasó ningún archivo.
            System.err.println("ERROR: No se especificó un archivo fuente.");
            System.err.println("Uso: java -jar suCompilador.jar <archivo.txt>");
            return; // Salimos del programa
        }

        // 2. Intentar leer el archivo que nos pasó el profesor
        try {
            codigoFuente = Files.readString(Path.of(nombreArchivo));
            
            System.out.println("--- Compilando archivo: " + nombreArchivo + " ---");
            // No imprimimos el código fuente, solo los resultados.
            System.out.println("----------------------------------------------\n");

        } catch (NoSuchFileException e) {
            System.err.println("ERROR: No se pudo encontrar el archivo '" + nombreArchivo + "'.");
            System.err.println("Asegúrese de que el archivo exista y la ruta sea correcta.");
            return;
        } catch (IOException e) {
            System.err.println("ERROR al leer el archivo: " + e.getMessage());
            return;
        }
        // --- FIN DE LA MODIFICACIÓN ---


        // ======================================================
        // FASE 1: ANÁLISIS LÉXICO
        // (Esta parte queda exactamente igual)
        // ======================================================
        
        AnalizadorLexico lexer = new AnalizadorLexico(codigoFuente);
        List<Token> tokens = lexer.analizarTokens();

        System.out.println("=== TOKENS ===");
        if (tokens.isEmpty()) {
            System.out.println("(ninguno)");
        } else {
            for (Token t : tokens) {
                System.out.println(t);
            }
        }

        System.out.println("\n=== ERRORES LÉXICOS ===");
        if (lexer.getErrores().isEmpty()) {
            System.out.println("(ninguno)");
        } else {
            lexer.getErrores().forEach(System.out::println);
        }

        // ======================================================
        // FASES FUTURAS (Comentadas)
        // ======================================================
        
        /*
        (Aquí va el resto de tu código Main.java que tenías comentado...)
        */
    }
}