/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package irc.model;

import irc.IRC;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
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
                System.out.println("watek");
                String serverMessage = this.reader.readLine();//irc.getReader().readLine();
                String[] data = serverMessage.split(";");
                String operation = data[0];
                Chanel chanel;
                switch (operation) {
                    //utworzenie chatroom
                    case "1":
                        System.out.println(Arrays.toString(data));

                        chanel = new Chanel(data[2]);

                        if (Collections.frequency(irc.getAllChanels(), chanel) < 1) {
                            this.irc.getAllChanels().add(chanel);
                        } else {
                            System.out.println("Istnieje taki chatroom");
                        }
                        this.irc.getAllChanels().add(chanel);
                        if(data[1].equals(irc.getUser().getUsername())){
                            irc.getUser().getChanels().add(chanel);
                        }

//                        if (data[1].equals(irc.getUser().getUsername())) {
//                            irc.getUser().getChanels().add(chanel);
//                        } else {
//                            System.out.println("Someone created chatroom");
//                        }

                        break;
                    //wejscie do chatroomu
                    case "2":
                        //serwer odpowiada: 2;username;chatroom
                        System.out.println(Arrays.toString(data));
                        chanel = findChanel(data[2]);
                        irc.getUser().getChanels().add(chanel);
                        break;
                    //wyjscie z chatroomu
                    case "3":
                        System.out.println(Arrays.toString(data));
                        chanel = findMyChanel(data[2]);
                        if (chanel == null) {
                            break;
                        }
                        if (data[1].equals(irc.getUser().getUsername())) {
                            irc.getUser().getChanels().remove(chanel);
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
                            Message message = new Message(chanel.getChanelName(), data[3],data[2], data[4]);
                            chanel.getMessages().add(message);
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
