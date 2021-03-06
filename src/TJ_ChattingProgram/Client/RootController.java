package TJ_ChattingProgram.Client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.ResourceBundle;

/**
* @class : RootController.java
* @modifyed : 2021-02-27 오후 7:39
* @usage : 클라이언트 컨트롤러.
**/
public class RootController implements Initializable {
    @FXML private TextField textField;
    @FXML private TextFlow textFlow;
    @FXML private Button btnSendMessage;
    @FXML private Button btnConnect;

    Socket socket;

    // connect , read , write 등 모두 UI 스레드가 아닌 작업스레드가 담당하는게 좋다.
    public void logIn(){
        Thread thread = new Thread(()->{
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress("192.168.10.104",5001));
                Platform.runLater(()-> {
                    clearTextBoard();
                    printTextBoard("서버에 접속했습니다. " + new Date());
                    printTextBoard("Server IP : " + socket.getRemoteSocketAddress());
                    printTextBoard("--------------------------------------------------------------------");
                    btnConnect.setText("Disconnect");
                });

                btnSendMessage.setDisable(false);
                receive();
            } catch (IOException exception) {
                exception.printStackTrace();
                Platform.runLater(()-> printTextBoard(Thread.currentThread().getName() + " : [서버에 접속할 수 없습니다.]"));
                if(!socket.isClosed()){exitServer();}
            }
        });
        thread.setDaemon(true);
        thread.setName("LogIn Thread");
        thread.start();
    }

    /**
    * @class : RootController.java
    * @modifyed : 2021-02-26 오전 1:56
    * @usage : 루프를 돌며 서버로부터 오는 데이터를 계속 받는다.
    **/
/*    public void receive() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        InputStream is = socket.getInputStream();
                        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                        char[] data = new char[100];

                        // 소켓이 서버에 의해 정상적으로 close 되었을 경우.
                        if (isr.read(data) == -1) {
                            Platform.runLater(() -> {
                                printTextArea(Thread.currentThread().getName() + " : 서버에 의해 연결이 끊어졌습니다.");
                                if (socket != null && !socket.isClosed()) {
                                    exitServer();
                                }
                            });
                            break;
                        }

                        Platform.runLater(() -> {
                            printTextArea(new String(data));
                        });
                    } catch (Exception e) {
                        // 클라이언트쪽에서 Disconnect 했을 경우
                        if (socket.isClosed()) {
                            Platform.runLater(() -> printTextArea("[연결이 끊어졌습니다]"));
                            break;
                        } else { // 그 밖에 예외가 발생한 경우( 에러 )
                            System.out.println("[에러 발생]");
                            System.out.println(e.getMessage());
                            exitServer();
                            break;
                        }
                    }
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
        thread.setName("MessageReceiving Thread");
    }*/


    public void receive() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        InputStream is = socket.getInputStream();
                        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                        char[] data = new char[100];

                        // 소켓이 서버에 의해 정상적으로 close 되었을 경우.
                        if (isr.read(data) == -1) {
                            Platform.runLater(() -> {
                                printTextBoard(Thread.currentThread().getName() + " : 서버에 의해 연결이 끊어졌습니다.");
                                if (socket != null && !socket.isClosed()) {
                                    exitServer();
                                }
                            });
                            break;
                        }

                        // #msg:color:0x0000ffff 형식으로 보내져올 것.
                        // index보다는 split을 활용하는게  시각적으로 더 좋겠다.
                        String strData = new String(data);
                        if(strData.startsWith("##msg-color:")){
                            String textColor = extractColorPart(strData);
                            String message = extractMessagePart(strData);

                            System.out.println("color part : " + textColor);
                            System.out.println("message part : " + message);

                            Platform.runLater(()->{
                                printColorTextBoard(message,textColor);
                            });
                        } else {
                            Platform.runLater(() -> {
                                printTextBoard(strData);
                            });
                        }
                    }
                } catch (Exception e) {
                    // 클라이언트쪽에서 Disconnect 했을 경우
                    if (socket.isClosed()) {
                        Platform.runLater(() -> printTextBoard("[연결이 끊어졌습니다]"));
                    } else { // 그 밖에 예외가 발생한 경우( 에러 )
                        System.out.println("[에러 발생]");
                        e.printStackTrace();
                        exitServer();
                    }
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
        thread.setName("MessageReceiving Thread");
    }

    // UI 관련작업 외에는 작업스레드에게 일을 시키자.
    public void exitServer() {
        Thread thread = new Thread(()->{
            try {
                Platform.runLater(() -> {
                    printTextBoard("[연결 종료..]");
                    btnSendMessage.setDisable(true);
                    btnConnect.setText("Connect");
                    printTextBoard("Disconnected..");
                });
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.setName("ExitServer Thread");
        thread.start();
    }


    public void printTextBoard(String message){
        Text text = new Text(message + System.lineSeparator());
        this.textFlow.getChildren().add(text);
    }

    public void printColorTextBoard(String message, Color color){
        Text text = new Text(message + System.lineSeparator());
        String textColor = color.toString().substring(2);
        text.setStyle("-fx-fill: #" + textColor + ";-fx-font-weight: bold;");
        this.textFlow.getChildren().add(text);
    }

    public void printColorTextBoard(String message, String color){
        Text text = new Text(message + System.lineSeparator());
        String textColor = color.substring(2);
        text.setStyle("-fx-fill: #" + textColor + ";");
        System.out.println("style:" + "-fx-fill: #" + textColor + ";");
        this.textFlow.getChildren().add(text);
    }

    public void clearTextBoard(){
        this.textFlow.getChildren().clear();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        btnSendMessage.setOnAction(this::handleBtnSendMessageAction);
        btnSendMessage.setDisable(true);
        btnConnect.setText("Connect");
        btnConnect.setOnAction(this::handleBtnConnectAction);
        printTextBoard("Disconnected..");
    }

    public void handleBtnConnectAction(ActionEvent event){
        switch (btnConnect.getText()){
            case "Connect" :
                logIn();
                break;
            case "Disconnect":
                exitServer();
                break;
            default:
                printTextBoard("Wrong ConnectBtn Name");
                break;
        }
    }
    public void handleBtnSendMessageAction(ActionEvent event){
       send(textField.getText());
       textField.setText("");
    }

    void send(String message){
        Thread thread = new Thread(()->{
            try{
                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                osw.write(message);
                osw.flush();
            }catch (Exception e){
                e.printStackTrace();
                Platform.runLater(()-> printTextBoard(Thread.currentThread().getName() + " : [서버 통신 오류]"));
                if(!socket.isClosed()) { exitServer(); }
            }
        });
        thread.setDaemon(true);
        thread.setName("MessageSending Thread");
        thread.start();
    }


    /**
    * @class : RootController.java
    * @modifyed : 2021-03-01 오전 2:19
    * @usage : Methods to extract colorPart/MessagePart of Receiving Data.
    **/
    public String extractMessagePart(String str){
        String[] strs = str.split("##");
        StringBuilder sb = new StringBuilder();
        if(strs.length > 2){
            for(int i = 2; i < strs.length; i++){
                sb.append(strs[i]);
            }
        }
        return sb.toString();
    }
    public String extractColorPart(String str){
        String[] strs = str.split("##");
        return strs[1].split(":")[1];
    }
}
