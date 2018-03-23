import gateway.ApiGateway;
import gateway.api.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.io.IoBuilder;
import spark.Service;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

public class Application {

    private static Logger l;
    private static Optional<Service> service;
    private static ApiGateway gateway;
    private static Api api;

    public static void main(String[] args) throws Exception {
        System.setProperty("log4j.shutdownCallbackRegistry", "com.djdch.log4j.StaticShutdownCallbackRegistry");
        Configurator.initialize("Log4j2Conf", "log4j2.xml");
        l = org.apache.logging.log4j.LogManager.getLogger(Application.class);
        System.setProperty("jsse.enableSNIExtension", "false");
        PrintStream oldErr = System.err;
        PrintStream oldOut = System.out;
        System.setErr(new PrintStream(IoBuilder.forLogger(l).filter(oldErr).buildOutputStream(), true));
        System.setOut(new PrintStream(IoBuilder.forLogger(l).filter(oldOut).buildOutputStream(), true));
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kiev"));
        Locale.setDefault(Locale.US);
        l.trace("Starting ...");

        System.setProperty("com.mchange.v2.c3p0.cfg.xml", "c3p0-config.xml");

        service = httpServer();
        api = new Api();
        service.ifPresent(http -> {
            http.get("/start", api::getArticles);
            l.debug("Api gateway started");
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            l.trace("Stopping ...");
            service.ifPresent(Service::stop);

            if (LogManager.getContext() instanceof LoggerContext) {
                l.info("Shutting down log4j2");
                Configurator.shutdown((LoggerContext) LogManager.getContext());
            } else l.warn("Unable recipients shutdown log4j2");
        }));
        l.trace("Started");
    }

    private static Optional<Service> httpServer() {
        Integer port = 9096;
        System.out.println("PORT" + port);
        Service httpService = Service.ignite();
        httpService.port(port);
        return Optional.of(httpService);
    }
}