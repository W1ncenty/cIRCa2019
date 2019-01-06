/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.model;

import irc.IRC;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import javafx.application.Platform;

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
            //this.reader = new BufferedReader(new InputStreamReader(irc.getSocket().getInputStream()));
            while (!stopped) {
                //Thread.sleep(1000);
                
                this.reader = new BufferedReader(new InputStreamReader(irc.getSocket().getInputStream()));
                String serverMessage = this.reader.readLine();
                
                //Czszczenie pustych komorek
                serverMessage = serverMessage.substring(serverMessage.indexOf("#"));
                
                
                Platform.runLater(
                        () -> {
                            this.irc.getUser().getChanels().clear();
                            this.irc.getAllChanels().clear();
                        }
                );
                
                
                if (serverMessage.startsWith("#") && serverMessage.endsWith("$") && serverMessage.length()>2) {
                    String[] rooms = serverMessage.substring(2, serverMessage.length() - 1).split("@");

                    System.out.println(Arrays.toString(rooms));

                    for (int i = 0; i < rooms.length; i++) {
                        String[] string1 = rooms[i].split("%");
                        String chatroomName = string1[0];// - nazwa chatroomu
                        System.out.println(chatroomName);

                        String[] users = string1[1].split(";");

                        boolean userInchanel = false;

                        System.out.println(Arrays.toString(users));
                        Chanel chanel = new Chanel(chatroomName);

                        for (int j = 0; j < users.length; j++) {
                            String user = users[j];
                            chanel.getUsers().add(new User(user));
                            if (userInchanel == false && user.equals(this.irc.getUser().getUsername())) {
                                userInchanel = true;
                            }
                        }

                        System.out.println("Linia 103");

                        if (string1.length > 2) {
                            System.out.println(string1[2]);
                            
                            String[] messages = string1[2].split(";");
                            
                            System.out.println(Arrays.toString(messages));
                            System.out.println("Dlugosc wiadomosci: " + messages.length);

                            for (int j = 0; j < messages.length; j++) {
                                if ((j + 1) % 3 == 0) {
                                    Message newMessageObject = new Message(chanel.getChanelName(), messages[j - 2], messages[j - 1], messages[j]);
                                    chanel.getMessages().add(newMessageObject);
                                }
                            }

                        }

                        if (irc.getChatRoomController().getActiveChanel() == null && userInchanel) {
                            irc.getChatRoomController().setActiveChanel(chanel);
                            irc.getChatRoomController().getChatRoomList().getSelectionModel().selectFirst();
                            
                            System.out.println("XXX");
                        }

                        if (irc.getChatRoomController().getActiveChanel() != null
                                && irc.getChatRoomController().getActiveChanel().getChanelName().equals(chanel.getChanelName())) {
                            irc.getChatRoomController().getAllMessages().clear();
                            chanel.getMessages().forEach(message
                                    -> irc.getChatRoomController().updateMessage(message.formatMessage()));
                            System.out.println("Rozne od null");
 
                        }
                        
                        System.out.println(userInchanel);
                        if (userInchanel) {
                            Platform.runLater(
                                    () -> {
                                        this.irc.getUser().getChanels().add(chanel);
                                        this.irc.getChatRoomController().displayChatroomList();
                                        irc.getChatRoomController().displayUserList();
                                    }
                            );
                        } else {
                            Platform.runLater(
                                    () -> {
                                        // Update UI here.
                                        this.irc.getAllChanels().add(chanel);
                                        irc.getChatRoomController().displayUserList();
                                    }
                            );
                        }

                    }

                } else {
                    System.out.println("Zly komunikat");
                }

            }
        } catch (Exception e) {
        }

    }

}
