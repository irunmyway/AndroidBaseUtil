package com.ez.easysocket.interfaces.config;


import com.ez.easysocket.entity.SocketAddress;
import com.ez.easysocket.interfaces.conn.IConnectionManager;

/**
 * Author：Alex
 * Date：2019/6/4
 * Note：
 */
public interface IConnectionSwitchListener {
    void onSwitchConnectionInfo(IConnectionManager manager, SocketAddress oldAddress, SocketAddress newAddress);
}
