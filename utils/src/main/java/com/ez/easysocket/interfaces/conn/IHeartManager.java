package com.ez.easysocket.interfaces.conn;


import com.ez.easysocket.connection.heartbeat.HeartManager;
import com.ez.easysocket.entity.basemsg.SuperSender;

/**
 * Author：Alex
 * Date：2019/12/8
 * Note：
 */
public interface IHeartManager {

    /**
     * 开始心跳
     *
     * @param clientHeart
     */
    void startHeartbeat(SuperSender clientHeart, HeartManager.HeartbeatListener listener);

    /**
     * 停止心跳
     */
    void stopHeartbeat();


    /**
     * 接收到心跳
     */
    void onReceiveHeartBeat();
}
