
# Wynter Market — Semester Project Phase 2

**Course:** SFWE 510 — Cloud Native Software Engineering  
**Project:** Wynter Market — E-Commerce Microservices Application  
**Phase:** 2 — Security, Service Discovery, and Performance Testing  

## About

Wynter Market is an e-commerce application built using Spring Boot microservices. Phase 2 focuses on securing the application with **Keycloak**, **Spring Security**, and **OAuth 2.0/OpenID Connect**, demonstrating communication between services, and testing scalability.

The application consists of these main components:

- **Keycloak:** Manages user accounts, authentication, roles, access tokens, registration, and password recovery.
- **Gateway Server:** Routes incoming API requests to the appropriate microservices.
- **Product Service:** Manages products and inventory.
- **Order Service:** Creates and manages customer orders and communicates with Product Service.
- **Customer Service:** Handles customer-related application data.
- **Config Server:** Provides centralized configuration.
- **Eureka Server:** Registers and discovers microservices.
- **PostgreSQL:** Stores application data.
- **Nginx Frontend:** Hosts the Wynter Market storefront.

Phase 2 demonstrates:

- **Authentication:** Users sign in through Keycloak and receive JWT access tokens.
- **Authorization:** CUSTOMER and ADMIN permissions restrict protected operations.
- **User Registration:** New customers can register for an account.
- **Password Reminder:** Customers can request a password reset through Keycloak's email workflow.
- **Bearer-Token Propagation:** Order Service forwards the incoming Authorization header to Product Service using WebClient.
- **Service Discovery:** Eureka and Spring Cloud LoadBalancer allow services to locate each other.
- **Stress Testing:** Gatling simulates concurrent requests and generates performance reports.

## Prerequisites

Make sure the following are installed:

- Java 21
- Apache Maven
- Docker Desktop
- Git
- Postman (for API testing)
- A test email account for the password-reset demonstration

## Project Structure

```text
WynterMarket-Semester-Project/
├── configserver/
├── eurekaserver/
├── gatewayserver/
├── product-service/
├── customer-service/
├── order-service/
├── frontend/
├── performance-tests/
├── docker/
│   ├── docker-compose.yml
│   ├── init.sql
│   └── data.sql
├── pom.xml
└── README.md
```

### Verify Java and Maven

Open PowerShell and run:

```powershell
java -version
mvn -version
```

Verify that Maven is using Java 21.

## Run the Application

Run the following commands from the **project root** unless otherwise specified.

### 1. Build the Application

Make sure Docker Desktop is running:

```powershell
mvn clean package -DskipTests
```

Build the Docker images using the Dockerfiles and image names configured in the project. If the Spotify Dockerfile Maven plugin works in your environment, you can use:

```powershell
mvn dockerfile:build
```

**Note:** If the Dockerfile Maven plugin fails with an `AccessDeniedException` under `target/docker`, build images directly with `docker build` instead. Ensure the resulting image tags match those referenced by Docker Compose.

### 2. Start Docker Containers

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Allow the services time to initialize and register with Eureka.

### 3. Verify Containers

```powershell
docker compose -f docker/docker-compose.yml ps
```

### 4. Access the Services

| Service | URL |
|---|---|
| Wynter Market Frontend | http://localhost:3000 |
| Keycloak | http://localhost:8080 |
| Config Server | http://localhost:8071 |
| Eureka Dashboard | http://localhost:8073 |
| Gateway Server | http://localhost:8072 |
| Gateway Routes (if actuator endpoint is exposed) | http://localhost:8072/actuator/gateway/routes |

## Keycloak Configuration

### 1. Configure the Realm and Client

Open the Keycloak Administration Console and verify the following settings:

| Setting | Value |
|---|---|
| Realm | `wyntermarket` |
| Client ID | `wyntermarket-project` |
| Protocol | `openid-connect` |
| Direct Access Grants | Enabled if testing password-grant token requests in Postman |

The client must be configured consistently with the application's Spring Security settings and frontend authentication flow.

### 2. Configure Roles and Permissions

Use test accounts with CUSTOMER and ADMIN permissions. The application code references client authorities including:

- `PLACE_ORDERS`
- `VIEW_OWN_ORDERS`
- `MANAGE_ORDERS`
- `MANAGE_PRODUCTS`

Confirm that the test users receive the expected authorities in their JWT claims. The exact Keycloak role hierarchy and composite-role assignments should match the realm configuration used by your deployment.

### 3. Configure Test Users

Prepare at least two users:

| User | Purpose |
|---|---|
| CUSTOMER test user | Browse products, create orders, and view their own orders |
| ADMIN test user | Demonstrate protected product and order management operations |

### 4. Configure Password Recovery

In Keycloak, configure the realm's SMTP settings and enable the password-reset workflow. Use a test email inbox and avoid committing SMTP passwords or application credentials to the repository.

## Test Authentication and Authorization

Use Postman to verify the application security features.

### 1. Generate Access Tokens

Create a POST request:

```http
POST http://localhost:8080/realms/wyntermarket/protocol/openid-connect/token
```

Under **Body**, select `x-www-form-urlencoded`:

| Key | Value |
|---|---|
| `grant_type` | `password` |
| `client_id` | `wyntermarket-project` |
| `username` | CUSTOMER or ADMIN test username |
| `password` | Corresponding test password |

Send a separate request for each test user. With Direct Access Grants enabled and valid credentials, the response should contain an `access_token`. Store tokens in Postman variables such as `customerToken` and `adminToken`; do not include them in screenshots or commits.

### 2. Test Public Product Endpoint

```http
GET http://localhost:8072/v1/product
```

Use **No Auth**. The Product Service security configuration allows GET requests to `/v1/product/**` without authentication. Expect `200 OK` if the service is available.

### 3. Test Protected Product Operations

```http
POST http://localhost:8072/v1/product
Content-Type: application/json
```

Example request body:

```json
{
  "productName": "Phase 2 Demo Product",
  "description": "Product used for the security demonstration",
  "category": "Electronics",
  "price": 29.99,
  "quantity": 20
}
```

Test with different credentials:

| Authentication | Expected behavior |
|---|---|
| No token | `401 Unauthorized` |
| CUSTOMER token | `403 Forbidden` if the controller enforces `MANAGE_PRODUCTS` |
| ADMIN token | Product created if the user has the required permission |

**Note:** Product Service's security filter chain requires authentication for protected `/v1/**` operations. ADMIN-only enforcement also depends on controller-level permission annotations.

### 4. Test Order Authorization

```http
POST http://localhost:8072/v1/order
Content-Type: application/json
```

Use a CUSTOMER token with the `PLACE_ORDERS` authority and an existing product ID:

```json
{
  "items": [
    {
      "productId": "<EXISTING_PRODUCT_ID>",
      "quantity": 1
    }
  ]
}
```

The `OrderController` requires `PLACE_ORDERS` for order creation. A successful request normally returns `200 OK` and an order with status `NEW`. Without a valid token, the protected request should be rejected.

## Test User Registration

1. Open http://localhost:3000.
2. Sign out of any existing account.
3. Select **Create Account** or the configured Keycloak registration link.
4. Register a new test customer with a unique username and email.
5. Complete the registration flow.
6. Sign in using the new account.
7. Verify the new user in Keycloak and confirm the appropriate customer permissions.

The specific registration screen depends on the frontend and Keycloak realm configuration.

## Test Password Reminder

1. Sign out of Wynter Market.
2. Select **Forgot Password** on the login flow.
3. Enter the registered test email address.
4. Open the password-reset message in the test inbox.
5. Follow the reset link and set a new password.
6. Sign in with the updated password.

This demonstration requires working SMTP settings and a valid reset-password flow in Keycloak.

## Test Bearer-Token Propagation

When a customer creates an order, the original Authorization header is forwarded to Product Service during the product lookup.

The existing implementation passes the token through these classes:

1. **OrderController:** Reads `@RequestHeader("Authorization") String token`.
2. **OrderService:** Passes the bearer token into `createOrder()` and `productLookup.get()`.
3. **ProductLookup:** Adds `.header(HttpHeaders.AUTHORIZATION, token)` to the outgoing WebClient request.

### 1. Monitor Service Logs

```powershell
docker compose -f docker/docker-compose.yml logs -f --tail=20 orderservice productservice
```

### 2. Create an Authenticated Order

Send the order POST request using a valid CUSTOMER token and an in-stock product.

### 3. Verify Propagation

If temporary, non-sensitive logging has been added to both services, verify that Order Service reports forwarding a bearer header and Product Service reports receiving one.

The Product Service's GET product endpoint is public, so a successful product lookup alone does **not** prove that a JWT was required. The code and logs demonstrate forwarding and receipt. Never log the complete access token.

**Inventory note:** Order Service uses a separate `X-Inventory-Secret` header for inventory reserve/restore calls; these are distinct from the customer JWT product lookup.

## Test Eureka Service Discovery

### 1. Verify Registered Services

Open the Eureka Dashboard:

```text
http://localhost:8073
```

Confirm that the expected microservices appear in the registry.

### 2. Test Gateway Routing

```http
GET http://localhost:8072/v1/product
```

The request should succeed when Gateway and Product Service are healthy.

### 3. Scale Product Service

If the Compose configuration supports multiple replicas without fixed-port or container-name conflicts:

```powershell
docker compose -f docker/docker-compose.yml up -d --scale productservice=2
```

Refresh Eureka to verify both instances are registered. Send requests through Gateway and inspect per-instance logs to confirm distribution if demonstrating load balancing.

### 4. Test Availability

Stop one replica, wait for Eureka and Gateway to update their instance lists, and retry the product GET request. The request should continue working through the healthy replica if the route is load-balanced.

Restore both instances:

```powershell
docker compose -f docker/docker-compose.yml up -d --scale productservice=2
```

## Test Performance Using Gatling

### 1. Run the Simulation

Navigate to the `performance-tests` module or directory containing the Gatling simulation.

```powershell
mvn gatling:test "-Dgatling.simulationClass=com.wyntermarket.performance.ProductSimulation" "-Dusers=100" "-Dduration=60"
```

This command assumes the simulation class and user/duration properties match the current test implementation.

### 2. Review the Results

Open the generated Gatling HTML report and review:

- Total requests
- Successful and failed requests
- Average and percentile response times
- Requests per second

If reports for one and two Product Service replicas are available, compare the measured results to evaluate scaling. Do not assume performance improved without checking the reports.

## Stop the Application

To stop and remove containers:

```powershell
docker compose -f docker/docker-compose.yml down
```

To start the application again:

```powershell
docker compose -f docker/docker-compose.yml up -d
```

Do not use `down -v` unless you intentionally want to delete persistent database volumes.

## Rebuild After Changes

After changing Java source code:

```powershell
mvn clean package -DskipTests
```

Build the affected Docker images with the project's configured image names, then recreate the updated services. For example:

```powershell
docker compose -f docker/docker-compose.yml up -d --no-deps --force-recreate orderservice productservice
```

If Docker Compose builds images directly from source, include `--build`. If it references prebuilt images, build and tag those images first.

For troubleshooting, inspect logs:

```powershell
docker compose -f docker/docker-compose.yml logs --tail=100 orderservice productservice gatewayserver
```
