package sigloxxi.ventaspapeleria.modelo;

public class Producto {
    private String codigo;
    private String nombre;
    private double precio;
    private double costoMayorista;
    private int cantidadDisponible;
    private String ubicacion;

    // Rango de monto opcional (columnas "Monto Mínimo" / "Monto Máximo" del
    // Excel). Se usa en productos-franja como "PAGOS DE FACTURAS 200" o
    // "RETIROS / RECARGAS NEQUI +100": cada franja es una fila de producto
    // normal, con su propio Precio Unitario (la comisión de esa franja), y
    // estos dos valores delimitan a qué monto de transacción aplica.
    private boolean tieneRangoConfigurado = false;
    private double montoMinimoRango = 0;
    private double montoMaximoRango = 0;

    public Producto(String codigo, String nombre, double precio, double costoMayorista, int cantidadDisponible, String ubicacion) {
        this.codigo = codigo != null ? codigo : "";
        this.nombre = nombre != null ? nombre : "";
        this.precio = precio;
        this.costoMayorista = costoMayorista;
        this.cantidadDisponible = cantidadDisponible;
        this.ubicacion = ubicacion != null ? ubicacion : "";
    }

    /** Marca este producto como una franja de un servicio por rango (ver Monto Mínimo/Máximo del Excel). */
    public void configurarRango(double montoMinimo, double montoMaximo) {
        this.montoMinimoRango = montoMinimo;
        this.montoMaximoRango = montoMaximo;
        this.tieneRangoConfigurado = true;
    }

    public boolean tieneRangoConfigurado() { return tieneRangoConfigurado; }
    public double getMontoMinimoRango() { return montoMinimoRango; }
    public double getMontoMaximoRango() { return montoMaximoRango; }

    /** True si este producto es la franja correcta para cobrar un monto/transacción dado. */
    public boolean aplicaParaMonto(double monto) {
        return tieneRangoConfigurado && monto >= montoMinimoRango && monto <= montoMaximoRango;
    }

    public boolean esServicioONoAplica() {
        return cantidadDisponible == -1;
    }

    public boolean tieneStockDisponible(int cantidad) {
        return esServicioONoAplica() || cantidad > 0 && cantidadDisponible >= cantidad;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo != null ? codigo.trim() : ""; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre != null ? nombre.trim() : ""; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public double getCostoMayorista() { return costoMayorista; }
    public void setCostoMayorista(double costoMayorista) { this.costoMayorista = costoMayorista; }

    public int getCantidadDisponible() { return cantidadDisponible; }
    public void setCantidadDisponible(int cantidadDisponible) { this.cantidadDisponible = cantidadDisponible; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion != null ? ubicacion.trim() : ""; }

    @Override
    public String toString() {
        String stockStr = esServicioONoAplica() ? "N/A" : String.valueOf(cantidadDisponible);
        return String.format("[%s] %s - Precio Venta: $%.2f (Stock: %s)", codigo, nombre, precio, stockStr);
    }
}