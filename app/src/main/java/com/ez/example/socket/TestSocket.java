package com.ez.example.socket;

import com.ez.socket.SocketClient;
import com.ez.socket.callback.SocketCallback;
import com.ez.utils.BDebug;

import java.net.Socket;

public class TestSocket implements SocketCallback {

    public void init() {
        BDebug.trace("测试" + "");
        SocketClient.getInstance().setSocketCallback(this);
        SocketClient.getInstance()
                .createConnection("192.168.0.110", "19730", false)
                .start();

    }

    @Override
    public void onError(Exception e) {
        BDebug.trace("测试" + e.toString());
    }

    @Override
    public void onClosed(Socket socket) {
        BDebug.trace("onClosed");
    }

    @Override
    public void onConnected(Socket socket) {
        BDebug.trace("onConnected");
    }

    @Override
    public void onConnectFail(Socket socket, boolean needReconnect) {
        BDebug.trace("onConnectFail");
    }

    @Override
    public void onReceive(Socket socket, byte[] bytes) {
        BDebug.trace(new String(bytes));
    }
}
