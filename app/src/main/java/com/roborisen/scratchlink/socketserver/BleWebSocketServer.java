package com.roborisen.scratchlink.socketserver;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class BleWebSocketServer extends WebSocketServer {
    private final String TAG = getClass().getSimpleName();
    private SocketMessageListener mSocketMessageListener;

    public BleWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        mSocketMessageListener.onOpen(conn,handshake);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        mSocketMessageListener.onClose(conn,code,reason,remote);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        mSocketMessageListener.onMessage(conn,message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        mSocketMessageListener.onError(conn,ex);
    }

    @Override
    public void onStart() {
        mSocketMessageListener.onStart();
    }

    public void setmSocketMessageListener(SocketMessageListener mSocketMessageListener) {
        this.mSocketMessageListener = mSocketMessageListener;
    }
}
