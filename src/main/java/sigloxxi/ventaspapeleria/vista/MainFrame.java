package sigloxxi.ventaspapeleria.vista;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

/**
 * Vista Principal del Sistema de Control de Ventas e Inventario - Papelería Siglo XXI.
 * Diseñada en Swing con arquitectura MVC.
 */
public class MainFrame extends JFrame {

    /**
     * Recursos visuales del sistema.
     * La aplicación utiliza rutas relativas para que funcione también
     * cuando el proyecto se mueva a otro equipo.
     */
    public static final String RUTA_IMAGENES = "imagenes/";
    public static final String RUTA_ICONOS = RUTA_IMAGENES + "ICONOS/";
    public static final String RUTA_DINERO = RUTA_IMAGENES + "IMAGENES-DINERO/";

    // =========================================================================
    // CENTRAL DE ICONOS DEL SISTEMA
    // =========================================================================
    public static final String ICONO_ACTUALIZAR = "ICONO-ACTUALIZAR.png";
    public static final String ICONO_ENGRANAJE = "ICONO-ENGRANAGE.png";
    public static final String ICONO_USUARIO = "ICONO-USUARIO.png";
    public static final String ICONO_INTERROGACION = "ICONO-INTERROGACION.png";
    public static final String ICONO_X = "ICONO-X.png";
    public static final String ICONO_DESARROLLO_UPDATES = "ICONO-DESARROLLO-UPDATES.png";
    public static final String ICONO_GUARDAR = "ICONO-GUARDAR.png";

    // --- Ventas / operaciones ------------------------------------------------
    public static final String ICONO_VALIDAR = "ICONO-VALIDAR.png";
    public static final String ICONO_CARRITO_COMPRAS = "ICONO-CARRITO-COMPRAS.png";
    public static final String ICONO_EXPORTAR = "ICONO-EXPORTAR.png";
    public static final String ICONO_COBRAR = "ICONO-COBRAR.png";

    // --- Empleados ------------------------------------------------------------
    public static final String ICONO_PAGO_EMPLEADO = "ICONO-PAGO-EMPLEADO.png";

    // --- Reportes -------------------------------------------------------------
    public static final String ICONO_REPORTES = "ICONO-REPORTES.png";
    public static final String ICONO_REPORTE_ANUAL = "ICONO-REPORTE-ANUAL.png";
    public static final String ICONO_REPORTE_MENSUAL = "ICONO-REPORTE-MENSUAL.png";
    public static final String ICONO_REPORTE_SEMANAL = "ICONO-REPORTE-SEMANAL.png";
    public static final String ICONO_REPORTE_DIARIO = "ICONO-REPORTE-DIARIO.png";

    // =========================================================================
    // PALETA DE COLORES CORPORATIVA Y ESTILOS DE INTERFAZ
    // =========================================================================
    public static final Color COLOR_PRIMARIO = new Color(41, 128, 185);
    public static final Color COLOR_PRIMARIO_DARK = new Color(31, 97, 141);
    public static final Color COLOR_SECUNDARIO = new Color(52, 152, 219);
    public static final Color COLOR_EXITO = new Color(39, 174, 96);
    public static final Color COLOR_EXITO_HOVER = new Color(30, 132, 73);
    public static final Color COLOR_PELIGRO = new Color(192, 57, 43);
    public static final Color COLOR_PELIGRO_HOVER = new Color(146, 43, 33);
    public static final Color COLOR_ADVERTENCIA = new Color(243, 156, 18);
    public static final Color COLOR_OSCURO = new Color(44, 62, 80);
    public static final Color COLOR_FONDO = new Color(245, 247, 250);
    public static final Color COLOR_PANEL = new Color(255, 255, 255);
    public static final Color COLOR_TEXTO_MUTED = new Color(127, 140, 141);
    public static final Color COLOR_CONSOLA_FONDO = new Color(25, 30, 36);
    public static final Color COLOR_CONSOLA_TEXTO = new Color(46, 204, 113);

    // =========================================================================
    // FUENTES TIPOGRÁFICAS
    // =========================================================================
    public static final Font FUENTE_TITULO = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FUENTE_SUBTITULO = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FUENTE_BOTON = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FUENTE_TEXTO = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FUENTE_TEXTO_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FUENTE_CONSOLA = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FUENTE_TOTAL = new Font("Segoe UI", Font.BOLD, 32);

    // =========================================================================
    // COMPONENTES DE MENÚ SUPERIOR
    // =========================================================================
    private JMenuBar menuBar;
    private JMenu menuOpciones;
    private JMenu menuHerramientas;
    private JMenu menuAyuda;
    private JMenuItem itemActualizaciones;
    private JMenuItem itemReportes;
    private JMenuItem itemPagosEmpleados;
    private JMenuItem itemLimpiarConsola;
    private JMenuItem itemAcercaDe;

    // =========================================================================
    // COMPONENTES DE BÚSQUEDA Y REGISTRO
    // =========================================================================
    private JTextField txtBusqueda;
    private JTextField txtCantidad;
    private JButton btnValidar;
    private JButton btnAgregar;
    private JButton btnLimpiarBusqueda;
    private JLabel lblEstadoProducto;

    // =========================================================================
    // COMPONENTES DE LA TABLA DE VENTAS Y CARRITO
    // =========================================================================
    private JTable tablaVentas;
    private DefaultTableModel modeloTabla;
    private JScrollPane scrollTabla;

    // =========================================================================
    // COMPONENTES DE CONSOLA Y LOGS
    // =========================================================================
    private JTextArea txtConsolaVentas;
    private JScrollPane scrollConsola;

    // =========================================================================
    // COMPONENTES DE PANEL INFERIOR Y LIQUIDACIÓN
    // =========================================================================
    private JLabel lblTotal;
    private JLabel lblSubtotalTexto;
    private JButton btnEliminarItem;
    private JButton btnCancelarVenta;
    private JButton btnExportarTxt;
    private JButton btnCobrar;

    // =========================================================================
    // BARRA DE ESTADO Y RELOJ EN TIEMPO REAL
    // =========================================================================
    private JLabel lblReloj;
    private JLabel lblEstadoSistema;
    private JLabel lblUsuarioActivo;
    private javax.swing.Timer timerReloj;
    private String nombreEmpleadoActivo = "SIN ASIGNAR";
    private boolean modoPruebas = true;

    // =========================================================================
    // CONSTRUCTORES
    // =========================================================================
    public MainFrame() {
        this("SIN ASIGNAR", true);
    }

    public MainFrame(String nombreEmpleado, boolean modoPruebas) {
        super("Papelería Siglo XXI - Sistema Profesional de Control de Ventas e Inventario");
        this.nombreEmpleadoActivo = (nombreEmpleado == null || nombreEmpleado.isBlank()) ? "SIN ASIGNAR" : nombreEmpleado.trim();
        this.modoPruebas = modoPruebas;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);

        actualizarTituloVentana();
        inicializarMenu();
        inicializarEstructuraPrincipal();
        configurarAtajosTeclado();
        iniciarRelojSistema();
    }

    private void actualizarTituloVentana() {
        String sufijoModo = modoPruebas ? " [MODO PRUEBAS]" : "";
        setTitle("Papelería Siglo XXI - POS | Empleado: " + nombreEmpleadoActivo.toUpperCase() + sufijoModo);
    }

    // =========================================================================
    // CONFIGURACIÓN DE MENÚS Y SUBMENÚS
    // =========================================================================
    private void inicializarMenu() {
        menuBar = new JMenuBar();
        menuBar.setBackground(COLOR_OSCURO);
        menuBar.setPreferredSize(new Dimension(getWidth(), 35));

        menuOpciones = new JMenu("Opciones del Sistema");
        menuOpciones.setForeground(Color.WHITE);
        menuOpciones.setFont(FUENTE_SUBTITULO);

        itemReportes = crearItemMenuConIcono("Centro de Reportes y Estadísticas", "Abrir el panel consolidado de ventas e informes (F12)", 20, 20, ICONO_REPORTES);
        itemPagosEmpleados = crearItemMenuConIcono("Centro de Pago a Empleados", "Liquidación de comisiones y salarios diarios", 20, 20, ICONO_PAGO_EMPLEADO);
        itemActualizaciones = crearItemMenuConIcono("Buscar Actualizaciones", "Verificar nuevas versiones del sistema", 20, 20, ICONO_ACTUALIZAR);

        menuOpciones.add(itemReportes);
        menuOpciones.add(itemPagosEmpleados);
        menuOpciones.addSeparator();
        menuOpciones.add(itemActualizaciones);

        menuHerramientas = new JMenu("Herramientas");
        menuHerramientas.setForeground(Color.WHITE);
        menuHerramientas.setFont(FUENTE_SUBTITULO);

        itemLimpiarConsola = crearItemMenuConIcono("Limpiar Consola de Registros", "Borrar el historial visible en pantalla", 20, 20, ICONO_X);
        itemLimpiarConsola.addActionListener(e -> txtConsolaVentas.setText(""));
        menuHerramientas.add(itemLimpiarConsola);

        menuAyuda = new JMenu("Ayuda");
        menuAyuda.setForeground(Color.WHITE);
        menuAyuda.setFont(FUENTE_SUBTITULO);

        itemAcercaDe = crearItemMenuConIcono("Acerca de Papelería Siglo XXI", "Información de la versión y desarrollador", 20, 20, ICONO_INTERROGACION);
        itemAcercaDe.addActionListener(e -> mostrarAcercaDe());
        menuAyuda.add(itemAcercaDe);

        menuBar.add(menuOpciones);
        menuBar.add(menuHerramientas);
        menuBar.add(menuAyuda);

        setJMenuBar(menuBar);
    }

    private JMenuItem crearItemMenu(String texto, String tooltip) {
        JMenuItem item = new JMenuItem(texto);
        item.setFont(FUENTE_TEXTO);
        item.setToolTipText(tooltip);
        return item;
    }

    private JMenuItem crearItemMenuConIcono(String texto, String tooltip, int anchoIcono, int altoIcono, String nombreIconoOClaves) {
        JMenuItem item = crearItemMenu(texto, tooltip);
        if (nombreIconoOClaves != null && !nombreIconoOClaves.isBlank()) {
            ImageIcon icono = cargarIcono(RUTA_ICONOS + nombreIconoOClaves, anchoIcono, altoIcono);
            if (icono == null) {
                String rutaIcono = buscarRutaIconoPorPalabras(nombreIconoOClaves.replace(".png", "").replace("ICONO-", ""));
                if (rutaIcono != null) {
                    icono = cargarIcono(rutaIcono, anchoIcono, altoIcono);
                }
            }
            if (icono != null) {
                item.setIcon(icono);
                item.setIconTextGap(8);
            }
        }
        return item;
    }

    // =========================================================================
    // ESTRUCTURA PRINCIPAL DE LA INTERFAZ
    // =========================================================================
    private void inicializarEstructuraPrincipal() {
        JPanel contenedorPrincipal = new JPanel(new BorderLayout(10, 10));
        contenedorPrincipal.setBackground(COLOR_FONDO);
        contenedorPrincipal.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        contenedorPrincipal.add(crearPanelCabecera(), BorderLayout.NORTH);
        contenedorPrincipal.add(crearPanelCentral(), BorderLayout.CENTER);

        JPanel panelInferiorYEstado = new JPanel(new BorderLayout(5, 5));
        panelInferiorYEstado.setOpaque(false);
        panelInferiorYEstado.add(crearPanelInferior(), BorderLayout.CENTER);
        panelInferiorYEstado.add(crearBarraEstado(), BorderLayout.SOUTH);

        contenedorPrincipal.add(panelInferiorYEstado, BorderLayout.SOUTH);

        setContentPane(contenedorPrincipal);
    }

    // =========================================================================
    // PANEL SUPERIOR DE CABECERA Y BÚSQUEDA
    // =========================================================================
    private JPanel crearPanelCabecera() {
        JPanel panelCabecera = new JPanel(new BorderLayout(8, 8));
        panelCabecera.setOpaque(false);

        JPanel panelInfoEmpresa = new JPanel(new BorderLayout());
        panelInfoEmpresa.setBackground(COLOR_OSCURO);
        panelInfoEmpresa.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        JLabel lblTitulo = new JLabel("PAPELERÍA SIGLO XXI — PUNTO DE VENTA");
        lblTitulo.setFont(FUENTE_TITULO);
        lblTitulo.setForeground(Color.WHITE);

        lblReloj = new JLabel();
        lblReloj.setFont(FUENTE_SUBTITULO);
        lblReloj.setForeground(new Color(236, 240, 241));
        lblReloj.setHorizontalAlignment(SwingConstants.RIGHT);

        panelInfoEmpresa.add(lblTitulo, BorderLayout.WEST);
        panelInfoEmpresa.add(lblReloj, BorderLayout.EAST);

        JPanel panelEntrada = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panelEntrada.setBackground(COLOR_PANEL);
        panelEntrada.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220), 1),
                " Búsqueda de Productos y Agregar al Carrito ",
                TitledBorder.LEFT, TitledBorder.TOP, FUENTE_SUBTITULO, COLOR_OSCURO
        ));

        JLabel lblCod = new JLabel("Código / Nombre:");
        lblCod.setFont(FUENTE_TEXTO_BOLD);

        txtBusqueda = new JTextField(22);
        txtBusqueda.setFont(FUENTE_TEXTO);
        txtBusqueda.setToolTipText("Escriba el código exacto o parte de la descripción del producto");

        btnLimpiarBusqueda = new JButton("❌");
        btnLimpiarBusqueda.setToolTipText("Limpiar campo de búsqueda");
        btnLimpiarBusqueda.setMargin(new Insets(2, 6, 2, 6));
        btnLimpiarBusqueda.addActionListener(e -> {
            txtBusqueda.setText("");
            txtBusqueda.requestFocus();
        });

        JLabel lblCant = new JLabel("Cantidad:");
        lblCant.setFont(FUENTE_TEXTO_BOLD);

        txtCantidad = new JTextField("1", 5);
        txtCantidad.setFont(FUENTE_TEXTO);
        txtCantidad.setHorizontalAlignment(JTextField.CENTER);

        btnValidar = crearBotonEstilizado(" Validar Info", ICONO_VALIDAR, COLOR_OSCURO, Color.WHITE);
        btnAgregar = crearBotonEstilizado(" Agregar al Carrito", ICONO_CARRITO_COMPRAS, COLOR_EXITO, Color.WHITE);

        lblEstadoProducto = new JLabel(" ");
        lblEstadoProducto.setFont(FUENTE_TEXTO);
        lblEstadoProducto.setForeground(COLOR_PRIMARIO);

        panelEntrada.add(lblCod);
        panelEntrada.add(txtBusqueda);
        panelEntrada.add(btnLimpiarBusqueda);
        panelEntrada.add(lblCant);
        panelEntrada.add(txtCantidad);
        panelEntrada.add(btnValidar);
        panelEntrada.add(btnAgregar);
        panelEntrada.add(lblEstadoProducto);

        panelCabecera.add(panelInfoEmpresa, BorderLayout.NORTH);
        panelCabecera.add(panelEntrada, BorderLayout.SOUTH);

        return panelCabecera;
    }

    // =========================================================================
    // PANEL CENTRAL DIVIDIDO: TABLA DE VENTAS Y CONSOLA
    // =========================================================================
    private JSplitPane crearPanelCentral() {
        JSplitPane splitCentro = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitCentro.setResizeWeight(0.70);
        splitCentro.setDividerSize(7);

        String[] columnas = {"Código", "Descripción del Producto", "Cant.", "Precio Unitario", "Subtotal", "Costo Mayorista"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaVentas = new JTable(modeloTabla);
        tablaVentas.setRowHeight(26);
        tablaVentas.setFont(FUENTE_TEXTO);
        tablaVentas.getTableHeader().setFont(FUENTE_SUBTITULO);
        tablaVentas.getTableHeader().setBackground(COLOR_OSCURO);
        tablaVentas.getTableHeader().setForeground(Color.WHITE);
        tablaVentas.getTableHeader().setReorderingAllowed(false);
        tablaVentas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaVentas.setGridColor(new Color(230, 230, 230));

        TablaVentasCellRenderer renderer = new TablaVentasCellRenderer();
        for (int i = 0; i < tablaVentas.getColumnCount(); i++) {
            tablaVentas.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        tablaVentas.getColumnModel().getColumn(0).setPreferredWidth(90);  
        tablaVentas.getColumnModel().getColumn(1).setPreferredWidth(350); 
        tablaVentas.getColumnModel().getColumn(2).setPreferredWidth(70);  
        tablaVentas.getColumnModel().getColumn(3).setPreferredWidth(110); 
        tablaVentas.getColumnModel().getColumn(4).setPreferredWidth(110); 
        
        TableColumn colCostoMayorista = tablaVentas.getColumnModel().getColumn(5);
        colCostoMayorista.setMinWidth(0);
        colCostoMayorista.setMaxWidth(0);
        colCostoMayorista.setPreferredWidth(0);

        scrollTabla = new JScrollPane(tablaVentas);
        scrollTabla.setBackground(COLOR_PANEL);
        scrollTabla.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220), 1),
                " Detalle de la Venta en Curso ",
                TitledBorder.LEFT, TitledBorder.TOP, FUENTE_SUBTITULO, COLOR_OSCURO
        ));

        txtConsolaVentas = new JTextArea();
        txtConsolaVentas.setEditable(false);
        txtConsolaVentas.setFont(FUENTE_CONSOLA);
        txtConsolaVentas.setBackground(COLOR_CONSOLA_FONDO);
        txtConsolaVentas.setForeground(COLOR_CONSOLA_TEXTO);
        txtConsolaVentas.setCaretColor(Color.WHITE);
        txtConsolaVentas.setMargin(new Insets(8, 8, 8, 8));

        scrollConsola = new JScrollPane(txtConsolaVentas);
        scrollConsola.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220), 1),
                " Registro de Consola y Eventos del Punto de Venta ",
                TitledBorder.LEFT, TitledBorder.TOP, FUENTE_SUBTITULO, COLOR_OSCURO
        ));

        splitCentro.setTopComponent(scrollTabla);
        splitCentro.setBottomComponent(scrollConsola);

        return splitCentro;
    }

    // =========================================================================
    // PANEL INFERIOR DE TOTALES Y ACCIONES
    // =========================================================================
    private JPanel crearPanelInferior() {
        JPanel panelInferior = new JPanel(new BorderLayout(10, 10));
        panelInferior.setBackground(COLOR_PANEL);
        panelInferior.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JPanel panelTotalInfo = new JPanel(new GridLayout(3, 1, 2, 2));
        panelTotalInfo.setOpaque(false);

        lblSubtotalTexto = new JLabel("Items en carrito: 0");
        lblSubtotalTexto.setFont(FUENTE_TEXTO_BOLD);
        lblSubtotalTexto.setForeground(COLOR_TEXTO_MUTED);

        JLabel lblTextoTotal = new JLabel("TOTAL GENERAL A COBRAR:");
        lblTextoTotal.setFont(FUENTE_SUBTITULO);
        lblTextoTotal.setForeground(COLOR_OSCURO);

        lblTotal = new JLabel("TOTAL: $0.00");
        lblTotal.setFont(FUENTE_TOTAL);
        lblTotal.setForeground(COLOR_PELIGRO);

        panelTotalInfo.add(lblSubtotalTexto);
        panelTotalInfo.add(lblTextoTotal);
        panelTotalInfo.add(lblTotal);

        JPanel panelBotonesAccion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        panelBotonesAccion.setOpaque(false);

        btnEliminarItem = crearBotonEstilizado("Eliminar Item (Supr)", ICONO_X, COLOR_ADVERTENCIA, Color.BLACK);
        btnCancelarVenta = crearBotonEstilizado("Cancelar Venta (Esc)", ICONO_X, COLOR_PELIGRO, Color.WHITE);
        btnExportarTxt = crearBotonEstilizado(" Exportar Venta Día", ICONO_EXPORTAR, COLOR_PRIMARIO, Color.WHITE);
        btnCobrar = crearBotonEstilizado("COBRAR VENTA (F5)", ICONO_COBRAR, COLOR_EXITO, Color.WHITE);
        btnCobrar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnCobrar.setPreferredSize(new Dimension(220, 46));

        panelBotonesAccion.add(btnEliminarItem);
        panelBotonesAccion.add(btnCancelarVenta);
        panelBotonesAccion.add(btnExportarTxt);
        panelBotonesAccion.add(btnCobrar);

        panelInferior.add(panelTotalInfo, BorderLayout.WEST);
        panelInferior.add(panelBotonesAccion, BorderLayout.EAST);

        return panelInferior;
    }

    // =========================================================================
    // BARRA DE ESTADO
    // =========================================================================
    private JPanel crearBarraEstado() {
        JPanel panelEstado = new JPanel(new BorderLayout(10, 0));
        panelEstado.setBackground(COLOR_OSCURO);
        panelEstado.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        lblEstadoSistema = new JLabel("🟢 Sistema en Línea | Inventario Excel conectado | " + (modoPruebas ? "PRUEBAS" : "PRODUCCIÓN"));
        lblEstadoSistema.setFont(FUENTE_TEXTO);
        lblEstadoSistema.setForeground(Color.WHITE);

        lblUsuarioActivo = new JLabel("Empleado Activo: " + nombreEmpleadoActivo.toUpperCase());
        lblUsuarioActivo.setFont(FUENTE_TEXTO);
        lblUsuarioActivo.setForeground(new Color(200, 210, 220));

        panelEstado.add(lblEstadoSistema, BorderLayout.WEST);
        panelEstado.add(lblUsuarioActivo, BorderLayout.EAST);

        return panelEstado;
    }

    // =========================================================================
    // HELPER PARA CREACIÓN DE BOTONES CON ESTILO
    // =========================================================================
    private JButton crearBotonEstilizado(String texto, String nombreIcono, Color fondo, Color textoColor) {
        JButton btn = crearBotonEstilizado(texto, fondo, textoColor);
        if (nombreIcono != null && !nombreIcono.isBlank()) {
            ImageIcon icono = cargarIcono(RUTA_ICONOS + nombreIcono, 18, 18);
            if (icono != null) {
                btn.setIcon(icono);
                btn.setIconTextGap(6);
            }
        }
        return btn;
    }

    private JButton crearBotonEstilizado(String texto, Color fondo, Color textoColor) {
        JButton btn = new JButton(texto);
        btn.setFont(FUENTE_BOTON);
        btn.setBackground(fondo);
        btn.setForeground(textoColor);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fondo.darker(), 1),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(fondo.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(fondo);
            }
        });

        return btn;
    }

    // =========================================================================
    // ATAJOS DE TECLADO GLOBALES
    // =========================================================================
    private void configurarAtajosTeclado() {
        JRootPane root = getRootPane();

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "ACCION_COBRAR");
        root.getActionMap().put("ACCION_COBRAR", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnCobrar.isEnabled()) btnCobrar.doClick();
            }
        });

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ACCION_CANCELAR");
        root.getActionMap().put("ACCION_CANCELAR", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnCancelarVenta.isEnabled()) btnCancelarVenta.doClick();
            }
        });

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "ACCION_ELIMINAR");
        root.getActionMap().put("ACCION_ELIMINAR", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnEliminarItem.isEnabled()) btnEliminarItem.doClick();
            }
        });

        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "ACCION_REPORTES");
        root.getActionMap().put("ACCION_REPORTES", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (itemReportes.isEnabled()) itemReportes.doClick();
            }
        });
    }

    // =========================================================================
    // RELOJ EN TIEMPO REAL
    // =========================================================================
    private void iniciarRelojSistema() {
        timerReloj = new javax.swing.Timer(1000, e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy — hh:mm:ss a", new Locale("es", "CO"));
            String fechaFormateada = sdf.format(new Date());
            lblReloj.setText(fechaFormateada.substring(0, 1).toUpperCase() + fechaFormateada.substring(1));
        });
        timerReloj.start();
    }

    // =========================================================================
    // UTILIDADES DE ICONOS Y LOGS
    // =========================================================================
    public ImageIcon cargarIcono(String ruta, int ancho, int alto) {
        if (ruta == null || ruta.isBlank()) return null;

        try {
            File file = new File(ruta);
            Image img = null;

            if (file.exists() && file.isFile()) {
                img = new ImageIcon(file.getAbsolutePath()).getImage();
            }

            if (img == null) {
                String recurso = ruta.replace("\\", "/");
                if (!recurso.startsWith("/")) recurso = "/" + recurso;

                java.net.URL url = MainFrame.class.getResource(recurso);
                if (url != null) img = new ImageIcon(url).getImage();
            }

            if (img != null) {
                Image imgEscalada = img.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
                return new ImageIcon(imgEscalada);
            }
        } catch (Exception ex) {
            System.err.println("Error cargando icono (" + ruta + "): " + ex.getMessage());
        }

        return null;
    }

    private String buscarRutaIconoPorPalabras(String... palabrasClave) {
        if (palabrasClave == null || palabrasClave.length == 0) return null;

        File carpeta = new File(RUTA_ICONOS);
        if (!carpeta.exists() || !carpeta.isDirectory()) return null;

        File[] archivos = carpeta.listFiles((dir, nombre) -> {
            String nombreMinusculas = nombre.toLowerCase(Locale.ROOT);
            return nombreMinusculas.endsWith(".png")
                    || nombreMinusculas.endsWith(".jpg")
                    || nombreMinusculas.endsWith(".jpeg")
                    || nombreMinusculas.endsWith(".gif")
                    || nombreMinusculas.endsWith(".webp");
        });

        if (archivos == null || archivos.length == 0) return null;

        File mejorCoincidencia = null;
        int mejorPuntuacion = -1;
        long mejorFecha = Long.MIN_VALUE;

        for (File archivo : archivos) {
            String nombre = archivo.getName().toLowerCase(Locale.ROOT);
            int puntuacion = 0;

            for (String palabra : palabrasClave) {
                if (palabra == null || palabra.isBlank()) continue;
                String clave = palabra.toLowerCase(Locale.ROOT).trim();
                if (nombre.contains(clave)) puntuacion += 10;
            }

            if (puntuacion <= 0) continue;

            long fechaModificacion = archivo.lastModified();
            if (puntuacion > mejorPuntuacion || (puntuacion == mejorPuntuacion && fechaModificacion > mejorFecha)) {
                mejorCoincidencia = archivo;
                mejorPuntuacion = puntuacion;
                mejorFecha = fechaModificacion;
            }
        }

        return mejorCoincidencia != null ? mejorCoincidencia.getPath() : null;
    }

    public void agregarLogConsola(String mensaje) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String hora = sdf.format(new Date());
        txtConsolaVentas.append("[" + hora + "] " + mensaje + "\n");
        txtConsolaVentas.setCaretPosition(txtConsolaVentas.getDocument().getLength());
    }

    public void actualizarResumenCarrito(int totalItems, double granTotal) {
        lblSubtotalTexto.setText("Items en carrito: " + totalItems);
        lblTotal.setText(String.format("TOTAL: $%.2f", granTotal));
    }

    private void mostrarAcercaDe() {
        String mensaje =
                "Papelería Siglo XXI - POS & Inventory System\n" +
                "Versión 1.0.0 Tienda de Barrio Edition\n" +
                "Desarrollado para Control Operativo y Facturación Diaria\n\n" +
                "© 2026 Todos los derechos reservados.";

        ImageIcon icono = cargarIcono(RUTA_ICONOS + ICONO_INTERROGACION, 56, 56);
        if (icono == null) {
            String rutaIcono = buscarRutaIconoPorPalabras("acerca", "about", "informacion", "info", "siglo", "ICONO-INTERROGACION");
            if (rutaIcono != null) icono = cargarIcono(rutaIcono, 56, 56);
        }

        JOptionPane.showMessageDialog(this, mensaje, "Acerca de Papelería Siglo XXI", JOptionPane.INFORMATION_MESSAGE, icono);
    }

    // =========================================================================
    // GETTERS DE COMPONENTES VISUALES
    // =========================================================================
    public JTextField getTxtBusqueda() { return txtBusqueda; }
    public JTextField getTxtCantidad() { return txtCantidad; }
    public JButton getBtnValidar() { return btnValidar; }
    public JButton getBtnAgregar() { return btnAgregar; }
    public JButton getBtnEliminarItem() { return btnEliminarItem; }
    public JButton getBtnCancelarVenta() { return btnCancelarVenta; }
    public JButton getBtnCobrar() { return btnCobrar; }
    public JButton getBtnExportarTxt() { return btnExportarTxt; }
    public JMenuItem getItemActualizaciones() { return itemActualizaciones; }
    public JMenuItem getItemReportes() { return itemReportes; }
    public JMenuItem getItemPagosEmpleados() { return itemPagosEmpleados; }
    public JTable getTablaVentas() { return tablaVentas; }
    public DefaultTableModel getModeloTabla() { return modeloTabla; }
    public JLabel getLblTotal() { return lblTotal; }
    public JTextArea getTxtConsolaVentas() { return txtConsolaVentas; }
    public JLabel getLblEstadoProducto() { return lblEstadoProducto; }
    public String getNombreEmpleadoActivo() { return nombreEmpleadoActivo; }
    public boolean isModoPruebas() { return modoPruebas; }

    public void setEstadoSistema(String mensaje) {
        if (lblEstadoSistema != null) lblEstadoSistema.setText(mensaje);
    }

    // =========================================================================
    // RENDERIZADOR PERSONALIZADO PARA TABLA DE VENTAS
    // =========================================================================
    public class TablaVentasCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                c.setForeground(COLOR_OSCURO);
            } else {
                c.setBackground(COLOR_SECUNDARIO);
                c.setForeground(Color.WHITE);
            }

            if (column == 0 || column == 2) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else if (column >= 3) {
                setHorizontalAlignment(SwingConstants.RIGHT); 
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);  
            }

            return c;
        }
    }

    // =========================================================================
    // DIÁLOGOS Y VENTANAS EMERGENTES (INCLUIDAS PARA COMPATIBILIDAD CON CONTROLADOR)
    // =========================================================================

    // =========================================================================
    // DIÁLOGO DE SELECCIÓN RÁPIDA DE EFECTIVO (BILLETES Y MONEDAS)
    // =========================================================================
    public class DialogoEfectivoBotones extends JDialog {
        private boolean confirmado = false;
        private double dineroRecibido = 0.0;
        private double devuelta = 0.0;
        private String desgloseDevueltaTexto = "";
        private final int[] valores = {100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100, 50};
        private final String[] etiquetas = {"$100.000", "$50.000", "$20.000", "$10.000", "$5.000", "$2.000", "$1.000", "$500", "$200", "$100", "$50"};
        private final String[] rutasImagenes = {
            MainFrame.RUTA_DINERO + "billete-100000.jpg", 
            MainFrame.RUTA_DINERO + "billete-50000.jpg", 
            MainFrame.RUTA_DINERO + "billete-20000.jpg", 
            MainFrame.RUTA_DINERO + "billete-10000.jpg", 
            MainFrame.RUTA_DINERO + "billete-5000.jpg", 
            MainFrame.RUTA_DINERO + "billete-2000.jpg", 
            MainFrame.RUTA_DINERO + "moneda-1000.jpg", 
            MainFrame.RUTA_DINERO + "moneda-500.jpg", 
            MainFrame.RUTA_DINERO + "moneda-200.jpg", 
            MainFrame.RUTA_DINERO + "moneda-100.jpg", 
            MainFrame.RUTA_DINERO + "moneda-50.jpg"
        };
        private final int[] cantidades = new int[11];
        private final JLabel[] lblContadores = new JLabel[11];
        private final JLabel lblTotalRecibidoVal;
        private final JLabel lblDevueltaVal;
        private final JLabel lblDesgloseVal;
        private final double totalPagar;

        public DialogoEfectivoBotones(Frame parent, double totalPagar) {
            super(parent, "Selección Rápida de Efectivo (COP)", true);
            this.totalPagar = totalPagar;
            setSize(750, 780); 
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout(10, 10));

            JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JLabel lblTotal = new JLabel(String.format("TOTAL A PAGAR: $%.2f", totalPagar));
            lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 20));
            lblTotal.setForeground(new Color(40, 116, 166));
            panelNorte.add(lblTotal);
            add(panelNorte, BorderLayout.NORTH);

            JPanel panelGridBotones = new JPanel(new GridLayout(4, 3, 10, 10));
            panelGridBotones.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            for (int i = 0; i < valores.length; i++) {
                final int index = i;
                JPanel panelBotonItem = new JPanel(new BorderLayout(2, 2));
                panelBotonItem.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
                JButton btnDenom = new JButton(etiquetas[i]);
                btnDenom.setFont(new Font("Segoe UI", Font.BOLD, 13));
                if (valores[i] >= 2000) btnDenom.setBackground(new Color(220, 237, 245));
                else btnDenom.setBackground(new Color(245, 240, 220));
                
                ImageIcon iconoDinero = cargarIcono(rutasImagenes[i], 90, 45);
                if (iconoDinero != null) {
                    btnDenom.setIcon(iconoDinero);
                    btnDenom.setVerticalTextPosition(SwingConstants.BOTTOM);
                    btnDenom.setHorizontalTextPosition(SwingConstants.CENTER);
                }
                
                lblContadores[i] = new JLabel("Cant: 0", SwingConstants.CENTER);
                lblContadores[i].setFont(new Font("Segoe UI", Font.PLAIN, 12));
                btnDenom.addActionListener(e -> {
                    cantidades[index]++;
                    actualizarCalculos();
                });
                panelBotonItem.add(btnDenom, BorderLayout.CENTER);
                panelBotonItem.add(lblContadores[i], BorderLayout.SOUTH);
                panelGridBotones.add(panelBotonItem);
            }

            JButton btnLimpiar = new JButton("🔄 Limpiar Todo");
            btnLimpiar.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnLimpiar.setBackground(new Color(220, 53, 69));
            btnLimpiar.setForeground(Color.WHITE);
            btnLimpiar.addActionListener(e -> {
                for (int i = 0; i < cantidades.length; i++) cantidades[i] = 0;
                actualizarCalculos();
            });
            panelGridBotones.add(btnLimpiar);
            add(panelGridBotones, BorderLayout.CENTER);

            JPanel panelSur = new JPanel();
            panelSur.setLayout(new BoxLayout(panelSur, BoxLayout.Y_AXIS));
            panelSur.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            JPanel panelResultados = new JPanel(new GridLayout(3, 1, 3, 3));
            lblTotalRecibidoVal = new JLabel("Total Recibido: $0.00");
            lblTotalRecibidoVal.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblDevueltaVal = new JLabel("Cambio (Devuelta): $0.00");
            lblDevueltaVal.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblDevueltaVal.setForeground(new Color(0, 128, 0));
            lblDesgloseVal = new JLabel("Desglose devuelta: -");
            lblDesgloseVal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            panelResultados.add(lblTotalRecibidoVal);
            panelResultados.add(lblDevueltaVal);
            panelResultados.add(lblDesgloseVal);
            panelSur.add(panelResultados);

            JPanel panelBotonesAccion = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnConfirmar = new JButton("Confirmar Pago");
            btnConfirmar.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnConfirmar.setBackground(new Color(40, 167, 69));
            btnConfirmar.setForeground(Color.WHITE);
            JButton btnCancelar = new JButton("Cancelar");
            btnCancelar.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnCancelar.setBackground(new Color(108, 117, 125));
            btnCancelar.setForeground(Color.WHITE);

            btnConfirmar.addActionListener(e -> {
                if (dineroRecibido < totalPagar) {
                    JOptionPane.showMessageDialog(this, "El dinero recibido es menor al total a pagar.", "Monto Insuficiente", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                confirmado = true;
                dispose();
            });
            btnCancelar.addActionListener(e -> {
                confirmado = false;
                dispose();
            });
            panelBotonesAccion.add(btnCancelar);
            panelBotonesAccion.add(btnConfirmar);
            panelSur.add(panelBotonesAccion);
            add(panelSur, BorderLayout.SOUTH);
            actualizarCalculos();
        }

        private void actualizarCalculos() {
            double recibido = 0.0;
            for (int i = 0; i < valores.length; i++) {
                recibido += (double) cantidades[i] * valores[i];
                lblContadores[i].setText("Cant: " + cantidades[i]);
            }
            dineroRecibido = recibido;
            devuelta = dineroRecibido - totalPagar;
            if (devuelta < 0) devuelta = 0.0;
            lblTotalRecibidoVal.setText(String.format("Total Recibido: $%.2f", dineroRecibido));
            lblDevueltaVal.setText(String.format("Cambio (Devuelta): $%.2f", devuelta));
            if (dineroRecibido >= totalPagar) {
                double cambioRestante = devuelta;
                StringBuilder desglose = new StringBuilder("Desglose devuelta: ");
                boolean hayCambio = false;
                for (int i = 0; i < valores.length; i++) {
                    int cantidadDenominacion = (int) (cambioRestante / valores[i]);
                    if (cantidadDenominacion > 0) {
                        desglose.append(cantidadDenominacion).append("x($").append(etiquetas[i]).append(") ");
                        cambioRestante %= valores[i];
                        hayCambio = true;
                    }
                }
                if (!hayCambio) desglose.append("Pago exacto (Sin devuelta)");
                desgloseDevueltaTexto = desglose.toString();
                lblDesgloseVal.setText(desgloseDevueltaTexto);
            } else {
                lblDesgloseVal.setText("Desglose devuelta: Monto insuficiente");
            }
        }

        public boolean isConfirmado() { return confirmado; }
        public double getDineroRecibido() { return dineroRecibido; }
        public double getDevuelta() { return devuelta; }
        public String getDesgloseDevueltaTexto() { return desgloseDevueltaTexto; }
    }

    public class DialogoPagoMixto extends JDialog {
        private boolean confirmado = false;
        private double montoEfectivo = 0.0;
        private double montoTransferencia = 0.0;
        private double devuelta = 0.0;

        public DialogoPagoMixto(Frame padre, double totalPagar) {
            super(padre, "Registro de Pago Combinado / Mixto", true);
            setSize(420, 300);
            setLocationRelativeTo(padre);
            setLayout(new BorderLayout(10, 10));
            setResizable(false);

            JPanel panelForm = new JPanel(new GridLayout(4, 2, 10, 12));
            panelForm.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
            panelForm.setBackground(COLOR_FONDO);

            panelForm.add(new JLabel("Total de la Venta:"));
            JLabel lblTotalVenta = new JLabel(String.format("$ %,.2f", totalPagar));
            lblTotalVenta.setFont(FUENTE_SUBTITULO);
            lblTotalVenta.setForeground(COLOR_OSCURO);
            panelForm.add(lblTotalVenta);

            panelForm.add(new JLabel("Monto en Efectivo:"));
            JTextField txtEfectivo = new JTextField();
            txtEfectivo.setFont(FUENTE_TEXTO);
            panelForm.add(txtEfectivo);

            panelForm.add(new JLabel("Monto Transferencia / Nequi / Daviplata:"));
            JTextField txtTransferencia = new JTextField();
            txtTransferencia.setFont(FUENTE_TEXTO);
            panelForm.add(txtTransferencia);

            JLabel lblEstado = new JLabel("Suma Cubierta:");
            lblEstado.setFont(FUENTE_TEXTO_BOLD);
            panelForm.add(lblEstado);

            add(panelForm, BorderLayout.CENTER);

            JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            panelAcciones.setBackground(COLOR_FONDO);

            JButton btnCancelar = crearBotonEstilizado("Cancelar", COLOR_PELIGRO, Color.WHITE);
            btnCancelar.addActionListener(e -> dispose());

            JButton btnAceptar = crearBotonEstilizado("Confirmar Pago Mixto", COLOR_EXITO, Color.WHITE);
            btnAceptar.addActionListener(e -> {
                try {
                    String efText = txtEfectivo.getText().trim().replace(".", "").replace(",", ".");
                    String trText = txtTransferencia.getText().trim().replace(".", "").replace(",", ".");

                    montoEfectivo = efText.isEmpty() ? 0.0 : Double.parseDouble(efText);
                    montoTransferencia = trText.isEmpty() ? 0.0 : Double.parseDouble(trText);

                    double suma = montoEfectivo + montoTransferencia;
                    if (suma < totalPagar) {
                        JOptionPane.showMessageDialog(this, "El dinero total sumado es menor al precio de la venta.", "Incompleto", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    devuelta = suma - totalPagar;
                    confirmado = true;
                    dispose();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Por favor revise los valores ingresados.", "Formato Inválido", JOptionPane.ERROR_MESSAGE);
                }
            });

            panelAcciones.add(btnCancelar);
            panelAcciones.add(btnAceptar);
            add(panelAcciones, BorderLayout.SOUTH);
        }

        public boolean isConfirmado() { return confirmado; }
        public double getMontoEfectivo() { return montoEfectivo; }
        public double getMontoTransferencia() { return montoTransferencia; }
        public double getDevuelta() { return devuelta; }
    }

    public class DialogoPagoEmpleados extends JDialog {
        private final JComboBox<String> comboEmpleado;
        private final JComboBox<String> comboConcepto;
        private final JSpinner spinnerFecha;
        private final JButton btnAbrirSelectorFecha;
        private final JLabel lblTextoFechaEvaluacion;
        private final JLabel lblVentasTotales;
        private final JTextField txtPorcentajeComision;
        private final JTextField txtMontoPagar;
        private final JLabel lblTotalPagadoHoy;
        private final JButton btnRegistrarPago;

        private LocalDate inicioRango;
        private LocalDate finRango;

        public DialogoPagoEmpleados(Frame padre, String[] empleados, String empleadoActual) {
            super(padre, "Centro de Pagos y Liquidación a Empleados", true);
            setSize(520, 520);
            setLocationRelativeTo(padre);
            setLayout(new BorderLayout(10, 10));
            setResizable(false);

            this.inicioRango = LocalDate.now();
            this.finRango = LocalDate.now();

            JPanel panelForm = new JPanel(new GridLayout(9, 2, 10, 10));
            panelForm.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
            panelForm.setBackground(COLOR_FONDO);

            panelForm.add(new JLabel("Empleado A Evaluar:"));
            comboEmpleado = new JComboBox<>(empleados);
            comboEmpleado.setFont(FUENTE_TEXTO);
            if (empleadoActual != null) comboEmpleado.setSelectedItem(empleadoActual);
            panelForm.add(comboEmpleado);

            panelForm.add(new JLabel("Concepto del Pago:"));
            comboConcepto = new JComboBox<>(new String[]{"Comisión de Ventas", "Sueldo Diario", "Adelanto de Salario", "Bono por Objetivos"});
            comboConcepto.setFont(FUENTE_TEXTO);
            panelForm.add(comboConcepto);

            panelForm.add(new JLabel("Fecha de Evaluación:"));
            JPanel panelFecha = new JPanel(new BorderLayout(5, 0));
            panelFecha.setOpaque(false);
            spinnerFecha = new JSpinner(new SpinnerDateModel());
            JSpinner.DateEditor editor = new JSpinner.DateEditor(spinnerFecha, "yyyy-MM-dd");
            spinnerFecha.setEditor(editor);
            spinnerFecha.setFont(FUENTE_TEXTO);

            btnAbrirSelectorFecha = new JButton("📅 Calendario");
            btnAbrirSelectorFecha.setFont(FUENTE_BOTON);
            btnAbrirSelectorFecha.setToolTipText("Abrir selector gráfico de fechas");

            panelFecha.add(spinnerFecha, BorderLayout.CENTER);
            panelFecha.add(btnAbrirSelectorFecha, BorderLayout.EAST);
            panelForm.add(panelFecha);

            panelForm.add(new JLabel("Período Evaluado:"));
            lblTextoFechaEvaluacion = new JLabel(inicioRango.toString());
            lblTextoFechaEvaluacion.setFont(FUENTE_TEXTO_BOLD);
            lblTextoFechaEvaluacion.setForeground(COLOR_OSCURO);
            panelForm.add(lblTextoFechaEvaluacion);

            panelForm.add(new JLabel("Ventas Base Período:"));
            lblVentasTotales = new JLabel("$ 0,00");
            lblVentasTotales.setFont(FUENTE_SUBTITULO);
            lblVentasTotales.setForeground(COLOR_PRIMARIO);
            panelForm.add(lblVentasTotales);

            panelForm.add(new JLabel("Comisión Sugerida (%):"));
            txtPorcentajeComision = new JTextField("5.0");
            txtPorcentajeComision.setFont(FUENTE_TEXTO);
            panelForm.add(txtPorcentajeComision);

            panelForm.add(new JLabel("Monto Final a Pagar ($):"));
            txtMontoPagar = new JTextField("0.00");
            txtMontoPagar.setFont(new Font("Segoe UI", Font.BOLD, 14));
            panelForm.add(txtMontoPagar);

            panelForm.add(new JLabel("Total Liquidado Hoy:"));
            lblTotalPagadoHoy = new JLabel("$ 0,00");
            lblTotalPagadoHoy.setFont(FUENTE_SUBTITULO);
            lblTotalPagadoHoy.setForeground(COLOR_PELIGRO);
            panelForm.add(lblTotalPagadoHoy);

            btnRegistrarPago = crearBotonEstilizado(" Registrar y Guardar Pago", ICONO_GUARDAR, COLOR_EXITO, Color.WHITE);
            btnRegistrarPago.setFont(FUENTE_SUBTITULO);

            add(panelForm, BorderLayout.CENTER);

            JPanel panelFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
            panelFooter.setBackground(COLOR_FONDO);
            panelFooter.add(btnRegistrarPago);
            add(panelFooter, BorderLayout.SOUTH);
        }

        public void setTextoFechaEvaluacion(String texto) {
            if (lblTextoFechaEvaluacion != null) {
                lblTextoFechaEvaluacion.setText(texto);
            }
        }

        public void setRangoEvaluacion(LocalDate inicio, LocalDate fin) {
            this.inicioRango = inicio;
            this.finRango = fin;
            if (inicio != null && fin != null) {
                if (inicio.equals(fin)) {
                    setTextoFechaEvaluacion(inicio.toString());
                } else {
                    setTextoFechaEvaluacion(inicio.toString() + " al " + fin.toString());
                }
            }
        }

        public LocalDate getInicioRango() {
            if (inicioRango == null) {
                Date d = getFechaSeleccionada();
                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            return inicioRango;
        }

        public LocalDate getFinRango() {
            if (finRango == null) {
                Date d = getFechaSeleccionada();
                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            return finRango;
        }

        public JComboBox<String> getComboEmpleado() { return comboEmpleado; }
        public JComboBox<String> getComboConcepto() { return comboConcepto; }
        public JSpinner getSpinnerFecha() { return spinnerFecha; }
        public JButton getBtnAbrirSelectorFecha() { return btnAbrirSelectorFecha; }
        public JLabel getLblTextoFechaEvaluacion() { return lblTextoFechaEvaluacion; }
        public JLabel getLblVentasTotales() { return lblVentasTotales; }
        public JTextField getTxtPorcentajeComision() { return txtPorcentajeComision; }
        public JTextField getTxtMontoPagar() { return txtMontoPagar; }
        public JLabel getLblTotalPagadoHoy() { return lblTotalPagadoHoy; }
        public JButton getBtnRegistrarPago() { return btnRegistrarPago; }

        public Date getFechaSeleccionada() {
            return (Date) spinnerFecha.getValue();
        }

        public void setFechaSeleccionada(Date fecha) {
            spinnerFecha.setValue(fecha);
            if (fecha != null) {
                LocalDate ld = fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                setRangoEvaluacion(ld, ld);
            }
        }
    }

    public class DialogoCentroReportes extends JDialog {
        private final JLabel lblTotalBrutoHoy;
        private final JLabel lblTotalGananciaHoy;
        private final JLabel lblTotalTransacciones;
        private final JTable tablaConsolidado;
        private final DefaultTableModel modeloConsolidado;

        public DialogoCentroReportes(Frame padre) {
            super(padre, "Centro de Reportes y Estadísticas de Ventas", true);
            setSize(780, 520);
            setLocationRelativeTo(padre);
            setLayout(new BorderLayout(10, 10));

            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(FUENTE_SUBTITULO);

            JPanel panelResumen = new JPanel(new BorderLayout(10, 10));
            panelResumen.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            panelResumen.setBackground(COLOR_FONDO);

            JPanel panelKpis = new JPanel(new GridLayout(1, 3, 15, 0));
            panelKpis.setOpaque(false);

            lblTotalBrutoHoy = crearTarjetaKPI("Ingresos Brutos", "$ 0,00", COLOR_PRIMARIO);
            lblTotalGananciaHoy = crearTarjetaKPI("Ganancia Estimada", "$ 0,00", COLOR_EXITO);
            lblTotalTransacciones = crearTarjetaKPI("Ventas Realizadas", "0", COLOR_OSCURO);

            panelKpis.add(lblTotalBrutoHoy.getParent());
            panelKpis.add(lblTotalGananciaHoy.getParent());
            panelKpis.add(lblTotalTransacciones.getParent());

            panelResumen.add(panelKpis, BorderLayout.NORTH);

            String[] col = {"Hora Venta", "Método Pago", "Items", "Monto Total", "Atendido Por"};
            modeloConsolidado = new DefaultTableModel(col, 0);
            tablaConsolidado = new JTable(modeloConsolidado);
            tablaConsolidado.setRowHeight(24);

            JScrollPane scrollConsolidado = new JScrollPane(tablaConsolidado);
            scrollConsolidado.setBorder(BorderFactory.createTitledBorder(" Detalle de Transacciones del Día "));
            panelResumen.add(scrollConsolidado, BorderLayout.CENTER);

            JPanel panelGrafico = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setFont(FUENTE_SUBTITULO);
                    g2.setColor(COLOR_OSCURO);
                    g2.drawString("Comportamiento Estímulo de Ventas por Tramo Horario", 30, 35);

                    int origenX = 60;
                    int origenY = 280;
                    g2.drawLine(origenX, origenY, 680, origenY);
                    g2.drawLine(origenX, 60, origenX, origenY);

                    int[] valores = {45, 90, 150, 80, 220, 260, 190, 110};
                    String[] horas = {"8am", "10am", "12pm", "2pm", "4pm", "6pm", "8pm", "10pm"};

                    int x = origenX + 25;
                    for (int i = 0; i < valores.length; i++) {
                        int h = valores[i];
                        g2.setColor(COLOR_SECUNDARIO);
                        g2.fillRect(x, origenY - h, 42, h);
                        g2.setColor(COLOR_OSCURO);
                        g2.drawRect(x, origenY - h, 42, h);

                        g2.setFont(FUENTE_TEXTO);
                        g2.drawString(horas[i], x + 5, origenY + 20);
                        x += 72;
                    }
                }
            };
            panelGrafico.setBackground(Color.WHITE);

            tabbedPane.addTab("📈 Resumen General", panelResumen);
            tabbedPane.addTab("📊 Gráfico Estadístico", panelGrafico);

            add(tabbedPane, BorderLayout.CENTER);
        }

        private JLabel crearTarjetaKPI(String titulo, String valorInicial, Color color) {
            JPanel card = new JPanel(new BorderLayout(5, 5));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color, 2),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            JLabel lblT = new JLabel(titulo);
            lblT.setFont(FUENTE_TEXTO);
            lblT.setForeground(COLOR_OSCURO);

            JLabel lblV = new JLabel(valorInicial);
            lblV.setFont(new Font("Segoe UI", Font.BOLD, 20));
            lblV.setForeground(color);

            card.add(lblT, BorderLayout.NORTH);
            card.add(lblV, BorderLayout.CENTER);

            return lblV;
        }

        public JLabel getLblTotalBrutoHoy() { return lblTotalBrutoHoy; }
        public JLabel getLblTotalGananciaHoy() { return lblTotalGananciaHoy; }
        public JLabel getLblTotalTransacciones() { return lblTotalTransacciones; }
        public DefaultTableModel getModeloConsolidado() { return modeloConsolidado; }
    }

    public class DialogoBuscarActualizaciones extends JDialog {
        public DialogoBuscarActualizaciones(Frame padre) {
            super(padre, "Verificación del Sistema", true);
            setSize(400, 220);
            setLocationRelativeTo(padre);
            setLayout(new BorderLayout(10, 10));
            setResizable(false);

            JPanel panel = new JPanel(new BorderLayout(15, 15));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel lblInfo = new JLabel("Conectando con el servidor de Papelería Siglo XXI...", SwingConstants.CENTER);
            lblInfo.setFont(FUENTE_SUBTITULO);

            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);

            panel.add(lblInfo, BorderLayout.NORTH);
            panel.add(progressBar, BorderLayout.CENTER);

            JButton btnCerrar = crearBotonEstilizado("Entendido", COLOR_OSCURO, Color.WHITE);
            btnCerrar.addActionListener(e -> dispose());
            panel.add(btnCerrar, BorderLayout.SOUTH);

            add(panel);

            javax.swing.Timer t = new javax.swing.Timer(2200, e -> {
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                lblInfo.setText("¡El sistema cuenta con la última versión instalada!");
            });
            t.setRepeats(false);
            t.start();
        }
    }

    public static class DialogoSelectorFecha extends JDialog {
        private boolean confirmado = false;
        private Date fechaSeleccionada;
        private LocalDate inicioRango;
        private LocalDate finRango;
        private boolean modoRango = false;

        private Calendar calendario;
        private JLabel lblMesAño;
        private JPanel panelCuadrillaDias;
        private JButton[] botonesDias;

        public DialogoSelectorFecha(JDialog padre, Date fechaInicial) {
            this(padre, fechaInicial, null, null, false);
        }

        public DialogoSelectorFecha(JDialog padre, Date fechaInicial, LocalDate inicio, LocalDate fin, boolean esRango) {
            super(padre, "Calendario — Seleccionar Fecha", true);
            setSize(380, 380);
            setLocationRelativeTo(padre);
            setLayout(new BorderLayout(8, 8));
            setResizable(false);

            this.modoRango = esRango;
            this.inicioRango = inicio != null ? inicio : LocalDate.now();
            this.finRango = fin != null ? fin : LocalDate.now();

            this.calendario = new GregorianCalendar();
            this.calendario.setTime(fechaInicial != null ? fechaInicial : new Date());

            botonesDias = new JButton[42];

            inicializarComponentesCalendario();
            actualizarCuadrillaCalendario();
        }

        private void inicializarComponentesCalendario() {
            JPanel panelHeader = new JPanel(new BorderLayout(5, 5));
            panelHeader.setBackground(COLOR_OSCURO);
            panelHeader.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

            JButton btnAnterior = new JButton("◄");
            btnAnterior.setForeground(Color.WHITE);
            btnAnterior.setContentAreaFilled(false);
            btnAnterior.setFocusPainted(false);
            btnAnterior.setFont(FUENTE_BOTON);

            btnAnterior.addActionListener(e -> {
                calendario.add(Calendar.MONTH, -1);
                actualizarCuadrillaCalendario();
            });

            JButton btnSiguiente = new JButton("►");
            btnSiguiente.setForeground(Color.WHITE);
            btnSiguiente.setContentAreaFilled(false);
            btnSiguiente.setFocusPainted(false);
            btnSiguiente.setFont(FUENTE_BOTON);

            btnSiguiente.addActionListener(e -> {
                calendario.add(Calendar.MONTH, 1);
                actualizarCuadrillaCalendario();
            });

            lblMesAño = new JLabel("", SwingConstants.CENTER);
            lblMesAño.setFont(FUENTE_SUBTITULO);
            lblMesAño.setForeground(Color.WHITE);

            panelHeader.add(btnAnterior, BorderLayout.WEST);
            panelHeader.add(lblMesAño, BorderLayout.CENTER);
            panelHeader.add(btnSiguiente, BorderLayout.EAST);

            add(panelHeader, BorderLayout.NORTH);

            JPanel panelCentral = new JPanel(new BorderLayout(2, 2));
            panelCentral.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            JPanel panelEncabezadoSemana = new JPanel(new GridLayout(1, 7, 2, 2));
            String[] diasSemana = {"Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
            for (String d : diasSemana) {
                JLabel lblD = new JLabel(d, SwingConstants.CENTER);
                lblD.setFont(FUENTE_TEXTO_BOLD);
                lblD.setForeground(COLOR_OSCURO);
                panelEncabezadoSemana.add(lblD);
            }

            panelCuadrillaDias = new JPanel(new GridLayout(6, 7, 2, 2));
            for (int i = 0; i < 42; i++) {
                JButton btnDia = new JButton();
                btnDia.setFont(FUENTE_TEXTO);
                btnDia.setMargin(new Insets(2, 2, 2, 2));
                btnDia.setFocusPainted(false);

                btnDia.addActionListener(e -> {
                    String text = btnDia.getText();
                    if (!text.isEmpty()) {
                        int dia = Integer.parseInt(text);
                        calendario.set(Calendar.DAY_OF_MONTH, dia);
                        fechaSeleccionada = calendario.getTime();

                        java.time.LocalDate ld = fechaSeleccionada.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                        inicioRango = ld;
                        finRango = ld;

                        confirmado = true;
                        dispose();
                    }
                });

                botonesDias[i] = btnDia;
                panelCuadrillaDias.add(btnDia);
            }

            panelCentral.add(panelEncabezadoSemana, BorderLayout.NORTH);
            panelCentral.add(panelCuadrillaDias, BorderLayout.CENTER);

            add(panelCentral, BorderLayout.CENTER);

            JPanel panelFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            panelFooter.setBackground(COLOR_FONDO);

            JButton btnHoy = new JButton("Hoy");
            btnHoy.setFont(FUENTE_BOTON);
            btnHoy.addActionListener(e -> {
                calendario.setTime(new Date());
                actualizarCuadrillaCalendario();
            });

            JButton btnCancelar = new JButton("Cancelar");
            btnCancelar.setFont(FUENTE_BOTON);
            btnCancelar.addActionListener(e -> dispose());

            panelFooter.add(btnHoy);
            panelFooter.add(btnCancelar);

            add(panelFooter, BorderLayout.SOUTH);
        }

        private void actualizarCuadrillaCalendario() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("es", "CO"));
            String mesAñoStr = sdf.format(calendario.getTime());
            lblMesAño.setText(mesAñoStr.substring(0, 1).toUpperCase() + mesAñoStr.substring(1));

            Calendar calTemp = (Calendar) calendario.clone();
            calTemp.set(Calendar.DAY_OF_MONTH, 1);

            int primerDiaSemana = calTemp.get(Calendar.DAY_OF_WEEK) - 1;
            int maxDiasMes = calTemp.getActualMaximum(Calendar.DAY_OF_MONTH);

            Calendar hoy = new GregorianCalendar();

            for (int i = 0; i < 42; i++) {
                JButton btn = botonesDias[i];
                int diaNumero = i - primerDiaSemana + 1;

                if (i < primerDiaSemana || diaNumero > maxDiasMes) {
                    btn.setText("");
                    btn.setEnabled(false);
                    btn.setBackground(new Color(240, 240, 240));
                } else {
                    btn.setText(String.valueOf(diaNumero));
                    btn.setEnabled(true);

                    boolean esHoy = (hoy.get(Calendar.YEAR) == calTemp.get(Calendar.YEAR)) &&
                                    (hoy.get(Calendar.MONTH) == calTemp.get(Calendar.MONTH)) &&
                                    (hoy.get(Calendar.DAY_OF_MONTH) == diaNumero);

                    if (esHoy) {
                        btn.setBackground(COLOR_SECUNDARIO);
                        btn.setForeground(Color.WHITE);
                        btn.setFont(FUENTE_TEXTO_BOLD);
                    } else {
                        btn.setBackground(Color.WHITE);
                        btn.setForeground(COLOR_OSCURO);
                        btn.setFont(FUENTE_TEXTO);
                    }
                }
            }
        }

        public boolean isConfirmado() { return confirmado; }
        public Date getFechaSeleccionada() { return fechaSeleccionada; }
        public LocalDate getInicioRango() { return inicioRango; }
        public LocalDate getFinRango() { return finRango; }
        public boolean isModoRango() { return modoRango; }
    }
}