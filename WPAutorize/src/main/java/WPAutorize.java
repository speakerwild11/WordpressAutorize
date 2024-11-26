import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;

import java.util.ArrayList;
import java.util.List;

/*
TODO:
- GUI tweaking
- More advanced valid response detection

KNOWN ISSUES:
- Requests made in the mock repeater tab tend to hang/are very slow.
- Mock repeater tab text field needs formatting. Very messy on large HTML responses.
- Mock repeater tabs only open when pressing enter or moving tabs in table.
- Rare false positives (likely due to bad regex/nonces that do not contain numbers)
 */

public class WPAutorize implements BurpExtension, ProxyRequestHandler {

    MontoyaApi api;
    UserInterface ui;
    AccountHandler accountHandler;
    List<String> authedPageHistory;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        this.accountHandler = new AccountHandler(this.api);
        this.ui = new UserInterface(api, accountHandler);
        authedPageHistory = new ArrayList<>();

        api.extension().setName("Wordpress Autorize");
        api.proxy().registerRequestHandler(this);
        api.userInterface().registerSuiteTab("WP Autorize", ui.getUI());
        api.logging().logToOutput("Initialization Successful.");
    }

    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest toSend) {
        if(ui.isLoggingEnabled()){
            ThreadedRequests nonceRequests = new ThreadedRequests(api, accountHandler);
            HttpResponse baselineResponse = api.http().sendRequest(toSend).response();
            if(Utils.applicableRequest(toSend)) {
                if(Utils.sentDataInRequest(toSend) && Utils.requestHasNonce(toSend)) {
                    //Generate a request with no nonce parameters
                    nonceRequests.addRequest(Utils.removeFromRequest(toSend, Utils.getNonceParams(toSend)), "Valid response on absence of nonce params @ ");

                    //Generate a request with no nonces, but with nonce parameters
                    nonceRequests.addRequest(Utils.removeFromRequest(toSend, Utils.getNonces(toSend)), "Valid response on absence of nonces @ ");

                    //Generate requests with an admin-level nonce on lower-privileged accounts
                    nonceRequests.forSession(toSend, " can use admin nonce @ ");
                }else if(Utils.sentDataInRequest(toSend)){
                    ui.logEvent(new Tuple<>(toSend, null), toSend.method(), "-", "Apparently nonceless POST-like request @ " + toSend.path());
                }else if (toSend.path().contains("wp-admin") && toSend.method().contains("GET")) {
                    //Generate HEAD requests for authed pages, blacklist authed pages from being relogged
                    if(!authedPageHistory.contains(toSend.path())){
                        authedPageHistory.add(toSend.path());
                        HttpRequest headRequest = toSend.withMethod("HEAD");
                        nonceRequests.forSession(headRequest, " can access authed page @ ");
                    }
                }
            }

            //Process all requests
            if(!nonceRequests.getRequests().isEmpty()){
                nonceRequests.doRequests();
                for(ThreadedRequest request : nonceRequests.getRequests()){
                    if(request.getResponse().statusCode() <= 399){
                        if(toSend.method().contains("GET")){
                            ui.logEvent(request.asTuple(), toSend.method(), String.valueOf(request.getResponse().statusCode()), request.getLogMessage() + toSend.path());
                        }else if(Utils.identicalResponse(request.getResponse(), baselineResponse)){
                            ui.logEvent(request.asTuple(), toSend.method(), String.valueOf(request.getResponse().statusCode()), request.getLogMessage() + toSend.path());
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest){return null;}
}
