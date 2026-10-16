# Guía Completa de Spring Security y su Implementación en el Microservicio de Login con MongoDB

## ¿Qué es Spring Security?

**Spring Security** es el framework estándar de la industria en el ecosistema Java para implementar autenticación (quién eres) y autorización (qué tienes permitido hacer) en aplicaciones basadas en Spring. 

A partir de **Spring Boot 3 y Spring Security 6**, el framework adoptó una arquitectura basada puramente en componentes desacoplados y configuración mediante **lambdas**, eliminando clases abstractas obsoletas (como el antiguo `WebSecurityConfigurerAdapter`) y apostando fuertemente por arquitecturas **sin estado (stateless)**, ideales para microservicios y APIs REST con tokens JWT.

---

## ¿Cómo se implementó en el proyecto de Login con MongoDB?

Para construir un microservicio de autenticación, estructuramos Spring Security conectando cada una de sus piezas clave con una base de datos **MongoDB**. A continuación, se detalla el rol de cada componente dentro de la implementación:

### 1. Adaptación del Modelo de Usuario (`UserDocument`)
* **Qué hace:** Spring Security no sabe cómo es la base de datos ni las tablas. Para que entienda los usuarios, la entidad `UserDocument` implementó la interfaz `UserDetails`.
* **Cómo se aplicó:** Se sobrescribieron métodos vitales como `getUsername()` (mapeándolo al email) y `getAuthorities()` (mapeando la lista de roles a `GrantedAuthority`), permitiendo que el framework reconozca los permisos del usuario directamente desde MongoDB a través del `UserRepository`.

### 2. Puente de Datos (`UserDetailsServiceImpl`)
* **Qué hace:** Implementa la interfaz estándar `UserDetailsService`. Su única misión es ir a buscar al usuario a la base de datos cuando alguien intenta iniciar sesión.
* **Cómo se aplicó:** Se creó `UserDetailsServiceImpl` para que utilice tu repositorio de MongoDB (`findByEmail`) y devuelva el objeto `UserDetails` que Spring Security necesita para validar las credenciales.

### 3. El Motor de Criptografía y Validación (`SecurityConfig`)
Aquí se configuran las reglas del juego para la API:
* **`PasswordEncoder` (BCrypt):** Se definió un bean de tipo `BCryptPasswordEncoder` para garantizar que las contraseñas nunca se guarden en texto plano en MongoDB, sino cifradas con hashes seguros.
* **`AuthenticationProvider` (DaoAuthenticationProvider):** Es el componente encargado de unir el servicio de usuarios (`UserDetailsService`) con el codificador de contraseñas para comparar el password que envía el usuario contra el almacenado.
* **`AuthenticationManager`:** El orquestador principal que recibe las credenciales del login y ejecuta el proceso de autenticación.

### 4. Arquitectura Stateless y Seguridad por Token (`JwtAuthenticationFilter` y `JwtUtils`)
Como estamos en microservicios, la aplicación **no guarda sesiones en memoria**. 
* **`JwtUtils`:** Utiliza la librería JJWT para firmar digitalmente un token con una clave secreta (`secret`) y un tiempo de expiración cada vez que un usuario hace login exitoso.
* **`JwtAuthenticationFilter` (Hereda de `OncePerRequestFilter`):** Es un filtro personalizado que intercepta **cada petición HTTP entrante**. Si la petición trae la cabecera `Authorization: Bearer <token>`, el filtro valida la firma criptográfica del JWT, extrae el correo, carga al usuario y lo inyecta directamente en el contexto de seguridad (`SecurityContextHolder`), simulando que el usuario ya está autenticado para esa petición.

### 5. Reglas de Acceso y Cadena de Filtros (`SecurityFilterChain`)
En `SecurityConfig` se configuró la política de seguridad web mediante lambdas:
* **CSRF deshabilitado (`csrf.disable()`):** Innecesario en APIs REST puras que consumen tokens JWT en lugar de cookies de sesión.
* **Sesión Stateless (`SessionCreationPolicy.STATELESS`):** Le indica a Spring Security que **nunca** cree HttpSession en el servidor.
* **Rutas Públicas vs Protegidas:** Se configuró que cualquier endpoint bajo `/api/v1/auth/**` sea totalmente público (para poder hacer login y registro), mientras que el resto requiera autenticación obligatoria.
* **Inyección del Filtro:** Se insertó tu `JwtAuthenticationFilter` justo antes del filtro estándar de usuario y contraseña (`UsernamePasswordAuthenticationFilter`), asegurando que las peticiones con JWT se validen antes de llegar a tus controladores.

### 6. Endpoint de Autenticación (`AuthController`)
Finalmente, el controlador expone los endpoints de negocio:
* **Registro:** Codifica la contraseña con `passwordEncoder` y guarda el documento en MongoDB con un rol por defecto (`ROLE_USER`).
* **Login:** Utiliza el `AuthenticationManager` para validar el correo y contraseña contra la base de datos. Si las credenciales son correctas, genera y devuelve el token JWT firmado para que el cliente lo utilice en futuras peticiones.