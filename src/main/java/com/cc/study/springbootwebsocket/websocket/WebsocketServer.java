package com.cc.study.springbootwebsocket.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chenc
 * @date 2020/02/07
 **/
@Component
@ServerEndpoint(value = "/imserver/{userId}")
@Slf4j
public class WebsocketServer {

    /**
     * 用于统计当前在线人数
     */
    private static volatile int online_count = 0;
    /**
     * 用于存放客户端对应的webcoketServer对象
     */
    private static ConcurrentHashMap<String, WebsocketServer> websocketMap = new ConcurrentHashMap<>();

    /**
     * 当前用户
     */
    private String userId;
    /**
     * 当前session，需要通过它来和客户端进行通信
     */
    private Session session;

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.session = session;
        this.userId = userId;

        if (!websocketMap.containsKey(userId)) {
            addOnlineCount();
        }
        websocketMap.put(userId, this);

        log.info("用户{}连接成功，当前连接人数{}", userId, getOnlineCount());
        try {
            sendMsg("连接成功");
        } catch (IOException e) {
            log.error("用户{}连接失败", userId);
        }
    }

    @OnClose
    public void onClose() {
        if (websocketMap.containsKey(userId)) {
            websocketMap.remove(userId);
            subOnlineCount();
        }
    }

    @OnMessage
    public void onMessage(String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }

        JSONObject jsonObject = JSON.parseObject(message);
        String toUserId = jsonObject.getString("toUserId");

        if (!StringUtils.isEmpty(toUserId) && websocketMap.containsKey(toUserId)) {
            try {
                websocketMap.get(toUserId).sendMsg(jsonObject.toJSONString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //否则不在这个服务器上，发送到mysql或者redis
            log.error("请求的userId:" + toUserId + "不在该服务器上");
        }
    }

    @OnError
    public void onError(Throwable error) {
        error.printStackTrace();
    }

    private synchronized void addOnlineCount() {
        online_count++;
    }

    private void sendMsg(String text) throws IOException {
        this.session.getBasicRemote().sendText(text);
    }

    private synchronized void subOnlineCount() {
        online_count--;
    }

    private int getOnlineCount() {
        return online_count;
    }
}
