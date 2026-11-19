package edu.ni.uam.proyecto_final_pae.reportes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class FormatoReporte {
    private FormatoReporte() { }
    public static String dinero(BigDecimal valor, String moneda) {
        String simbolo = switch (moneda) {
            case "NIO" -> "C$";
            case "USD" -> "$";
            case "EUR" -> "€";
            case "CRC" -> "₡";
            case "GTQ" -> "Q";
            case "HNL" -> "L";
            case "PAB" -> "B/.";
            case "MXN" -> "MX$";
            default -> moneda;
        };
        return simbolo + " " + String.format(Locale.US, "%,.2f", valor.setScale(2, RoundingMode.HALF_UP));
    }
}
