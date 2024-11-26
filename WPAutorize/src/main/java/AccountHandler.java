
/*

Handles the capturing of sessions from provided credentials, and performing
actions for each session.

 */

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountHandler {

    Map<String, String> sessions;
    MontoyaApi api;
    HttpRequest defaultLoginRequest;

    public AccountHandler(MontoyaApi api){
        this.sessions = new HashMap<>();
        this.api = api;
        HttpRequest loginRequest = HttpRequest.httpRequest();
        loginRequest = loginRequest.withMethod("POST");
        loginRequest = loginRequest.withService(HttpService.httpService("http://127.0.0.1/wp-login.php"));
        loginRequest = loginRequest.withPath("/wp-login.php");
        loginRequest = loginRequest.withAddedHeader(HttpHeader.httpHeader("Host", "127.0.0.1"));
        loginRequest = loginRequest.withAddedHeader(HttpHeader.httpHeader("Content-Type", "application/x-www-form-urlencoded"));
        loginRequest = loginRequest.withAddedHeader(HttpHeader.httpHeader("Origin", "http://127.0.0.1"));
        loginRequest = loginRequest.withAddedHeader(HttpHeader.httpHeader("Cookie", "wp-settings-4=unfold%3D0%26mfold%3Do; wp-settings-time-4=1729205590; pps_show_100=1; pps_show_101=1; pps_actions_101=_JSON%3A%7B%22subscribe%22%3A1%7D; pps_show_102=1; pps_actions_102=_JSON%3A%7B%22subscribe%22%3A1%7D; wp-settings-time-2=1730765934; wordpress_test_cookie=WP%20Cookie%20check; wp_lang=en_US"));
        this.defaultLoginRequest = loginRequest;
    }

    public Map<String, String> getSessions(){
        return sessions;
    }

    //Login to accounts stored in a csv file
    public void doLoginsFromFile(String path){
        Map<String, String> credentials = getCredsFromFile(path);
        doLogins(credentials);
    }

    //Get a string map of credentials from a csv file
    public Map<String, String> getCredsFromFile(String path){
        String fileExt = path.split("\\.")[path.split("\\.").length-1];
        if(fileExt.equals("csv")){
            try{
                Map<String, String> credentials = new HashMap<>();
                BufferedReader reader = new BufferedReader(new FileReader(path));
                String l = reader.readLine();
                List<String> lines = new ArrayList<>();
                while(l != null){
                    lines.add(l);
                    l = reader.readLine();
                }
                for(String line : lines){
                    String[] splitLine = line.split(",");
                    credentials.put(splitLine[0], splitLine[1]);
                }
                return credentials;
            }catch(IOException e){
                api.logging().logToError("ERROR: A fatal error has occurred while loading a CSV file:");
                api.logging().logToError(e.toString());
            }
        }else{
            api.logging().logToError("ERROR: Supplied file to getCredsFromFile() in CredentialHandler.java should be a csv!");
        }
        return null;
    }

    //Create a login job for threading
    public Runnable getLoginJob(String username, String password){
        return (() -> {
            String body = "log="+username+"&pwd="+password+"&rememberme=forever&wp-submit=Log+In&redirect_to=http%3A%2F%2F127.0.0.1%2Fwp-admin%2F&testcookie=1";
            HttpRequest accountRequest = defaultLoginRequest.withAddedHeader(HttpHeader.httpHeader("Content-Length", String.valueOf(body.length())));
            accountRequest = accountRequest.withBody(body);

            HttpResponse response = api.http().sendRequest(accountRequest).response();

            String cookies = "";
            for(Object header : response.headers().toArray()){
                HttpHeader responseHeader = (HttpHeader)header;
                if(responseHeader.name().contains("Set-Cookie")){
                    String cookie = responseHeader.value().split(" ")[0];
                    cookie = cookie.replace("\n", "");
                    cookies = cookies + " " + cookie;
                }
            }
            sessions.put(username, cookies);
        });
    }


    //For each credential supplied, perform a login request and save the session cookies
    public void doLogins(Map<String, String> credentials){
        sessions = new HashMap<>();

        //Create threaded login request, start and join
        List<Thread> threads = new ArrayList<>();
        for(String key : credentials.keySet()){threads.add(new Thread(getLoginJob(key, credentials.get(key))));}
        for(Thread thread : threads){thread.start();}
        for(Thread thread : threads){
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        api.logging().logToOutput("Successfully logged in to:");
        for(String accountName: sessions.keySet()){
            api.logging().logToOutput(accountName);
        }
    }
}
