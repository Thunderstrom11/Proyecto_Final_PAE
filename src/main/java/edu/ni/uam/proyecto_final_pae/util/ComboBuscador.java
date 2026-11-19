package edu.ni.uam.proyecto_final_pae.util;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Convierte un ComboBox en un selector filtrable cuando tiene suficientes opciones. */
public final class ComboBuscador {

    private static final String OPCIONES = "comboBuscador.opciones";
    private static final String INSTALADO = "comboBuscador.instalado";
    private static final String AJUSTANDO = "comboBuscador.ajustando";

    private ComboBuscador() {
    }

    public static <T> void configurar(ComboBox<T> combo, List<T> opciones) {
        List<T> copia = new ArrayList<>(opciones);
        combo.getProperties().put(OPCIONES, copia);
        combo.setItems(FXCollections.observableArrayList(copia));
        combo.setEditable(true);
        combo.setPromptText("buscador");
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : String.valueOf(item);
            }

            @Override
            public T fromString(String texto) {
                if (texto == null) {
                    return null;
                }
                @SuppressWarnings("unchecked")
                List<T> todos = (List<T>) combo.getProperties().get(OPCIONES);
                if (todos == null) {
                    return null;
                }
                String buscado = normalizar(texto);
                return todos.stream()
                        .filter(item -> normalizar(String.valueOf(item)).equals(buscado))
                        .findFirst()
                        .orElse(null);
            }
        });
        if (Boolean.TRUE.equals(combo.getProperties().get(INSTALADO))) {
            return;
        }
        combo.getProperties().put(INSTALADO, true);

        combo.getEditor().textProperty().addListener((o, anterior, texto) -> {
            if (ajustando(combo)) {
                return;
            }
            @SuppressWarnings("unchecked")
            List<T> todos = (List<T>) combo.getProperties().get(OPCIONES);
            if (todos == null) {
                return;
            }
            String filtro = normalizar(texto);
            String valorActual = combo.getValue() == null
                    ? "" : normalizar(etiqueta(combo, combo.getValue()));
            List<T> filtrados = filtro.isBlank() || filtro.equals(valorActual)
                    ? todos
                    : todos.stream()
                            .filter(item -> normalizar(etiqueta(combo, item)).contains(filtro))
                            .toList();
            ajustando(combo, true);
            try {
                combo.setItems(FXCollections.observableArrayList(filtrados));
                combo.getEditor().setText(texto == null ? "" : texto);
                if (!filtro.isBlank() && !filtro.equals(valorActual) && !filtrados.isEmpty()) {
                    combo.show();
                }
            } finally {
                ajustando(combo, false);
            }
        });

        combo.valueProperty().addListener((o, anterior, actual) -> {
            if (actual == null || ajustando(combo)) {
                return;
            }
            ajustando(combo, true);
            try {
                @SuppressWarnings("unchecked")
                List<T> todos = (List<T>) combo.getProperties().get(OPCIONES);
                if (todos != null) {
                    combo.setItems(FXCollections.observableArrayList(todos));
                }
                combo.getEditor().setText(etiqueta(combo, actual));
            } finally {
                ajustando(combo, false);
            }
        });

        combo.getEditor().focusedProperty().addListener((o, anterior, enfocado) -> {
            if (enfocado) {
                @SuppressWarnings("unchecked")
                List<T> todos = (List<T>) combo.getProperties().get(OPCIONES);
                if (todos != null) {
                    combo.setItems(FXCollections.observableArrayList(todos));
                }
                combo.getEditor().selectAll();
            }
        });

        combo.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (evento.getCode() != KeyCode.ENTER) {
                return;
            }
            String texto = normalizar(combo.getEditor().getText());
            String valorActual = combo.getValue() == null
                    ? "" : normalizar(etiqueta(combo, combo.getValue()));
            if (!texto.isBlank() && !texto.equals(valorActual) && !combo.getItems().isEmpty()) {
                combo.getSelectionModel().selectFirst();
                evento.consume();
            }
        });
    }

    public static <T> void agregarOpcion(ComboBox<T> combo, T opcion) {
        @SuppressWarnings("unchecked")
        List<T> todos = (List<T>) combo.getProperties().get(OPCIONES);
        if (todos == null) {
            todos = new ArrayList<>(combo.getItems());
            combo.getProperties().put(OPCIONES, todos);
        }
        if (todos.stream().noneMatch(item -> item.equals(opcion))) {
            todos.add(opcion);
        }
        combo.setItems(FXCollections.observableArrayList(todos));
    }

    /** Agrega la opción si hace falta y la deja seleccionada, sin que el filtro la descarte. */
    public static <T> void seleccionar(ComboBox<T> combo, T opcion) {
        if (opcion == null) {
            return;
        }
        agregarOpcion(combo, opcion);
        ajustando(combo, true);
        try {
            combo.getSelectionModel().select(opcion);
            combo.setValue(opcion);
            combo.getEditor().setText(etiqueta(combo, opcion));
        } finally {
            ajustando(combo, false);
        }
    }

    private static boolean ajustando(ComboBox<?> combo) {
        return Boolean.TRUE.equals(combo.getProperties().get(AJUSTANDO));
    }

    private static void ajustando(ComboBox<?> combo, boolean valor) {
        combo.getProperties().put(AJUSTANDO, valor);
    }

    private static <T> String etiqueta(ComboBox<T> combo, T item) {
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
