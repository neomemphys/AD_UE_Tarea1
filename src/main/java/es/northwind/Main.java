package es.northwind;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Título | Northwind");
        System.out.println("Módulo | Acceso a datos");
        System.out.println("Descargando todos los productos de https://dummyjson.com/products ...");
        List<ImportadorProductos.Producto> productos = ImportadorProductos.descargar();
        if (productos.isEmpty()) {
            throw new IllegalStateException("El catálogo está vacío; no se pueden crear pedidos.");
        }

        try (Conexion gestor = Conexion.getInstancia()) {
            Connection conexion = gestor.getConexion();
            // MySQL realiza commits implícitos para DDL: crear antes de la transacción de datos.
            Esquema.crear(conexion);
            conexion.setAutoCommit(false);
            try {
                ImportadorProductos.guardar(conexion, productos);
                insertarEmpleadosYPedidos(conexion);
                insertarFavoritos(conexion);
                conexion.commit();
            } catch (Exception e) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }
                throw e;
            }
            conexion.setAutoCommit(true);
            System.out.println("Productos importados: " + productos.size());
            describirTablas(conexion);
            mostrar(conexion, "Todos los productos",
                    "SELECT id, nombre, " + Esquema.DESCRIPCION + ", cantidad, precio FROM "
                            + Esquema.PRODUCTOS + " ORDER BY id");
            mostrar(conexion, "Todos los productos favoritos con sus datos",
                    "SELECT f.id AS id_favorito, p.id AS id_producto, p.nombre, p."
                            + Esquema.DESCRIPCION + ", p.cantidad, p.precio FROM "
                            + Esquema.FAVORITOS + " f INNER JOIN " + Esquema.PRODUCTOS
                            + " p ON f.id_producto = p.id ORDER BY p.id");
            mostrar(conexion, "Productos con precio inferior a 600€",
                    "SELECT id, nombre, " + Esquema.DESCRIPCION + ", cantidad, precio FROM "
                            + Esquema.PRODUCTOS + " WHERE precio < 600 ORDER BY id");
        }
    }

    private static void insertarEmpleadosYPedidos(Connection conexion) throws SQLException {
        // Datos de demostración: el enunciado no proporciona empleados ni pedidos concretos.
        // Se utiliza Statement expresamente, tal como solicita el ejercicio.
        try (Statement st = conexion.createStatement()) {
            st.executeUpdate("INSERT INTO " + Esquema.EMPLEADOS
                    + " (id, nombre) SELECT 1, 'Empleado 1' WHERE NOT EXISTS "
                    + "(SELECT 1 FROM " + Esquema.EMPLEADOS + " WHERE id = 1)");
            st.executeUpdate("INSERT INTO " + Esquema.EMPLEADOS
                    + " (id, nombre) SELECT 2, 'Empleado 2' WHERE NOT EXISTS "
                    + "(SELECT 1 FROM " + Esquema.EMPLEADOS + " WHERE id = 2)");
            st.executeUpdate("INSERT INTO " + Esquema.PEDIDOS
                    + " (id, id_producto, cantidad) SELECT 1, p.id, 1 FROM "
                    + Esquema.PRODUCTOS + " p WHERE NOT EXISTS "
                    + "(SELECT 1 FROM " + Esquema.PEDIDOS + " WHERE id = 1) ORDER BY p.id LIMIT 1");
            st.executeUpdate("INSERT INTO " + Esquema.PEDIDOS
                    + " (id, id_producto, cantidad) SELECT 2, p.id, 2 FROM "
                    + Esquema.PRODUCTOS + " p WHERE NOT EXISTS "
                    + "(SELECT 1 FROM " + Esquema.PEDIDOS + " WHERE id = 2) ORDER BY p.id DESC LIMIT 1");
        }
    }

    private static void insertarFavoritos(Connection conexion) throws SQLException {
        try (Statement st = conexion.createStatement()) {
            int insertados = st.executeUpdate("INSERT INTO " + Esquema.FAVORITOS
                    + " (id_producto) SELECT p.id FROM " + Esquema.PRODUCTOS
                    + " p WHERE p.precio > 1000 AND NOT EXISTS (SELECT 1 FROM "
                    + Esquema.FAVORITOS + " f WHERE f.id_producto = p.id)");
            System.out.println("Nuevos favoritos con precio superior a 1000€: " + insertados);
        }
    }

    private static void describirTablas(Connection conexion) throws SQLException {
        for (String tabla : new String[]{Esquema.PRODUCTOS, Esquema.FAVORITOS,
                Esquema.EMPLEADOS, Esquema.PEDIDOS}) {
            mostrar(conexion, "Estructura de " + tabla, "DESCRIBE " + tabla);
        }
    }

    private static void mostrar(Connection conexion, String titulo, String sql) throws SQLException {
        System.out.println();
        System.out.println(titulo);
        try (Statement st = conexion.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnas = meta.getColumnCount();
            for (int i = 1; i <= columnas; i++) {
                if (i > 1) {
                    System.out.print(" | ");
                }
                System.out.print(meta.getColumnLabel(i));
            }
            System.out.println();
            int filas = 0;
            while (rs.next()) {
                for (int i = 1; i <= columnas; i++) {
                    if (i > 1) {
                        System.out.print(" | ");
                    }
                    String valor = rs.getString(i);
                    System.out.print(valor == null ? "NULL" : valor.replace('\n', ' ').replace('\r', ' '));
                }
                System.out.println();
                filas++;
            }
            System.out.println("Filas: " + filas);
        }
    }
}
