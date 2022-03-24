package com.example.NBASTARS;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import freemarker.template.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import freemarker.template.Template;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Controller
public class HomeController {
    private Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);

    @GetMapping("/")    // home page
    public String home() throws IOException {
        this.configuration.setDirectoryForTemplateLoading(new File("src/main/resources/templates/"));
        return "home";
    }

    @GetMapping("/results")     // for action results in html file
    public @ResponseBody
    String home(@RequestParam String name, @RequestParam String surname) throws Exception {
        Map<String, Object> model = new HashMap<>();
        URL urlToGetData = new URL(String.format("https://www.balldontlie.io/api/v1/players/?search=" + name));


        String dataString = sendRequest(urlToGetData);
        JsonElement jsonElement = processJons(dataString).getAsJsonObject().get("data");
        JsonArray arr = jsonElement.getAsJsonArray();
        name = "\"" + name + "\"";
        surname = "\"" + surname + "\"";
        boolean exists = false;
        JsonObject playerObject = null;

        for(JsonElement el : arr){
            JsonObject obj = el.getAsJsonObject();

            if(Objects.equals(obj.get("first_name").toString(), name) && Objects.equals(obj.get("last_name").toString(), surname)){
                playerObject = obj;
                exists = true;
            }
        }

        if(!exists){
            model.put("name", "NOT FOUND");
            model.put("surname", "NOT FOUND");
            model.put("height_feet", "NOT FOUND");
            model.put("height_inches", "NOT FOUND");
            model.put("pts", "NOT FOUND");
            model.put("ast", "NOT FOUND");
            model.put("reb", "NOT FOUND");
            model.put("res", "CANNOT GET RESULT");
        }else{
            model.put("name", name);
            model.put("surname", surname);
            model.put("height_feet", playerObject.get("height_feet"));
            model.put("height_inches", playerObject.get("height_inches"));
            String id = String.valueOf(playerObject.get("id"));
            System.out.println(id);
            URL urlGetStats = new URL(String.format("https://www.balldontlie.io/api/v1/season_averages?season=2020&player_ids[]=" + id));
            JsonArray statsArr = processJons(sendRequest(urlGetStats)).getAsJsonObject().get("data").getAsJsonArray();
            JsonObject statsObj = statsArr.get(0).getAsJsonObject();
            String pts = String.valueOf(statsObj.get("pts"));
            String ast = String.valueOf(statsObj.get("ast"));
            String reb = String.valueOf(statsObj.get("reb"));

            model.put("pts", statsObj.get("pts"));
            model.put("ast", statsObj.get("ast"));
            model.put("reb", statsObj.get("reb"));

            URL urlGetPreviousStats = new URL(String.format("https://www.balldontlie.io/api/v1/season_averages?season=2019&player_ids[]=" + id));
            JsonArray statsPreviousArr = processJons(sendRequest(urlGetPreviousStats)).getAsJsonObject().get("data").getAsJsonArray();
            JsonObject statsPrevObj = statsPreviousArr.get(0).getAsJsonObject();
            String pts2 = String.valueOf(statsPrevObj.get("pts"));
            String ast2 = String.valueOf(statsPrevObj.get("ast"));
            String reb2 = String.valueOf(statsPrevObj.get("reb"));
            String res = "Comparison = ";
            if(Float.parseFloat(pts) > Float.parseFloat(pts2)){
                res += "has better points ";
            }
            if(Float.parseFloat(ast) > Float.parseFloat(ast2)){
                res += "has better assists ";
            }
            if(Float.parseFloat(reb) > Float.parseFloat(reb2)){
                res += "has better rebounds";
            }
            if(res.equals("Comparison = ")){
                res = "Player worsened in every compared category";
            }
            model.put("res", res);

        }


        try {
            Template template = configuration.getTemplate("results.html");
            Writer stringWriter = new StringWriter();
            template.process(model, stringWriter);
            String responseStr = stringWriter.toString();
            stringWriter.close();
            return responseStr;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong while creating result template");
        }
    }

    private JsonElement processJons(String data) {
        JsonElement jElem = new Gson().fromJson(data, JsonElement.class);
        return jElem;
    }

    private String sendRequest(URL url) throws IOException {    // https://reqbin.com/req/java/5nqtoxbx/get-json-example
        // https://www.baeldung.com/httpurlconnection-post
        try {
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod("GET");
            http.setRequestProperty("Content-Type", "application/json; utf-8");
            http.setRequestProperty("Accept", "application/json");
            http.setDoOutput(true);

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(http.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                br.close();
                http.connect();
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Somethin went wrong while sendig request -> " + url.toString());
        }
    }
}
