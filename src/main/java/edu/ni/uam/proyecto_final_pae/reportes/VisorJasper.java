package edu.ni.uam.proyecto_final_pae.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerController;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JRSaveContributor;
import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

/** JRViewer completo: su barra nativa conserva los controles de sesión al guardar e imprimir. */
public final class VisorJasper extends JRViewer {
    private final Runnable validarAcceso;
    private final Consumer<SecurityException> accesoDenegado;

    /** Crear y utilizar únicamente desde el EDT de Swing. */
    public VisorJasper(JasperPrint impresion, Runnable validarAcceso, Consumer<SecurityException> accesoDenegado) {
        super(impresion, Locale.forLanguageTag("es"));
        this.validarAcceso = validarAcceso;
        this.accesoDenegado = accesoDenegado;
        setMinimumSize(new Dimension(0, 0));
        setPreferredSize(new Dimension(1000, 500));
        // SwingNode puede recibir su tamaño después del primer pintado del visor.
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent evento) { repintarCompleto(); }
            @Override public void componentShown(ComponentEvent evento) { repintarCompleto(); }
        });
        tlbToolBar.setSaveContributors(Arrays.stream(tlbToolBar.getSaveContributors())
                .map(GuardadoProtegido::new).toArray(JRSaveContributor[]::new));
    }

    private void repintarCompleto() {
        SwingUtilities.invokeLater(() -> {
            if (isShowing()) {
                validate();
                paintImmediately(0, 0, getWidth(), getHeight());
            }
        });
    }

    @Override protected JRViewerToolbar createToolbar() { return new BarraProtegida(viewerContext); }

    private final class BarraProtegida extends JRViewerToolbar {
        private BarraProtegida(JRViewerController contexto) {
            super(contexto);
            proteger(btnSave);
            proteger(btnPrint);
        }
        private void proteger(AbstractButton boton) {
            ActionListener[] acciones = boton.getActionListeners();
            for (var accion : acciones) boton.removeActionListener(accion);
            boton.addActionListener(evento -> {
                try {
                    validarAcceso.run();
                    for (var accion : acciones) accion.actionPerformed(evento);
                } catch (SecurityException e) {
                    accesoDenegado.accept(e);
                    JOptionPane.showMessageDialog(VisorJasper.this, e.getMessage(), "Acceso al reporte", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private final class GuardadoProtegido extends JRSaveContributor {
        private final JRSaveContributor original;
        private GuardadoProtegido(JRSaveContributor original) { this.original = original; }
        @Override public boolean accept(File archivo) { return original.accept(archivo); }
        @Override public String getDescription() { return original.getDescription(); }
        @Override public void save(JasperPrint impresion, File archivo) throws JRException {
            try {
                validarAcceso.run();
                original.save(impresion, archivo);
            } catch (SecurityException e) {
                accesoDenegado.accept(e);
                throw new JRException(e.getMessage(), e);
            }
        }
    }
}
