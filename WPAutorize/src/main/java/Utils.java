import burp.api.montoya.http.Http;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private final static Pattern noncePattern = Pattern.compile("(?<=(\\R|['\"=]|: ))[a-z0-9]{10}(?=['\"&]|\\R|$)");

    //Determine if the request method can send data or not
    public static boolean sentDataInRequest(HttpRequest request){
        String[] goodMethods = {"POST", "PATCH", "PUT", "UPDATE"};
        for(String method : goodMethods){if(request.method().contains(method)){return true;}}
        return false;
    }

    //Determine if request issued to handleRequestToBeSent should be analyzed
    public static boolean applicableRequest(HttpRequest request){

        if(request.bodyToString().contains("action=heartbeat")){
            return false;
        }

        String fileExt = request.path().split("\\.")[request.path().split("\\.").length-1];
        return fileExt.contains("php");
    }

    public static boolean requestHasNonce(HttpRequest request){
        return !getNonce(request.toString()).isBlank();
    }

    //Check if response has header by name
    public static boolean hasHeader(HttpResponse response, String headerName){
        for(HttpHeader header : response.headers()){
            if(header.name().contains(headerName)){
                return true;
            }
        }
        return false;
    }

    private static boolean linesMatch(String line1, String line2){
        return line1.equals(line2);
    }

    public static boolean identicalResponse(HttpResponse response1, HttpResponse response2){
        if(response1.statusCode() == response2.statusCode()){
            if(response1.hasHeader("Location") || response2.hasHeader("Location")){
                return getHeader(response1, "Location").equals(getHeader(response2, "Location"));
            }
            return response1.bodyToString().equals(response2.bodyToString());
        }
        return false;
    }

    //Get header by name from response
    public static String getHeader(HttpResponse response, String headerName){
        for(HttpHeader header : response.headers()){
            if(header.name().contains(headerName)){
                return header.value();
            }
        }
        return "";
    }

    //If a string contains a lowercase letter, a number, and no special chars or uppercase letters
    public static boolean isNonce(String string){
        boolean hasSpecialChars = false, hasNumber = false, hasCharacter = false;
        for(char chr : string.toCharArray()){
            if(Character.isDigit(chr)){
                hasNumber = true;
                break;
            }
        }
        for(char chr : string.toCharArray()){
            if(Character.isLowerCase(chr)){
                hasCharacter = true;
                break;
            }
        }
        for(char chr : string.toCharArray()){
            if((!Character.isLetterOrDigit(chr)) || Character.isUpperCase(chr)){
                hasSpecialChars = true;
                break;
            }
        }
        return hasNumber && hasCharacter && (!hasSpecialChars);
    }

    //Get the first nonce in a string
    public static String getNonce(String toSearch){
        Matcher nonceMatcher = noncePattern.matcher(toSearch);
        if(nonceMatcher.find()){
            String nonce = nonceMatcher.group();
            if(isNonce(nonce)){
                return nonce;
            }
        }
        return "";
    }

    //Get all nonces in a request
    public static List<String> getNonces(HttpRequest toSearch){
        Matcher nonceMatcher = noncePattern.matcher(toSearch.toString());
        List<String> nonces = new ArrayList<>();
        while(nonceMatcher.find()){
            String nonce = nonceMatcher.group();
            if(isNonce(nonce)) {
                nonces.add(nonce);
            }
        }
        return nonces;
    }

    public static List<String> getNonceParams(HttpRequest toSearch){
        List<String> nonces = getNonces(toSearch);
        String boundary = getHeaderBoundary(toSearch);
        List<String> nonceParams = new ArrayList<>();
        for(String nonce : nonces) {
            String pattern;
            if (boundary.isEmpty()) {
                pattern = "[^&?/ \n\r\t]*" + nonce;
            } else {
                //Handles multipart/form request nonces
                pattern = "--" + boundary + "\\R.*\\R.*\\R.*" + nonce;
            }
            Matcher nonceParamMatcher = Pattern.compile(pattern).matcher(toSearch.toString());
            while(nonceParamMatcher.find()){
                nonceParams.add(nonceParamMatcher.group());
            }
        }
        return nonceParams;
    }

    public static String getHeaderBoundary(HttpRequest request){
        String boundary = "";
        for(HttpHeader header : request.headers()){
            if(header.value().contains("boundary")){
                boundary = header.value().split("=")[1];
                break;
            }
        }
        return boundary;
    }

    public static HttpRequest removeFromRequest(HttpRequest request, List<String> keywords){
        for(String keyword : keywords){
            request = request.withPath(request.path().replaceAll(keyword, ""));
            for (HttpHeader header : request.headers()) {
                request = request.withRemovedHeader(header.name());
                request = request.withAddedHeader(header.name(), header.value().replaceAll(keyword, ""));
            }
            request = request.withBody(request.bodyToString().replaceAll(keyword, ""));
        }
        return request;
    }
}
