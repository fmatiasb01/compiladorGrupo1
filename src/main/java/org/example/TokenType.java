
package org.example;

/**
 *
 * @author Nico
 */

public enum TokenType {
    // Fin de archivo
    EOF,

    // Literales
    INT_LITERAL,
    DOUBLE_LITERAL,
    STRING_LITERAL,

    // Identificadores y palabras reservadas
    IDENTIFIER,
    KW_LONG, KW_DOUBLE,
    KW_IF, KW_THEN, KW_ELSE, KW_WHILE, KW_BREAK,
    KW_READ, KW_WRITE,
    KW_TRUE, KW_FALSE, // opcional (booleanas)

    // Operadores aritméticos
    PLUS, MINUS, STAR, SLASH,

    // Operadores relacionales y lógicos
    GT, LT, GTE, LTE, EQEQ, NEQ, NEQ_ALT, // > < >= <= == != <>
    ANDAND, OROR, BANG,                  // && || !

    // Asignación
    EQUAL, PLUS_EQ, MINUS_EQ, STAR_EQ, SLASH_EQ,

    // Agrupación y puntuación
    LPAREN, RPAREN, LBRACE, RBRACE, SEMICOLON, COMMA
}
