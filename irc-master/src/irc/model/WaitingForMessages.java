/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.model;

import irc.IRC;
import irc.controller.ChatRoomController;
import irc.utils.Utils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javafx.application.Platform;
import javax.sound.midi.Soundbank;

/**
 *
 * @author weronika
 */
public class WaitingForMessages implements Runnable {

    private static boolean stopped = false;

    private irc.IRC irc;
    private BufferedReader reader;

    public WaitingForMessages(IRC irc) {
        this.irc = irc;
    }

    public static boolean isStopped() {
        return stopped;
    }

    public static void setStopped(boolean stopped) {
        WaitingForMessages.stopped = stopped;
    }

    public Chanel findChanel(String chanelName) {
        Chanel result = null;
        for (Chanel c : irc.getAllChanels()) {
            if (chanelName.equals(c.getChanelName())) {
                result = c;
                break;
            }
        }
        return result;
    }

    public Chanel findMyChanel(String chanelName) {
        Chanel result = null;
        for (Chanel c : irc.getAllChanels()) {
            if (chanelName.equals(c.getChanelName())) {
                result = c;
                break;
            }
        }
        return result;

    }

    @Override
    public void run() {
        try {
            this.reader = new BufferedReader(new InputStreamReader(irc.getSocket().getInputStream()));

            while (!stopped) {
                Thread.sleep(1000);
                System.out.println("watek1");
                String serverMessage = this.reader.readLine();
                Platform.runLater(
                        () -> {
                            this.irc.getUser().getChanels().clear();
                            this.irc.getAllChanels().clear();
                            //this.irc.getChatRoomController().getAllMessages().setText(""); //bledy

                        }
                );

                if (serverMessage.startsWith("#") && serverMessage.endsWith("$")) {

                    System.out.println("watek11");

                    String[] rooms = serverMessage.substring(2, serverMessage.length() - 1).split("@");

                    for (int i = 0; i < rooms.length; i++) {
                        String[] string1 = rooms[i].split("%");
                        String chatroomName = string1[0];// - nazwa chatroomu

                        String[] users = string1[1].split(";");

                        boolean userInchanel = false;

                        Chanel chanel = new Chanel(chatroomName);

                        for (int j = 0; j < users.length; j++) {
                            String user = users[j];
                            chanel.getUsers().add(new User(user));
                            if (userInchanel == false && user.equals(this.irc.getUser().getUsername())) {
                                userInchanel = true;
                            }
                        }

                        String[] messages = string1[2].split(";");

                        for (int j = 0; j < messages.length; j++) {
                            if ((j + 1) % 3 == 0) {
                                Message newMessageObject = new Message(chanel.getChanelName(), messages[j - 2], messages[j - 1], messages[j]);
                                chanel.getMessages().add(newMessageObject);
                                if(j == 0){
                                    irc.getChatRoomController().getAllMessages().setText("");
                                }
                                //sprawdz
                                if (i == 0) {
                                    irc.getChatRoomController().updateMessage(newMessageObject.formatMessage());
                                }
                                

                            }
                        }

                        if (userInchanel) {
                            Platform.runLater(
                                    () -> {
                                        this.irc.getUser().getChanels().add(chanel);
                                        this.irc.getChatRoomController().displayChatroomList();
                                    }
                            );
                        }

                        Platform.runLater(
                                () -> {
                                    // Update UI here.
                                    this.irc.getAllChanels().add(chanel);
                                }
                        );

                    }

                } else {
                    System.out.println("BUBEL");
                }

            }
            System.out.println("stoppend");
        } catch (Exception e) {
        }

    }

}
