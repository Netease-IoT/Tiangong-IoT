package com.netease.iot.rule.proxy.metadata;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryUtil.class);

    public static boolean isJobAlive(String baseUrl, String jobID) {

        final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        final HttpGet req = new HttpGet(baseUrl + "/jobs");
        CloseableHttpResponse resp = null;
        try {
            resp = httpClient.execute(req);
            HttpEntity respEntity = resp.getEntity();
            String text = EntityUtils.toString(respEntity);
            LOGGER.info("Query cluster " + baseUrl + ", got " + text);
            JSONObject obj = new JSONObject(text);
            JSONArray jobList = obj.getJSONArray("jobs-running");
            for (int i = 0; i < jobList.length(); ++i) {
                if (jobList.get(i).toString().equalsIgnoreCase(jobID)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.info("Query cluster " + baseUrl + " failed. ");
        } finally {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
                if (resp != null) {
                    resp.close();
                }
            } catch (Exception e) {
                LOGGER.info("Query cluster " + baseUrl + " failed. ");
            }
        }
        return false;
    }
}
