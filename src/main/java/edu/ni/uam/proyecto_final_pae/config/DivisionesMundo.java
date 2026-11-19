package edu.ni.uam.proyecto_final_pae.config;

import edu.ni.uam.proyecto_final_pae.dao.PaisDAO;
import edu.ni.uam.proyecto_final_pae.dao.UbicacionDAO;
import edu.ni.uam.proyecto_final_pae.model.Ubicacion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Provincias, departamentos y ciudades de cada país, listos para registrarse como ubicaciones.
 * Los datos vienen incluidos en la aplicación, así que no hace falta conexión a internet.
 */
public final class DivisionesMundo {

    private static final String RECURSO = "/edu/ni/uam/proyecto_final_pae/datos/mundo.tsv.gz";
    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    private static final Map<String, List<Ubicacion>> CACHE = new HashMap<>();
    private static Map<String, String> codigos;
    private static Map<String, Integer> totales;

    private DivisionesMundo() {
    }

    /** Resultado de activar países: cuántos quedaron activos y cuántas ubicaciones se agregaron. */
    public record Resumen(int paises, int ubicaciones) {
    }

    /** Cuántas ubicaciones aporta un país. Devuelve 0 si no hay datos para él. */
    public static synchronized int cantidad(String pais) {
        String codigo = codigo(pais);
        return codigo == null ? 0 : totales().getOrDefault(codigo, 0);
    }

    /** Provincias y ciudades del país, en el orden en que se guardan. */
    public static synchronized List<Ubicacion> de(String pais) {
        String codigo = codigo(pais);
        if (codigo == null) {
            return List.of();
        }
        return CACHE.computeIfAbsent(codigo, clave -> leer(clave, pais));
    }

    /**
     * Deja activos exactamente los países indicados y agrega las ubicaciones que falten.
     * Las ubicaciones que ya existen no se tocan.
     */
    public static Resumen activar(Collection<String> paises) {
        List<String> activos = new ArrayList<>(new LinkedHashSet<>(paises));
        new PaisDAO().reemplazar(activos);
        List<Ubicacion> nuevas = new ArrayList<>();
        for (String pais : activos) {
            nuevas.addAll(de(pais));
        }
        return new Resumen(activos.size(), new UbicacionDAO().guardarNuevas(nuevas));
    }

    private static List<Ubicacion> leer(String codigo, String pais) {
        List<Ubicacion> lista = new ArrayList<>();
        try (BufferedReader lector = abrir()) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                String[] campos = linea.split("\t");
                if (campos.length < 5 || !codigo.equals(campos[1])) {
                    continue;
                }
                if ("P".equals(campos[0])) {
                    lista.add(ubicacion(campos[2], campos[2], pais, campos[3], campos[4]));
                } else if ("C".equals(campos[0]) && campos.length >= 6) {
                    lista.add(ubicacion(campos[3], campos[2], pais, campos[4], campos[5]));
                }
            }
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
        return List.copyOf(lista);
    }

    private static Map<String, Integer> totales() {
        if (totales != null) {
            return totales;
        }
        Map<String, Integer> cuenta = new HashMap<>();
        try (BufferedReader lector = abrir()) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                String[] campos = linea.split("\t");
                if (campos.length >= 2) {
                    cuenta.merge(campos[1], 1, Integer::sum);
                }
            }
        } catch (IOException | RuntimeException e) {
            cuenta.clear();
        }
        totales = cuenta;
        return totales;
    }

    private static Map<String, String> codigos() {
        if (codigos != null) {
            return codigos;
        }
        Map<String, String> mapa = new HashMap<>();
        for (String iso : Locale.getISOCountries()) {
            String nombre = new Locale("", iso).getDisplayCountry(ESPANOL);
            if (nombre != null && !nombre.isBlank()) {
                mapa.putIfAbsent(nombre, iso);
            }
        }
        codigos = mapa;
        return codigos;
    }

    private static String codigo(String pais) {
        if (pais == null || pais.isBlank()) {
            return null;
        }
        Map<String, String> mapa = codigos();
        String directo = mapa.get(pais);
        if (directo != null) {
            return directo;
        }
        String buscado = normalizar(pais);
        return mapa.entrySet().stream()
                .filter(entrada -> normalizar(entrada.getKey()).equals(buscado))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static Ubicacion ubicacion(String nombre, String departamento, String pais,
                                       String latitud, String longitud) {
        return new Ubicacion(nombre, departamento, pais, decimal(latitud), decimal(longitud));
    }

    private static Double decimal(String texto) {
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BufferedReader abrir() throws IOException {
        InputStream entrada = DivisionesMundo.class.getResourceAsStream(RECURSO);
        if (entrada == null) {
            throw new IOException("No se encontró el catálogo de lugares.");
        }
        return new BufferedReader(new InputStreamReader(new GZIPInputStream(entrada), StandardCharsets.UTF_8));
    }

    private static String normalizar(String texto) {
        return java.text.Normalizer.normalize(texto.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
