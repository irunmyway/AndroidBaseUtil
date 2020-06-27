package com.ez.socket.callback;

import java.io.DataOutputStream;
import java.net.Socket;

public class EmptyCallbackImpl implements SocketCallback, HeartbeatCallback {
    @Override
    public void onStopHeartbeat() {

    }

    @Override
    public void onPreSend(DataOutputStream out) {

    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onClosed(Socket socket) {

    }

    @Override
    public void onConnected(Socket socket) {

    }

    @Override
    public void onConnectFail(Socket socket, boolean needReconnect) {

    }

    @Override
    public void onReceive(Socket socket, byte[] bytes) {

    }
}
