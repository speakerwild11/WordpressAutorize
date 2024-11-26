import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public class ThreadedRequest implements Runnable{

    HttpRequest request;
    HttpResponse response;
    MontoyaApi api;
    String logMessage;

    public ThreadedRequest(HttpRequest request, String logMessage, MontoyaApi api){
        this.request = request;
        this.logMessage = logMessage;
        this.api = api;
    }

    @Override
    public void run() {
        this.response = api.http().sendRequest(request).response();
    }

    public HttpRequest getRequest(){
        return request;
    }

    public HttpResponse getResponse(){
        return response;
    }

    public Tuple<HttpRequest, HttpResponse> asTuple(){
        return new Tuple<>(request, response);
    }

    public String getLogMessage(){
        return logMessage;
    }
}
