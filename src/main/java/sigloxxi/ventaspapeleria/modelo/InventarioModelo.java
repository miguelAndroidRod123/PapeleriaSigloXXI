package sigloxxi.ventaspapeleria.modelo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.Component;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class InventarioModelo {

    private boolean modoPruebas = false;
    private final DataFormatter dataFormatter = new DataFormatter();

    /** Constructor conservado para compatibilidad. Mantiene el modo de pruebas activado por defecto. */
    public InventarioModelo() {
        this(true);
    }

    /** Permite controlar explícitamente el entorno desde el Main. */
    public InventarioModelo(boolean modoPruebas) {
        this.modoPruebas = modoPruebas;
    }

    public boolean isModoPruebas() {
        return modoPruebas;
    }

    public void setModoPruebas(boolean modoPruebas) {
        this.modoPruebas = modoPruebas;
    }

    public File obtenerArchivoExcel(Component parent) {
        if (modoPruebas) {
            File archivoPruebas = new File("INVENTARIO_PRUEBAS.xlsx");
            if (!archivoPruebas.exists()) {
                File original = new File("INVENTARIO PAPELERIA SIGLO XXI.xlsx");
                if (!original.exists()) original = new File("inventario.xlsx");

                if (original.exists()) {
                    try {
                        Files.copy(original.toPath(), archivoPruebas.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        System.err.println("No se pudo duplicar el Excel para pruebas: " + e.getMessage());
                    }
                }
            }
            if (!archivoPruebas.exists()) {
                JOptionPane.showMessageDialog(parent,
                        "No se encontró el archivo de inventario para modo pruebas.",
                        "Inventario no disponible",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return archivoPruebas;
        }

        Preferences prefs = Preferences.userNodeForPackage(InventarioModelo.class);
        String rutaGuardada = prefs.get("RUTA_EXCEL_INVENTARIO", null);
        File archivoExcel = null;

        // =====================================================================
        // DETECCIÓN AUTOMÁTICA DE LA BASE DE DATOS DE INVENTARIO
        // ---------------------------------------------------------------------
        // 1. Ruta guardada anteriormente.
        // 2. Ubicaciones conocidas del proyecto/aplicación.
        // 3. Documentos / Escritorio / OneDrive.
        // 4. Búsqueda controlada por nombre.
        // 5. Si no se encuentra, se conserva la selección manual original.
        // =====================================================================

        if (rutaGuardada != null && !rutaGuardada.isBlank()) {
            archivoExcel = new File(rutaGuardada);

            if (archivoExcel.exists() && archivoExcel.isFile()) {
                return archivoExcel;
            }

            // La ruta guardada dejó de ser válida.
            prefs.remove("RUTA_EXCEL_INVENTARIO");
        }

        archivoExcel = buscarInventarioAutomaticamente();

        if (archivoExcel != null && archivoExcel.exists()) {
            prefs.put("RUTA_EXCEL_INVENTARIO", archivoExcel.getAbsolutePath());

            System.out.println(
                    "Inventario Excel detectado automáticamente en: "
                    + archivoExcel.getAbsolutePath()
            );

            return archivoExcel;
        }

        // ---------------------------------------------------------------------
        // Si no fue posible localizarlo automáticamente, se conserva el
        // comportamiento original: selección manual.
        // ---------------------------------------------------------------------
        while (archivoExcel == null || !archivoExcel.exists()) {
            int opcion = JOptionPane.showConfirmDialog(parent, """
                                                            ⚠️ No se encontró la Base de Datos de Inventario (Excel).

                                                            El archivo se ha borrado, movido o es la primera vez que inicia el sistema.
                                                            ¿Desea ubicar el archivo manualmente en su equipo?""",
                    "Base de Datos No Encontrada",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (opcion == JOptionPane.YES_OPTION) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Seleccionar Base de Datos de Inventario (.xlsx)");
                fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de Excel (*.xlsx)", "xlsx"));

                int seleccion = fileChooser.showOpenDialog(parent);
                if (seleccion == JFileChooser.APPROVE_OPTION) {
                    archivoExcel = fileChooser.getSelectedFile();

                    if (archivoExcel.exists() && archivoExcel.isFile()) {
                        prefs.put("RUTA_EXCEL_INVENTARIO", archivoExcel.getAbsolutePath());

                        JOptionPane.showMessageDialog(parent,
                                "¡Ubicación actualizada con éxito!\n" + archivoExcel.getAbsolutePath(),
                                "Conexión Exitosa",
                                JOptionPane.INFORMATION_MESSAGE);

                        return archivoExcel;
                    }
                } else {
                    // Si el usuario cancela la búsqueda en la ventana de archivos, salimos para evitar bucle infinito
                    return null;
                }
            } else {
                JOptionPane.showMessageDialog(parent,
                        "No se puede operar el sistema sin vincular el archivo de Excel.",
                        "Operación Cancelada",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }

        return archivoExcel;
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PARA DETECCIÓN AUTOMÁTICA DEL INVENTARIO
    // =========================================================================

    /**
     * Busca automáticamente el archivo Excel del inventario en ubicaciones
     * probables sin obligar al usuario a seleccionarlo manualmente.
     */
    private File buscarInventarioAutomaticamente() {
        List<File> ubicacionesPrioritarias = new ArrayList<>();

        // 1) Directorio desde el cual se lanzó Java.
        agregarUbicacionSiExiste(
                ubicacionesPrioritarias,
                new File(System.getProperty("user.dir"))
        );

        // 2) Carpeta donde está el JAR / clases de la aplicación.
        File ubicacionAplicacion = obtenerUbicacionAplicacion();
        if (ubicacionAplicacion != null) {
            agregarUbicacionSiExiste(ubicacionesPrioritarias, ubicacionAplicacion);
            agregarUbicacionSiExiste(ubicacionesPrioritarias, ubicacionAplicacion.getParentFile());

            if (ubicacionAplicacion.getParentFile() != null) {
                agregarUbicacionSiExiste(
                        ubicacionesPrioritarias,
                        ubicacionAplicacion.getParentFile().getParentFile()
                );
            }
        }

        // 3) Proyecto antiguo y proyecto actual.
        agregarUbicacionSiExiste(
                ubicacionesPrioritarias,
                new File(System.getProperty("user.home"),
                        "Documents/NetBeansProjects/ventasPapeleria")
        );

        agregarUbicacionSiExiste(
                ubicacionesPrioritarias,
                new File(System.getProperty("user.home"),
                        "Documents/NetBeansProjects/PapeleriaSigloXXI")
        );

        // 4) Carpetas habituales del usuario.
        File carpetaUsuario = FileSystemView.getFileSystemView().getHomeDirectory();

        agregarUbicacionSiExiste(ubicacionesPrioritarias, carpetaUsuario);

        agregarUbicacionSiExiste(
                ubicacionesPrioritarias,
                new File(System.getProperty("user.home"), "Documents")
        );

        agregarUbicacionSiExiste(
                ubicacionesPrioritarias,
                new File(System.getProperty("user.home"), "OneDrive/Documents")
        );

        agregarUbicacionSiExiste(
                ubicacionesPrioritarias,
                new File(System.getProperty("user.home"), "Desktop")
        );

        // 5) Primero probar nombres exactos.
        String[] nombresExactos = {
            "INVENTARIO PAPELERIA SIGLO XXI.xlsx",
            "INVENTARIO PAPELERÍA SIGLO XXI.xlsx",
            "INVENTARIO.xlsx",
            "inventario.xlsx",
            "Inventario.xlsx"
        };

        for (File carpeta : ubicacionesPrioritarias) {
            File encontrado = buscarPorNombresExactos(carpeta, nombresExactos);

            if (esExcelValido(encontrado)) {
                return encontrado;
            }
        }

        // 6) Como respaldo, búsqueda controlada por nombre.
        for (File carpeta : ubicacionesPrioritarias) {
            File encontrado = buscarPorNombreDeInventario(carpeta, 3);

            if (esExcelValido(encontrado)) {
                return encontrado;
            }
        }

        return null;
    }

    /**
     * Obtiene la carpeta real donde está el JAR/clase de la aplicación.
     * Funciona desde NetBeans y desde un JAR ejecutable.
     */
    private File obtenerUbicacionAplicacion() {
        try {
            java.net.URI ubicacion = InventarioModelo.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();

            File archivo = new File(ubicacion);

            if (archivo.isFile()) {
                return archivo.getParentFile();
            }

            return archivo;

        } catch (Exception e) {
            System.err.println(
                    "No fue posible determinar la ubicación de la aplicación: "
                    + e.getMessage()
            );
            return null;
        }
    }

    /**
     * Agrega una carpeta a la lista solo si existe.
     * También evita duplicados.
     */
    private void agregarUbicacionSiExiste(List<File> ubicaciones, File carpeta) {
        if (carpeta == null || !carpeta.exists() || !carpeta.isDirectory()) {
            return;
        }

        try {
            File absoluta = carpeta.getCanonicalFile();

            for (File existente : ubicaciones) {
                if (existente.getCanonicalFile().equals(absoluta)) {
                    return;
                }
            }

            ubicaciones.add(absoluta);

        } catch (IOException e) {
            if (!ubicaciones.contains(carpeta)) {
                ubicaciones.add(carpeta);
            }
        }
    }

    /**
     * Busca nombres conocidos en la carpeta y en el primer nivel de subcarpetas.
     */
    private File buscarPorNombresExactos(File carpeta, String[] nombres) {
        if (carpeta == null || !carpeta.exists() || !carpeta.isDirectory()) {
            return null;
        }

        for (String nombre : nombres) {
            File archivo = new File(carpeta, nombre);

            if (esExcelValido(archivo)) {
                return archivo;
            }
        }

        File[] subcarpetas = carpeta.listFiles(File::isDirectory);

        if (subcarpetas != null) {
            for (File subcarpeta : subcarpetas) {
                for (String nombre : nombres) {
                    File archivo = new File(subcarpeta, nombre);

                    if (esExcelValido(archivo)) {
                        return archivo;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Búsqueda recursiva limitada para evitar recorrer todo el disco.
     */
    private File buscarPorNombreDeInventario(File carpeta, int profundidadMaxima) {
        if (carpeta == null
                || !carpeta.exists()
                || !carpeta.isDirectory()
                || profundidadMaxima < 0) {
            return null;
        }

        File[] elementos = carpeta.listFiles();

        if (elementos == null) {
            return null;
        }

        // Primero archivos del nivel actual.
        for (File elemento : elementos) {
            if (elemento.isFile() && esNombreDeInventario(elemento.getName())) {
                if (esExcelValido(elemento)) {
                    return elemento;
                }
            }
        }

        if (profundidadMaxima == 0) {
            return null;
        }

        // Luego subcarpetas.
        for (File elemento : elementos) {
            if (!elemento.isDirectory()) {
                continue;
            }

            String nombreCarpeta =
                    elemento.getName().toLowerCase(java.util.Locale.ROOT);

            // Evitar carpetas generadas o innecesarias.
            if (nombreCarpeta.equals("target")
                    || nombreCarpeta.equals("node_modules")
                    || nombreCarpeta.equals(".git")
                    || nombreCarpeta.equals("$recycle.bin")
                    || nombreCarpeta.equals("appdata")) {
                continue;
            }

            File encontrado = buscarPorNombreDeInventario(
                    elemento,
                    profundidadMaxima - 1
            );

            if (esExcelValido(encontrado)) {
                return encontrado;
            }
        }

        return null;
    }

    /**
     * Determina si el nombre corresponde a un posible archivo de inventario.
     */
    private boolean esNombreDeInventario(String nombreArchivo) {
        if (nombreArchivo == null) {
            return false;
        }

        String nombre =
                nombreArchivo.toLowerCase(java.util.Locale.ROOT);

        if (!nombre.endsWith(".xlsx")) {
            return false;
        }

        return nombre.contains("inventario")
                || (nombre.contains("papeleria") && nombre.contains("siglo"));
    }

    /**
     * Validación final del archivo encontrado.
     */
    private boolean esExcelValido(File archivo) {
        return archivo != null
                && archivo.exists()
                && archivo.isFile()
                && archivo.getName()
                        .toLowerCase(java.util.Locale.ROOT)
                        .endsWith(".xlsx");
    }

    /**
     * Permite abrir directamente el buscador de archivos para vincular una nueva base de datos,
     * desactivando el modo de pruebas y guardando la nueva ruta seleccionada.
     */
    public boolean seleccionarNuevoArchivoExcel(Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar Base de Datos de Inventario (.xlsx)");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos de Excel (*.xlsx)", "xlsx"));

        int seleccion = fileChooser.showOpenDialog(parent);
        if (seleccion == JFileChooser.APPROVE_OPTION) {
            File nuevoArchivo = fileChooser.getSelectedFile();

            if (nuevoArchivo.exists()) {
                Preferences prefs = Preferences.userNodeForPackage(InventarioModelo.class);
                prefs.put("RUTA_EXCEL_INVENTARIO", nuevoArchivo.getAbsolutePath());
                this.modoPruebas = false; // Desactiva modo pruebas para usar la base de datos real seleccionada

                JOptionPane.showMessageDialog(parent,
                        "¡Ubicación actualizada con éxito!\n" + nuevoArchivo.getAbsolutePath(),
                        "Conexión Exitosa",
                        JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
        }
        return false;
    }

    public List<Producto> buscarEnExcel(String criterio, Component parent) {
        List<Producto> resultados = new ArrayList<>();
        if (criterio == null || criterio.isBlank()) {
            return resultados;
        }

        File archivoExcel = obtenerArchivoExcel(parent);
        if (archivoExcel == null) return resultados;

        try (FileInputStream fis = new FileInputStream(archivoExcel); Workbook workbook = new XSSFWorkbook(fis)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            int totalHojas = workbook.getNumberOfSheets();
            String criterioLimpio = criterio.trim().toLowerCase(java.util.Locale.ROOT);

            for (int h = 0; h < totalHojas; h++) {
                Sheet sheet = workbook.getSheetAt(h);
                for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null || i == 0) continue;

                    Cell celdaCodigo = row.getCell(0);
                    Cell celdaNombre = row.getCell(2);
                    Cell celdaCostoMayorista = row.getCell(3);
                    Cell celdaPrecio = row.getCell(4);
                    Cell celdaCantidad = row.getCell(5);
                    Cell celdaUbicacion = row.getCell(6);

                    String codigoVal = obtenerValorCelda(celdaCodigo, evaluator).trim();
                    String nombreVal = obtenerValorCelda(celdaNombre, evaluator).trim();

                    if (!codigoVal.isEmpty() || !nombreVal.isEmpty()) {
                        if (codigoVal.equalsIgnoreCase(criterioLimpio) || nombreVal.toLowerCase().contains(criterioLimpio)) {
                            double precioVal = obtenerPrecioCelda(celdaPrecio, evaluator);
                            double costoMayoristaVal = obtenerPrecioCelda(celdaCostoMayorista, evaluator);
                            int cantidadVal = obtenerCantidadCelda(celdaCantidad, evaluator);
                            String ubicacionVal = obtenerValorCelda(celdaUbicacion, evaluator).trim();

                            resultados.add(new Producto(codigoVal, nombreVal, precioVal, costoMayoristaVal, cantidadVal, ubicacionVal));
                        }
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "Error al leer el archivo Excel: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return resultados;
    }

    public void descontarStockExcel(List<Object[]> itemsVendidos, Component parent) {
        File archivoExcel = obtenerArchivoExcel(parent);
        if (archivoExcel == null || !archivoExcel.exists()) {
            JOptionPane.showMessageDialog(parent, "No se encontró el archivo de Excel para descontar inventario.", "Error de Inventario", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (FileInputStream fis = new FileInputStream(archivoExcel);
             Workbook workbook = new XSSFWorkbook(fis)) {

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            boolean cambiosRealizados = false;

            for (Object[] item : itemsVendidos) {
                String codigoTabla = item[0].toString().trim();
                int cantidadVendida = Integer.parseInt(item[2].toString());

                for (int h = 0; h < workbook.getNumberOfSheets(); h++) {
                    Sheet sheet = workbook.getSheetAt(h);
                    for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;

                        Cell celdaCodigo = row.getCell(0);
                        String codVal = obtenerValorCelda(celdaCodigo, evaluator).trim();

                        if (codVal.equalsIgnoreCase(codigoTabla)) {
                            Cell celdaCantidad = row.getCell(5);

                            if (celdaCantidad != null) {
                                String valorCantidadStr = obtenerValorCelda(celdaCantidad, evaluator).trim();
                                if (valorCantidadStr.equalsIgnoreCase("N/A") || valorCantidadStr.equalsIgnoreCase("NA")) {
                                    break;
                                }
                            }

                            if (celdaCantidad == null) celdaCantidad = row.createCell(5);

                            int stockActual = obtenerCantidadCelda(celdaCantidad, evaluator);
                            if (stockActual >= 0) {
                                int nuevoStock = Math.max(0, stockActual - cantidadVendida);
                                celdaCantidad.setCellValue(nuevoStock);
                                cambiosRealizados = true;
                            }
                            break;
                        }
                    }
                }
            }

            if (cambiosRealizados) {
                try (FileOutputStream fos = new FileOutputStream(archivoExcel)) {
                    workbook.write(fos);
                }
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Error al actualizar el stock en Excel:\n" + ex.getMessage(), "Error E/S Excel", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Analiza reportes TXT de una carpeta y suma el total bruto de ventas encontrado.
     * Se conserva el método para compatibilidad con la versión monolítica.
     */
    public double analizarReporte(String rutaCarpeta) {
        if (rutaCarpeta == null || rutaCarpeta.isBlank()) {
            return 0.0;
        }

        File carpeta = new File(rutaCarpeta);
        if (!carpeta.exists() || !carpeta.isDirectory()) {
            return 0.0;
        }

        double total = 0.0;
        File[] archivos = carpeta.listFiles((dir, name) ->
                name.startsWith("Reporte_Ventas_") && name.endsWith(".txt"));

        if (archivos != null) {
            for (File archivo : archivos) {
                total += extraerTotalVentaDeArchivo(archivo);
            }
        }
        return total;
    }

    public double calcularVentasSemanalesParaPago() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1);
        return calcularVentasPorRangoFechas(inicioSemana, hoy, 0.0);
    }

    public double calcularVentasPorRangoFechas(LocalDate inicio, LocalDate fin, double ventasHoyEnMemoria) {
        double totalVentasRango = 0.0;
        try {
            File escritorio = FileSystemView.getFileSystemView().getHomeDirectory();
            File carpetaExportar = new File(escritorio, "DOCUMENTOS SIGLO XXI/REPORTES/EXPORTAR VENTA DEL DIA");

            LocalDate hoy = LocalDate.now();
            boolean hoyIncluido = (!hoy.isBefore(inicio) && !hoy.isAfter(fin));
            boolean reporteHoyLeido = false;

            if (carpetaExportar.exists() && carpetaExportar.isDirectory()) {
                File[] archivos = carpetaExportar.listFiles((dir, name) -> name.startsWith("Reporte_Ventas_") && name.endsWith(".txt"));
                if (archivos != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    for (File archivo : archivos) {
                        try {
                            String nombre = archivo.getName();
                            String fechaStr = nombre.replace("Reporte_Ventas_", "").replace(".txt", "");
                            LocalDate fechaArchivo = LocalDate.parse(fechaStr, formatter);

                            if (!fechaArchivo.isBefore(inicio) && !fechaArchivo.isAfter(fin)) {
                                double ventaArchivo = extraerTotalVentaDeArchivo(archivo);
                                totalVentasRango += ventaArchivo;
                                if (fechaArchivo.equals(hoy)) {
                                    reporteHoyLeido = true;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (hoyIncluido && !reporteHoyLeido && ventasHoyEnMemoria > 0) {
                totalVentasRango += ventasHoyEnMemoria;
            }

            if (modoPruebas && totalVentasRango == 0.0) {
                return 500000.00;
            }

        } catch (Exception e) {
            System.err.println("Error al calcular ventas por rango de fechas: " + e.getMessage());
        }
        return totalVentasRango;
    }

    private double extraerTotalVentaDeArchivo(File archivo) {
        double total = 0.0;
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.contains("TOTAL INGRESO BRUTO DE VENTA:")) {
                    String sub = linea.substring(linea.indexOf("$") + 1).trim();
                    // Limpieza resistente a separadores de miles y decimales
                    sub = sub.replaceAll("[^0-9,. ]", "").trim();
                    if (sub.contains(",") && sub.contains(".")) {
                        if (sub.lastIndexOf(",") > sub.lastIndexOf(".")) {
                            sub = sub.replace(".", "").replace(",", ".");
                        } else {
                            sub = sub.replace(",", "");
                        }
                    } else if (sub.contains(",")) {
                        sub = sub.replace(",", ".");
                    }
                    total = Double.parseDouble(sub);
                    break;
                }
            }
        } catch (Exception ignored) {}
        return total;
    }

    private double obtenerPrecioCelda(Cell celdaPrecio, FormulaEvaluator evaluator) {
        try {
            if (celdaPrecio != null) {
                if (celdaPrecio.getCellType() == CellType.NUMERIC) {
                    return celdaPrecio.getNumericCellValue();
                } else {
                    String precioStr = obtenerValorCelda(celdaPrecio, evaluator).trim();
                    if (!precioStr.isEmpty()) {
                        precioStr = precioStr.replace("$", "").replace(" ", "");
                        if (precioStr.contains(",") && precioStr.contains(".")) {
                            if (precioStr.lastIndexOf(',') > precioStr.lastIndexOf('.')) {
                                precioStr = precioStr.replace(".", "").replace(",", ".");
                            } else {
                                precioStr = precioStr.replace(",", "");
                            }
                        } else {
                            precioStr = precioStr.replace(",", ".");
                        }
                        return Double.parseDouble(precioStr);
                    }
                }
            }
        } catch (NumberFormatException e) { return 0.0; }
        return 0.0;
    }

    private int obtenerCantidadCelda(Cell celdaCantidad, FormulaEvaluator evaluator) {
        try {
            if (celdaCantidad != null) {
                if (celdaCantidad.getCellType() == CellType.NUMERIC) {
                    return (int) celdaCantidad.getNumericCellValue();
                } else {
                    String cantidadStr = obtenerValorCelda(celdaCantidad, evaluator).trim();
                    if (cantidadStr.equalsIgnoreCase("N/A") || cantidadStr.equalsIgnoreCase("NA")) {
                        return -1;
                    }
                    if (!cantidadStr.isEmpty()) {
                        return (int) Double.parseDouble(cantidadStr.replace(",", "."));
                    }
                }
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        return 0;
    }

    private String obtenerValorCelda(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        try {
            return dataFormatter.formatCellValue(cell, evaluator);
        } catch (Exception e) {
            return dataFormatter.formatCellValue(cell);
        }
    }
}