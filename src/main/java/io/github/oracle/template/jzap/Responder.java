package io.github.oracle.template.jzap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class Responder {

    public Responder() {
    }

    public String getResponse() throws Exception {
        String respond;
        OkHttpClient httpClient = new OkHttpClient();

        // API for App ideas
        Request request = new Request.Builder()
                .url("http://itsthisforthat.com/api.php?text")
                .build();
        
        Response response = httpClient.newCall(request).execute();
        respond = "App idea: " + response.body().string();

        // API for advices
        request = new Request.Builder()
            .url("https://api.adviceslip.com/advice")
            .build();
        
        response = httpClient.newCall(request).execute();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response.body().string());
        respond += "\nAdvice: " + node.get("slip").get("advice").asText();

        // API for quotes
        request = new Request.Builder()
            .url("http://quotes.stormconsultancy.co.uk/random.json")
            .build();

        response = httpClient.newCall(request).execute();

        node = mapper.readTree(response.body().string());
        respond += "\nQuote: " + node.get("quote") + " -" + node.get("author");

        return respond;
    }
}
