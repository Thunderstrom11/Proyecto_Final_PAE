package edu.ni.uam.proyecto_final_pae.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Convierte un ComboBox en un selector filtrable cuando tiene suficientes opciones.
 * El filtro se aplica sobre la misma lista visible en lugar de reemplazarla, y el valor
 * elegido se conserva cuando la lista se vuelve a filtrar.
 */
public final class ComboBuscador {

    private static final String OPCIONES = "comboBuscador.opciones";
    private static final String VISIBLES = "comboBuscador.visibles";
    private static final String SIN_FILTRO = "comboBuscador.sinFiltro";
    private static final String INSTALADO = "comboBuscador.instalado";
    private static final String AJUSTANDO = "comboBuscador.ajustando";

    private ComboBuscador() {
    }

    public static <T> void configurar(ComboBox<T> combo, List<T> opciones) {
        ObservableList<T> todas = opciones(combo);
        T previo = combo.getValue();
        todas.setAll(opciones);
        combo.setEditable(true);
        if (combo.getPromptText() == null || combo.getPromptText().isBlank()) {
            combo.setPromptText("buscador");
        }
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) {
                return etiqueta(item);
            }

            @Override
            public T fromString(String texto) {
                if (texto == null) {
                    return null;
                }
                String buscado = normalizar(texto);
                return opciones(combo).stream()
                        .filter(item -> normalizar(etiqueta(item)).equals(buscado))
                        .findFirst()
                        .orElse(null);
            }
        });
        FilteredList<T> visibles = visibles(combo);
        if (Boolean.TRUE.equals(combo.getProperties().get(INSTALADO))) {
            if (visibles != null) {
                visibles.setPredicate(sinFiltro(combo));
            }
            restablecer(combo, previo);
            return;
        }
        combo.getProperties().put(INSTALADO, true);

        FilteredList<T> filtrados = new FilteredList<>(todas, sinFiltro(combo));
        combo.getProperties().put(VISIBLES, filtrados);
        combo.setItems(filtrados);

        combo.getEditor().textProperty().addListener((o, anterior, texto) -> {
            if (ajustando(combo)) {
                return;
            }
            String filtro = normalizar(texto);
            String elegido = combo.getValue() == null ? "" : normalizar(etiqueta(combo.getValue()));
            ajustando(combo, true);
            try {
                if (filtro.isBlank() || filtro.equals(elegido)) {
                    filtrados.setPredicate(sinFiltro(combo));
                } else {
                    filtrados.setPredicate(item -> normalizar(etiqueta(item)).contains(filtro));
                }
            } finally {
                ajustando(combo, false);
            }
            if (filtro.isBlank() || filtro.equals(elegido) || filtrados.isEmpty()) {
                combo.hide();
            } else {
                combo.show();
            }
        });

        combo.valueProperty().addListener((o, anterior, actual) -> {
            if (actual == null || ajustando(combo)) {
                return;
            }
            ajustando(combo, true);
            try {
                combo.getEditor().setText(etiqueta(actual));
                combo.getEditor().positionCaret(combo.getEditor().getText().length());
                filtrados.setPredicate(sinFiltro(combo));
                // Al quitar el filtro la lista avisa del cambio y el combo pierde el valor.
                if (combo.getValue() != actual) {
                    combo.setValue(actual);
                }
            } finally {
                ajustando(combo, false);
            }
        });

        combo.getEditor().focusedProperty().addListener((o, anterior, enfocado) -> {
            if (!enfocado) {
                return;
            }
            T elegido = combo.getValue();
            ajustando(combo, true);
            try {
                filtrados.setPredicate(sinFiltro(combo));
                if (elegido != null && combo.getValue() == null) {
                    combo.setValue(elegido);
                    combo.getEditor().setText(etiqueta(elegido));
                }
            } finally {
                ajustando(combo, false);
            }
            combo.getEditor().selectAll();
        });

        combo.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() != KeyCode.ENTER) {
                return;
            }
            String texto = normalizar(combo.getEditor().getText());
            if (texto.isBlank()) {
                return;
            }
            String elegido = combo.getValue() == null ? "" : normalizar(etiqueta(combo.getValue()));
            if (texto.equals(elegido)) {
                return;
            }
            coincidencia(filtrados, texto).ifPresent(item -> {
                seleccionar(combo, item);
                evento.consume();
            });
        });
    }

    /** Agrega la opción a la lista del combo si todavía no está. */
    public static <T> void agregarOpcion(ComboBox<T> combo, T opcion) {
        if (opcion == null) {
            return;
        }
        ObservableList<T> todas = opciones(combo);
        if (todas.stream().noneMatch(item -> item.equals(opcion))) {
            todas.add(opcion);
        }
    }

    /** Agrega la opción si hace falta y la deja seleccionada, sin que el filtro la descarte. */
    public static <T> void seleccionar(ComboBox<T> combo, T opcion) {
        if (opcion == null) {
            return;
        }
        agregarOpcion(combo, opcion);
        FilteredList<T> filtrados = visibles(combo);
        ajustando(combo, true);
        try {
            if (filtrados != null) {
                filtrados.setPredicate(sinFiltro(combo));
            }
            combo.setValue(opcion);
            if (combo.getValue() != opcion) {
                combo.setValue(opcion);
            }
            combo.getEditor().setText(etiqueta(opcion));
            combo.getEditor().positionCaret(etiqueta(opcion).length());
        } finally {
            ajustando(combo, false);
        }
    }

    /** Si el valor anterior sigue en la lista, lo deja elegido después de recargar las opciones. */
    private static <T> void restablecer(ComboBox<T> combo, T previo) {
        if (previo == null || combo.getValue() != null || !opciones(combo).contains(previo)) {
            return;
        }
        seleccionar(combo, previo);
    }

    /** Valor elegido según el texto visible, o nulo si el texto no corresponde a ninguna opción. */
    public static <T> T valor(ComboBox<T> combo) {
        String texto = normalizar(combo.getEditor().getText());
        T elegido = combo.getValue();
        if (texto.isBlank()) {
            return elegido;
        }
        java.util.Optional<T> porTexto = opciones(combo).stream()
                .filter(item -> normalizar(etiqueta(item)).equals(texto))
                .findFirst();
        if (porTexto.isPresent()) {
            return porTexto.get();
        }
        return elegido != null && normalizar(etiqueta(elegido)).equals(texto) ? elegido : null;
    }

    private static <T> java.util.Optional<T> coincidencia(FilteredList<T> lista, String buscado) {
        return lista.stream()
                .filter(item -> normalizar(etiqueta(item)).equals(buscado))
                .findFirst()
                .or(() -> lista.stream().findFirst());
    }

    @SuppressWarnings("unchecked")
    private static <T> Predicate<T> sinFiltro(ComboBox<T> combo) {
        Object guardado = combo.getProperties().get(SIN_FILTRO);
        if (guardado instanceof Predicate<?> predicado) {
            return (Predicate<T>) predicado;
        }
        Predicate<T> nuevo = valor -> true;
        combo.getProperties().put(SIN_FILTRO, nuevo);
        return nuevo;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObservableList<T> opciones(ComboBox<T> combo) {
        Object guardada = combo.getProperties().get(OPCIONES);
        if (guardada instanceof ObservableList<?> lista) {
            return (ObservableList<T>) lista;
        }
        ObservableList<T> nueva = FXCollections.observableArrayList();
        combo.getProperties().put(OPCIONES, nueva);
        return nueva;
    }

    @SuppressWarnings("unchecked")
    private static <T> FilteredList<T> visibles(ComboBox<T> combo) {
        Object guardada = combo.getProperties().get(VISIBLES);
        return guardada instanceof FilteredList<?> lista ? (FilteredList<T>) lista : null;
    }

    private static boolean ajustando(ComboBox<?> combo) {
        return Boolean.TRUE.equals(combo.getProperties().get(AJUSTANDO));
    }

    private static void ajustando(ComboBox<?> combo, boolean valor) {
        combo.getProperties().put(AJUSTANDO, valor);
    }

    private static <T> String etiqueta(T item) {
        return item == null ? "" : String.valueOf(item);
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
