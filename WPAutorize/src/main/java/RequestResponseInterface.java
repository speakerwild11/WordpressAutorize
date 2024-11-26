import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestResponseInterface implements ActionListener {
    private JButton repeatButton;
    private JTextArea requestTextArea;
    private JTextArea responseTextArea;
    private JPanel mainPanel;
    private JScrollPane requestScrollPanel;
    private JScrollPane responseScrollPanel;
    private MontoyaApi api;
    private HttpRequest currentRequest;
    private HttpResponse currentResponse;

    public RequestResponseInterface(MontoyaApi api, HttpRequest initialRequest, HttpResponse initialResponse){
        this.api = api;
        currentRequest = initialRequest;
        currentResponse = initialResponse;
        requestTextArea.setLineWrap(true);
        responseTextArea.setLineWrap(true);
        requestTextArea.setText(initialRequest.toString());
        responseTextArea.setText(initialResponse.toString());
        repeatButton.addActionListener(this);
        JFrame frame = new JFrame("Proxy History Window");
        frame.setPreferredSize(new Dimension(650, 625));
        frame.setResizable(false);
        frame.setContentPane(mainPanel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    //Parse a Montoya HttpRequest from a string
    public HttpRequest parseRequest(String requestString){
        HttpRequest request = currentRequest.withBody("");
        request = request.withRemovedHeaders(request.headers());
        request = request.withPath("");
        List<String> lines = new ArrayList<>(requestString.lines().toList());
        String[] pathInfo = lines.remove(0).split(" ");
        request = request.withMethod(pathInfo[0]);
        request = request.withPath(pathInfo[1]);

        for(String line : lines){
            if(line.contains(": ") && !line.contains("Content-Disposition")){
                String[] header = line.split(": ");
                request = request.withAddedHeader(header[0], header[1]);
            }else{
                request = request.withBody(request.bodyToString() + line + "\r\n");
            }
        }
        return request;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("Repeat")){
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    responseTextArea.setText("");
                    HttpRequest parsedRequest = parseRequest(requestTextArea.getText());
                    currentRequest = parsedRequest;
                    HttpResponse response = api.http().sendRequest(parsedRequest).response();
                    responseTextArea.setText(response.toString());
                    currentResponse = response;
                }
            };
            new Thread(runnable).start();
        }
    }
}
