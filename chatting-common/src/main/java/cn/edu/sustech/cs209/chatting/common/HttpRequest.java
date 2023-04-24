package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.Map;

public class HttpRequest implements Serializable {
    private String url;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> params;
    private String data;

    public HttpRequest() {
    }

    public HttpRequest(String url, String method, Map<String, String> headers, Map<String, String> params, String data) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.params = params;
        this.data = data;
    }



    @Override
    public String toString() {
        return "HttpRequest{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", headers=" + headers +
                ", params=" + params +
                ", data='" + data + '\'' +
                '}';
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
