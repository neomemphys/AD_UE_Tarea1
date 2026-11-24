package es.northwind;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ImportadorProductos {
    private static final String URL = "https://dummyjson.com/products";
    private static final int TAMANO_PAGINA = 100;

    public record Producto(int id, String nombre, String descripcion, int cantidad, BigDecimal precio) {
    }

    private ImportadorProductos() {
    }

    public static List<Producto> descargar() throws IOException, InterruptedException {
        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        ObjectMapper mapper = new ObjectMapper();
        List<Producto> productos = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        int salto = 0;
        int total = -1;

        do {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(URL + "?limit=" + TAMANO_PAGINA + "&skip=" + salto))
                    .timeout(Duration.ofSeconds(60))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> respuesta = cliente.send(peticion,
                    HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                throw new IOException("Error HTTPS al descargar productos: " + respuesta.statusCode());
            }
            JsonNode raiz = mapper.readTree(respuesta.body());
            JsonNode pagina = raiz.path("products");
            if (!raiz.path("total").canConvertToInt() || !pagina.isArray()) {
                throw new IOException("El JSON no contiene products y total válidos.");
            }
            int totalPagina = raiz.path("total").intValue();
            if (totalPagina < 0 || (total != -1 && total != totalPagina)) {
                throw new IOException("El catálogo ha cambiado durante la descarga; vuelva a ejecutar.");
            }
            total = totalPagina;
            if (pagina.isEmpty() && salto < total) {
                throw new IOException("La API devolvió una página vacía antes de completar el catálogo.");
            }
            for (JsonNode nodo : pagina) {
                if (!nodo.path("id").isIntegralNumber() || !nodo.path("id").canConvertToInt()
                        || !nodo.path("title").isTextual() || !nodo.path("description").isTextual()
                        || !nodo.path("stock").isIntegralNumber() || !nodo.path("stock").canConvertToInt()
                        || !nodo.path("price").isNumber()) {
                    throw new IOException("Producto con campos ausentes o tipos incorrectos.");
                }
                int id = nodo.path("id").intValue();
                int cantidad = nodo.path("stock").intValue();
                BigDecimal precio = nodo.path("price").decimalValue();
                if (id <= 0 || cantidad < 0 || precio.signum() < 0 || !ids.add(id)) {
                    throw new IOException("Producto inválido o repetido: " + id);
                }
                productos.add(new Producto(id, nodo.path("title").textValue(),
                        nodo.path("description").textValue(), cantidad, precio));
            }
            salto += pagina.size();
        } while (salto < total);

        if (productos.size() != total) {
            throw new IOException("El número de productos descargados no coincide con el total.");
        }
        return productos;
    }

    public static void guardar(Connection conexion, List<Producto> productos) throws SQLException {
        String sql = "INSERT INTO " + Esquema.PRODUCTOS
                + " (id, nombre, " + Esquema.DESCRIPCION + ", cantidad, precio) VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), "
                + Esquema.DESCRIPCION + "=VALUES(" + Esquema.DESCRIPCION + "), "
                + "cantidad=VALUES(cantidad), precio=VALUES(precio)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (Producto producto : productos) {
                ps.setInt(1, producto.id());
                ps.setString(2, producto.nombre());
                ps.setString(3, producto.descripcion());
                ps.setInt(4, producto.cantidad());
                ps.setBigDecimal(5, producto.precio());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
