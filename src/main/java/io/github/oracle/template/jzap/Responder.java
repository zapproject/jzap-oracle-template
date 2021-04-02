package io.github.oracle.template.jzap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class Responder {

    public Responder() {
    }

    public String getResponse(String query) throws Exception {
        String to = query.substring(0, query.indexOf("-"));
        String from = query.substring(query.indexOf("-")+1);
        String text = from.replace(" ", "%20");
        OkHttpClient httpClient = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("text", text).build();
        Request request = new Request.Builder()
                .url("https://api.funtranslations.com/translate/"+to+".json")
                .post(formBody)
                .build();
        
        Response response = httpClient.newCall(request).execute();
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response.body().string());
        
        return node.get("contents").get("translated").asText().replace("%", " ");
    }
}
