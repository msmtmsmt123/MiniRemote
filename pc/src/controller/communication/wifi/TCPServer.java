package controller.communication.wifi;

import java.awt.AWTException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import controller.communication.events.ActionException;
import controller.communication.events.EventWrapper;
import main.Controller;


/**
 * Created by cyprien on 04/11/15.
 */
public class TCPServer {
    private final static int PORT = 8888;
    private Thread serverInputThread;
    private Thread serverOutputThread;
    private Socket socket=null;
    private ServerSocket serverSocket=null;
    private List<EventWrapper> events;

    public TCPServer() {
        events= Collections.synchronizedList(new ArrayList<>());
    }

    public void startServer() throws IOException {
        serverSocket=new ServerSocket(PORT);
        System.out.println("en attente de client");
        socket=serverSocket.accept();
        System.out.println("client connecté");

        ServerInput actionInput = new ServerInput(socket, events);
        ServerOutput actionOutput = new ServerOutput(socket, events);
        serverInputThread=new Thread(actionInput);
        serverOutputThread=new Thread(actionOutput);
        serverInputThread.start();
        serverOutputThread.start();
    }

    public void send(EventWrapper event){
        events.add(event);
        events.notifyAll();
    }


    public void stop(){
        serverInputThread.interrupt();
    }

    private class ServerInput implements Runnable{
        private Socket socket=null;
        private List<EventWrapper> events;
        private EventWrapper received;
        private EventWrapper response;

        public ServerInput(Socket socket, List<EventWrapper> events) throws IOException {
            this.socket=socket;
            this.events=events;
        }

        @Override
        public void run(){
            try {
                ObjectInputStream in=new ObjectInputStream(socket.getInputStream());

                while(true){
                    received=(EventWrapper)in.readObject();
                    response=Controller.handleControl(received);
                    events.add(response);
                    events.notifyAll();
                }

            } catch (IOException | ClassNotFoundException | AWTException | ActionException e) {
                e.printStackTrace();
            }
        }
    }
    private class ServerOutput implements Runnable{
        private List<EventWrapper> events;
        private Socket socket=null;

        public ServerOutput(Socket socket, List<EventWrapper> events) throws IOException {
            this.socket=socket;
            this.events=events;
        }

        @Override
        public void run(){
            try {
                ObjectOutputStream out=new ObjectOutputStream(socket.getOutputStream());

                while (true){
                    events.wait();
                    while (!events.isEmpty()){
                        out.writeObject(events.remove(0));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
