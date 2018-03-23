package gateway;

import gateway.api.Api;
import org.apache.logging.log4j.Logger;
import spark.Service;

import java.util.Optional;

public class ApiGateway {

    private static final Logger l = org.apache.logging.log4j.LogManager.getLogger(ApiGateway.class);

    private Optional<Service> httpService;
    //APIs
    private Api api;

    public ApiGateway(Optional<Service> httpService,
                      Api api) {
        this.httpService = httpService;
        this.api = api;
    }

    public void start() {
        l.debug("Api gateway starting");
        httpService.ifPresent(http -> {
            http.post("/start", api::getArticles);
            l.debug("Api gateway started");
        });
    }
}
