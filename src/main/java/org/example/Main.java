package org.example;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        String codigoFuente;
        
        if (args.length > 0) {
            // Opción 1: Leer el código desde un archivo pasado como argumento
            codigoFuente = Files.readString(Path.of(args[0]));
        } else {
            // Opción 2: Usar un bloque de código de prueba si no se pasa archivo
            // (He añadido casos de prueba léxicos, incluyendo errores)
            codigoFuente = """
                /* Programa de prueba para el Analizador Léxico.
                */
                    long contador, _sumaTotal;
                    double promedio_final;
                   
                    read(contador); // Leer el límite
                    _sumaTotal = 0;
                   
                        while (contador > 0 && !false) { // Prueba '&&' y '!'
                            long valor_actual;
                            read(valor_actual);
                   
                            if (valor_actual == 0) then
                                break; // Prueba de 'break' y 'then'
                   
                                _sumaTotal += valor_actual; // Prueba de '+=
                                contador = contador - 1;
                                }
                   
                            /* Comprobación final con operadores relacionales */
                            if (_sumaTotal <> 0) {
                                promedio_final = _sumaTotal / 2.0; // Literal double
                                write("El promedio es: ");
                                write(promedio_final);
                            } else {
                                write("No se ingresaron números.");
                            }
                   
                            // Prueba de identificador largo (debería dar error léxico)
                                long este_es_un_identificador_muy_largo_que_supera_los_32_chars;
                                   
                            // Prueba de error de caracter (debería dar error léxico)
                                double pi = 3.14;
                                long invalido = pi % 2; // '%' no es un token válido en la especificación
            """;
        }

        // ======================================================
        // FASE 1: ANÁLISIS LÉXICO
        // ======================================================
        
        // Usamos nuestra clase traducida
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

        
    }
}