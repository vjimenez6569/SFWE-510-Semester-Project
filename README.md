# E-Commerce Semester Project

Phase 1 starter project modeled closely after the completed SFWE 510 Assignment 5 project.

## Modules

- `configserver` - Spring Cloud Config Server
- `product-service` - Product bounded context
- `order-service` - Order bounded context
- `docker` - PostgreSQL initialization and Docker Compose
- `postman` - Phase 1 Postman collection

## Assignment 5 conventions retained

This project intentionally retains the Assignment 5 style:

- Java 11
- Spring Boot 2.2.x
- Spring Cloud Hoxton.SR1
- `javax.persistence`
- Lombok `@Getter`, `@Setter`, and `@ToString`
- `@Autowired` service/controller dependencies
- Spring Data `CrudRepository`
- Spring HATEOAS `RepresentationModel`
- `bootstrap.yml`
- Spring Cloud Config profiles
- PostgreSQL
- two-stage Eclipse Temurin Dockerfiles
- Docker Compose backend network
- Maven multi-module parent project

## Important difference from Assignment 5

The Config Server uses the `native` profile so the starter project is self-contained. Its configuration files are under:

`configserver/src/main/resources/config`

You can later move these files to a GitHub configuration repository and change the Config Server back to the `git` profile, just like Assignment 5.

## Bounded contexts

### Product Service

Owns product catalog data.

- Product
- ProductRepository
- ProductService
- ProductController

### Order Service

Owns customer order data.

- CustomerOrder
- OrderItem
- OrderRepository
- OrderService
- OrderController

The Order Service stores a `productId` and product snapshot information instead of directly mapping to the Product entity/database. This keeps the two microservices in separate bounded contexts.

## Profiles

Both services have:

- `dev`
- `prod`

The Docker Compose file runs `dev` by default.

## Build

From the project root:

```bash
mvn clean package
```

## Docker

After the Maven build finishes:

```bash
cd docker
docker compose up --build
```

Services:

- Config Server: `http://localhost:8071`
- Product Service: `http://localhost:8081`
- Order Service: `http://localhost:8082`
- PostgreSQL: `localhost:5432`

## Config Server checks

Development:

`http://localhost:8071/product-service/dev`

`http://localhost:8071/order-service/dev`

Production:

`http://localhost:8071/product-service/prod`

`http://localhost:8071/order-service/prod`

## Product REST endpoints

- `GET /v1/product`
- `GET /v1/product/{productId}`
- `POST /v1/product`
- `PUT /v1/product`
- `DELETE /v1/product/{productId}`

## Order REST endpoints

- `GET /v1/order`
- `GET /v1/order/{orderId}`
- `POST /v1/order`
- `PUT /v1/order`
- `DELETE /v1/order/{orderId}`

Import `postman/E-Commerce-Phase1.postman_collection.json` into Postman for starter requests.

## Note

This is a Phase 1 skeleton. Security, gateway, service discovery, bearer-token propagation, resilience/stress testing, JMS, and tracing are intentionally not included yet because they belong to later project phases.
