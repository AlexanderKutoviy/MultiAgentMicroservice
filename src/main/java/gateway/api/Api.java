package gateway.api;

import com.google.gson.Gson;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Response;

public class Api {

    private static final Logger l = org.apache.logging.log4j.LogManager.getLogger(Api.class);
    private Gson gson = new Gson();

    public Api() {
    }

    public String getArticles(Request request, Response response) throws Exception {
        return "Already started, man";
    }
}