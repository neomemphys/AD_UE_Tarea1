package es.northwind;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Centraliza los nombres SQL y la creación de la estructura. */
public final class Esquema {
    public static final String BASE_DATOS = "`almacén`";
    public static final String PRODUCTOS = "`Productos`";
    public static final String FAVORITOS = "`Productos_Fav`";
    public static final String EMPLEADOS = "`Empleados`";
    public static final String PEDIDOS = "`Pedidos`";
    public static final String DESCRIPCION = "`descripción`";

    private Esquema() {
    }

    public static void crear(Connection conexion) throws SQLException {
        try (Statement st = conexion.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + BASE_DATOS
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            st.execute("USE " + BASE_DATOS);
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + PRODUCTOS + " ("
                    + "id INT PRIMARY KEY, "
                    + "nombre VARCHAR(255) NOT NULL, "
                    + DESCRIPCION + " TEXT NOT NULL, "
                    + "cantidad INT NOT NULL, "
                    + "precio DECIMAL(12,2) NOT NULL) ENGINE=InnoDB");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + FAVORITOS + " ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "id_producto INT NOT NULL UNIQUE, "
                    + "CONSTRAINT fk_favorito_producto FOREIGN KEY (id_producto) REFERENCES "
                    + PRODUCTOS + "(id)) ENGINE=InnoDB");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + EMPLEADOS + " ("
                    + "id INT PRIMARY KEY, nombre VARCHAR(100) NOT NULL) ENGINE=InnoDB");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + PEDIDOS + " ("
                    + "id INT PRIMARY KEY, id_producto INT NOT NULL, cantidad INT NOT NULL, "
                    + "CONSTRAINT fk_pedido_producto FOREIGN KEY (id_producto) REFERENCES "
                    + PRODUCTOS + "(id)) ENGINE=InnoDB");
        }
    }
}
