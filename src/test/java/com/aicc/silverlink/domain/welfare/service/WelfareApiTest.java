package com.aicc.silverlink.domain.welfare.service;

import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class WelfareApiTest {

    @Test
    public void testApi() throws Exception {
        // .env에 있는 키 (Hex)
        String serviceKey = "7bf39356670fec9de7836185dabad32aefbbf1dabeedbb42008d6ec216d96a5";

        // 1. 디코딩된 키(원본)라고 가정하고 그대로 사용
        // UriComponentsBuilder는 queryParam 사용시 인코딩하므로, 여기서도 URLEncoder.encode 사용해봄

        StringBuilder urlBuilder = new StringBuilder(
                "https://apis.data.go.kr/B554287/NationalWelfareInformationsV001/NationalWelfarelistV001");
        urlBuilder.append("?" + URLEncoder.encode("serviceKey", "UTF-8") + "=" + serviceKey);
        // Hex 키는 encode해도 그대로임.

        urlBuilder.append("&" + URLEncoder.encode("numOfRows", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("pageNo", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("lifeArray", "UTF-8") + "=" + URLEncoder.encode("006", "UTF-8")); // 노년
        urlBuilder.append("&" + URLEncoder.encode("callTp", "UTF-8") + "=" + URLEncoder.encode("L", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("srchKeyCode", "UTF-8") + "=" + URLEncoder.encode("001", "UTF-8"));

        System.out.println("Request URL: " + urlBuilder.toString());

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");
        // Note: Public Data Portal usually returns XML unless header Accept:
        // application/json or _type=json param is sent.
        // We want to see DEFAULT response (XML).

        System.out.println("Response code: " + conn.getResponseCode());

        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();
        System.out.println("Response Body:");
        System.out.println(sb.toString());
    }
}
