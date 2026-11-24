# Northwind — Acceso a datos

Proyecto Maven de consola para abrir en IntelliJ IDEA. Requiere JDK 17, Maven 3.8 o superior, MySQL 8.0 o superior e Internet.

## Configuración y ejecución

1. Iniciar un servidor MySQL. El proyecto no instala ni inicia el servidor.
2. Abrir `pom.xml` como proyecto Maven en IntelliJ y seleccionar JDK 17.
3. En la configuración de ejecución de `es.northwind.Main`, definir las variables de entorno:
   - `DB_URL`: `jdbc:mysql://localhost:3306/?serverTimezone=UTC&characterEncoding=UTF-8`
   - `DB_USER`: usuario de MySQL.
   - `DB_PASSWORD`: contraseña de ese usuario.
4. El usuario necesita permisos para crear la base de datos y sus tablas, así como consultar, insertar y actualizar datos. La URL inicial no debe seleccionar una base de datos inexistente.
5. Ejecutar `Main` o utilizar `mvn compile exec:java` desde la carpeta del proyecto, con esas mismas variables de entorno configuradas en la terminal.

Si no se definen las variables, se utiliza localhost:3306, usuario root y contraseña vacía. Son valores de desarrollo, no una configuración recomendada para producción. No subir credenciales al repositorio.

## Funcionamiento

- Descarga por HTTPS todas las páginas del catálogo de `https://dummyjson.com/products`, no solamente la primera página.
- Crea la base de datos `almacén`, conservando el acento solicitado, y las tablas `Productos`, `Productos_Fav`, `Empleados` y `Pedidos`.
- Centraliza los nombres SQL en `Esquema` y utiliza el patrón singleton en `Conexion`.
- Mapea `id` a `id`, `title` a `nombre`, `description` a `descripción`, `stock` a `cantidad` y `price` a `precio`. No modifica ni convierte los importes del JSON.
- Importa con PreparedStatement para conservar correctamente comillas y caracteres de los textos recibidos.
- Inserta empleados y pedidos con Statement. Los pedidos hacen referencia a productos que existen realmente en la base de datos.
- Inserta como favoritos los productos cuyo precio sea estrictamente superior a 1000.
- Describe las tablas con DESCRIBE y muestra mediante consultas Statement todos los productos, todos los favoritos con sus datos y los productos con precio estrictamente inferior a 600.
- La importación y las inserciones se confirman en una transacción. Ante un error se revierten los cambios de datos de esa ejecución. La creación de estructura queda fuera de esa transacción por el comportamiento DDL de MySQL.
- Puede repetirse la ejecución: los productos existentes se actualizan por su id y no se duplican los favoritos, empleados ni pedidos de ejemplo. No elimina datos existentes ni favoritos previamente almacenados.

## Decisiones por falta de detalle del enunciado

El motor relacional no está especificado; se utiliza MySQL mediante su conector JDBC oficial. Tampoco se indican campos ni registros concretos para empleados y pedidos: se adopta la estructura mínima `Empleados(id, nombre)` y `Pedidos(id, id_producto, cantidad)`, con dos empleados genéricos y dos pedidos de demostración. No se añade una relación empleado-pedido porque no se solicita.

La referencia visual es el logotipo de la institución del documento, no una pantalla de la aplicación. La solución mantiene la salida por consola exigida y no incorpora una interfaz gráfica inventada.

## Entrega

Completar el nombre del alumno en la documentación de entrega. Comprimir el proyecto como `nombre_apellido_DAM2_t1_t1.zip`, grabar el vídeo demostrativo y proporcionar el enlace del repositorio GitHub. El vídeo, la publicación del repositorio y el ZIP deben prepararse por separado.
