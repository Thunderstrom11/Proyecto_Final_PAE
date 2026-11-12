package edu.ni.uam.proyecto_final_pae.config;

import edu.ni.uam.proyecto_final_pae.model.Ubicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculo aproximado de distancia y tiempo usando coordenadas geográficas. */
public final class GeoUtil {

    private static final double RADIO_TIERRA_KM = 6371.0;
    private static final double VELOCIDAD_PROMEDIO_KM_H = 55.0;
    private static final double FACTOR_CARRETERA = 1.25;

    private GeoUtil() {
    }

    public static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static double distanciaKm(Ubicacion origen, Ubicacion destino) {
        if (!tieneCoordenadas(origen) || !tieneCoordenadas(destino)) {
            throw new IllegalArgumentException("Las ubicaciones seleccionadas no tienen coordenadas configuradas.");
        }
        return distanciaKm(origen.getLatitud(), origen.getLongitud(),
                destino.getLatitud(), destino.getLongitud());
    }

    public static BigDecimal horasEstimadas(Ubicacion origen, Ubicacion destino) {
        double km = distanciaKm(origen, destino) * FACTOR_CARRETERA;
        double horas = Math.max(1.0, km / VELOCIDAD_PROMEDIO_KM_H);
        return BigDecimal.valueOf(horas).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean tieneCoordenadas(Ubicacion ubicacion) {
        return ubicacion != null && ubicacion.getLatitud() != null && ubicacion.getLongitud() != null;
    }
}
