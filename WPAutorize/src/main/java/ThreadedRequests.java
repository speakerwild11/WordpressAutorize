import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThreadedRequests {

    MontoyaApi api;
    AccountHandler accountHandler;
    List<Thread> pool;
    List<ThreadedRequest> requests;

    public ThreadedRequests(MontoyaApi api, AccountHandler accountHandler){
        this.api = api;
        this.accountHandler = accountHandler;
        pool = new ArrayList<>();
        requests = new ArrayList<>();
    }

    public void addRequest(HttpRequest request, String logMessage){
        ThreadedRequest threadedRequest = new ThreadedRequest(request, logMessage, api);
        requests.add(threadedRequest);
        pool.add(new Thread(threadedRequest));
    }

    public void forSession(HttpRequest request, String logMessage){
        Map<String, String> sessions = accountHandler.getSessions();
        for(String accountName : sessions.keySet()){
            HttpRequest tempReq = request;
            tempReq = tempReq.withRemovedHeader("Cookie");
            tempReq = tempReq.withAddedHeader("Cookie", sessions.get(accountName));
            addRequest(tempReq, accountName + logMessage);
        }
    }

    public void doRequests(){
        for(Thread thread : pool){thread.start();}
        for(Thread thread : pool){
            try{
                thread.join();
            }catch(InterruptedException e){
                api.logging().logToError(e.toString());
            }
        }
    }

    public List<ThreadedRequest> getRequests(){
        return requests;
    }
}
