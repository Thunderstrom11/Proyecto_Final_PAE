package edu.ni.uam.proyecto_final_pae.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Tooltip;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Instala tooltips y texto accesible en botones, filtros y toggles. */
public final class Tooltips {

    private static final Map<String, String> POR_ID = new HashMap<>();
    private static final Map<String, String> POR_TEXTO = new HashMap<>();
    private static final Map<Node, Boolean> INSTALADOS = new ConcurrentHashMap<>();

    static {
        POR_ID.put("btnRefrescarFlota", "Actualizar los datos de flota.");
        POR_ID.put("btnActualizar", "Actualizar la cabina con los datos más recientes.");
        POR_ID.put("btnReporte", "Abrir el reporte financiero.");
        POR_ID.put("btnPanelAdmin", "Abrir el panel administrativo.");
        POR_ID.put("btnLimpiarBusquedaCuenta", "Limpiar la búsqueda de cuentas.");
        POR_ID.put("btnLimpiarBusquedaUnidad", "Limpiar la búsqueda de unidades.");
        POR_ID.put("btnLimpiarBusquedaOperador", "Limpiar la búsqueda de operadores.");
        POR_ID.put("btnLimpiarBusquedaCliente", "Limpiar la búsqueda de clientes.");
        POR_ID.put("btnLimpiarBusquedaViaje", "Limpiar la búsqueda y los rangos de cobro.");
        POR_ID.put("btnLimpiarFiltroViaje", "Limpiar el filtro de viajes.");
        POR_ID.put("btnLimpiarFiltroUnidades", "Limpiar el filtro de unidades.");
        POR_ID.put("btnLimpiarFiltroOperadores", "Limpiar el filtro de operadores.");
        POR_ID.put("btnLimpiarFiltros", "Limpiar todos los filtros de cargas.");
        POR_ID.put("btnNuevoCliente", "Crear un cliente nuevo.");
        POR_ID.put("btnBuscar", "Buscar la dirección o el lugar escrito.");
        POR_ID.put("btnPegar", "Pegar un enlace de Google Maps o coordenadas.");
        POR_ID.put("btnEditarLugar", "Habilitar o bloquear la edición del lugar.");
        POR_ID.put("btnGuardarCambios", "Guardar los cambios del lugar.");
        POR_ID.put("btnBuscarOrigen", "Elegir la ubicación exacta del origen. Primero seleccione la base.");
        POR_ID.put("btnBuscarDestino", "Elegir la ubicación exacta del destino. Primero seleccione la base.");
        POR_ID.put("btnGoogleOrigen", "Abrir el origen seleccionado en Google Maps.");
        POR_ID.put("btnGoogleDestino", "Abrir el destino seleccionado en Google Maps.");
        POR_ID.put("btnCancelarViaje", "Cancelar el viaje seleccionado y liberar sus recursos.");
        POR_ID.put("btnCuentaTodos", "Mostrar cuentas de todos los roles.");
        POR_ID.put("btnCuentaAdmins", "Mostrar solo cuentas de administrador.");
        POR_ID.put("btnCuentaOperadores", "Mostrar solo cuentas de operador.");
        POR_ID.put("btnCuentaEstadoTodos", "Mostrar cuentas activas e inactivas.");
        POR_ID.put("btnCuentaActivos", "Mostrar solo cuentas activas.");
        POR_ID.put("btnCuentaInactivos", "Mostrar solo cuentas inactivas.");
        POR_ID.put("btnLimpiarCuentaForm", "Vaciar el formulario de cuentas.");
        POR_ID.put("btnLimpiarUbicacionForm", "Vaciar el formulario de ubicaciones.");
        POR_ID.put("btnLimpiarRutaForm", "Vaciar el formulario de rutas.");
        POR_ID.put("btnClave", "Cambiar la contraseña de la cuenta actual.");

        POR_TEXTO.put("Ingresar", "Validar las credenciales e iniciar sesión.");
        POR_TEXTO.put("Administrador", "Entrar con una cuenta de administrador.");
        POR_TEXTO.put("Operador", "Entrar con una cuenta de operador.");
        POR_TEXTO.put("Origen", "Buscar por el origen de la ruta.");
        POR_TEXTO.put("Destino", "Buscar por el destino de la ruta.");
        POR_TEXTO.put("Cerrar sesión", "Cerrar la sesión actual y volver al inicio.");
        POR_TEXTO.put("Cargas", "Abrir la gestión de cargas.");
        POR_TEXTO.put("Viajes", "Abrir la gestión de viajes.");
        POR_TEXTO.put("Monitor", "Abrir el monitor de flota.");
        POR_TEXTO.put("Guardar", "Guardar los datos del formulario.");
        POR_TEXTO.put("Guardar nueva", "Guardar una unidad nueva.");
        POR_TEXTO.put("Guardar nuevo", "Guardar un registro nuevo.");
        POR_TEXTO.put("Guardar y continuar", "Guardar la configuración inicial y continuar.");
        POR_TEXTO.put("Guardar ubicación", "Guardar una ubicación nueva.");
        POR_TEXTO.put("Guardar ruta", "Guardar una ruta y su tiempo estimado.");
        POR_TEXTO.put("Crear cuenta", "Crear una cuenta de acceso.");
        POR_TEXTO.put("Crear viaje", "Crear el viaje con los datos del formulario.");
        POR_TEXTO.put("Actualizar", "Actualizar el registro seleccionado.");
        POR_TEXTO.put("Actualizar seleccionada", "Actualizar el registro seleccionado.");
        POR_TEXTO.put("Actualizar seleccionado", "Actualizar el registro seleccionado.");
        POR_TEXTO.put("Actualizar reporte", "Volver a calcular el reporte.");
        POR_TEXTO.put("Eliminar", "Eliminar el registro seleccionado.");
        POR_TEXTO.put("Eliminar seleccionada", "Eliminar el registro seleccionado.");
        POR_TEXTO.put("Eliminar seleccionado", "Eliminar el registro seleccionado.");
        POR_TEXTO.put("Limpiar", "Vaciar el formulario.");
        POR_TEXTO.put("Limpiar formulario", "Vaciar el formulario sin guardar.");
        POR_TEXTO.put("Buscar lugar", "Buscar un punto exacto en el mapa.");
        POR_TEXTO.put("Google Maps", "Abrir la ubicación en Google Maps.");
        POR_TEXTO.put("Ver en Google Maps", "Abrir la ubicación en Google Maps.");
        POR_TEXTO.put("Cancelar", "Cerrar sin guardar cambios.");
        POR_TEXTO.put("Confirmar lugar", "Guardar el lugar seleccionado y volver a Viajes.");
        POR_TEXTO.put("Finalizar entrega", "Solo los viajes en curso se pueden finalizar. Si está en pausa, el operador debe reanudar primero.");
        POR_TEXTO.put("Limpiar fechas", "Quitar el rango de fechas y calcular sobre toda la historia.");
        POR_TEXTO.put("Iniciar trayecto", "Iniciar el viaje asignado.");
        POR_TEXTO.put("Pausa", "Pausar el viaje actual.");
        POR_TEXTO.put("Reanudar", "Reanudar el viaje pausado.");
        POR_TEXTO.put("Confirmar entrega", "Confirmar la entrega del viaje actual.");
        POR_TEXTO.put("Todos", "Mostrar todos los registros.");
        POR_TEXTO.put("Pendientes", "Mostrar solo cargas pendientes.");
        POR_TEXTO.put("Entregadas", "Mostrar solo cargas entregadas.");
        POR_TEXTO.put("En tránsito", "Mostrar solo cargas en tránsito.");
        POR_TEXTO.put("Activos", "Mostrar solo registros activos.");
        POR_TEXTO.put("Finalizados", "Mostrar solo registros finalizados.");
        POR_TEXTO.put("Cerrar", "Cerrar esta ventana.");
        POR_TEXTO.put("Nuevo cliente", "Crear un cliente nuevo.");
        POR_TEXTO.put("Crear cliente", "Crear un cliente nuevo.");
        POR_TEXTO.put("Restablecer contraseña", "Asignar una contraseña nueva a la cuenta seleccionada.");
    }

    private Tooltips() {
    }

    public static void instalar(Node raiz) {
        if (raiz == null) {
            return;
        }
        if (raiz instanceof ButtonBase boton) {
            instalarBoton(boton);
        }
        if (raiz instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) {
                instalar(hijo);
            }
        }
    }

    private static void instalarBoton(ButtonBase boton) {
        if (INSTALADOS.putIfAbsent(boton, Boolean.TRUE) != null) {
            return;
        }
        String texto = boton.getText() == null ? "" : boton.getText().trim();
        String ayuda = boton.getId() == null ? null : POR_ID.get(boton.getId());
        if (ayuda == null) {
            ayuda = texto.isEmpty() ? null : POR_TEXTO.get(texto);
        }
        if (ayuda == null && boton.getTooltip() != null
                && boton.getTooltip().getText() != null) {
            ayuda = boton.getTooltip().getText();
        }
        if (ayuda == null && !texto.isEmpty()) {
            ayuda = texto;
        }
        if (ayuda == null) {
            ayuda = "Ejecutar esta acción.";
        }
        if (boton.getTooltip() == null) {
            Tooltip tooltip = new Tooltip(ayuda);
            tooltip.setWrapText(true);
            tooltip.setShowDelay(javafx.util.Duration.millis(300));
            boton.setTooltip(tooltip);
        }
        if (boton.getAccessibleText() == null || boton.getAccessibleText().isBlank()) {
            boton.setAccessibleText(texto.isEmpty() ? ayuda : texto);
        }
    }
}
