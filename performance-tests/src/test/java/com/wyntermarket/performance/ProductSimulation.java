
package com.wyntermarket.performance;

import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class ProductSimulation extends Simulation {

    private final String baseUrl =
        System.getProperty("baseUrl", "http://localhost:8072");

    private final int users =
        Integer.getInteger("users", 25);

    private final int duration =
        Integer.getInteger("duration", 60);

    {
        var httpProtocol = http
            .baseUrl(baseUrl)
            .acceptHeader("application/json");

        var browseProducts = scenario("Browse Wynter Market Products")
            .exec(
                http("GET Product Catalog")
                    .get("/v1/product")
                    .check(status().is(200))
            )
            .pause(1, 3);

        setUp(
            browseProducts.injectOpen(
                constantUsersPerSec(users)
                    .during(duration)
            )
        ).protocols(httpProtocol)
         .assertions(
             global().failedRequests().percent().lt(1.0),
             global().responseTime().percentile3().lt(2000)
         );
    }
}
