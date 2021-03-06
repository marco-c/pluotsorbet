package org.mozilla.io;

import java.io.*;
import javax.microedition.io.*;
import com.nokia.mid.s40.io.LocalMessageProtocolMessage;
import com.nokia.mid.s40.io.LocalMessageProtocolConnection;
import com.nokia.mid.s40.io.LocalMessageProtocolServerConnection;
import gnu.testlet.TestHarness;
import gnu.testlet.Testlet;

public class TestLocalMsgProtocol implements Testlet {
    public int getExpectedPass() { return 23; }
    public int getExpectedFail() { return 0; }
    public int getExpectedKnownFail() { return 0; }
    LocalMessageProtocolServerConnection server;
    LocalMessageProtocolConnection client;
    static final String PROTO_NAME = "marco";
    static int serverNum = 0;
    TestHarness th;

    public void serverSendData() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeByte(5);
        dataOutputStream.writeInt(16491);
        byte[] serverData = byteArrayOutputStream.toByteArray();
        ((LocalMessageProtocolConnection)server).send(serverData, 0, serverData.length);
        dataOutputStream.close();
        byteArrayOutputStream.close();
    }

    public void clientReceiveData() throws IOException {
        byte[] clientData = new byte[5];
        client.receive((byte[])clientData);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[])clientData);
        DataInputStream dataInputStream = new DataInputStream((InputStream)byteArrayInputStream);
        byte by = dataInputStream.readByte();
        int n = dataInputStream.readInt();
        th.check(by, 5);
        th.check(n, 16491);
        dataInputStream.close();
        byteArrayInputStream.close();
    }

    public void clientSendData() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeByte(9);
        dataOutputStream.writeInt(24891);
        byte[] clientData = byteArrayOutputStream.toByteArray();
        client.send(clientData, 0, clientData.length);
        dataOutputStream.close();
        byteArrayOutputStream.close();
    }

    public void serverReceiveData() throws IOException {
        byte[] serverData = new byte[5];
        ((LocalMessageProtocolConnection)server).receive((byte[])serverData);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[])serverData);
        DataInputStream dataInputStream = new DataInputStream((InputStream)byteArrayInputStream);
        byte by = dataInputStream.readByte();
        int n = dataInputStream.readInt();
        th.check(by, 9);
        th.check(n, 24891);
        dataInputStream.close();
        byteArrayInputStream.close();
    }

    public void testServerSendsClientReceives() throws IOException {
        serverSendData();
        clientReceiveData();
    }

    public void testClientSendsServerReceives() throws IOException {
        clientSendData();
        serverReceiveData();
    }

    public void testServerSendsClientReceives2() throws IOException {
        // Server sends data
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeByte(5);
        dataOutputStream.writeInt(16491);
        byte[] serverData = byteArrayOutputStream.toByteArray();
        ((LocalMessageProtocolConnection)server).send(serverData, 0, serverData.length);
        dataOutputStream.close();
        byteArrayOutputStream.close();

        // Client receives data
        LocalMessageProtocolMessage msg = client.newMessage(null);
        client.receive(msg);
        th.check(msg.getLength(), 5);
        byte[] clientData = msg.getData();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[])clientData);
        DataInputStream dataInputStream = new DataInputStream((InputStream)byteArrayInputStream);
        byte by = dataInputStream.readByte();
        int n = dataInputStream.readInt();
        th.check(by, 5);
        th.check(n, 16491);
        dataInputStream.close();
        byteArrayInputStream.close();
    }

    public void testClientSendsServerReceives2() throws IOException {
        // Client sends data
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeByte(9);
        dataOutputStream.writeInt(24891);
        byte[] clientData = byteArrayOutputStream.toByteArray();
        client.send(clientData, 0, clientData.length);
        dataOutputStream.close();
        byteArrayOutputStream.close();

        // Server receives data
        LocalMessageProtocolMessage msg = ((LocalMessageProtocolConnection)server).newMessage(null);
        ((LocalMessageProtocolConnection)server).receive(msg);
        th.check(msg.getLength(), 5);
        byte[] serverData = msg.getData();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[])serverData);
        DataInputStream dataInputStream = new DataInputStream((InputStream)byteArrayInputStream);
        byte by = dataInputStream.readByte();
        int n = dataInputStream.readInt();
        th.check(by, 9);
        th.check(n, 24891);
        dataInputStream.close();
        byteArrayInputStream.close();
    }

    class TestThread extends Thread {
        int sleep1;
        int sleep2;
        int content;
        LocalMessageProtocolServerConnection server;
        LocalMessageProtocolConnection client;

        public TestThread(int sleepBeforeSend, int sleepBeforeReceive, int content) throws IOException {
            this.sleep1 = sleepBeforeSend;
            this.sleep2 = sleepBeforeReceive;
            this.content = content;
            // To prevent the native localmsg connections mess with each other, we initialize
            // client and server in constructor here.
            this.server = (LocalMessageProtocolServerConnection)Connector.open("localmsg://:" + PROTO_NAME + serverNum);
            this.client = (LocalMessageProtocolConnection)Connector.open("localmsg://" + PROTO_NAME + serverNum);
        }

        public void testServerSendsClientReceives() throws IOException, InterruptedException {
            Thread.sleep(sleep1);

            // Server sends data
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(5);
            dataOutputStream.writeInt(content);
            byte[] serverData = byteArrayOutputStream.toByteArray();
            ((LocalMessageProtocolConnection)server).send(serverData, 0, serverData.length);
            dataOutputStream.close();
            byteArrayOutputStream.close();

            Thread.sleep(sleep2);

            // Client receives data
            byte[] clientData = new byte[5];
            client.receive((byte[])clientData);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[])clientData);
            DataInputStream dataInputStream = new DataInputStream((InputStream)byteArrayInputStream);
            byte by = dataInputStream.readByte();
            int n = dataInputStream.readInt();
            th.check(by, 5);
            th.check(n, content);
            dataInputStream.close();
            byteArrayInputStream.close();
        }


        public void run() {
            try {
                testServerSendsClientReceives();
            } catch (IOException ioe) {
                th.fail("Unexpected exception");
                ioe.printStackTrace();
            } catch (InterruptedException ie) {
                th.fail("Unexpected exception");
                ie.printStackTrace();
            }
        }
    }

    class ThreadClientCreate extends Thread {
        public void run() {
            try {
                client = (LocalMessageProtocolConnection)Connector.open("localmsg://" + PROTO_NAME + serverNum);
            } catch (Exception e) {
                th.fail("Unexpected exception: " + e);
            }
        }
    }

    class ThreadServerCreate extends Thread {
        public void run() {
            try {
                server = (LocalMessageProtocolServerConnection)Connector.open("localmsg://:" + PROTO_NAME + serverNum);
            } catch (Exception e) {
                    th.fail("Unexpected exception: " + e);
            }
        }
    }

    class ThreadServerAcceptAndOpen extends Thread {
        public void run() {
            try {
                server.acceptAndOpen();
            } catch (Exception e) {
                th.fail("Unexpected exception: " + e);
            }
        }
    }

    class ThreadClientWaitMessage extends Thread {
        public void run() {
            try {
                clientReceiveData();
            } catch (IOException e) {
                th.fail("Unexpected exception: " + e);
            }
        }
    }

    class ThreadServerSendMessage extends Thread {
        public void run() {
            try {
                serverSendData();
            } catch (Exception e) {
                th.fail("Unexpected exception: " + e);
            }
        }
    }

    class ThreadServerWaitMessage extends Thread {
        public void run() {
            try {
                serverReceiveData();
            } catch (IOException e) {
                th.fail("Unexpected exception: " + e);
            }
        }
    }

    class ThreadClientSendMessage extends Thread {
        public void run() {
            try {
                clientSendData();
            } catch (Exception e) {
                th.fail("Unexpected exception: " + e);
            }
        }
    }

    public void test(TestHarness th) {
        this.th = th;

        try {
            server = (LocalMessageProtocolServerConnection)Connector.open("localmsg://:" + PROTO_NAME + serverNum);

            client = (LocalMessageProtocolConnection)Connector.open("localmsg://" + PROTO_NAME + serverNum);

            serverNum++;

            testServerSendsClientReceives();
            testClientSendsServerReceives();

            testServerSendsClientReceives2();
            testClientSendsServerReceives2();

            Thread t1 = new TestThread(10,   2000, 12421);
            Thread t2 = new TestThread(500, 500, 32311);
            Thread t3 = new TestThread(1000, 500, 92330);
            t1.start();
            t2.start();
            t3.start();
            t1.join();
            t2.join();
            t3.join();

            serverNum++;

            // Test client waiting for a message from the server when the message isn't available yet
            Thread clientWait = new ThreadClientWaitMessage();
            clientWait.start();
            Thread serverSend = new ThreadServerSendMessage();
            serverSend.start();
            clientWait.join();
            serverSend.join();

            // Test server waiting for a message from the client when the message isn't available yet
            Thread serverWait = new ThreadServerWaitMessage();
            serverWait.start();
            Thread clientSend = new ThreadClientSendMessage();
            clientSend.start();
            serverWait.join();
            clientSend.join();

            client.close();
            server.close();

            // Test three scenarios that might exhibit race conditions.

            // Scenario 1
            // 1) Server creates a localmsg server
            // 2) Server calls acceptAndOpen
            // 3) Client opens a localmsg server
            Thread serverCreateThread = new ThreadServerCreate();
            serverCreateThread.start();
            serverCreateThread.join();
            Thread serverAcceptAndOpenThread = new ThreadServerAcceptAndOpen();
            serverAcceptAndOpenThread.start();
            Thread.sleep(1000);
            Thread clientThread = new ThreadClientCreate();
            clientThread.start();
            serverAcceptAndOpenThread.join();
            clientThread.join();
            client.close();
            server.close();
            th.check(true, "Server create, server accept and open, client open");
            serverNum++;

            // Scenario 2
            // 1) Server creates a localmsg server
            // 2) Client opens a localmsg server
            // 3) Server calls acceptAndOpen
            serverCreateThread.start();
            serverCreateThread.join();
            clientThread.start();
            Thread.sleep(1000);
            serverAcceptAndOpenThread.start();
            serverAcceptAndOpenThread.join();
            clientThread.join();
            client.close();
            server.close();
            th.check(true, "Server create, client open, server accept and open");
            serverNum++;

            // Scenario 3
            // 1) Client opens a localmsg server
            // 2) Server creates a localmsg server
            // 3) Server calls acceptAndOpen
            clientThread.start();
            Thread.sleep(1000);
            serverCreateThread.start();
            serverCreateThread.join();
            serverAcceptAndOpenThread.start();
            serverAcceptAndOpenThread.join();
            clientThread.join();
            client.close();
            server.close();
            th.check(true, "Client open, server create, server accept and open");
            serverNum++;
        } catch (IOException ioe) {
            th.fail("Unexpected exception");
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            th.fail("Unexpected exception");
            ie.printStackTrace();
        }
    }
}
