package com.netease.iot.rule.proxy.util;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.AbstractHttpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class HttpClientHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientHelper.class);

    private static final String CHARSET_DEFAULT = "UTF-8";

    private static final ContentType APPLICATION_FORM_URLENCODED = ContentType
            .create("application/x-www-form-urlencoded", Charset.forName(CHARSET_DEFAULT));

    private HttpClient httpClient;

    public String get(String serverAddress, Map<String, Object> params) {
        return get(serverAddress, params, null, false);
    }

    public String get(String serverAddress, Map<String, Object> params, Map<String, String> headers,
                      boolean ignoreStatus) {
        String serverAddressWithParam = buildServerAddressWithParams(serverAddress, params);
        return getInternal(serverAddressWithParam, headers, ignoreStatus);
    }

    private String getInternal(String serverAddress, Map<String, String> headers, boolean ignoreStatus) {
        HttpGet request = new HttpGet(serverAddress);
        addHeaders(request, headers);
        return execute(request, ignoreStatus);
    }

    public String postForm(String serverAddress, Map<String, Object> params) {
        return postForm(serverAddress, params, null, false);
    }

    public String postForm(String serverAddress, Map<String, Object> params, Map<String, String> headers,
                           boolean ignoreStatus) {
        return postInternal(serverAddress, APPLICATION_FORM_URLENCODED, getParamString(params), headers, ignoreStatus);
    }

    public String postJson(String serverAddress, Object body) {
        return postJson(serverAddress, body, null, false);
    }

    public String postJson(String serverAddress, Object body, Map<String, String> headers, boolean ignoreStatus) {
        return postInternal(serverAddress, ContentType.APPLICATION_JSON, JSON.toJSONString(body), headers,
                ignoreStatus);
    }

    private String postInternal(String serverAddress, ContentType contentType, String body, Map<String, String> headers,
                                boolean ignoreStatus) {
        HttpPost httpPost = new HttpPost(serverAddress);
        setEntity(httpPost, contentType, body);
        addHeaders(httpPost, headers);
        return execute(httpPost, ignoreStatus);
    }

    public String putForm(String serverAddress, Map<String, Object> params) {
        return putForm(serverAddress, params, null, false);
    }

    public String putForm(String serverAddress, Map<String, Object> params, Map<String, String> headers,
                          boolean ignoreStatus) {
        return putInternal(serverAddress, APPLICATION_FORM_URLENCODED, getParamString(params), headers, ignoreStatus);
    }

    public String putJson(String serverAddress, Object body) {
        return putJson(serverAddress, body, null, false);
    }

    public String putJson(String serverAddress, Object body, Map<String, String> headers, boolean ignoreStatus) {
        return putInternal(serverAddress, ContentType.APPLICATION_JSON, JSON.toJSONString(body), headers, ignoreStatus);
    }

    private String putInternal(String serverAddress, ContentType contentType, String body, Map<String, String> headers,
                               boolean ignoreStatus) {
        HttpPut request = new HttpPut(serverAddress);
        setEntity(request, contentType, body);
        addHeaders(request, headers);
        return execute(request, ignoreStatus);
    }

    public String patchForm(String serverAddress, Map<String, Object> params) {
        return patchForm(serverAddress, params, null, false);
    }

    public String patchForm(String serverAddress, Map<String, Object> params, Map<String, String> headers,
                            boolean ignoreStatus) {
        return patchInternal(serverAddress, APPLICATION_FORM_URLENCODED, getParamString(params), headers, ignoreStatus);
    }

    public String patchJson(String serverAddress, Object body) {
        return patchJson(serverAddress, body, null, false);
    }

    public String patchJson(String serverAddress, Object body, Map<String, String> headers, boolean ignoreStatus) {
        return patchInternal(serverAddress, ContentType.APPLICATION_JSON, JSON.toJSONString(body), headers,
                ignoreStatus);
    }

    private String patchInternal(String serverAddress, ContentType contentType, String body,
                                 Map<String, String> headers, boolean ignoreStatus) {
        HttpPatch request = new HttpPatch(serverAddress);
        setEntity(request, contentType, body);
        addHeaders(request, headers);
        return execute(request, ignoreStatus);
    }

    public String delete(String serverAddress, Map<String, Object> params) {
        return delete(serverAddress, params, null, false);
    }

    public String delete(String serverAddress, Map<String, Object> params, Map<String, String> headers,
                         boolean ignoreStatus) {
        String serverAddressWithParam = buildServerAddressWithParams(serverAddress, params);
        return deleteInternal(serverAddressWithParam, headers, ignoreStatus);
    }

    private String deleteInternal(String serverAddress, Map<String, String> headers, boolean ignoreStatus) {
        HttpDelete request = new HttpDelete(serverAddress);
        addHeaders(request, headers);
        return execute(request, ignoreStatus);
    }

    private String buildServerAddressWithParams(String serverAddress, Map<String, Object> params) {
        StringBuilder serverAddressWithParam = new StringBuilder(serverAddress);
        if (params != null && !params.isEmpty()) {
            if (serverAddress.indexOf('?') == -1) {
                serverAddressWithParam.append('?').append(getParamString(params));
            } else if (serverAddress.endsWith("&")) {
                serverAddressWithParam.append(getParamString(params));
            } else {
                serverAddressWithParam.append('&').append(getParamString(params));
            }
        }
        return serverAddressWithParam.toString();
    }

    private String getParamString(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return StringUtils.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        for (Entry<String, Object> entry : params.entrySet()) {
            if (StringUtils.isBlank(entry.getKey())) {
                continue;
            }
            try {
                if (entry.getValue() instanceof List) {
                    builder.append(getListParamString(entry.getKey(), (List<?>) entry.getValue()));
                } else {
                    builder.append(getSingleParamString(entry.getKey(), entry.getValue()));
                }
            } catch (UnsupportedEncodingException e) {
                LOGGER.error("[err:getParamString]", e);
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    private String getListParamString(String key, List<?> values) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            builder.append(getSingleParamString(key, value));
        }
        return builder.toString();
    }

    private String getSingleParamString(String key, Object value) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        builder.append(URLEncoder.encode(key.trim(), CHARSET_DEFAULT));
        builder.append("=");
        if (value != null) {
            builder.append(URLEncoder.encode(value.toString().trim(), CHARSET_DEFAULT));
        }
        builder.append("&");
        return builder.toString();
    }

    private void addHeaders(AbstractHttpMessage message, Map<String, String> headers) {
        if (headers != null) {
            for (Entry<String, String> entry : headers.entrySet()) {
                message.setHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    private void setEntity(HttpEntityEnclosingRequestBase request, ContentType contentType, String body) {
        request.setEntity(new StringEntity(body, contentType));
    }

    private String execute(HttpRequestBase request, boolean ignoreStatus) {
        String req = toString(request);
        LOGGER.debug("[cmd:execute,request:{}]", req);
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request)) {
            int code = response.getStatusLine().getStatusCode();
            String content = readContent(response);
            if (ignoreStatus || isValid(code)) {
                LOGGER.debug("[cmd:execute,request:{},status:ok,code:{},content:{}]", req, code, content);
                return content;
            } else {
                LOGGER.error("[err:execute,request:{},status:bad response,code:{},content:{}]", req, code, content);
                throw new RuntimeException("execute error");
            }
        } catch (IOException e) {
            String msg = String.format("[err:execute,request:%s]", req);
            LOGGER.error(msg, e);
            throw new RuntimeException("execute error", e);
        }
    }

    private String toString(HttpRequestBase request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("method", request.getMethod());
        map.put("uri", request.getURI());
        map.put("headers", toString(request.getAllHeaders()));
        if (request instanceof HttpEntityEnclosingRequestBase) {
            HttpEntityEnclosingRequestBase entityRequest = (HttpEntityEnclosingRequestBase) request;
            map.put("entity", toString(entityRequest.getEntity()));
        }
        return JSON.toJSONString(map);
    }

    private Object toString(Header[] headers) {
        if (headers == null || headers.length == 0) {
            return StringUtils.EMPTY;
        }
        StringBuilder result = new StringBuilder();
        for (Header header : headers) {
            result.append(header.getName()).append("=").append(header.getValue()).append(",");
        }
        return result.deleteCharAt(result.length() - 1);
    }

    private String toString(HttpEntity entity) {
        if (entity != null) {
            try (InputStream is = entity.getContent()) {
                return IOUtils.toString(is, CHARSET_DEFAULT);
            } catch (UnsupportedOperationException | IOException e) {
                LOGGER.error("[cmd:toString]", e);
                throw new RuntimeException("execute error", e);
            }
        } else {
            return StringUtils.EMPTY;
        }
    }

    private String readContent(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return StringUtils.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), CHARSET_DEFAULT))) {
            String tmp = null;
            while ((tmp = reader.readLine()) != null) {
                builder.append(tmp).append(System.lineSeparator());
            }
            return builder.toString();
        }
    }

    private boolean isValid(int code) {
        return code == HttpStatus.SC_OK || code == HttpStatus.SC_CREATED || code == HttpStatus.SC_ACCEPTED
                || code == HttpStatus.SC_NO_CONTENT;
    }

    public void download(String serverAddress, Map<String, Object> params, File outputFile) {
        download(serverAddress, params, null, outputFile);
    }

    public void download(String serverAddress, Map<String, Object> params, Map<String, String> headers,
                         File outputFile) {
        String serverAddressWithParam = buildServerAddressWithParams(serverAddress, params);
        HttpGet httpGet = new HttpGet(serverAddressWithParam);
        addHeaders(httpGet, headers);
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(httpGet)) {
            int code = response.getStatusLine().getStatusCode();
            if (code == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    LOGGER.error("[err:httpDownload,url:{},params:{},file:{},msg:empty_response]", serverAddress,
                            params, outputFile.getAbsolutePath());
                    throw new RuntimeException("download error");
                }
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                try (InputStream is = entity.getContent(); OutputStream os = new FileOutputStream(outputFile)) {
                    IOUtils.copy(is, os);
                    os.flush();
                }
            } else if (code == HttpStatus.SC_MOVED_PERMANENTLY || code == HttpStatus.SC_MOVED_TEMPORARILY) {
                Header[] hs = response.getHeaders("Location");
                if (hs.length > 0) {
                    download(hs[0].getValue(), null, outputFile);
                } else {
                    LOGGER.error("[err:httpDownload,url:{},status:no location,code:{}]", serverAddress, code);
                    throw new RuntimeException("download error");
                }
            } else {
                LOGGER.error("[err:httpDownload,url:{},status:bad response,code:{}]", serverAddress, code);
                throw new RuntimeException("download error");
            }
        } catch (IOException e) {
            String msg = String.format("[err:httpDownload,url:%s,params:%s]", serverAddress, params);
            LOGGER.error(msg, e);
            throw new RuntimeException("execute error", e);
        }
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
