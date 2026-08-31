package sigloxxi.ventaspapeleria.modelo;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reloj checador: guarda entrada, inicio/fin de almuerzo y salida por
 * empleado y por día, y calcula las horas realmente trabajadas (descontando
 * el almuerzo) para un rango de fechas. Se usa desde el Centro de Pagos para
 * que el sueldo por hora se calcule sobre tiempo real trabajado, no sobre un
 * conteo manual de días.
 *
 * Persistencia: un archivo de texto simple en
 * "DOCUMENTOS SIGLO XXI/ASISTENCIA/registro_asistencia.csv", separado por
 * punto y coma. Se reescribe completo cada vez que cambia (el volumen de
 * datos de una papelería es pequeño, esto es más que suficiente).
 */
public class AsistenciaModelo {

    private static final String NOMBRE_ARCHIVO = "registro_asistencia.csv";
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String VACIO = "--";

    /** Un día de asistencia de un empleado. */
    public static class RegistroAsistencia {
        public String empleado;
        public LocalDate fecha;
        public LocalTime horaEntrada;
        public LocalTime horaInicioAlmuerzo;
        public LocalTime horaFinAlmuerzo;
        public LocalTime horaSalida;

        /**
         * Horas trabajadas hasta ahora, descontando el almuerzo. Funciona en
         * vivo: si aún no se ha marcado Salida, calcula hasta el momento
         * actual (útil para ver el avance del día sin esperar a la salida).
         * El tiempo de almuerzo NUNCA se contabiliza como hora laboral
         * (así lo exige la ley), aunque sí se sigue mostrando por separado.
         */
        public double calcularHoras() {
            if (horaEntrada == null) {
                return 0.0;
            }
            LocalTime finEfectivo = (horaSalida != null) ? horaSalida : LocalTime.now();
            double minutosTotales = java.time.Duration.between(horaEntrada, finEfectivo).toMinutes();
            minutosTotales -= calcularMinutosAlmuerzo();
            return Math.max(0.0, minutosTotales / 60.0);
        }

        /**
         * Minutos de almuerzo transcurridos (informativo, NO se suman a las
         * horas trabajadas). Si el almuerzo ya terminó, es la duración fija
         * marcada; si sigue en curso, avanza en vivo hasta ahora.
         */
        public double calcularMinutosAlmuerzo() {
            if (horaInicioAlmuerzo == null) {
                return 0.0;
            }
            LocalTime finAlmuerzoEfectivo = (horaFinAlmuerzo != null) ? horaFinAlmuerzo : LocalTime.now();
            return Math.max(0.0, java.time.Duration.between(horaInicioAlmuerzo, finAlmuerzoEfectivo).toMinutes());
        }
    }

    private File obtenerArchivo() {
        File escritorio = FileSystemView.getFileSystemView().getHomeDirectory();
        File carpeta = new File(new File(escritorio, "DOCUMENTOS SIGLO XXI"), "ASISTENCIA");
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        return new File(carpeta, NOMBRE_ARCHIVO);
    }

    private List<RegistroAsistencia> leerTodos() {
        List<RegistroAsistencia> registros = new ArrayList<>();
        File archivo = obtenerArchivo();
        if (!archivo.exists()) {
            return registros;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) continue;
                String[] campos = linea.split(";", -1);
                if (campos.length != 6) continue;

                RegistroAsistencia r = new RegistroAsistencia();
                r.empleado = campos[0];
                r.fecha = LocalDate.parse(campos[1], FMT_FECHA);
                r.horaEntrada = VACIO.equals(campos[2]) ? null : LocalTime.parse(campos[2], FMT_HORA);
                r.horaInicioAlmuerzo = VACIO.equals(campos[3]) ? null : LocalTime.parse(campos[3], FMT_HORA);
                r.horaFinAlmuerzo = VACIO.equals(campos[4]) ? null : LocalTime.parse(campos[4], FMT_HORA);
                r.horaSalida = VACIO.equals(campos[5]) ? null : LocalTime.parse(campos[5], FMT_HORA);
                registros.add(r);
            }
        } catch (Exception e) {
            System.err.println("No se pudo leer el registro de asistencia: " + e.getMessage());
        }
        return registros;
    }

    private void guardarTodos(List<RegistroAsistencia> registros) {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(obtenerArchivo()), StandardCharsets.UTF_8))) {
            for (RegistroAsistencia r : registros) {
                writer.println(String.join(";",
                        r.empleado,
                        r.fecha.format(FMT_FECHA),
                        r.horaEntrada == null ? VACIO : r.horaEntrada.format(FMT_HORA),
                        r.horaInicioAlmuerzo == null ? VACIO : r.horaInicioAlmuerzo.format(FMT_HORA),
                        r.horaFinAlmuerzo == null ? VACIO : r.horaFinAlmuerzo.format(FMT_HORA),
                        r.horaSalida == null ? VACIO : r.horaSalida.format(FMT_HORA)
                ));
            }
        } catch (IOException e) {
            System.err.println("No se pudo guardar el registro de asistencia: " + e.getMessage());
        }
    }

    /** Devuelve el registro de hoy del empleado, o uno vacío si aún no ha marcado nada. */
    public RegistroAsistencia obtenerEstadoHoy(String empleado) {
        LocalDate hoy = LocalDate.now();
        for (RegistroAsistencia r : leerTodos()) {
            if (r.empleado.equalsIgnoreCase(empleado) && r.fecha.equals(hoy)) {
                return r;
            }
        }
        RegistroAsistencia vacio = new RegistroAsistencia();
        vacio.empleado = empleado;
        vacio.fecha = hoy;
        return vacio;
    }

    private enum Resultado { OK, YA_MARCADO, FUERA_DE_ORDEN }

    public String marcarEntrada(String empleado) {
        List<RegistroAsistencia> registros = leerTodos();
        LocalDate hoy = LocalDate.now();
        for (RegistroAsistencia r : registros) {
            if (r.empleado.equalsIgnoreCase(empleado) && r.fecha.equals(hoy)) {
                if (r.horaEntrada != null) {
                    return "Ya se marcó la entrada de hoy a las " + r.horaEntrada.format(FMT_HORA) + ".";
                }
            }
        }
        RegistroAsistencia r = new RegistroAsistencia();
        r.empleado = empleado;
        r.fecha = hoy;
        r.horaEntrada = LocalTime.now().withNano(0);
        registros.add(r);
        guardarTodos(registros);
        return null; // sin error
    }

    public String marcarInicioAlmuerzo(String empleado) {
        List<RegistroAsistencia> registros = leerTodos();
        LocalDate hoy = LocalDate.now();
        for (RegistroAsistencia r : registros) {
            if (r.empleado.equalsIgnoreCase(empleado) && r.fecha.equals(hoy)) {
                if (r.horaEntrada == null) {
                    return "Primero debe marcar la Entrada de hoy.";
                }
                if (r.horaSalida != null) {
                    return "Ya se marcó la Salida de hoy, no se puede iniciar almuerzo.";
                }
                if (r.horaInicioAlmuerzo != null) {
                    return "Ya se marcó el inicio del almuerzo a las " + r.horaInicioAlmuerzo.format(FMT_HORA) + ".";
                }
                r.horaInicioAlmuerzo = LocalTime.now().withNano(0);
                guardarTodos(registros);
                return null;
            }
        }
        return "Primero debe marcar la Entrada de hoy.";
    }

    public String marcarFinAlmuerzo(String empleado) {
        List<RegistroAsistencia> registros = leerTodos();
        LocalDate hoy = LocalDate.now();
        for (RegistroAsistencia r : registros) {
            if (r.empleado.equalsIgnoreCase(empleado) && r.fecha.equals(hoy)) {
                if (r.horaInicioAlmuerzo == null) {
                    return "Primero debe marcar el Inicio de Almuerzo.";
                }
                if (r.horaFinAlmuerzo != null) {
                    return "Ya se marcó el fin del almuerzo a las " + r.horaFinAlmuerzo.format(FMT_HORA) + ".";
                }
                r.horaFinAlmuerzo = LocalTime.now().withNano(0);
                guardarTodos(registros);
                return null;
            }
        }
        return "Primero debe marcar la Entrada de hoy.";
    }

    public String marcarSalida(String empleado) {
        List<RegistroAsistencia> registros = leerTodos();
        LocalDate hoy = LocalDate.now();
        for (RegistroAsistencia r : registros) {
            if (r.empleado.equalsIgnoreCase(empleado) && r.fecha.equals(hoy)) {
                if (r.horaEntrada == null) {
                    return "Primero debe marcar la Entrada de hoy.";
                }
                if (r.horaSalida != null) {
                    return "Ya se marcó la Salida de hoy a las " + r.horaSalida.format(FMT_HORA) + ".";
                }
                if (r.horaInicioAlmuerzo != null && r.horaFinAlmuerzo == null) {
                    return "Marcó Inicio de Almuerzo pero no lo ha terminado. Marque 'Terminar Almuerzo' antes de salir.";
                }
                r.horaSalida = LocalTime.now().withNano(0);
                guardarTodos(registros);
                return null;
            }
        }
        return "Primero debe marcar la Entrada de hoy.";
    }

    /** Suma de horas trabajadas (ya descontando almuerzo) por un empleado en un rango de fechas, ambos inclusive. */
    public double calcularHorasTrabajadas(String empleado, LocalDate inicio, LocalDate fin) {
        double total = 0.0;
        for (RegistroAsistencia r : leerTodos()) {
            if (r.empleado.equalsIgnoreCase(empleado)
                    && !r.fecha.isBefore(inicio) && !r.fecha.isAfter(fin)) {
                total += r.calcularHoras();
            }
        }
        return total;
    }

    /** Cuántos días con entrada y salida completas tiene el empleado en el rango (útil como referencia). */
    public int contarDiasTrabajados(String empleado, LocalDate inicio, LocalDate fin) {
        int dias = 0;
        for (RegistroAsistencia r : leerTodos()) {
            if (r.empleado.equalsIgnoreCase(empleado)
                    && !r.fecha.isBefore(inicio) && !r.fecha.isAfter(fin)
                    && r.horaEntrada != null && r.horaSalida != null) {
                dias++;
            }
        }
        return dias;
    }
}