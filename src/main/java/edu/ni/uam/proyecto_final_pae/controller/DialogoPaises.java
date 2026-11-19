package edu.ni.uam.proyecto_final_pae.controller;

import edu.ni.uam.proyecto_final_pae.config.DivisionesMundo;
import edu.ni.uam.proyecto_final_pae.util.Listas;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ventana para marcar los países con los que trabaja la empresa.
 * La usan el panel administrativo y el asistente de configuración inicial.
 */
public final class DialogoPaises {

    private DialogoPaises() {
    }

    /**
     * Muestra la lista de países con los activos ya marcados.
     * Devuelve la selección completa cuando se guarda, o vacío si se cancela.
     */
    public static Optional<List<String>> elegir(Window duenio, Collection<String> activos,
                                                String titulo, String encabezado, String ayuda) {
        ObservableSet<String> seleccionados =
                FXCollections.observableSet(new LinkedHashSet<>(activos == null ? List.of() : activos));
        FilteredList<String> filtrados = new FilteredList<>(
                FXCollections.observableArrayList(Listas.paises()), pais -> true);

        Label detalle = new Label(resumen(seleccionados));
        detalle.setWrapText(true);

        ListView<String> lista = new ListView<>(filtrados);
        lista.setPrefHeight(340);
        lista.setCellFactory(CheckBoxListCell.forListView(pais -> {
            SimpleBooleanProperty activo = new SimpleBooleanProperty(seleccionados.contains(pais));
            activo.addListener((o, anterior, marcado) -> {
                if (marcado) {
                    seleccionados.add(pais);
                } else {
                    seleccionados.remove(pais);
                }
                detalle.setText(resumen(seleccionados));
            });
            return activo;
        }));

        TextField buscar = new TextField();
        buscar.setPromptText("buscador");
        buscar.textProperty().addListener((o, anterior, texto) -> {
            String filtro = normalizar(texto);
            filtrados.setPredicate(pais -> filtro.isBlank() || normalizar(pais).contains(filtro));
        });

        Label instruccion = new Label(ayuda);
        instruccion.setWrapText(true);

        VBox contenido = new VBox(8, instruccion, buscar, lista, detalle);
        contenido.setPadding(new Insets(2, 0, 0, 0));
        contenido.setPrefWidth(440);

        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(encabezado);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, cancelar);
        dialogo.getDialogPane().setContent(contenido);
        if (duenio != null) {
            dialogo.initOwner(duenio);
            dialogo.initModality(Modality.WINDOW_MODAL);
        }
        Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(guardar);
        botonGuardar.addEventFilter(ActionEvent.ACTION, evento -> {
            if (seleccionados.isEmpty()) {
                detalle.setText("Marque al menos un país para continuar.");
                evento.consume();
            }
        });

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != guardar) {
            return Optional.empty();
        }
        List<String> elegidos = seleccionados.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toCollection(ArrayList::new));
        return Optional.of(elegidos);
    }

    /** Texto de apoyo con la cantidad de ubicaciones que aportan los países marcados. */
    public static String resumen(Set<String> paises) {
        if (paises.isEmpty()) {
            return "Ningún país marcado.";
        }
        int total = 0;
        for (String pais : paises) {
            total += DivisionesMundo.cantidad(pais);
        }
        String cuantos = paises.size() == 1 ? "1 país marcado" : paises.size() + " países marcados";
        return cuantos + " · " + total + " ubicaciones en total.";
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
