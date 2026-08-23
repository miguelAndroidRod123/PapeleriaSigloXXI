package sigloxxi.ventaspapeleria.controlador;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.prefs.Preferences;
import sigloxxi.ventaspapeleria.modelo.InventarioModelo;
import sigloxxi.ventaspapeleria.modelo.Producto;
import sigloxxi.ventaspapeleria.vista.MainFrame;

public class VentasController {

    private final MainFrame vista;
    private final InventarioModelo modeloInventario;
    private final String nombreEmpleado;

    private static final String VERSION_SOFTWARE = "v1.1.0";
    private static final String NOMBRE_ARCHIVO_SESION = "sesion_actual.recuperacion";
    private static final List<String> registroVentasDelDia = new ArrayList<>();
    private static double acumuladoTotalDia = 0.0;
    private static double acumuladoEfectivo = 0.0;
    private static double acumuladoTransferencia = 0.0;
    private static double acumuladoCostoMayoristaDia = 0.0;

    private static double acumuladoPagosEmpleadosDia = 0.0;
    private static final List<String> registroPagosEmpleadosDia = new ArrayList<>();
    private static int contadorFacturas = 1;

    private double totalGeneral = 0.0;
    private javax.swing.Timer timerMonitorInventario;

    public VentasController(MainFrame vista, InventarioModelo modeloInventario, String nombreEmpleado) {
        this.vista = vista;
        this.modeloInventario = modeloInventario;
        this.nombreEmpleado = nombreEmpleado;

        initListeners();
        verificarSesionInterrumpida();
        iniciarMonitorInventario();
    }

    /**
     * Verifica el estado REAL de la conexión con el inventario Excel
     * (sin diálogos emergentes) y refleja ese resultado en el indicador
     * de la barra de estado. Se ejecuta al iniciar y luego periódicamente,
     * para que el punto verde/rojo siempre corresponda a la realidad y no
     * a un texto fijo de adorno.
     */
    private void actualizarIndicadorInventario() {
        File archivo = modeloInventario.verificarConexionSilenciosa();
        boolean conectado = (archivo != null);
        String detalle = conectado ? archivo.getName() : null;
        SwingUtilities.invokeLater(() -> vista.actualizarEstadoInventario(conectado, detalle));
    }

    private void iniciarMonitorInventario() {
        actualizarIndicadorInventario();
        timerMonitorInventario = new javax.swing.Timer(8000, e -> actualizarIndicadorInventario());
        timerMonitorInventario.start();
    }

    // =========================================================================
    // RESPALDO Y RECUPERACIÓN DE SESIÓN ANTE CIERRES INESPERADOS
    // -------------------------------------------------------------------------
    // Guarda el carrito en curso y los acumulados del día en un archivo local
    // cada vez que hay un cambio relevante. Si el programa se cierra de forma
    // abrupta (corte de energía, cuelgue, cierre accidental) antes de exportar
    // la venta del día, al volver a abrir se ofrece restaurar todo tal como
    // estaba, sin tener que volver a digitar nada.
    // =========================================================================
    private File obtenerArchivoSesion() {
        File escritorio = FileSystemView.getFileSystemView().getHomeDirectory();
        File carpetaSesion = new File(new File(escritorio, "DOCUMENTOS SIGLO XXI"), "SESION");
        if (!carpetaSesion.exists()) {
            carpetaSesion.mkdirs();
        }
        return new File(carpetaSesion, NOMBRE_ARCHIVO_SESION);
    }

    private String codificar(String texto) {
        return Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private String decodificar(String textoBase64) {
        return new String(Base64.getDecoder().decode(textoBase64), StandardCharsets.UTF_8);
    }

    /**
     * Vuelca el estado actual de la sesión (carrito en curso + acumulados
     * del día) al archivo de respaldo. Se llama después de cada acción que
     * cambia ese estado, para que el respaldo esté siempre al día sin
     * depender de un cierre ordenado del programa.
     */
    private void guardarSesionActual() {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(obtenerArchivoSesion()), StandardCharsets.UTF_8))) {

            writer.println("FECHA_SESION=" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            writer.println("EMPLEADO=" + codificar(nombreEmpleado == null ? "" : nombreEmpleado));
            writer.println("CONTADOR_FACTURAS=" + contadorFacturas);
            writer.println("ACUMULADO_TOTAL_DIA=" + acumuladoTotalDia);
            writer.println("ACUMULADO_EFECTIVO=" + acumuladoEfectivo);
            writer.println("ACUMULADO_TRANSFERENCIA=" + acumuladoTransferencia);
            writer.println("ACUMULADO_COSTO_MAYORISTA_DIA=" + acumuladoCostoMayoristaDia);
            writer.println("ACUMULADO_PAGOS_EMPLEADOS_DIA=" + acumuladoPagosEmpleadosDia);
            writer.println("TOTAL_GENERAL_CARRITO=" + totalGeneral);

            writer.println("---CARRITO---");
            DefaultTableModel modelo = vista.getModeloTabla();
            for (int i = 0; i < modelo.getRowCount(); i++) {
                StringBuilder fila = new StringBuilder();
                for (int c = 0; c < modelo.getColumnCount(); c++) {
                    Object valor = modelo.getValueAt(i, c);
                    fila.append(valor == null ? "" : valor.toString());
                    if (c < modelo.getColumnCount() - 1) fila.append("|");
                }
                writer.println(codificar(fila.toString()));
            }

            writer.println("---VENTAS_DEL_DIA---");
            for (String venta : registroVentasDelDia) {
                writer.println(codificar(venta));
            }

            writer.println("---PAGOS_EMPLEADOS_DIA---");
            for (String pago : registroPagosEmpleadosDia) {
                writer.println(codificar(pago));
            }

            writer.println("---FIN---");
        } catch (IOException e) {
            System.err.println("No se pudo guardar el respaldo de la sesión: " + e.getMessage());
        }
    }

    /** Elimina el respaldo de sesión: se llama cuando ya no hace falta (día exportado y cerrado en orden, o el usuario descarta la recuperación). */
    private void borrarArchivoSesion() {
        File archivo = obtenerArchivoSesion();
        if (archivo.exists()) {
            archivo.delete();
        }
    }

    /**
     * Al iniciar el programa, revisa si quedó un respaldo de una sesión del
     * MISMO día que no se cerró en orden (venta en curso o ventas cobradas
     * que aún no se habían exportado). Si lo encuentra, ofrece restaurarlo.
     * Un respaldo de un día anterior se descarta en silencio: ya no aplica.
     */
    private void verificarSesionInterrumpida() {
        File archivo = obtenerArchivoSesion();
        if (!archivo.exists()) {
            return;
        }

        try {
            List<String> lineas = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    lineas.add(linea);
                }
            }

            String fechaSesion = null;
            for (String linea : lineas) {
                if (linea.startsWith("FECHA_SESION=")) {
                    fechaSesion = linea.substring("FECHA_SESION=".length());
                    break;
                }
            }

            String hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if (fechaSesion == null || !fechaSesion.equals(hoy)) {
                // Respaldo de un día distinto: ya no es relevante, se descarta.
                borrarArchivoSesion();
                return;
            }

            boolean hayAlgoQueRecuperar = false;
            String seccion = "";
            for (String linea : lineas) {
                if (linea.equals("---CARRITO---") || linea.equals("---VENTAS_DEL_DIA---")
                        || linea.equals("---PAGOS_EMPLEADOS_DIA---") || linea.equals("---FIN---")) {
                    seccion = linea;
                    continue;
                }
                if (!seccion.isEmpty() && !linea.isBlank()) {
                    hayAlgoQueRecuperar = true;
                    break;
                }
            }

            if (!hayAlgoQueRecuperar) {
                // El carrito estaba vacío y no había ventas del día: nada que recuperar.
                borrarArchivoSesion();
                return;
            }

            int opcion = JOptionPane.showConfirmDialog(vista,
                    "Se detectó que el sistema no se cerró correctamente la última vez\n"
                    + "(posible corte de energía o cierre inesperado) y hay ventas de hoy\n"
                    + "que aún no se habían exportado.\n\n"
                    + "¿Desea recuperar el carrito y las ventas acumuladas del día tal como estaban?",
                    "Sesión Anterior Sin Cerrar",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (opcion == JOptionPane.YES_OPTION) {
                restaurarSesionDesdeLineas(lineas);
                JOptionPane.showMessageDialog(vista,
                        "Sesión restaurada con éxito. Puede continuar donde se quedó.",
                        "Recuperación Completa",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                borrarArchivoSesion();
            }
        } catch (Exception e) {
            System.err.println("No se pudo leer el respaldo de sesión: " + e.getMessage());
        }
    }

    private void restaurarSesionDesdeLineas(List<String> lineas) {
        String seccionActual = "";
        for (String linea : lineas) {
            if (linea.equals("---CARRITO---") || linea.equals("---VENTAS_DEL_DIA---")
                    || linea.equals("---PAGOS_EMPLEADOS_DIA---") || linea.equals("---FIN---")) {
                seccionActual = linea;
                continue;
            }

            if (seccionActual.isEmpty()) {
                if (linea.startsWith("CONTADOR_FACTURAS=")) {
                    contadorFacturas = Integer.parseInt(linea.substring(linea.indexOf('=') + 1));
                } else if (linea.startsWith("ACUMULADO_TOTAL_DIA=")) {
                    acumuladoTotalDia = Double.parseDouble(linea.substring(linea.indexOf('=') + 1));
                } else if (linea.startsWith("ACUMULADO_EFECTIVO=")) {
                    acumuladoEfectivo = Double.parseDouble(linea.substring(linea.indexOf('=') + 1));
                } else if (linea.startsWith("ACUMULADO_TRANSFERENCIA=")) {
                    acumuladoTransferencia = Double.parseDouble(linea.substring(linea.indexOf('=') + 1));
                } else if (linea.startsWith("ACUMULADO_COSTO_MAYORISTA_DIA=")) {
                    acumuladoCostoMayoristaDia = Double.parseDouble(linea.substring(linea.indexOf('=') + 1));
                } else if (linea.startsWith("ACUMULADO_PAGOS_EMPLEADOS_DIA=")) {
                    acumuladoPagosEmpleadosDia = Double.parseDouble(linea.substring(linea.indexOf('=') + 1));
                } else if (linea.startsWith("TOTAL_GENERAL_CARRITO=")) {
                    totalGeneral = Double.parseDouble(linea.substring(linea.indexOf('=') + 1));
                }
            } else if (seccionActual.equals("---CARRITO---")) {
                String[] campos = decodificar(linea).split("\\|", -1);
                if (campos.length == 6) {
                    vista.getModeloTabla().addRow(new Object[]{
                        campos[0], campos[1], Integer.parseInt(campos[2]), campos[3], campos[4], campos[5]
                    });
                }
            } else if (seccionActual.equals("---VENTAS_DEL_DIA---")) {
                registroVentasDelDia.add(decodificar(linea));
            } else if (seccionActual.equals("---PAGOS_EMPLEADOS_DIA---")) {
                registroPagosEmpleadosDia.add(decodificar(linea));
            }
        }

        vista.getLblTotal().setText(String.format("TOTAL: $%.2f", totalGeneral));
        if (!registroVentasDelDia.isEmpty()) {
            vista.getTxtConsolaVentas().append("• Sesión recuperada: " + registroVentasDelDia.size()
                    + " venta(s) del día restauradas desde el respaldo.\n");
        }
    }

    private void initListeners() {
        vista.getBtnValidar().addActionListener(e -> validarProducto());
        vista.getBtnAgregar().addActionListener(e -> agregarProductoVenta());
        vista.getBtnEliminarItem().addActionListener(e -> eliminarItemCarrito());
        vista.getBtnCancelarVenta().addActionListener(e -> cancelarVenta());
        vista.getBtnCobrar().addActionListener(e -> cobrarVenta());
        vista.getBtnExportarTxt().addActionListener(e -> exportarVentasAEscritorio());

        vista.getItemActualizaciones().addActionListener(e -> abrirCentroActualizaciones());
        vista.getItemReportes().addActionListener(e -> abrirCentroReportes());
        vista.getItemPagosEmpleados().addActionListener(e -> abrirCentroPagosEmpleados());
    }

    private void validarProducto() {
        String busqueda = vista.getTxtBusqueda().getText().trim();
        if (busqueda.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Ingrese el código o nombre del producto a validar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Producto> coincidencias = modeloInventario.buscarEnExcel(busqueda, vista);
        Producto producto = elegirProductoDeLista(coincidencias, busqueda);

        if (producto != null) {
            String ubicacionTexto = (producto.getUbicacion() == null || producto.getUbicacion().isEmpty()) ? "No asignada" : producto.getUbicacion();
            String stockTexto = producto.esServicioONoAplica() ? "N/A (No Aplica)" : String.valueOf(producto.getCantidadDisponible());

            String mensaje = """
                             ¡Producto encontrado!
                             
                             Código: """ + producto.getCodigo() + "\n"
                    + "Nombre: " + producto.getNombre() + "\n"
                    + "Precio Venta: $" + String.format("%.2f", producto.getPrecio()) + "\n"
                    + "Costo Mayorista: $" + String.format("%.2f", producto.getCostoMayorista()) + "\n"
                    + "Cantidad Disponible: " + stockTexto + "\n"
                    + "Ubicación: " + ubicacionTexto;
            JOptionPane.showMessageDialog(vista, mensaje, "Validación Exitosa", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void agregarProductoVenta() {
        String busqueda = vista.getTxtBusqueda().getText().trim();
        String cantidadStr = vista.getTxtCantidad().getText().trim();

        if (busqueda.isEmpty() || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Ingrese el producto y la cantidad.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadStr);
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(vista, "La cantidad debe ser un número válido mayor a 0.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Producto> coincidencias = modeloInventario.buscarEnExcel(busqueda, vista);
        Producto producto = elegirProductoDeLista(coincidencias, busqueda);

        if (producto == null) {
            return;
        }

        if (producto.getPrecio() <= 0) {
            JOptionPane.showMessageDialog(vista,
                    "El producto '" + producto.getNombre() + "' no tiene un precio unitario asignado ($0).\n"
                    + "Para impresión 3D, el Precio Unitario del Excel representa tu cobro base/ganancia de gestión.",
                    "Producto sin precio",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!producto.esServicioONoAplica()) {
            if (producto.getCantidadDisponible() <= 0) {
                JOptionPane.showMessageDialog(vista,
                        "El producto '" + producto.getNombre() + "' no tiene stock disponible (Cantidad: 0).",
                        "Producto sin stock",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (producto.getCantidadDisponible() < 10) {
                JOptionPane.showMessageDialog(vista,
                        "¡Atención! Quedan menos de 10 unidades en stock.\n"
                        + "Solicitar pedido para surtir: " + producto.getNombre()
                        + " (" + producto.getCantidadDisponible() + " unidades).",
                        "Aviso de Inventario Bajo",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }

        // =====================================================================
        // CASO ESPECIAL EXCLUSIVO: "impresión 3d pedido"
        // =====================================================================
        // El Precio Unitario del Excel NO es el precio final: representa el
        // cobro base/ganancia por gestión. El costo variable del proveedor se
        // solicita aquí en cada pedido. El envío se cobra solamente si el
        // usuario marca la casilla correspondiente.
        boolean esImpresion3DPedido = "impresión 3d pedido".equalsIgnoreCase(
                producto.getNombre() == null ? "" : producto.getNombre().trim());

        if (esImpresion3DPedido) {
            CotizacionImpresion3D cotizacion = mostrarDialogoCostoImpresion3D(
                    producto.getNombre(), producto.getPrecio(), cantidad);

            if (cotizacion == null) {
                // El usuario canceló: no se agrega nada al carrito.
                return;
            }

            // En impresión 3D no se agrupan filas por código porque dos pedidos
            // del mismo artículo pueden tener costos diferentes de proveedor.
            double subtotal = cotizacion.totalCliente;
            vista.getModeloTabla().addRow(new Object[]{
                producto.getCodigo(),
                producto.getNombre(),
                cantidad,
                String.format("%.2f", cotizacion.precioVentaUnitario),
                String.format("%.2f", subtotal),
                String.format("%.2f", cotizacion.costoRealUnitario)
            });

            totalGeneral += subtotal;

            vista.agregarLogConsola(String.format(
                    "IMPRESIÓN 3D PEDIDO | Proveedor/U: $%.2f | Base gestión/U: $%.2f | Envío: $%.2f | Cantidad: %d | Total cliente: $%.2f",
                    cotizacion.precioProveedorUnitario,
                    cotizacion.precioGestionUnitario,
                    cotizacion.envioTotal,
                    cantidad,
                    cotizacion.totalCliente
            ));

        } else {
            // =================================================================
            // FLUJO NORMAL DE TODOS LOS DEMÁS PRODUCTOS
            // =================================================================
            int filaExistente = -1;
            for (int i = 0; i < vista.getModeloTabla().getRowCount(); i++) {
                String codTabla = vista.getModeloTabla().getValueAt(i, 0).toString();
                if (codTabla.equalsIgnoreCase(producto.getCodigo())) {
                    filaExistente = i;
                    break;
                }
            }

            if (filaExistente != -1) {
                int cantidadAnterior = Integer.parseInt(vista.getModeloTabla().getValueAt(filaExistente, 2).toString());
                int nuevaCantidad = cantidadAnterior + cantidad;
                double nuevoSubtotal = producto.getPrecio() * nuevaCantidad;

                vista.getModeloTabla().setValueAt(nuevaCantidad, filaExistente, 2);
                vista.getModeloTabla().setValueAt(String.format("%.2f", nuevoSubtotal), filaExistente, 4);

                totalGeneral += (producto.getPrecio() * cantidad);
            } else {
                double subtotal = producto.getPrecio() * cantidad;
                vista.getModeloTabla().addRow(new Object[]{
                    producto.getCodigo(),
                    producto.getNombre(),
                    cantidad,
                    String.format("%.2f", producto.getPrecio()),
                    String.format("%.2f", subtotal),
                    String.format("%.2f", producto.getCostoMayorista())
                });
                totalGeneral += subtotal;
            }
        }

        vista.getLblTotal().setText(String.format("TOTAL: $%.2f", totalGeneral));
        vista.getTxtBusqueda().setText("");
        vista.getTxtCantidad().setText("1");
        vista.getTxtBusqueda().requestFocus();
        guardarSesionActual();
    }

    /**
     * Datos calculados de una cotización de impresión 3D.
     * precioProveedorUnitario = costo que cobra el proveedor por pieza.
     * precioGestionUnitario   = Precio Unitario configurado en Excel.
     * envioTotal              = costo de envío del pedido (si se marcó).
     * costoRealUnitario       = costo que debe reflejarse como costo mayorista
     *                           por pieza para calcular la utilidad real.
     * precioVentaUnitario     = precio que verá el cliente por pieza.
     * totalCliente            = total de la línea para la cantidad solicitada.
     */
    private static class CotizacionImpresion3D {
        final double precioProveedorUnitario;
        final double precioGestionUnitario;
        final double envioTotal;
        final double costoRealUnitario;
        final double precioVentaUnitario;
        final double totalCliente;

        CotizacionImpresion3D(double precioProveedorUnitario,
                              double precioGestionUnitario,
                              double envioTotal,
                              double costoRealUnitario,
                              double precioVentaUnitario,
                              double totalCliente) {
            this.precioProveedorUnitario = precioProveedorUnitario;
            this.precioGestionUnitario = precioGestionUnitario;
            this.envioTotal = envioTotal;
            this.costoRealUnitario = costoRealUnitario;
            this.precioVentaUnitario = precioVentaUnitario;
            this.totalCliente = totalCliente;
        }
    }

    /**
     * Solicita el costo variable del proveedor únicamente para el producto
     * "impresión 3d pedido".
     *
     * Fórmula:
     *   sin envío: (proveedor por pieza + gestión por pieza) × cantidad
     *   con envío:  (proveedor por pieza + gestión por pieza) × cantidad + envío
     *
     * El envío se interpreta como costo total del pedido y se reparte entre
     * las piezas para guardar un costo unitario correcto en el carrito.
     */
    private CotizacionImpresion3D mostrarDialogoCostoImpresion3D(
            String nombreProducto,
            double precioGestionUnitario,
            int cantidad) {

        final JDialog dialogo = new JDialog(vista,
                "Cotización - Impresión 3D Pedido",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialogo.setSize(470, 390);
        dialogo.setLocationRelativeTo(vista);
        dialogo.setResizable(false);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(15, 18, 12, 18));
        panelPrincipal.setBackground(Color.WHITE);

        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel lblTitulo = new JLabel("Cotización de Impresión 3D");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(MainFrame.COLOR_OSCURO);

        JLabel lblProducto = new JLabel("Producto: " + nombreProducto);
        lblProducto.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblCantidad = new JLabel("Cantidad solicitada: " + cantidad);
        lblCantidad.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblPrecioGestion = new JLabel(
                String.format("Precio Unitario Excel (gestión/ganancia): $%,.2f", precioGestionUnitario));
        lblPrecioGestion.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPrecioGestion.setForeground(MainFrame.COLOR_PRIMARIO_DARK);

        JTextField txtPrecioProveedor = new JTextField();
        txtPrecioProveedor.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtPrecioProveedor.setToolTipText("Precio que cobra el proveedor por cada pieza");

        JCheckBox chkEnvio = new JCheckBox("Cobrar envío");
        chkEnvio.setOpaque(false);
        chkEnvio.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JTextField txtEnvio = new JTextField("0");
        txtEnvio.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtEnvio.setEnabled(false);
        txtEnvio.setToolTipText("Costo total del envío del pedido");

        JLabel lblResultado = new JLabel("TOTAL AL CLIENTE: $0,00");
        lblResultado.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblResultado.setForeground(MainFrame.COLOR_EXITO);

        JLabel lblDetalle = new JLabel(" ");
        lblDetalle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDetalle.setForeground(MainFrame.COLOR_OSCURO);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panelForm.add(lblTitulo, gbc);
        gbc.gridy++;
        panelForm.add(lblProducto, gbc);
        gbc.gridy++;
        panelForm.add(lblCantidad, gbc);
        gbc.gridy++;
        panelForm.add(lblPrecioGestion, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        panelForm.add(new JLabel("Precio del proveedor por pieza ($):"), gbc);
        gbc.gridx = 1;
        panelForm.add(txtPrecioProveedor, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panelForm.add(chkEnvio, gbc);
        gbc.gridx = 1;
        panelForm.add(txtEnvio, gbc);

        gbc.gridy++;
        gbc.gridx = 0; gbc.gridwidth = 2;
        panelForm.add(lblDetalle, gbc);
        gbc.gridy++;
        panelForm.add(lblResultado, gbc);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        panelAcciones.setOpaque(false);

        JButton btnCancelar = new JButton("Cancelar");
        JButton btnAceptar = new JButton("Aceptar");
        btnAceptar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAceptar.setBackground(MainFrame.COLOR_EXITO);
        btnAceptar.setForeground(Color.WHITE);
        btnAceptar.setFocusPainted(false);

        panelAcciones.add(btnCancelar);
        panelAcciones.add(btnAceptar);

        panelPrincipal.add(panelForm, BorderLayout.CENTER);
        panelPrincipal.add(panelAcciones, BorderLayout.SOUTH);
        dialogo.setContentPane(panelPrincipal);

        final CotizacionImpresion3D[] resultado = new CotizacionImpresion3D[1];

        Runnable recalcular = () -> {
            try {
                double proveedor = parsearMontoFlexible(txtPrecioProveedor.getText());
                double envio = chkEnvio.isSelected()
                        ? parsearMontoFlexible(txtEnvio.getText())
                        : 0.0;

                if (proveedor < 0 || envio < 0 || precioGestionUnitario < 0) {
                    throw new NumberFormatException();
                }

                double totalSinEnvio = (proveedor + precioGestionUnitario) * cantidad;
                double totalCliente = totalSinEnvio + envio;
                double costoRealUnitario = proveedor + (envio / cantidad);
                double precioVentaUnitario = totalCliente / cantidad;

                lblDetalle.setText(String.format(
                        "Proveedor/U: $%,.2f  +  Gestión/U: $%,.2f  +  Envío: $%,.2f  =  $%,.2f",
                        proveedor, precioGestionUnitario, envio, totalCliente));
                lblResultado.setText(String.format("TOTAL AL CLIENTE: $%,.2f", totalCliente));
                lblResultado.setForeground(MainFrame.COLOR_EXITO);
            } catch (Exception ex) {
                lblDetalle.setText("Ingrese un precio de proveedor válido.");
                lblResultado.setText("TOTAL AL CLIENTE: $0,00");
                lblResultado.setForeground(MainFrame.COLOR_PELIGRO);
            }
        };

        DocumentListener recalculoListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcular.run(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcular.run(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcular.run(); }
        };
        txtPrecioProveedor.getDocument().addDocumentListener(recalculoListener);
        txtEnvio.getDocument().addDocumentListener(recalculoListener);
        chkEnvio.addActionListener(e -> {
            txtEnvio.setEnabled(chkEnvio.isSelected());
            if (chkEnvio.isSelected()) {
                txtEnvio.requestFocus();
                txtEnvio.selectAll();
            }
            recalcular.run();
        });

        btnCancelar.addActionListener(e -> dialogo.dispose());

        btnAceptar.addActionListener(e -> {
            try {
                double proveedor = parsearMontoFlexible(txtPrecioProveedor.getText());
                double envio = chkEnvio.isSelected()
                        ? parsearMontoFlexible(txtEnvio.getText())
                        : 0.0;

                if (proveedor < 0 || envio < 0 || precioGestionUnitario < 0) {
                    throw new NumberFormatException();
                }

                double totalCliente = (proveedor + precioGestionUnitario) * cantidad + envio;
                double costoRealUnitario = proveedor + (envio / cantidad);
                double precioVentaUnitario = totalCliente / cantidad;

                resultado[0] = new CotizacionImpresion3D(
                        proveedor,
                        precioGestionUnitario,
                        envio,
                        costoRealUnitario,
                        precioVentaUnitario,
                        totalCliente);

                dialogo.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialogo,
                        "Ingrese valores numéricos válidos para el precio del proveedor"
                        + (chkEnvio.isSelected() ? " y el envío." : "."),
                        "Datos inválidos",
                        JOptionPane.WARNING_MESSAGE);
                txtPrecioProveedor.requestFocus();
            }
        });

        dialogo.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                txtPrecioProveedor.requestFocus();
            }
        });

        dialogo.setVisible(true);
        return resultado[0];
    }

    /** Convierte montos escritos con formato colombiano o decimal simple. */
    private double parsearMontoFlexible(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return 0.0;
        }

        String valor = texto.trim()
                .replace("$", "")
                .replace(" ", "");

        // 20.000,50 -> 20000.50
        if (valor.contains(".") && valor.contains(",")) {
            valor = valor.replace(".", "").replace(",", ".");
        } else if (valor.contains(",")) {
            // 20000,50 -> 20000.50
            valor = valor.replace(",", ".");
        } else if (valor.matches(".*\\.\\d{3}$")) {
            // 20.000 -> 20000
            valor = valor.replace(".", "");
        }

        return Double.parseDouble(valor);
    }

    private void eliminarItemCarrito() {
        int filaSeleccionada = vista.getTablaVentas().getSelectedRow();
        if (filaSeleccionada >= 0) {
            double subtotalFila = Double.parseDouble(vista.getModeloTabla().getValueAt(filaSeleccionada, 4).toString().replace(",", "."));
            totalGeneral -= subtotalFila;
            if (totalGeneral < 0) totalGeneral = 0.0;
            vista.getLblTotal().setText(String.format("TOTAL: $%.2f", totalGeneral));
            vista.getModeloTabla().removeRow(filaSeleccionada);
            guardarSesionActual();
        } else {
            JOptionPane.showMessageDialog(vista, "Seleccione de la tabla el producto que desea eliminar.", "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void cancelarVenta() {
        if (vista.getModeloTabla().getRowCount() == 0) {
            JOptionPane.showMessageDialog(vista, "El carrito ya está vacío.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(vista,
                "¿Está seguro de que desea cancelar la venta actual y vaciar el carrito?",
                "Confirmar Cancelación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opcion == JOptionPane.YES_OPTION) {
            vista.getModeloTabla().setRowCount(0);
            totalGeneral = 0.0;
            vista.getLblTotal().setText("TOTAL: $0.00");
            vista.getTxtBusqueda().setText("");
            vista.getTxtCantidad().setText("1");
            vista.getTxtBusqueda().requestFocus();
            guardarSesionActual();
        }
    }

    private void cobrarVenta() {
        if (vista.getModeloTabla().getRowCount() == 0) {
            JOptionPane.showMessageDialog(vista, "No hay productos en el carrito de compras.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] opcionesPago = {"Efectivo", "Transferencia Digital", "Pago Mixto (Dual)"};
        int seleccionPago = JOptionPane.showOptionDialog(vista, "Seleccione el método de pago:", "Método de Pago", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opcionesPago, opcionesPago[0]);
        if (seleccionPago == JOptionPane.CLOSED_OPTION) return;

        String metodoPago = "EFECTIVO";
        double recibido = totalGeneral;
        double devuelta = 0.0;
        String desgloseDevuelta = "N/A";
        double abonadoEfectivo = 0.0;
        double abonadoTransferencia = 0.0;

        if (seleccionPago == 0) { // EFECTIVO
            metodoPago = "EFECTIVO";
            
            MainFrame.DialogoEfectivoBotones dialogoEfectivo = vista.new DialogoEfectivoBotones(vista, totalGeneral);
            dialogoEfectivo.setVisible(true);
            
            if (!dialogoEfectivo.isConfirmado()) {
                return;
            }
            
            recibido = dialogoEfectivo.getDineroRecibido();
            devuelta = dialogoEfectivo.getDevuelta();
            desgloseDevuelta = dialogoEfectivo.getDesgloseDevueltaTexto();
            abonadoEfectivo = totalGeneral;
            acumuladoEfectivo += totalGeneral;

        } else if (seleccionPago == 1) {
            metodoPago = "TRANSFERENCIA DIGITAL";
            abonadoTransferencia = totalGeneral;
            acumuladoTransferencia += totalGeneral;

        } else if (seleccionPago == 2) {
            metodoPago = "PAGO MIXTO";
            MainFrame.DialogoPagoMixto dialogoMixto = vista.new DialogoPagoMixto(vista, totalGeneral);
            dialogoMixto.setVisible(true);
            if (!dialogoMixto.isConfirmado()) return;

            abonadoEfectivo = dialogoMixto.getMontoEfectivo();
            abonadoTransferencia = dialogoMixto.getMontoTransferencia();
            recibido = abonadoEfectivo + abonadoTransferencia;
            devuelta = dialogoMixto.getDevuelta();

            double efectivoNeto = abonadoEfectivo - devuelta;
            acumuladoEfectivo += efectivoNeto;
            acumuladoTransferencia += abonadoTransferencia;

            desgloseDevuelta = String.format("Efectivo: $%.2f | Transf: $%.2f", abonadoEfectivo, abonadoTransferencia);
        }

        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        double costoMayoristaEstaVenta = 0.0;
        List<Object[]> itemsVendidos = new ArrayList<>();

        for (int i = 0; i < vista.getModeloTabla().getRowCount(); i++) {
            String cod = vista.getModeloTabla().getValueAt(i, 0).toString();
            int cant = Integer.parseInt(vista.getModeloTabla().getValueAt(i, 2).toString());
            double costoU = Double.parseDouble(vista.getModeloTabla().getValueAt(i, 5).toString().replace(",", "."));
            costoMayoristaEstaVenta += (costoU * cant);

            itemsVendidos.add(new Object[]{cod, null, cant});
        }
        acumuladoCostoMayoristaDia += costoMayoristaEstaVenta;

        // --- REGISTRO DETALLADO PARA LOGS Y AUDITORÍA EN DISCO ---
        StringBuilder detalleVenta = new StringBuilder();
        detalleVenta.append("--- VENTA #").append(contadorFacturas).append(" [Fecha: ").append(fecha).append(" Hora: ").append(hora).append("] ---\n");
        detalleVenta.append("Empleado a cargo: ").append(this.nombreEmpleado.toUpperCase()).append("\n");
        detalleVenta.append("Método de Pago: ").append(metodoPago).append("\n");

        if (metodoPago.equals("EFECTIVO")) {
            detalleVenta.append(String.format("Efectivo Recibido: $%.2f | Cambio (Devuelta): $%.2f\n", recibido, devuelta));
            detalleVenta.append("Desglose Devuelta: ").append(desgloseDevuelta).append("\n");
        } else if (metodoPago.equals("PAGO MIXTO")) {
            detalleVenta.append(String.format("Abono Efectivo: $%.2f | Abono Transferencia: $%.2f | Devuelta: $%.2f\n", abonadoEfectivo, abonadoTransferencia, devuelta));
        }

        detalleVenta.append(String.format("%-10s | %-25s | %-8s | %-10s | %-10s\n", "CÓDIGO", "PRODUCTO", "CANT.", "PRECIO U.", "SUBTOTAL"));
        detalleVenta.append("------------------------------------------------------------------------------------\n");

        for (int i = 0; i < vista.getModeloTabla().getRowCount(); i++) {
            String cod = vista.getModeloTabla().getValueAt(i, 0).toString();
            String prod = vista.getModeloTabla().getValueAt(i, 1).toString();
            String cant = vista.getModeloTabla().getValueAt(i, 2).toString();
            String precio = vista.getModeloTabla().getValueAt(i, 3).toString();
            String sub = vista.getModeloTabla().getValueAt(i, 4).toString();
            detalleVenta.append(String.format("%-10s | %-25s | %-8s | $%s      | $%s\n", cod, prod, cant, precio, sub));
        }
        detalleVenta.append(String.format("TOTAL COBRADO: $%.2f\n\n", totalGeneral));

        registroVentasDelDia.add(detalleVenta.toString());
        acumuladoTotalDia += totalGeneral;

        if (registroVentasDelDia.size() == 1) {
            vista.getTxtConsolaVentas().setText("");
        }

        String descPagoLog = metodoPago;
        if (metodoPago.equals("PAGO MIXTO")) {
            descPagoLog = String.format("Mixto (Ef: $%.0f | Tr: $%.0f)", abonadoEfectivo, abonadoTransferencia);
        }

        String lineaConsola = String.format("• [%s] Venta #%03d | Total: $%.2f | Pago: %s\n",
                hora, contadorFacturas, totalGeneral, descPagoLog);
        vista.getTxtConsolaVentas().append(lineaConsola);
        vista.getTxtConsolaVentas().setCaretPosition(vista.getTxtConsolaVentas().getDocument().getLength());

        modeloInventario.descontarStockExcel(itemsVendidos, vista);
        actualizarIndicadorInventario();

        // --- CONSTRUCCIÓN DEL TICKET DE IMPRESIÓN (CÓDIGO ORIGINAL EXACTO) ---
        StringBuilder ticket = new StringBuilder();
        ticket.append("         PAPELERÍA SIGLO XXI          \n");
        ticket.append("       COMPROBANTE DE COMPRA          \n");
        ticket.append("======================================\n");
        ticket.append(String.format(" Ticket Nº: %06d\n", contadorFacturas));
        ticket.append(" Fecha: ").append(fecha).append("  Hora: ").append(hora).append("\n");
        ticket.append(" Atendido por: ").append(this.nombreEmpleado.toUpperCase()).append("\n");
        ticket.append("--------------------------------------\n");
        ticket.append(String.format("%-4s %-22s %10s\n", "CANT", "PRODUCTO", "SUBTOTAL"));
        ticket.append("--------------------------------------\n");

        for (int i = 0; i < vista.getModeloTabla().getRowCount(); i++) {
            String prod = vista.getModeloTabla().getValueAt(i, 1).toString();
            if (prod.length() > 22) prod = prod.substring(0, 20) + "..";
            String cant = vista.getModeloTabla().getValueAt(i, 2).toString() + "x";
            String sub = "$" + vista.getModeloTabla().getValueAt(i, 4).toString();
            ticket.append(String.format("%-4s %-22s %10s\n", cant, prod, sub));
        }

        ticket.append("--------------------------------------\n");
        ticket.append(String.format(" TOTAL:                   $%10.2f\n", totalGeneral));
        ticket.append(" Método de Pago:           ").append(metodoPago).append("\n");

        if (metodoPago.equals("EFECTIVO")) {
            ticket.append(String.format(" Efectivo Recibido:       $%10.2f\n", recibido));
            ticket.append(String.format(" Cambio (Devuelta):       $%10.2f\n", devuelta));
        } else if (metodoPago.equals("PAGO MIXTO")) {
            ticket.append(String.format(" Abono Efectivo:          $%10.2f\n", abonadoEfectivo));
            ticket.append(String.format(" Abono Transferencia:     $%10.2f\n", abonadoTransferencia));
            ticket.append(String.format(" Cambio (Devuelta):       $%10.2f\n", devuelta));
        }

        ticket.append("======================================\n");
        ticket.append("         ¡Gracias por su compra!      \n");
        ticket.append("======================================\n");

        // --- VENTANA EMERGENTE RESTAURADA DE IMPRESIÓN ---
        int opcionImprimir = JOptionPane.showConfirmDialog(
                vista,
                "¡Venta #" + String.format("%03d", contadorFacturas) + " registrada con éxito!\n\n¿Desea imprimir la factura / comprobante de pago?",
                "Imprimir Factura",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (opcionImprimir == JOptionPane.YES_OPTION) {
            imprimirFacturaVenta(contadorFacturas, ticket.toString());
        }

        contadorFacturas++;
        vista.getModeloTabla().setRowCount(0);
        totalGeneral = 0.0;
        vista.getLblTotal().setText("TOTAL: $0.00");
        guardarSesionActual();
    }

    private void imprimirFacturaVenta(int numFactura, String contenidoTicket) {
        try {
            JTextArea areaImpresion = new JTextArea(contenidoTicket);
            areaImpresion.setFont(new Font("Monospaced", Font.PLAIN, 9));

            boolean impreso = areaImpresion.print(
                    new MessageFormat("PAPELERÍA SIGLO XXI - TICKET #" + String.format("%06d", numFactura)),
                    new MessageFormat("Página {0}"),
                    true,
                    null,
                    null,
                    true
            );

            if (impreso) {
                JOptionPane.showMessageDialog(vista, "Comprobante #" + String.format("%06d", numFactura) + " enviado a la impresora.", "Impresión Exitosa", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(vista, "Error al intentar imprimir el comprobante: " + ex.getMessage(), "Error de Impresión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Producto elegirProductoDeLista(List<Producto> lista, String busqueda) {
        if (lista.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "No se encontró ningún producto que coincida con: '" + busqueda + "'.", "Sin resultados", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        if (lista.size() == 1) {
            return lista.get(0);
        }

        Producto[] opciones = lista.toArray(Producto[]::new);
        return (Producto) JOptionPane.showInputDialog(
                vista,
                "Se encontraron " + lista.size() + " productos que coinciden.\nPor favor, seleccione el correcto:",
                "Múltiples opciones encontradas",
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]);
    }

    private void abrirCentroPagosEmpleados() {
        MainFrame.DialogoPagoEmpleados dialogoPagos = vista.new DialogoPagoEmpleados(vista, new String[]{this.nombreEmpleado, "Admin", "Auxiliar"}, this.nombreEmpleado);

        dialogoPagos.getTxtMontoPagar().setEditable(false);
        dialogoPagos.getTxtMontoPagar().setFocusable(false);
        dialogoPagos.getTxtMontoPagar().setBackground(new Color(245, 245, 245));

        dialogoPagos.getComboConcepto().setModel(new DefaultComboBoxModel<>(new String[]{
            "Sueldo Diario", 
            "Sueldo Semanal", 
            "Sueldo Mensual"
        }));

        ImageIcon icoPago = vista.cargarIcono(MainFrame.RUTA_ICONOS + "ICONO-PAGOS.png", 18, 18);
        if (icoPago == null) {
            icoPago = vista.cargarIcono(MainFrame.RUTA_ICONOS + "ICONO-PAGO-EMPLEADOS.png", 18, 18);
        }
        if (icoPago != null) {
            dialogoPagos.getBtnRegistrarPago().setIcon(icoPago);
            dialogoPagos.getBtnRegistrarPago().setIconTextGap(8);
        }

        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Runnable calcularAutomatico = () -> {
            try {
                String conceptoSel = (String) dialogoPagos.getComboConcepto().getSelectedItem();
                Date fechaElegida = dialogoPagos.getFechaSeleccionada();
                LocalDate localDateSel = fechaElegida.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                LocalDate inicioPeriodo = localDateSel;
                LocalDate finPeriodo = localDateSel;

                if ("Sueldo Semanal".equals(conceptoSel)) {
                    inicioPeriodo = localDateSel.minusDays(localDateSel.getDayOfWeek().getValue() - 1);
                    finPeriodo = localDateSel;

                    String textoSemana = String.format("Semana del %s al %s", 
                            inicioPeriodo.format(fmtFecha), 
                            finPeriodo.format(fmtFecha));
                    dialogoPagos.setTextoFechaEvaluacion(textoSemana);

                } else if ("Sueldo Mensual".equals(conceptoSel)) {
                    inicioPeriodo = localDateSel.withDayOfMonth(1);
                    finPeriodo = localDateSel;

                    String textoMes = String.format("Mes del %s al %s", 
                            inicioPeriodo.format(fmtFecha), 
                            finPeriodo.format(fmtFecha));
                    dialogoPagos.setTextoFechaEvaluacion(textoMes);

                } else if ("Sueldo Diario".equals(conceptoSel)) {
                    inicioPeriodo = localDateSel;
                    finPeriodo = localDateSel;
                    dialogoPagos.setTextoFechaEvaluacion(localDateSel.format(fmtFecha));
                }

                dialogoPagos.setRangoEvaluacion(inicioPeriodo, finPeriodo);

                double ventasBase = modeloInventario.calcularVentasPorRangoFechas(inicioPeriodo, finPeriodo, acumuladoTotalDia);
                dialogoPagos.getLblVentasTotales().setText(String.format("$%.2f", ventasBase));

                double porcentaje = 0.0;
                String pctText = dialogoPagos.getTxtPorcentajeComision().getText().trim().replace(",", ".");
                if (!pctText.isEmpty()) {
                    porcentaje = Double.parseDouble(pctText);
                }

                double montoPagar = ventasBase * (porcentaje / 100.0);
                dialogoPagos.getTxtMontoPagar().setText(String.format("%.2f", montoPagar));

            } catch (Exception ignored) {
                dialogoPagos.getTxtMontoPagar().setText("0.00");
            }
        };

        calcularAutomatico.run();

        dialogoPagos.getSpinnerFecha().addChangeListener(e -> calcularAutomatico.run());
        dialogoPagos.getComboConcepto().addActionListener(e -> calcularAutomatico.run());

        dialogoPagos.getBtnAbrirSelectorFecha().addActionListener(e -> {
            Date fechaActual = dialogoPagos.getFechaSeleccionada();
            String conceptoSel = (String) dialogoPagos.getComboConcepto().getSelectedItem();

           MainFrame.DialogoSelectorFecha selector = vista.new DialogoSelectorFecha(
        dialogoPagos,
        fechaActual,
        dialogoPagos.getInicioRango(),
        dialogoPagos.getFinRango(),
        "Sueldo Semanal".equals(conceptoSel)
);
            selector.setVisible(true);

            if (selector.isConfirmado()) {
                dialogoPagos.getSpinnerFecha().setValue(selector.getFechaSeleccionada());
                calcularAutomatico.run();
            }
        });

        dialogoPagos.getTxtPorcentajeComision().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { calcularAutomatico.run(); }
            @Override public void removeUpdate(DocumentEvent e) { calcularAutomatico.run(); }
            @Override public void changedUpdate(DocumentEvent e) { calcularAutomatico.run(); }
        });

        dialogoPagos.getLblTotalPagadoHoy().setText(String.format("$%.2f", acumuladoPagosEmpleadosDia));

        dialogoPagos.getBtnRegistrarPago().addActionListener(e -> {
            try {
                String emp = (String) dialogoPagos.getComboEmpleado().getSelectedItem();
                String concepto = (String) dialogoPagos.getComboConcepto().getSelectedItem();
                double monto = Double.parseDouble(dialogoPagos.getTxtMontoPagar().getText().replace(",", "."));

                if (emp == null || emp.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialogoPagos, "Ingrese el nombre del empleado.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (monto <= 0) {
                    JOptionPane.showMessageDialog(dialogoPagos, "El monto a pagar calculado debe ser mayor a $0.00.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String[] opcionesPago = {"Efectivo", "Transferencia"};
                int seleccion = JOptionPane.showOptionDialog(dialogoPagos,
                        "Seleccione el método de pago:",
                        "Método de Pago",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        opcionesPago,
                        opcionesPago[0]);

                if (seleccion != -1) {
                    String metodoPago = opcionesPago[seleccion];
                    String fechaActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                    String mensajeExito = String.format(
                        "¡Pago registrado exitosamente!\n\n[%s] Empleado: %s | Concepto: %s | Monto: $%.2f | Método: %s",
                        fechaActual, emp.toUpperCase(), concepto, monto, metodoPago
                    );

                    String registroParaLista = String.format("[%s] Empleado: %s | Concepto: %s | Monto: $%.2f | Método: %s",
                        fechaActual, emp.toUpperCase(), concepto, monto, metodoPago);

                    registroPagosEmpleadosDia.add(registroParaLista);
                    acumuladoPagosEmpleadosDia += monto;
                    dialogoPagos.getLblTotalPagadoHoy().setText(String.format("$%.2f", acumuladoPagosEmpleadosDia));
                    guardarSesionActual();

                    JOptionPane.showMessageDialog(dialogoPagos, mensajeExito, "Registro Guardado", JOptionPane.INFORMATION_MESSAGE);
                    dialogoPagos.getTxtMontoPagar().setText("0.00");
                    dialogoPagos.dispose();
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialogoPagos, "Ocurrió un error al procesar el monto.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialogoPagos.setVisible(true);
    }

    private void abrirCentroReportes() {
        JDialog dialogoReportes = new JDialog(vista, "Centro de Reportes y Estadísticas - Sistema de Ventas", true);
        dialogoReportes.setSize(480, 480);
        dialogoReportes.setLocationRelativeTo(vista);
        dialogoReportes.setLayout(new BorderLayout(10, 10));

        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(new Color(240, 244, 248));
        panelHeader.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblTituloHeader = new JLabel("Centro de Reportes");
        lblTituloHeader.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTituloHeader.setForeground(new Color(33, 37, 41));

        ImageIcon icoHeaderReportes = vista.cargarIcono(MainFrame.RUTA_ICONOS + "ICONO-REPORTES.png", 22, 22);
        if (icoHeaderReportes != null) {
            lblTituloHeader.setIcon(icoHeaderReportes);
            lblTituloHeader.setIconTextGap(10);
        }

        JLabel lblSubtituloHeader = new JLabel("Consulta métricas de ventas, costos de mayoristas y ganancias netas");
        lblSubtituloHeader.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtituloHeader.setForeground(new Color(108, 117, 125));

        panelHeader.add(lblTituloHeader, BorderLayout.NORTH);
        panelHeader.add(lblSubtituloHeader, BorderLayout.SOUTH);
        dialogoReportes.add(panelHeader, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel(new GridLayout(4, 1, 10, 10));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JButton btnAnual = new JButton("📅 Reporte Anual de Ventas");
        ImageIcon icoAnual = vista.cargarIcono(MainFrame.RUTA_ICONOS + "ICONO-REPORTE-ANUAL.png", 20, 20);
        if (icoAnual != null) btnAnual.setIcon(icoAnual);

        JButton btnMensual = new JButton("📆 Reporte Mensual de Ventas");
        ImageIcon icoMensual = vista.cargarIcono(MainFrame.RUTA_ICONOS + "ICONO-REPORTE-MENSUAL.png", 20, 20);
        if (icoMensual != null) btnMensual.setIcon(icoMensual);

        JButton btnSemanal = new JButton("🗓️ Reporte Semanal de Ventas");
        ImageIcon icoSemanal = vista.cargarIcono(MainFrame.RUTA_ICONOS + "ICONO-REPORTE-SEMANAL.png", 20, 20);
        if (icoSemanal != null) btnSemanal.setIcon(icoSemanal);

        JButton btnPromedioDiario = new JButton("📈 Reporte Promedio Diario");
        ImageIcon icoDiario = vista.cargarIcono(MainFrame.RUTA_ICONOS + "ICONO-REPORTE-DIARIO.png", 20, 20);
        if (icoDiario != null) btnPromedioDiario.setIcon(icoDiario);

        Font fontBoton = new Font("Segoe UI", Font.BOLD, 13);

        btnAnual.setFont(fontBoton);
        btnAnual.setBackground(new Color(40, 116, 166));
        btnAnual.setForeground(Color.WHITE);

        btnMensual.setFont(fontBoton);
        btnMensual.setBackground(new Color(23, 162, 184));
        btnMensual.setForeground(Color.WHITE);

        btnSemanal.setFont(fontBoton);
        btnSemanal.setBackground(new Color(111, 66, 193));
        btnSemanal.setForeground(Color.WHITE);

        btnPromedioDiario.setFont(fontBoton);
        btnPromedioDiario.setBackground(new Color(40, 167, 69));
        btnPromedioDiario.setForeground(Color.WHITE);

        btnAnual.addActionListener(e -> generarReporteAnual());
        btnMensual.addActionListener(e -> generarReporteMensual());
        btnSemanal.addActionListener(e -> generarReporteSemanal());
        btnPromedioDiario.addActionListener(e -> generarReportePromedioDiario());

        panelBotones.add(btnAnual);
        panelBotones.add(btnMensual);
        panelBotones.add(btnSemanal);
        panelBotones.add(btnPromedioDiario);

        dialogoReportes.add(panelBotones, BorderLayout.CENTER);

        JPanel panelFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCerrar.addActionListener(e -> dialogoReportes.dispose());
        panelFooter.add(btnCerrar);

        dialogoReportes.add(panelFooter, BorderLayout.SOUTH);
        dialogoReportes.setVisible(true);
    }

    private void generarReporteAnual() {
        int anioActual = LocalDate.now().getYear();
        double gananciaNeta = acumuladoTotalDia - acumuladoCostoMayoristaDia - acumuladoPagosEmpleadosDia;
        double margenUtilidad = (acumuladoTotalDia > 0) ? (gananciaNeta / acumuladoTotalDia) * 100.0 : 0.0;
        String fechaHoraEmision = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String reporte = String.format("""
                ====================================================================
                                      PAPELERÍA SIGLO XXI                           
                        INFORME FINANCIERO Y REPORTE ANUAL DE VENTAS                 
                ====================================================================
                AÑO DE EJERCICIO    : %d
                FECHA DE EMISIÓN    : %s
                RESPONSABLE         : %s
                ESTADO SISTEMA      : OPERATIVO (%s)
                ====================================================================
                
                1. RESUMEN DE FACTURACIÓN Y VENTAS BRUTAS
                   -----------------------------------------------------------------
                   • Total Facturas Emitidas     : %d
                   • Ingresos Brutos Totales     : $%,14.2f
                   • Promedio Facturado / Venta  : $%,14.2f
                
                2. COSTOS DE OPERACIÓN Y NÓMINA
                   -----------------------------------------------------------------
                   • Costo Mayorista Inventario  : $%,14.2f
                   • Pagos y Nómina a Empleados  : $%,14.2f
                   • Total Egresos Operativos    : $%,14.2f
                
                3. BALANCE FINANCIERO Y RENDIMIENTO NETO
                   -----------------------------------------------------------------
                   • GANANCIA NETA EN CAJA       : $%,14.2f
                   • Margen de Utilidad Neta     : %.2f%%
                
                4. DISTRIBUCIÓN POR MÉTODOS DE PAGO
                   -----------------------------------------------------------------
                   • Recaudo en Efectivo         : $%,14.2f
                   • Recaudo en Transferencias   : $%,14.2f
                ====================================================================
                                 DOCUMENTO OFICIAL DE CONTROL INTERNO               
                ====================================================================
                """,
            anioActual, fechaHoraEmision, this.nombreEmpleado.toUpperCase(), VERSION_SOFTWARE,
            (contadorFacturas - 1), acumuladoTotalDia, ((contadorFacturas - 1) > 0 ? acumuladoTotalDia / (contadorFacturas - 1) : 0.0),
            acumuladoCostoMayoristaDia, acumuladoPagosEmpleadosDia, (acumuladoCostoMayoristaDia + acumuladoPagosEmpleadosDia),
            gananciaNeta, margenUtilidad, acumuladoEfectivo, acumuladoTransferencia
        );

        mostrarVentanaResultadoReporte("Reporte Anual de Ventas - " + anioActual, reporte, "REPORTE VENTAS ANUAL");
    }

    private void generarReporteMensual() {
        String mesAnio = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase();
        double gananciaNeta = acumuladoTotalDia - acumuladoCostoMayoristaDia - acumuladoPagosEmpleadosDia;
        double margenUtilidad = (acumuladoTotalDia > 0) ? (gananciaNeta / acumuladoTotalDia) * 100.0 : 0.0;
        String fechaHoraEmision = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String reporte = String.format("""
                ====================================================================
                                      PAPELERÍA SIGLO XXI                           
                           REPORTE MENSUAL DE RENDIMIENTO DE VENTAS                  
                ====================================================================
                PERÍODO EVALUADO    : %s
                FECHA DE EMISIÓN    : %s
                RESPONSABLE         : %s
                ====================================================================
                
                1. VOLUMEN DE VENTA Y REGISTRO DE CAJA
                   -----------------------------------------------------------------
                   • Transacciones Registradas   : %d
                   • Venta Bruta Acumulada       : $%,14.2f
                
                2. DESGLOSE DE COSTOS Y EGRESOS
                   -----------------------------------------------------------------
                   • Costo de Adquisición Stock  : $%,14.2f
                   • Compensación Empleados      : $%,14.2f
                   • Total Costos Operativos     : $%,14.2f
                
                3. BALANCE Y GANANCIA NETA DEL MES
                   -----------------------------------------------------------------
                   • GANANCIA NETA DEL MES       : $%,14.2f
                   • Rendimiento Neto Sobre Venta: %.2f%%
                
                4. RECAUDO SEGÚN CANAL DE PAGO
                   -----------------------------------------------------------------
                   • Total Efectivo en Caja      : $%,14.2f
                   • Total Medios Digitales      : $%,14.2f
                ====================================================================
                                 DOCUMENTO OFICIAL DE CONTROL INTERNO               
                ====================================================================
                """,
            mesAnio, fechaHoraEmision, this.nombreEmpleado.toUpperCase(),
            (contadorFacturas - 1), acumuladoTotalDia,
            acumuladoCostoMayoristaDia, acumuladoPagosEmpleadosDia, (acumuladoCostoMayoristaDia + acumuladoPagosEmpleadosDia),
            gananciaNeta, margenUtilidad, acumuladoEfectivo, acumuladoTransferencia
        );

        mostrarVentanaResultadoReporte("Reporte Mensual de Ventas - " + mesAnio, reporte, "REPORTE VENTAS MENSUAL");
    }

    private void generarReporteSemanal() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1);
        double gananciaNeta = acumuladoTotalDia - acumuladoCostoMayoristaDia - acumuladoPagosEmpleadosDia;
        double margenUtilidad = (acumuladoTotalDia > 0) ? (gananciaNeta / acumuladoTotalDia) * 100.0 : 0.0;
        String fechaHoraEmision = hoy.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String reporte = String.format("""
                ====================================================================
                                      PAPELERÍA SIGLO XXI                           
                           REPORTE SEMANAL DE INVENTARIO Y VENTAS                    
                ====================================================================
                RANGO DE FECHAS     : Del %s al %s
                FECHA DE EMISIÓN    : %s
                ATENDIDO POR        : %s
                ====================================================================
                
                1. RESUMEN DE VENTAS SEMANALES
                   -----------------------------------------------------------------
                   • Facturas Expedidas          : %d
                   • Ingresos Brutos Totales     : $%,14.2f
                
                2. DEDUCCIONES Y COSTOS DE INVENTARIO
                   -----------------------------------------------------------------
                   • Inversión Costo Mayorista   : $%,14.2f
                   • Liquidación a Empleados     : $%,14.2f
                   • Egresos Semanales Totales   : $%,14.2f
                
                3. BALANCE FINANCIERO SEMANAL
                   -----------------------------------------------------------------
                   • GANANCIA NETA SEMANAL       : $%,14.2f
                   • Eficiencia Financiera       : %.2f%%
                
                4. CANALES DE INGRESO
                   -----------------------------------------------------------------
                   • Recaudo Efectivo            : $%,14.2f
                   • Recaudo Transferencia       : $%,14.2f
                ====================================================================
                                 DOCUMENTO OFICIAL DE CONTROL INTERNO               
                ====================================================================
                """,
            inicioSemana.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            hoy.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            fechaHoraEmision, this.nombreEmpleado.toUpperCase(),
            (contadorFacturas - 1), acumuladoTotalDia,
            acumuladoCostoMayoristaDia, acumuladoPagosEmpleadosDia, (acumuladoCostoMayoristaDia + acumuladoPagosEmpleadosDia),
            gananciaNeta, margenUtilidad, acumuladoEfectivo, acumuladoTransferencia
        );

        mostrarVentanaResultadoReporte("Reporte Semanal de Ventas", reporte, "REPORTE VENTAS SEMANAL");
    }

    private void generarReportePromedioDiario() {
        int totalTransacciones = contadorFacturas - 1;
        double promedioPorVenta = (totalTransacciones > 0) ? (acumuladoTotalDia / totalTransacciones) : 0.0;
        double gananciaNetaDia = acumuladoTotalDia - acumuladoCostoMayoristaDia - acumuladoPagosEmpleadosDia;
        double margenUtilidad = (acumuladoTotalDia > 0) ? (gananciaNetaDia / acumuladoTotalDia) * 100.0 : 0.0;
        String fechaHoraEmision = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String reporte = String.format("""
                ====================================================================
                                      PAPELERÍA SIGLO XXI                           
                        INFORME DE MÉTRICAS Y PROMEDIO DIARIO DE VENTAS              
                ====================================================================
                FECHA DE EVALUACIÓN : %s
                GENERADO POR        : %s
                ====================================================================
                
                1. MÉTRICAS GENERALES DEL DÍA
                   -----------------------------------------------------------------
                   • Venta Total Bruta del Día   : $%,14.2f
                   • Operaciones Realizadas      : %d
                   • Ticket Promedio por Cliente : $%,14.2f
                
                2. ESTRUCTURA DE COSTOS DEL DÍA
                   -----------------------------------------------------------------
                   • Costo Mayorista de Productos: $%,14.2f
                   • Pagos Realizados a Empleados: $%,14.2f
                   • Costo Operativo Total Hoy   : $%,14.2f
                
                3. BALANCE DE CAJA Y GANANCIA NETA
                   -----------------------------------------------------------------
                   • GANANCIA NETA DEL DÍA       : $%,14.2f
                   • Margen Neto Diario          : %.2f%%
                
                4. RECAUDO POR MEDIOS DE PAGO
                   -----------------------------------------------------------------
                   • Balance Efectivo en Caja    : $%,14.2f
                   • Balance Transferencias      : $%,14.2f
                ====================================================================
                                 DOCUMENTO OFICIAL DE CONTROL INTERNO               
                ====================================================================
                """,
                fechaHoraEmision, this.nombreEmpleado.toUpperCase(),
                acumuladoTotalDia, totalTransacciones, promedioPorVenta,
                acumuladoCostoMayoristaDia, acumuladoPagosEmpleadosDia, (acumuladoCostoMayoristaDia + acumuladoPagosEmpleadosDia),
                gananciaNetaDia, margenUtilidad, acumuladoEfectivo, acumuladoTransferencia
        );

        mostrarVentanaResultadoReporte("Reporte Promedio Diario de Ventas", reporte, "REPORTE VENTAS DIARIO");
    }

    private void mostrarVentanaResultadoReporte(String titulo, String contenido, String nombreSubcarpeta) {
        JDialog dialogoResultado = new JDialog(vista, titulo, true);
        dialogoResultado.setSize(520, 480);
        dialogoResultado.setLocationRelativeTo(vista);
        dialogoResultado.setLayout(new BorderLayout(10, 10));

        JTextArea txtReporte = new JTextArea(contenido);
        txtReporte.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtReporte.setEditable(false);
        txtReporte.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(txtReporte);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        dialogoResultado.add(scroll, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton btnDescargarPDF = new JButton("📄 Descargar Reporte (PDF)");
        btnDescargarPDF.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDescargarPDF.setBackground(new Color(220, 53, 69));
        btnDescargarPDF.setForeground(Color.WHITE);

        JButton btnAceptar = new JButton("Aceptar");
        btnAceptar.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnDescargarPDF.addActionListener(e -> descargarReportePDF(nombreSubcarpeta, titulo, contenido));
        btnAceptar.addActionListener(e -> dialogoResultado.dispose());

        panelBotones.add(btnDescargarPDF);
        panelBotones.add(btnAceptar);

        dialogoResultado.add(panelBotones, BorderLayout.SOUTH);
        dialogoResultado.setVisible(true);
    }

    private void descargarReportePDF(String subcarpeta, String tituloReporte, String contenido) {
        try {
            File escritorio = FileSystemView.getFileSystemView().getHomeDirectory();
            File rootFolder = new File(escritorio, "DOCUMENTOS SIGLO XXI");
            File carpetaReportes = new File(rootFolder, "REPORTES");
            File carpetaDestino = new File(carpetaReportes, subcarpeta);

            if (!carpetaDestino.exists()) {
                carpetaDestino.mkdirs();
            }

            String timeStamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "_" + LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String nombreLimpio = tituloReporte.replaceAll("[^a-zA-Z0-9_-]", "_");
            File archivoPDF = new File(carpetaDestino, nombreLimpio + "_" + timeStamp + ".pdf");

            guardarReportePDFEstructurado(archivoPDF, tituloReporte, contenido);

            int opcion = JOptionPane.showConfirmDialog(
                vista,
                "¡Reporte PDF generado exitosamente!\n\nUbicación:\n" + archivoPDF.getAbsolutePath() + "\n\n¿Desea abrir y ver el reporte al instante?",
                "Reporte Guardado",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
            );

            if (opcion == JOptionPane.YES_OPTION) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(archivoPDF);
                } else {
                    JOptionPane.showMessageDialog(vista, "No se pudo abrir automáticamente el visor de PDF.", "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (HeadlessException | IOException ex) {
            JOptionPane.showMessageDialog(vista, "Error al generar el archivo PDF: " + ex.getMessage(), "Error PDF", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarReportePDFEstructurado(File archivoDestino, String titulo, String contenido) throws IOException {
        List<String> lineas = new ArrayList<>();
        for (String line : contenido.split("\n")) {
            String escaped = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            lineas.add(escaped);
        }

        StringBuilder stream = new StringBuilder();
        stream.append("BT\n");
        stream.append("/F1 8.5 Tf\n");
        stream.append("10.5 TL\n");
        stream.append("35 760 Td\n");

        for (String l : lineas) {
            stream.append("(").append(l).append(") Tj T*\n");
        }
        stream.append("ET\n");

        byte[] streamData = stream.toString().getBytes("ISO-8859-1");

        try (FileOutputStream fos = new FileOutputStream(archivoDestino);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            List<Long> offsets = new ArrayList<>();
            long currentOffset = 0;

            String header = "%PDF-1.4\n";
            bos.write(header.getBytes("ISO-8859-1"));
            currentOffset += header.getBytes("ISO-8859-1").length;

            offsets.add(currentOffset);
            String obj1 = "1 0 obj\n<</Type /Catalog /Pages 2 0 R>>\nendobj\n";
            bos.write(obj1.getBytes("ISO-8859-1"));
            currentOffset += obj1.getBytes("ISO-8859-1").length;

            offsets.add(currentOffset);
            String obj2 = "2 0 obj\n<</Type /Pages /Kids [3 0 R] /Count 1>>\nendobj\n";
            bos.write(obj2.getBytes("ISO-8859-1"));
            currentOffset += obj2.getBytes("ISO-8859-1").length;

            offsets.add(currentOffset);
            String obj3 = "3 0 obj\n<</Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources <</Font <</F1 4 0 R>>>> /Contents 5 0 R>>\nendobj\n";
            bos.write(obj3.getBytes("ISO-8859-1"));
            currentOffset += obj3.getBytes("ISO-8859-1").length;

            offsets.add(currentOffset);
            String obj4 = "4 0 obj\n<</Type /Font /Subtype /Type1 /BaseFont /Courier>>\nendobj\n";
            bos.write(obj4.getBytes("ISO-8859-1"));
            currentOffset += obj4.getBytes("ISO-8859-1").length;

            offsets.add(currentOffset);
            String obj5Head = "5 0 obj\n<</Length " + streamData.length + ">>\nstream\n";
            bos.write(obj5Head.getBytes("ISO-8859-1"));
            bos.write(streamData);
            String obj5Tail = "\nendstream\nendobj\n";
            bos.write(obj5Tail.getBytes("ISO-8859-1"));
            currentOffset += obj5Head.getBytes("ISO-8859-1").length + streamData.length + obj5Tail.getBytes("ISO-8859-1").length;

            long xrefOffset = currentOffset;
            StringBuilder xref = new StringBuilder();
            xref.append("xref\n0 6\n");
            xref.append("0000000000 65535 f \n");
            for (Long off : offsets) {
                xref.append(String.format("%010d 00000 n \n", off));
            }
            xref.append("trailer\n<</Size 6 /Root 1 0 R>>\n");
            xref.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");

            bos.write(xref.toString().getBytes("ISO-8859-1"));
            bos.flush();
        }
    }

    private void exportarVentasAEscritorio() {
        if (registroVentasDelDia.isEmpty() && registroPagosEmpleadosDia.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "No hay ventas ni pagos de empleados registrados todavía para exportar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fechaHoy = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String horaExportacion = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        File escritorio = FileSystemView.getFileSystemView().getHomeDirectory();
        File rootFolder = new File(escritorio, "DOCUMENTOS SIGLO XXI");
        File carpetaReportes = new File(rootFolder, "REPORTES");
        File carpetaExportar = new File(carpetaReportes, "EXPORTAR VENTA DEL DIA");
        if (!carpetaExportar.exists()) {
            carpetaExportar.mkdirs();
        }

        File archivoReporte = new File(carpetaExportar, "Reporte_Ventas_" + fechaHoy + ".txt");

        try (PrintWriter writer = new PrintWriter(new FileWriter(archivoReporte))) {
            writer.println("==================================================================");
            writer.println("                      PAPELERÍA SIGLO XXI                         ");
            writer.println("                REPORTE AUDITABLE DE CAJA DIARIA                  ");
            writer.println("==================================================================");
            writer.println(" FECHA DEL REPORTE : " + fechaHoy);
            writer.println(" HORA DE EMISIÓN   : " + horaExportacion);
            writer.println(" EMBARCADOR / CAJA : " + this.nombreEmpleado.toUpperCase());
            writer.println("==================================================================\n");
            
            writer.println("------------------------------------------------------------------");
            writer.println(" 1. REGISTRO DETALLADO DE TRANSACCIONES DE VENTA");
            writer.println("------------------------------------------------------------------");
            for (String venta : registroVentasDelDia) writer.println(venta);

            if (!registroPagosEmpleadosDia.isEmpty()) {
                writer.println("------------------------------------------------------------------");
                writer.println(" 2. REGISTRO DE SALIDAS Y PAGOS A EMPLEADOS");
                writer.println("------------------------------------------------------------------");
                for (String pago : registroPagosEmpleadosDia) writer.println(pago);
                writer.println();
            }

            double gananciaNeta = acumuladoTotalDia - acumuladoCostoMayoristaDia - acumuladoPagosEmpleadosDia;
            double margenUtilidad = (acumuladoTotalDia > 0) ? (gananciaNeta / acumuladoTotalDia) * 100.0 : 0.0;

            writer.println("==================================================================");
            writer.println("                     RESUMEN Y CONCILIACIÓN DE CAJA               ");
            writer.println("==================================================================");
            writer.println(String.format(" Total Recaudado Efectivo:     $%,14.2f", acumuladoEfectivo));
            writer.println(String.format(" Total Recaudado Digital:      $%,14.2f", acumuladoTransferencia));
            writer.println("------------------------------------------------------------------");
            writer.println(String.format(" TOTAL INGRESO BRUTO DE VENTA: $%,14.2f", acumuladoTotalDia));
            writer.println(String.format(" COSTO MAYORISTA INVENTARIO:   $%,14.2f", acumuladoCostoMayoristaDia));
            writer.println(String.format(" TOTAL PAGOS Y NÓMINA:         $%,14.2f", acumuladoPagosEmpleadosDia));
            writer.println("------------------------------------------------------------------");
            writer.println(String.format(" GANANCIA NETA EN CAJA:        $%,14.2f", gananciaNeta));
            writer.println(String.format(" MARGEN DE GANANCIA NETO:       %.2f%%", margenUtilidad));
            writer.println("==================================================================");
            writer.println("                 FIN DEL INFORME AUDITABLE DE VENTAS              ");
            writer.println("==================================================================");

            int opcionAbrir = JOptionPane.showConfirmDialog(
                    vista,
                    "¡Reporte guardado con éxito!\n\nUbicación:\n" + archivoReporte.getAbsolutePath() + "\n\n¿Desea abrir el archivo ahora mismo?",
                    "Exportación Exitosa",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );

            if (opcionAbrir == JOptionPane.YES_OPTION) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(archivoReporte);
                } else {
                    JOptionPane.showMessageDialog(vista, "No se pudo abrir el archivo automáticamente.", "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(vista, "Error al generar o abrir el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirCentroActualizaciones() {
        JDialog dialogoUpdates = new JDialog(vista, "Centro de Actualizaciones - Sistema de Ventas", true);
        dialogoUpdates.setSize(550, 420);
        dialogoUpdates.setLocationRelativeTo(vista);
        dialogoUpdates.setLayout(new BorderLayout(15, 15));

        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(new Color(240, 244, 248));
        panelHeader.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblTituloHeader = new JLabel("Centro de Actualizaciones y Base de Datos");
        lblTituloHeader.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTituloHeader.setForeground(new Color(33, 37, 41));

        ImageIcon icoHeaderActualizar = vista.cargarIcono(MainFrame.RUTA_ICONOS + "ICONO-ACTUALIZAR.png", 22, 22);
        if (icoHeaderActualizar != null) {
            lblTituloHeader.setIcon(icoHeaderActualizar);
            lblTituloHeader.setIconTextGap(10);
        }

        JLabel lblSubtituloHeader = new JLabel("Mantenga el inventario y el sistema sincronizados con su base de datos Excel.");
        lblSubtituloHeader.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtituloHeader.setForeground(new Color(108, 117, 125));

        panelHeader.add(lblTituloHeader, BorderLayout.NORTH);
        panelHeader.add(lblSubtituloHeader, BorderLayout.SOUTH);
        dialogoUpdates.add(panelHeader, BorderLayout.NORTH);

        JPanel panelInfo = new JPanel();
        panelInfo.setLayout(new BoxLayout(panelInfo, BoxLayout.Y_AXIS));
        panelInfo.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        File excelActual = modeloInventario.obtenerArchivoExcel(vista);
        String rutaExcel = (excelActual != null) ? excelActual.getAbsolutePath() : "No vinculado";

        JPanel panelVersion = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelVersion.setOpaque(false);
        panelVersion.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblVersion = new JLabel("Versión actual del software: " + VERSION_SOFTWARE);
        lblVersion.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton btnBuscarUpdates = new JButton("Buscar Actualizaciones");
        btnBuscarUpdates.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnBuscarUpdates.setBackground(new Color(40, 116, 166));
        btnBuscarUpdates.setForeground(Color.WHITE);

        ImageIcon icoBuscar = vista.cargarIcono(MainFrame.RUTA_ICONOS + "ICONO-ACTUALIZAR.png", 16, 16);
        if (icoBuscar != null) {
            btnBuscarUpdates.setIcon(icoBuscar);
            btnBuscarUpdates.setIconTextGap(8);
        }

        btnBuscarUpdates.addActionListener(e -> {
            MainFrame.DialogoBuscarActualizaciones dialogo = vista.new DialogoBuscarActualizaciones(vista);
            dialogo.setVisible(true);
        });

        panelVersion.add(lblVersion);
        panelVersion.add(btnBuscarUpdates);

        JLabel lblModo = new JLabel(" Modo de ejecución: " + (modeloInventario.isModoPruebas() ? "PRUEBAS (En desarrollo)" : "PRODUCCIÓN"));
        lblModo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblModo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblRutaBD = new JLabel("<html><b>📁 Archivo Excel en uso:</b><br>" + rutaExcel + "</html>");
        lblRutaBD.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblRutaBD.setForeground(new Color(40, 116, 166));
        lblRutaBD.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelInfo.add(panelVersion);
        panelInfo.add(Box.createRigidArea(new Dimension(0, 10)));
        panelInfo.add(lblModo);
        panelInfo.add(Box.createRigidArea(new Dimension(0, 10)));
        panelInfo.add(lblRutaBD);

        dialogoUpdates.add(panelInfo, BorderLayout.CENTER);

        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));

        JButton btnRelinkBD = new JButton("Cambiar / Vincular Base de Datos");
        btnRelinkBD.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRelinkBD.setBackground(new Color(23, 162, 184));
        btnRelinkBD.setForeground(Color.WHITE);

        ImageIcon icoRelink = vista.cargarIcono(MainFrame.RUTA_ICONOS + "ICONO-DESARROLLO-UPDATES.png", 16, 16);
        if (icoRelink != null) {
            btnRelinkBD.setIcon(icoRelink);
            btnRelinkBD.setIconTextGap(8);
        }

        btnRelinkBD.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Seleccionar nueva Base de Datos (.xlsx)");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de Excel (*.xlsx)", "xlsx"));

            int selec = fileChooser.showOpenDialog(dialogoUpdates);
            if (selec == JFileChooser.APPROVE_OPTION) {
                File nuevo = fileChooser.getSelectedFile();
                if (nuevo.exists()) {
                    Preferences prefs = Preferences.userNodeForPackage(InventarioModelo.class);
                    prefs.put("RUTA_EXCEL_INVENTARIO", nuevo.getAbsolutePath());
                    JOptionPane.showMessageDialog(dialogoUpdates, "¡Base de datos vinculada correctamente!\n\n" + nuevo.getAbsolutePath(), "Actualizado", JOptionPane.INFORMATION_MESSAGE);
                    dialogoUpdates.dispose();
                }
            }
        });

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCerrar.addActionListener(e -> dialogoUpdates.dispose());

        panelAcciones.add(btnRelinkBD);
        panelAcciones.add(btnCerrar);

        dialogoUpdates.add(panelAcciones, BorderLayout.SOUTH);
        dialogoUpdates.setVisible(true);
    }
}