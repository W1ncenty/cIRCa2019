/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.controller;

import irc.IRC;
import irc.model.Chanel;
import irc.utils.Utils;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Weronika
 */
public class MainViewController implements Initializable {

    @FXML
    private Label connectionLabel;

    @FXML
    private TextField nicknameTextArea;

    @FXML
    private Button joinButton;

    private irc.IRC irc;
    @FXML
    private TextField ipField;
    @FXML
    private TextField portField;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.connectionLabel.setText("Disconnected");

        Utils.addTextLimiter(nicknameTextArea, 18);
        Utils.addTextLimiter(ipField, 15);

    }

    @FXML
    private void connect(ActionEvent event) {

        if (irc.getUser().getUsername() == null && !nicknameTextArea.getText().isEmpty() && !ipField.getText().isEmpty() && !portField.getText().isEmpty()) {
            bindUser();
            irc.getChatRoomController().displayChatroomList();

            try {
                Socket socket = new Socket(ipField.getText(), Integer.parseInt(portField.getText()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                irc.setSocket(socket);
                irc.setWriter(writer);

                System.out.println("Connected: " + socket);

                //Sprawdz dostepnosc nickname na serwerze
                irc.getUser().setUsername(nicknameTextArea.getText());
                irc.getUser().setConnected(true);

                if (irc.getSocket() != null) {
                    //#0%Wojtek$
                    irc.getWriter().println("#0%" + irc.getUser().getUsername()+"$");
                    irc.getChatRoomController().getWaitingForMessagesRunnable().setStopped(false);
                    irc.getChatRoomController().displayMessages();

                }

                this.connectionLabel.setTextFill(Color.web("00ff00"));
            } catch (IOException ex) {
                Logger.getLogger(MainViewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    @FXML
    private void disconnect(ActionEvent event) {

        if (irc.getUser().getConnected().get()) {
            

            irc.getChatRoomController().getWaitingForMessagesRunnable().setStopped(true);

            //napisz do servera wychodze ze szystkich pokoi 
            irc.getUser().getChanels().forEach(x ->
                    irc.getWriter().println("#3%;" + x.getChanelName()+"$")
            );
            
            this.connectionLabel.setTextFill(Color.web("ff0000"));
            try {
                irc.getSocket().close();
            } catch (IOException ex) {
                Logger.getLogger(MainViewController.class.getName()).log(Level.SEVERE, null, ex);
            }

            irc.getUser().setUsername(null);
            irc.getUser().setConnected(false);

        }

    }

    public IRC getIrc() {
        return irc;
    }

    public void setIrc(IRC irc) {
        this.irc = irc;

    }

    public void bindUser() {
        this.connectionLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            if (irc.getUser().getConnected().get()) {
                return "Connected";
            }
            return "Disconnected";
        }, irc.getUser().getConnected()));
    }

    @FXML
    private void join(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(irc.getClass().getResource("view/JoinChanels.fxml"));
            AnchorPane chooseChanel = loader.load();

            Stage chanelsStage = new Stage();
            chanelsStage.setTitle("Choose chanel");
            chanelsStage.initModality(Modality.WINDOW_MODAL);
            chanelsStage.initOwner(irc.getStage());

            Scene scene = new Scene(chooseChanel);
            chanelsStage.setScene(scene);

            JoinChanelsController controller = loader.getController();
            controller.setStage(chanelsStage);
            controller.setIrc(irc);

            chanelsStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void leaveChatroom(ActionEvent event) {
        if (irc.getUser().getConnected().get() && irc.getChatRoomController().getActiveChanel() != null) {
            
            Chanel active = irc.getChatRoomController().getActiveChanel();

            //#3%chanelname$ - wyjcie z pokoju o id 25
            irc.getWriter().println("#3%;" + active.getChanelName()+"$");
            
        }

    }

    @FXML
    private void createChatroom(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(irc.getClass().getResource("view/CreateChanel.fxml"));
            AnchorPane createChanel = loader.load();

            Stage chanelsStage = new Stage();
            chanelsStage.setTitle("Create chanel");
            chanelsStage.initModality(Modality.WINDOW_MODAL);
            chanelsStage.initOwner(irc.getStage());

            Scene scene = new Scene(createChanel);
            chanelsStage.setScene(scene);

            CreateChanelController controller = loader.getController();
            controller.setStage(chanelsStage);
            controller.setIrc(irc);

            chanelsStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
