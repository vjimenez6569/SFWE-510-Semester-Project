# Stage 1
# Start with Java 21 JDK
FROM eclipse-temurin:21-jdk AS build

# The application's jar file
ARG JAR_FILE

# Add the application's jar to the container
COPY ${JAR_FILE} app.jar

# Unpackage jar file
RUN mkdir -p target/dependency && \
    (cd target/dependency; jar -xf /app.jar)


# Stage 2
# Java 21 runtime
FROM eclipse-temurin:21-jre

VOLUME /tmp

ARG DEPENDENCY=/target/dependency

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

ENTRYPOINT ["java","-cp","app:app/lib/*","com.ecommerce.product.ProductServiceApplication"]