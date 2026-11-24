package es.northwind;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Una única conexión JDBC para la aplicación. */
public final class Conexion implements AutoCloseable {
    private static Conexion instancia;
    private final Connection conexion;

    private Conexion() throws SQLException {
        String url = entorno("DB_URL", "jdbc:mysql://localhost:3306/?serverTimezone=UTC&characterEncoding=UTF-8");
        String usuario = entorno("DB_USER", "root");
        String clave = entorno("DB_PASSWORD", "");
        conexion = DriverManager.getConnection(url, usuario, clave);
    }

    public static synchronized Conexion getInstancia() throws SQLException {
        if (instancia == null || instancia.conexion.isClosed()) {
            instancia = new Conexion();
        }
        return instancia;
    }

    public Connection getConexion() {
        return conexion;
    }

    private static String entorno(String nombre, String defecto) {
        String valor = System.getenv(nombre);
        return valor == null ? defecto : valor;
    }

    @Override
    public void close() throws SQLException {
        conexion.close();
    }
}
