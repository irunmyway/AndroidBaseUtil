package com.ez.socket;

import com.ez.socket.callback.SocketCallback;
import com.ez.utils.BObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

/**
 * 作者: hhx QQ1025334900
 * 时间: 2020-06-26 10:54
 * 功能: 套接字客户端
 **/
public class SocketClient extends Thread {
    SocketAddress address;//连接地址
    private Socket socket = null;//定义客户
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private StringBuffer bufMsg = new StringBuffer();
    private ByteArrayOutputStream respMsg = new ByteArrayOutputStream(2048);
    private SocketCallback socketCallback;//套接字事件
    private boolean needReconnect;

    public SocketClient createConnection(String target, int port, boolean needReconnect) {
        this.needReconnect = needReconnect;
        address = new InetSocketAddress(target, port);
        return this;
    }

    public void init() {
        try {
            socket = new Socket();
            socket.connect(address);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            if (!socket.isOutputShutdown()) {//连接成功。
                socketCallback.onConnected(socket);
            } else {
                socketCallback.onConnectFail(socket, needReconnect);
            }
        } catch (UnknownHostException e1) {
            socketCallback.onError(e1);
        } catch (IOException ex) {
            socketCallback.onError(ex);
        } catch (IllegalArgumentException e2) {
            socketCallback.onError(e2);
        }
    }


    public void sendMsg(final Object... args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (BObject.isEmpty(args)) {
                    socketCallback.onError(new Exception("发送了空的消息。"));
                    return;
                }
                if (socket != null && socket.isConnected()) {
                    if (!socket.isOutputShutdown()) {
                        for (int i = 0; i < args.length; i++) {
                            try {
                                Object val = args[i];
                                if (val instanceof byte[])
                                    out.write((byte[]) val);
                                if (val instanceof String)
                                    out.writeChars(val.toString());
                            } catch (Exception e) {
                                socketCallback.onError(e);
                            }
                        }
                        try {
                            out.flush();
                            bufMsg.delete(0, bufMsg.length());
                        } catch (Exception e) {
                            socketCallback.onError(e);
                        }
                    } else {
                        socketCallback.onClosed(socket);
                    }
                } else {
                    socketCallback.onConnectFail(socket, needReconnect);
                }
            }
        }).start();


    }


    @Override
    public void run() {
        super.run();
        init();
        try {
            while (true) {
                if (!socket.isClosed()) {
                    if (socket.isConnected()) {
                        if (!socket.isInputShutdown()) {
                            bufMsg.delete(0, bufMsg.length());
                            respMsg.reset();
                            int ch = 0;
                            while ((ch = in.read()) != -1) {
                                bufMsg.append((char) ch);
                                respMsg.write(ch);
                            }
                            if (bufMsg.length() > 0) {//有数据接收
                                socketCallback.onReceive(socket, respMsg);
                            }
                        }
                    } else {
                        socketCallback.onConnectFail(socket, needReconnect);
                    }
                } else {
                    socketCallback.onClosed(socket);
                }
            }
        } catch (Exception e) {
            socketCallback.onError(e);
        }
    }


    public SocketClient setSocketCallback(SocketCallback socketCallback) {
        this.socketCallback = socketCallback;
        return this;
    }
}