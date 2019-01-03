/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.model;

import irc.IRC;
import irc.controller.ChatRoomController;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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

                String serverMessage = this.reader.readLine();
                String[] data = serverMessage.split(";");
                String operation = data[0];
                Chanel chanel;
                switch (operation) {
                    //utworzenie chatroom
                    case "1":
                        System.out.println(Arrays.toString(data));

                        Chanel newChanel = new Chanel(data[2]);

                        if (Collections.frequency(irc.getAllChanels(), newChanel) < 1) {
                            this.irc.getAllChanels().add(newChanel);
                        } else {
                            System.out.println("Istnieje taki chatroom");
                        }

                        
                        if (data[1].equals(irc.getUser().getUsername())) {

                            Platform.runLater(
                                    () -> {
                                        // Update UI here.
                                        this.irc.getUser().getChanels().add(newChanel);
                                        this.irc.getChatRoomController().displayChatroomList();
                                    }
                            );

                            
                        } else {
                            System.out.println("Someone created chatroom");
                        }

                        break;
                    //wejscie do chatroomu
                    case "2":
                        //serwer odpowiada: 2;chatroom;username
                        System.out.println(Arrays.toString(data));
                        if (data[2].equals(irc.getUser().getUsername())) {
                            chanel = findChanel(data[1]);

                            Platform.runLater(
                                    () -> {
                                        // Update UI here.
                                        chanel.getUsers().add(irc.getUser());
                                        irc.getUser().getChanels().add(chanel);
                                        this.irc.getChatRoomController().displayUserList();
                                        this.irc.getChatRoomController().displayChatroomList();
                                    }
                            );

                        }
                        Chanel myChanel = findMyChanel(data[1]);
                        if (myChanel != null) {
                            Platform.runLater(
                                    () -> {
                                        // Update UI here.
                                        myChanel.getUsers().add(new User(data[2]));
                                        this.irc.getChatRoomController().displayUserList();
                                       
                                    }
                            );

                        }

                        break;
                    //wyjscie z chatroomu
                    case "3":
                        System.out.println(Arrays.toString(data));
                        chanel = findMyChanel(data[2]);
                        if (chanel == null) {
                            break;
                        }
                        if (data[1].equals(irc.getUser().getUsername())) {
                            
                            Platform.runLater(
                                    () -> {
                                        // Update UI here.
                                        irc.getUser().getChanels().remove(chanel);
                                        this.irc.getChatRoomController().displayChatroomList();
                                        this.irc.getChatRoomController().displayUserList();
                                       
                                    }
                            );
                        }
                        break;
                    //wyslano wiadomosc: 4;chanel;czas;uzytkownik;wiadomosc
                    case "4":
                        if (data.length == 5) {
                            chanel = findMyChanel(data[1]);
                            if (chanel == null) {
                                System.out.println("Nie ma takiego kanalu");
                                break;
                            }
                            Message message = new Message(chanel.getChanelName(), data[2], data[3], data[4]);
                            chanel.getMessages().add(message);
                            irc.getChatRoomController().updateMessage(message.formatMessage());

                            System.out.println(message.toString());
                        }

                        break;

                    default:
                        System.out.println(Arrays.toString(data));

                }

            }
            System.out.println("stoppend");
        } catch (Exception e) {
        }

    }

}
