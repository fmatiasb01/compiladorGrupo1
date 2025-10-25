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
            System.err.println("ERROR: No se especifico un archivo fuente.");
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
            System.err.println("Asegurese de que el archivo exista y la ruta sea correcta.");
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

        System.out.println("\n=== ERRORES LEXICOS ===");
        if (lexer.getErrores().isEmpty()) {
            System.out.println("(ninguno)");
        } else {
            lexer.getErrores().forEach(System.out::println);
        }

        // ======================================================
        // FASE 2: TABLA DE SÍMBOLOS (RECOLECCIÓN)
        // ======================================================

        // Usamos nuestras nuevas clases en español
        RecolectorDeDeclaraciones recolector = new RecolectorDeDeclaraciones(tokens);
        recolector.recolectar(); // Inicia el mini-parser

        System.out.println("\n=== TABLA DE SIMBOLOS ===");
        // Comprobamos si la tabla está vacía
        if (recolector.getTablaSimbolos().obtenerTodos().isEmpty()) {
            System.out.println("(vacia)");
        } else {
            // Imprimimos cabeceras que coinciden con el Simbolo.toString()
            System.out.println("Nombre         | Tipo     | Ambito   | Linea | Valor");
            System.out.println("---------------+----------+----------+-------+-------");
            // Imprimimos cada símbolo
            recolector.getTablaSimbolos().obtenerTodos().values().forEach(System.out::println);
        }

        // Imprimimos los errores sintácticos encontrados por el Recolector
        System.out.println("\n=== ERRORES SINTACTICOS (declaraciones) ===");
        if (recolector.getErrores().isEmpty()) {
            System.out.println("(ninguno)");
        } else {
            recolector.getErrores().forEach(System.out::println);
        }

        // ======================================================
        // FASE 3: PARSER (CONSTRUCCIÓN DEL AST)
        // ======================================================
        
        AnalizadorSintactico parser = new AnalizadorSintactico(tokens);
        List<Sentencia> sentencias = parser.analizar();

        System.out.println("\n=== PARSER: SENTENCIAS (AST) ===");
        if (sentencias.isEmpty()) {
             System.out.println("(ninguna)");
        } else {
            for (Sentencia s : sentencias) {
                System.out.println(s);
            }
        }

        System.out.println("\n=== ERRORES SINTATICOS (parser) ===");
        if (parser.getErrores().isEmpty()) {
            System.out.println("(ninguno)");
        } else {
            parser.getErrores().forEach(System.out::println);
        }
        
        
        // ======================================================
        // FASE 4: ANÁLISIS SEMÁNTICO
        // ======================================================
        
        // Pasamos el AST (sentencias) y la Tabla de Símbolos al analizador
        AnalizadorSemantico sema = new AnalizadorSemantico(sentencias, recolector.getTablaSimbolos());
        sema.analizar(); 

        System.out.println("\n=== ERRORES SEMANTICOS ===");
        if (sema.getErrores().isEmpty()) {
            System.out.println("(ninguno)");
        } else {
            sema.getErrores().forEach(System.out::println);
        }
        
        // Volvemos a imprimir la tabla, esta vez con los valores actualizados
        System.out.println("\n=== TABLA DE SIMBOLOS (post-semántico) ===");
        if (recolector.getTablaSimbolos().obtenerTodos().isEmpty()) {
            System.out.println("(vacia)");
        } else {
            System.out.println("Nombre         | Tipo     | Ambito   | Linea | Valor");
            System.out.println("---------------+----------+----------+-------+-------");
            recolector.getTablaSimbolos().obtenerTodos().values().forEach(System.out::println);
        }
    }
}