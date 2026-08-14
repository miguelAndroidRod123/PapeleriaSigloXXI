package sigloxxi.ventaspapeleria.modelo;

public class Producto {
    private String codigo;
    private String nombre;
    private double precio;
    private double costoMayorista;
    private int cantidadDisponible;
    private String ubicacion;

    public Producto(String codigo, String nombre, double precio, double costoMayorista, int cantidadDisponible, String ubicacion) {
        this.codigo = codigo != null ? codigo : "";
        this.nombre = nombre != null ? nombre : "";
        this.precio = precio;
        this.costoMayorista = costoMayorista;
        this.cantidadDisponible = cantidadDisponible;
        this.ubicacion = ubicacion != null ? ubicacion : "";
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