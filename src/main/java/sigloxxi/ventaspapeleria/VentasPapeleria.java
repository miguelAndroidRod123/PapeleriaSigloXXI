package sigloxxi.ventaspapeleria;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.FlowLayout;
import sigloxxi.ventaspapeleria.controlador.VentasController;
import sigloxxi.ventaspapeleria.modelo.InventarioModelo;
import sigloxxi.ventaspapeleria.vista.MainFrame;

public final class VentasPapeleria {

    private VentasPapeleria() {
        // Clase de arranque: no se instancia.
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ignored) {}
        }

        SwingUtilities.invokeLater(() -> {
            JTextField fieldEmpleado = new JTextField("MIGUEL", 15);
            JPanel panelLogin = new JPanel(new FlowLayout());
            panelLogin.add(new JLabel("Ingrese el nombre del Empleado o Administrador:"));
            panelLogin.add(fieldEmpleado);

            int resultado = JOptionPane.showConfirmDialog(
                    null,
                    panelLogin,
                    "Inicio de Sesión - Sistema de Ventas Papelería",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (resultado == JOptionPane.OK_OPTION) {
                String empleado = fieldEmpleado.getText().trim();
                String nombreValido = empleado.isEmpty() ? "MIGUEL" : empleado;

                // Inicialización de Arquitectura MVC
                InventarioModelo modelo = new InventarioModelo();
                MainFrame vista = new MainFrame(nombreValido);
                new VentasController(vista, modelo, nombreValido);

                vista.setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}