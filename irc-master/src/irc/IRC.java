/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc;

import irc.controller.ChatRoomController;
import irc.controller.MainViewController;
import irc.model.Chanel;
import irc.model.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * 
 * 
 *
 * @author Weronika
 * Main class
 */
public class IRC extends Application {

    private Stage stage;
    private BorderPane root;
    
    private ChatRoomController chatRoomController;
    
    private User user = new User(false);
    public static ObservableList<Chanel> allChanels = FXCollections.observableArrayList();
    
    private Socket socket;
    
    private PrintWriter writer;
            
    @Override
    public void start(Stage stage) throws Exception {

        this.stage = stage;
        this.stage.setTitle("IRC");

        initRootLayout();
        showChatRoomView();

    }

    public void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("view/MainView.fxml"));
            root = loader.load();

            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.show();
            
            MainViewController mainViewController = loader.getController();
            mainViewController.setIrc(this);

        } catch (IOException ex) {
            Logger.getLogger(IRC.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void showChatRoomView() {
        try {

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("view/ChatRoom.fxml"));
            AnchorPane mainPage = loader.load();

            root.setCenter(mainPage);
            
            
            this.chatRoomController =  loader.getController();
            this.chatRoomController.setIrc(this);
                    

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }


    public PrintWriter getWriter() {
        return writer;
    }

    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public ChatRoomController getChatRoomController() {
        return chatRoomController;
    }

    public BorderPane getRoot() {
        return root;
    }

    public void setRoot(BorderPane root) {
        this.root = root;
    }

    public ObservableList<Chanel> getAllChanels() {
        return allChanels;
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
        
    }

}
