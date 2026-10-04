# E-Commerce Microservices Project

This project is an e-commerce backend application developed using a microservice architecture. The system separates the major areas of the application into independent services responsible for products, customers, and orders.

The application was developed as part of the SFWE 510 Cloud Native Software Engineering semester project.

## Project Architecture

The application currently consists of four Spring Boot services:

- **Config Server** - Provides centralized configuration for the microservices and supports environment-specific configuration profiles.
- **Product Service** - Manages product information, pricing, categories, and available quantities.
- **Customer Service** - Manages customer information such as names, email addresses, phone numbers, and addresses.
- **Order Service** - Manages customer orders and the products associated with each order.

Each business microservice has its own PostgreSQL database.

### Service Ports

| Service | Port |
|---|---:|
| Config Server | 8071 |
| Product Service | 8081 |
| Order Service | 8082 |
| Customer Service | 8083 |
| PostgreSQL | 5432 |

## Technologies

The project uses:

- Java 21
- Spring Boot
- Spring Cloud Config
- Spring Data JPA
- Spring HATEOAS
- PostgreSQL
- Maven
- Docker
- Docker Compose
- REST APIs

## Project Structure

```text
ECommerce-Semester-Project/
├── configserver/
├── product-service/
├── customer-service/
├── order-service/
├── docker/
│   ├── docker-compose.yml
│   ├── init.sql
│   └── data.sql
├── pom.xml
└── README.md
```

## Data Model

The application separates data ownership between the three business services.

### Product Service

Products contain information including:

- Product ID
- Product name
- Description
- Price
- Quantity
- Category

### Customer Service

Customers contain information including:

- Customer ID
- First name
- Last name
- Email
- Phone number
- Address

### Order Service

Orders contain information including:

- Order ID
- Customer ID
- Customer name
- Order date
- Order status
- Order total
- Order items

Each order item contains a reference to a product along with a snapshot of the product name and price at the time of the order.

The Order Service stores customer and product identifiers as references rather than using database foreign keys across microservice databases. Each service maintains ownership of its own data.

## Configuration Profiles

The application uses Spring Cloud Config to centrally manage configuration.

The business services support:

- `dev`
- `prod`

The active profile can be selected using the `SPRING_PROFILES_ACTIVE` environment variable.

When running through Docker Compose, the services use the `dev` profile by default.

## Running the Application

### Prerequisites

The following software is required:

- Java 21
- Maven
- Docker Desktop
- Docker Compose

### Build the Project

From the root directory of the project, run:

```bash
mvn clean package
```

### Start the Application

After the Maven build completes, start the application using Docker Compose:

```bash
docker compose -f docker/docker-compose.yml up --build
```

Docker Compose starts:

- PostgreSQL
- Config Server
- Product Service
- Customer Service
- Order Service

Once startup is complete, the REST APIs can be accessed through their respective ports.

### Stop the Application

To stop the application:

```bash
docker compose -f docker/docker-compose.yml down
```

The PostgreSQL Docker volume persists database data between normal container restarts.

To stop the application and remove the persisted database volume:

```bash
docker compose -f docker/docker-compose.yml down -v
```

## REST API Endpoints

### Product Service

Base URL:

```text
http://localhost:8081/v1/product
```

Available operations:

```text
GET     /v1/product
GET     /v1/product/{productId}
POST    /v1/product
PUT     /v1/product
DELETE  /v1/product/{productId}
```

### Customer Service

Base URL:

```text
http://localhost:8083/v1/customer
```

Available operations:

```text
GET     /v1/customer
GET     /v1/customer/{customerId}
POST    /v1/customer
PUT     /v1/customer
DELETE  /v1/customer/{customerId}
```

### Order Service

Base URL:

```text
http://localhost:8082/v1/order
```

Available operations:

```text
GET     /v1/order
GET     /v1/order/{orderId}
POST    /v1/order
PUT     /v1/order
DELETE  /v1/order/{orderId}
```

## Config Server

Configuration can be viewed through the Config Server.

Development configurations:

```text
http://localhost:8071/product-service/dev
http://localhost:8071/customer-service/dev
http://localhost:8071/order-service/dev
```

Production configurations:

```text
http://localhost:8071/product-service/prod
http://localhost:8071/customer-service/prod
http://localhost:8071/order-service/prod
```

## Testing

The REST APIs can be tested using Postman or another HTTP client.

The Product, Customer, and Order services provide CRUD operations that allow records to be created, retrieved, updated, and deleted through their REST endpoints.