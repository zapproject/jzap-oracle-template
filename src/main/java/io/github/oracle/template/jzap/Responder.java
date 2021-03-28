package io.github.oracle.template.jzap;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Responder {

    public Responder() {
    }

    public String getResponse(String to, String from) throws Exception {
        String text = from.replace(" ", "%20");
        OkHttpClient httpClient = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("text", text).build();
        Request request = new Request.Builder()
                .url("https://api.funtranslations.com/translate/"+to+".json")
                .post(formBody)
                .build();
        
        Response response = httpClient.newCall(request).execute();
        System.out.println("#####" + response.body().string());
        return response.body().string();
        
    }
}
