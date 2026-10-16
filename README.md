# Login Service

Spring Boot REST service for registering users and authenticating them with JWTs. User accounts are stored in MongoDB, passwords are hashed with BCrypt, and authenticated requests use stateless bearer-token authentication.

## Requirements

- Java 21
- MongoDB

The project uses Maven Wrapper, so a separate Maven installation is not required.

## Run Locally

Start MongoDB on `localhost:27017`, then run the development profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows Command Prompt or PowerShell:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

The `run.sh` script starts the same development profile. By default, the service uses database `auth_db`, listens on port `8080`, and exposes authentication endpoints under `/api/v1/auth`.

## Configuration

The default application config is in `src/main/resources/application.yaml`. The `dev` profile in `application-dev.yaml` configures the local MongoDB URI, JWT signing key and expiration, and API base path.

For another environment, provide the corresponding Spring properties through environment variables or an external configuration source:

| Environment variable | Purpose |
| --- | --- |
| `SPRING_DATA_MONGODB_URI` | MongoDB connection URI |
| `JWT_SECRET` | JWT HMAC signing key; use a strong, private key |
| `JWT_EXPIRATION` | Token lifetime in milliseconds |
| `API_BASE_PATH` | Base path for authentication endpoints |

The checked-in development profile contains a development signing key. Do not reuse it outside local development; supply a securely managed key for deployed environments.

## API

All endpoints use JSON. Registration and login are public; other routes require a valid JWT in the `Authorization` header.

### Register

`POST /api/v1/auth/register`

Request:

```json
{
  "username": "alex",
  "email": "alex@example.com",
  "password": "example-password"
}
```

On success, returns HTTP `200` with:

```json
{
  "message": "User registered successfully."
}
```

If the username is already registered, returns HTTP `400` with `{"message":"Username is already taken."}`. New accounts receive the `ROLE_USER` role, and passwords are stored as BCrypt hashes.

### Login

`POST /api/v1/auth/login`

Request:

```json
{
  "username": "alex",
  "password": "example-password"
}
```

On successful authentication, returns HTTP `200` with a signed JWT:

```json
{
  "token": "<jwt>"
}
```

Use the token on requests to protected routes:

```http
Authorization: Bearer <jwt>
```

The token subject is the username. Its expiration is controlled by `JWT_EXPIRATION`.

## Build and Test

Run the test suite:

```bash
./mvnw test
```

On Windows:

```powershell
.\mvnw.cmd test
```

Build the executable JAR:

```bash
./mvnw package
```

## Project Structure

- `controller/AuthController.java` — registration and login endpoints.
- `config/SecurityConfig.java` — stateless security rules, authentication provider, and password encoder.
- `filter/JwtAuthenticationFilter.java` — reads bearer tokens and sets the authenticated security context.
- `util/JwtUtil.java` — creates, parses, and validates JWTs.
- `service/UserDetailsServiceImpl.java` — loads users by username for Spring Security.
- `repository/UserRepository.java` — MongoDB user queries.
- `model/UserDocument.java` — MongoDB user document and Spring Security user details.
- `dto/` — request and response records used by the API.

Additional Spring Security implementation notes are available in [`doc/spring_security_guide.md`](doc/spring_security_guide.md).

## Authoring

PhD. Alfonso José Barroso Barajas