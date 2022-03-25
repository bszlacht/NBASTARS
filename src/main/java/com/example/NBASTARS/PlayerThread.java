package com.example.NBASTARS;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URL;

import static com.example.NBASTARS.HomeController.*;

public class PlayerThread extends Thread{

    public String getPts() {
        return pts;
    }

    public String getAst() {
        return ast;
    }

    public String getReb() {
        return reb;
    }

    private String pts;
    private String ast;
    private String reb;
    private String GETPLAYERSSTATS;
    private final String id;


    public PlayerThread(String id, String url){
        this.id = id;
        this.GETPLAYERSSTATS = url;
    }
    public void run() {
        URL urlGetStats = null;
        try {
            urlGetStats = new URL(String.format(GETPLAYERSSTATS + id));
            JsonArray statsArr = processJons(sendRequest(urlGetStats)).getAsJsonObject().get("data").getAsJsonArray();
            JsonObject statsObj = statsArr.get(0).getAsJsonObject();
            pts = String.valueOf(statsObj.get("pts"));
            ast = String.valueOf(statsObj.get("ast"));
            reb = String.valueOf(statsObj.get("reb"));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Player Thread Error");
        }
    }

}
