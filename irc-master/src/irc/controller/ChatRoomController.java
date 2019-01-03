/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.controller;

import irc.IRC;
import irc.model.Chanel;
import irc.model.Message;
import irc.model.User;
import irc.model.WaitingForMessages;
import irc.utils.Utils;
import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * FXML Controller class
 *
 * @author Weronika
 */
public class ChatRoomController implements Initializable {

    @FXML
    private TextArea messageTextArea;
    @FXML
    private Button sendButton;
    @FXML
    private ListView<Chanel> chatRoomList;
    @FXML
    private TextArea allMessages;
    @FXML
    private ListView<User> userList;

    private irc.IRC irc;
    private WaitingForMessages waitingForMessagesRunnable;
    private Chanel activeChanel;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        chatRoomList.setCellFactory(param -> new ListCell<Chanel>() {
            @Override
            protected void updateItem(Chanel item, boolean empty) {
                if (item != null) {
                    super.updateItem(item, empty);

                    if (empty || item == null || item.getChanelName() == null) {
                        setText(null);
                    } else {
                        setText(item.getChanelName());
                    }
                }

            }
        });
        
        
        userList.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                if (item != null) {
                    super.updateItem(item, empty);

                    if (empty || item == null || item.getUsername()== null) {
                        setText(null);
                    } else {
                        setText(item.getUsername());
                    }
                }

            }
        });
        allMessages.textProperty().addListener((observable, oldValue, newValue) -> {
            allMessages.setText(newValue);
        });

        chatRoomList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Chanel>() {

            @Override
            public void changed(ObservableValue<? extends Chanel> observable, Chanel oldValue, Chanel newValue) {
                allMessages.clear();
                if (newValue != null) {
                    activeChanel = newValue;
                    activeChanel.getMessages().forEach(message -> {
                        if (message.getChanelName().equals(activeChanel.getChanelName())) {
                            allMessages.appendText(message.formatMessage() + "\n");
                        }
                    displayUserList();
                    });
                    

                }
            }
        });

    }

    public IRC getIrc() {
        return irc;
    }

    public void setIrc(IRC irc) {
        this.irc = irc;
    }

    @FXML
    private void send(ActionEvent event) {
        sendFunction();

    }

    @FXML
    private void sendOnEnterPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            event.consume();
            if (event.isShiftDown()) {
                messageTextArea.appendText(System.getProperty("line.separator"));
            } else {
                if (!messageTextArea.getText().isEmpty()) {
                    sendFunction();
                }
            }

        }
    }

    public void displayMessages() {
        this.waitingForMessagesRunnable = new WaitingForMessages(this.irc);

        Thread t = new Thread(this.waitingForMessagesRunnable);
        t.start();

    }

    public void updateMessage(String messege) {
        allMessages.appendText(messege);
    }

    public void displayChatroomList() {
        chatRoomList.setItems(irc.getUser().getChanels());
    }

    public void displayUserList() {
        userList.setItems(activeChanel.getUsers());
    }

    private void sendFunction() {
        if (irc.getUser().getConnected().get() && activeChanel!=null) {
            String time = DateTimeFormatter.ofPattern("hh:mm:ss").format(ZonedDateTime.now());

            //4 to wysłanie wiadomości:
            Message message = new Message(activeChanel.getChanelName(), irc.getUser().getUsername(), time, messageTextArea.getText());
            irc.getWriter().println(message.toString());

            activeChanel.getMessages().add(message);

            this.allMessages.appendText(message.formatMessage());

            this.messageTextArea.clear();

        }

    }

    public WaitingForMessages getWaitingForMessagesRunnable() {
        return waitingForMessagesRunnable;
    }

    public Chanel getActiveChanel() {
        return activeChanel;
    }

    public void setActiveChanel(Chanel activeChanel) {
        this.activeChanel = activeChanel;
    }

    public ListView<Chanel> getChatRoomList() {
        return chatRoomList;
    }

    public ListView<User> getUserList() {
        return userList;
    }
    
    

}
