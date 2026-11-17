package edu.ni.uam.proyecto_final_pae.util;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/** Listas completas de países, monedas y zonas horarias para los selectores. */
public final class Listas {

    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    private Listas() {
    }

    public static List<String> paises() {
        return Arrays.stream(Locale.getISOCountries())
                .map(codigo -> new Locale("", codigo).getDisplayCountry(ESPANOL))
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public static List<String> monedas() {
        return Currency.getAvailableCurrencies().stream()
                .map(moneda -> moneda.getCurrencyCode() + " — " + moneda.getDisplayName(ESPANOL))
                .sorted()
                .toList();
    }

    public static List<String> zonasHorarias() {
        return ZoneId.getAvailableZoneIds().stream().sorted().toList();
    }
}