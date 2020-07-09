package com.ez.socket;

import com.ez.socket.callback.EmptyCallbackImpl;
import com.ez.socket.callback.HeartbeatCallback;
import com.ez.socket.callback.SocketCallback;
import com.ez.utils.BObject;
import com.ez.utils.BString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import static com.ez.utils.BArray.byteArrayToInt;
import static com.ez.utils.BArray.intToByteArray;
import static java.lang.System.arraycopy;

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
    private SocketCallback socketCallback;//套接字回调
    private boolean needReconnect;//需要重连
    private long reconnectDelay;
    private boolean needHeartbeat;//需要心跳
    private int maxReceiveMB = 1;//最大接收字节mb
    private HeartbeatClient heartbeatClient;//心跳线程
    private HeartbeatCallback heartbeatCallback;//心跳回调
    private static SocketClient Instance;//单例模式
    private long keepAliveDelay;//心跳速率
    private boolean isExit;//结束标记
    private int HEAD_LENGTH = 0;//包头长度存储空间


    //获取单例
    public static SocketClient getInstance() {
        if (Instance == null) {
            Instance = new SocketClient();
        }
        return Instance;
    }


    //设置连接属性
    public SocketClient createConnection(String target, String strPort, boolean needHeartbeat) {
        int port = 80;
        try {
            port = Integer.parseInt(strPort);
        } catch (Exception e) {
        }
        this.needReconnect = false;
        this.needHeartbeat = needHeartbeat;
        if (port < 2) return this;
        if (!BString.isIP(target)) return this;
        address = new InetSocketAddress(target, port);
        return this;
    }

    public SocketClient setNeedReconnect(boolean needReconnect, long delay) {
        this.needReconnect = needReconnect;
        this.reconnectDelay = delay;
        return this;
    }

    //设置连接属性
    public SocketClient createConnection(String target, String strPort, long reconnectDelay, boolean needHeartbeat) {
        int port = 80;
        try {
            port = Integer.parseInt(strPort);
        } catch (Exception e) {
        }
        this.needReconnect = true;
        this.reconnectDelay = reconnectDelay;
        this.needHeartbeat = needHeartbeat;
        if (port < 2) return this;
        if (!BString.isIP(target)) return this;
        address = new InetSocketAddress(target, port);
        return this;
    }

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    //初始化连接
    public void init() {
        try {
            if (socketCallback == null) socketCallback = new EmptyCallbackImpl();//设置空监听
            if (heartbeatCallback == null) heartbeatCallback = new EmptyCallbackImpl();//设置空监听
            socket = new Socket();
            socket.connect(address);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            if (!socket.isOutputShutdown()) {//连接成功。
                socketCallback.onConnected(socket);
                if (needHeartbeat) {//开启心跳
                    if (BObject.isNotEmpty(heartbeatClient)) {
                        heartbeatClient.setExit(true);
                        heartbeatClient = null;
                    }
                    heartbeatClient = new HeartbeatClient();
                    heartbeatClient.keepAliveDelay = keepAliveDelay;
                    heartbeatClient.createHeatbeat(socket, heartbeatCallback).start();
                }
            } else {
                socketCallback.onConnectFail(socket, needReconnect);
            }
        } catch (ConnectException e3) {
            socketCallback.onConnectFail(socket, needReconnect);
        } catch (UnknownHostException e1) {
            socketCallback.onError(e1);
        } catch (IOException ex) {
            socketCallback.onError(ex);
        } catch (IllegalArgumentException e2) {
            socketCallback.onError(e2);
        }
    }

    //设置接收的最大值 默认为1m
    public SocketClient setMaxReceiveMB(int maxReceiveMB) {
        this.maxReceiveMB = maxReceiveMB;
        return this;
    }

    //获取套接字
    public Socket getSocket() {
        return socket;
    }

    //发送数据 通过长度包头 可以解决沾包拆包
    public void sendMsgByLength(final Object args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (BObject.isEmpty(args)) {
                    socketCallback.onError(new Exception("发送了空的消息。"));
                    return;
                }
                if (socket != null && socket.isConnected()) {
                    if (!socket.isOutputShutdown()) {
                        try {
                            try {
                                if (args instanceof byte[]) {
                                    out.write(intToByteArray(((byte[]) args).length, HEAD_LENGTH));
                                    out.write((byte[]) args);
                                }
                                if (args instanceof String) {
                                    out.write(intToByteArray(((String) args).getBytes().length, HEAD_LENGTH));
                                    out.write(((String) args).getBytes());
                                }
                            } catch (Exception e) {
                                socketCallback.onError(e);
                            }
                            out.flush();
                        } catch (Exception e) {
                            try {
                                socket.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            socketCallback.onError(e);
                        }
                    } else {
                        if (needReconnect) reConnect();
                        socketCallback.onClosed(socket);
                    }
                } else {
                    socketCallback.onConnectFail(socket, needReconnect);
                }
            }
        }).start();
    }

    //重写连接
    public void reConnect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(reconnectDelay);
                } catch (InterruptedException e1) {
                }
                try {
                    if (BObject.isNotEmpty(in) && BObject.isNotEmpty(out)) {
                        in.close();
                        out.close();
                    }
                    socket.close();
                    socket = null;
                    init();
                } catch (IOException e) {
                    socketCallback.onError(new Exception("重连失败"));
                }
            }
        }).start();
    }

    //普通的发送数据 不能解决沾包拆包
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
                        try {

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
                            out.flush();
                        } catch (Exception e) {
                            try {
                                socket.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            socketCallback.onError(e);
                        }
                    } else {
                        if (needReconnect) reConnect();
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
            byte[] b = new byte[1024 * maxReceiveMB];
            byte[] body = new byte[0];
            boolean isReceive = false;
            int curLen = 0;//读取进度
            int bodyLen = 0;//报文总长
            byte[] head = new byte[HEAD_LENGTH];
            while (!isExit) {
                if (!socket.isClosed()) {
                    if (socket.isConnected()) {
                        if (!socket.isInputShutdown()) {
                            try {
                                //沾包问题临时解决部分///////////////////////////////////////
                                if (HEAD_LENGTH > 0) {
                                    if (byteArrayToInt(head, HEAD_LENGTH) < 1) {//还没有读取到包头
                                        int tmpLen = in.read(b, 0, 2);
                                        if (tmpLen > 0) {
                                            arraycopy(b, 0, head, 0, HEAD_LENGTH);
                                            bodyLen = byteArrayToInt(head, HEAD_LENGTH);
                                        }
                                        body = new byte[bodyLen];
                                        isReceive = true;
                                    }
                                    if (isReceive) {
                                        curLen += in.read(body, 0, bodyLen);
                                        if (curLen == bodyLen && curLen != 0) {
                                            socketCallback.onReceive(socket, body);
                                            isReceive = false;
                                            curLen = 0;
                                            head = new byte[HEAD_LENGTH];
                                        }
                                    }
                                } else {//未开启解决沾包
                                    int len = in.read(b);//读取到数据了
                                    if (b[0] != 0) {//有数据接收
                                        byte[] temp = new byte[len];
                                        arraycopy(b, 0, temp, 0, len);
                                        try {
                                            socketCallback.onReceive(socket, temp);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    if (needReconnect) reConnect();
                    socketCallback.onClosed(socket);
                }
            }
            socketCallback.onClosed(socket);
            if (needReconnect) {
                try {
                    Thread.sleep(reconnectDelay);
                } catch (InterruptedException e1) {
                }
                run();
            }
        } catch (Exception e) {
            if (needReconnect) {
                socketCallback.onError(new Exception("监听线程异常结束,并重连。"));
                try {
                    Thread.sleep(reconnectDelay);
                } catch (InterruptedException e1) {
                }
                run();
            } else {
                socketCallback.onError(new Exception("监听线程异常结束"));
            }
        }
    }
    //设置结束套接字
    public void destroyInstance() {
        isExit = true;
        Instance = null;
    }

    //设置心跳速率
    public void setHeartbeatDelay(long delay) {
        keepAliveDelay = delay;
    }

    //设置套接字回调
    public SocketClient setSocketCallback(SocketCallback socketCallback) {
        this.socketCallback = socketCallback;
        return this;
    }

    //设置心跳回调
    public SocketClient setHeartbeatCallback(HeartbeatCallback heartbeatCallback) {
        this.heartbeatCallback = heartbeatCallback;
        return this;
    }

    //设置发包安全 即解决沾包问题
    public void setPackageSafety(int byteLen) {
        this.HEAD_LENGTH = byteLen;
    }


}