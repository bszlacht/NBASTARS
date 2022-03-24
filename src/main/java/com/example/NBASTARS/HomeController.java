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
    public static final String GETPLAYERS = "https://www.balldontlie.io/api/v1/players/?search=";
    public static final String GETPLAYERSSTATS2020 = "https://www.balldontlie.io/api/v1/season_averages?season=2020&player_ids[]=";
    public static final String GETPLAYERSSTATS2019 = "https://www.balldontlie.io/api/v1/season_averages?season=2019&player_ids[]=";


    @GetMapping("/")    // home page
    public String home() throws IOException {
        this.configuration.setDirectoryForTemplateLoading(new File("src/main/resources/templates/"));
        return "home";
    }

    @GetMapping("/results")     // for action results in html file
    public @ResponseBody
    String home(@RequestParam String name, @RequestParam String surname) throws Exception {
        Map<String, Object> model = new HashMap<>();

        // getting player data
        URL urlToGetData = new URL(String.format(GETPLAYERS + name));
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

            PlayerThread t1 = new PlayerThread(id, "https://www.balldontlie.io/api/v1/season_averages?season=2020&player_ids[]=");
            PlayerThread t2 = new PlayerThread(id, "https://www.balldontlie.io/api/v1/season_averages?season=2019&player_ids[]=");
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            String pts1 = t1.getPts();
            String ast1 = t1.getAst();
            String reb1 = t1.getReb();
            String pts2 = t2.getPts();
            String ast2 = t2.getAst();
            String reb2 = t2.getReb();
            model.put("pts", pts1);
            model.put("ast", ast1);
            model.put("reb", reb1);


            // Comaprison :

            String res = "Comparison = ";
            if(Float.parseFloat(pts1) > Float.parseFloat(pts2)){
                res += "has better points ";
            }
            if(Float.parseFloat(ast1) > Float.parseFloat(ast2)){
                res += "has better assists ";
            }
            if(Float.parseFloat(reb1) > Float.parseFloat(reb2)){
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

    public static JsonElement processJons(String data) {
        JsonElement jElem = new Gson().fromJson(data, JsonElement.class);
        return jElem;
    }

    public static String sendRequest(URL url) throws IOException {
        // https://reqbin.com/req/java/5nqtoxbx/get-json-example
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
            throw new Error("Something went wrong while sending request -> " + url.toString());
        }
    }
}
