import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumnModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UserInterface implements ActionListener{
    private JPanel ui;
    private JTable log;
    private JButton loadSessionsButton;
    private JTextField credentialPathField;
    private JButton chooseFileButton;
    private JLabel loadingLabel;
    private JButton clearButton;
    private JButton enableLogButton;
    private final JFileChooser fileChooser;
    private final ReversedTableModel logModel;
    private final MontoyaApi api;
    private final AccountHandler accountHandler;
    private Map<Integer, Tuple<HttpRequest, HttpResponse>> logHistory;
    private boolean loggingEnabled;

    public UserInterface(MontoyaApi api, AccountHandler accountHandler) {
        logHistory = new HashMap<>();
        this.api = api;
        this.accountHandler = accountHandler;
        loggingEnabled = false;
        logModel = new ReversedTableModel(0, 3);
        log.setModel(logModel);

        //Add listener that opens http history on list click
        log.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                Tuple<HttpRequest, HttpResponse> httpHistory = logHistory.get(log.getSelectedRow());
                new RequestResponseInterface(api, httpHistory.x, httpHistory.y);
            }
        });

        //Define sizes and labels of each column
        TableColumnModel model = log.getColumnModel();
        int index = 0;
        model.getColumn(0).setHeaderValue("Method");
        model.getColumn(1).setHeaderValue("Code");
        model.getColumn(2).setHeaderValue("Message");
        while (index <= 1) {
            model.getColumn(index).setMinWidth(80);
            model.getColumn(index).setMaxWidth(80);
            model.getColumn(index).setPreferredWidth(80);
            index++;
        }

        fileChooser = new JFileChooser();

        chooseFileButton.addActionListener(this);
        loadSessionsButton.addActionListener(this);
        clearButton.addActionListener(this);
        enableLogButton.setActionCommand("Logging");
        enableLogButton.addActionListener(this);
    }

    //Handle all GUI input such as button presses, etc.
    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        switch(action){
            case "Choose File":
                int option = fileChooser.showOpenDialog(null);
                if (option == JFileChooser.APPROVE_OPTION) {
                    String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                    credentialPathField.setText(filePath);
                }
                break;
            case "Load Sessions":
                loadSessions();
            case "Clear":
                logHistory = new HashMap<>();
                logModel.setRowCount(0);
                break;
            case "Logging":
                if(loggingEnabled){
                    enableLogButton.setText("Logging: Off");
                    loggingEnabled = false;
                }else{
                    enableLogButton.setText("Logging: On");
                    loggingEnabled = true;
                }
        }
    }

    public boolean isLoggingEnabled(){
        return loggingEnabled;
    }

    //Load all sessions from the CSV path provided. Needs to be threaded, otherwise
    //Burp will throw and exception.
    public void loadSessions() {
        Thread thread = new Thread(() -> {
            loadingLabel.setText("Loading...");
            try {
                String path = credentialPathField.getText();
                accountHandler.doLoginsFromFile(path);
                loadingLabel.setText("Success!");
                TimeUnit.SECONDS.sleep(2);
                loadingLabel.setText("");
            } catch (Exception e) {
                loadingLabel.setText("Error: See Extensions > Wordpress Autorize > Errors");
                api.logging().logToOutput(e.toString());
            }
        });
        thread.start();
    }

    //Log a finding in the log window
    public void logEvent(Tuple<HttpRequest, HttpResponse> httpHistory, String method, String code, String description){
        logModel.addRow(new String[]{method, code, description});
        logHistory.put(logModel.getRowCount(), httpHistory);
    }

    public JPanel getUI() {
        return this.ui;
    }
}

