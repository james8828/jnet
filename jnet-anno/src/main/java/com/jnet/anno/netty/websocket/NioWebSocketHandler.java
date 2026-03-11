package com.jnet.anno.netty.websocket;

import com.jnet.anno.netty.global.ChannelSupervise;
import com.jnet.anno.netty.global.ChatGroup;
import com.jnet.anno.netty.message.AnnotationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;

@Slf4j
@Component
public class NioWebSocketHandler extends SimpleChannelInboundHandler<Object> {

    @Resource
    private ObjectMapper objectMapper;

    /**
     * . 注入的时候，给类的 service 注入 ,直接使用装饰符进行装饰会报错 空指针异常
     */
    private WebSocketServerHandshaker handshaker;
    public void sendMessage(AnnotationMessage message) {
        if (message == null) {
            log.warn("尝试发送空消息");
            return;
        }
        String msg;
        try {
            msg = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("序列化消息失败: type={}, slideId={}, error={}",
                    message.getType(), message.getSlideId(), e.getMessage(), e);
            return;
        } catch (Exception e) {
            log.error("未知错误导致消息序列化失败: type={}, slideId={}, error={}",
                    message.getType(), message.getSlideId(), e.getMessage(), e);
            return;
        }

        Long targetSlideId = message.getSlideId();
        for (Map.Entry<Channel, Long> vo : ChannelSupervise.CHANNEL_MAP.entrySet()) {
            if (targetSlideId.equals(vo.getValue())) {
                Channel channel = vo.getKey();
                if (channel != null && channel.isActive()) {
                    TextWebSocketFrame tws = new TextWebSocketFrame(msg);
                    channel.writeAndFlush(tws);
                }
            }
        }

        log.debug("发送消息成功：type={}, slideId={}", message.getType(), message.getSlideId());
    }


    /**
     * . 拒绝不合法的请求，并返回错误信息
     */
    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        // 如果是非Keep-Alive，关闭连接
        if (!isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }



    /**
     * . 使用channelRead0不用释放资源,jvm会自动释放
     */

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        log.debug("收到消息：" + msg);
        // 测试msg是否是FullHttpRequest的类的实例   FullHttpRequest:请求参数中的信息
        if (msg instanceof FullHttpRequest) {
            //以http请求形式接入，但是走的是websocket
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            //处理websocket客户端的消息
            handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //添加连接
        log.debug("客户端加入连接：" + ctx.channel());
        ChannelSupervise.addChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //断开连接
        log.debug("客户端断开连接：" + ctx.channel());
        ChannelSupervise.removeChannel(ctx.channel());
        ChannelSupervise.removeChannelTest(ctx.channel());
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public void delete(Channel channel) {

        // 根据channel.id 删除map中的元素

        ChatGroup.CHANNEL_MAP.values().removeIf(value -> value.toString().contains(channel.toString()));

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        // 判断是否ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 仅支持文本消息，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            log.debug("仅支持文本消息，不支持二进制消息");
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }
        // 返回应答消息
        String request = ((TextWebSocketFrame) frame).text();
        log.debug("服务端收到：" + request);
        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString() + ctx.channel().id() + "：" + request);

        // 群发
        ChannelSupervise.send2All(tws);
        // 返回【谁发的发给谁】
        //        ctx.channel().writeAndFlush(tws);
    }

    /**
     * . 唯一的一次http请求，用于创建websocket
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        //要求Upgrade为websocket，过滤掉get/Post
        if (!req.decoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
            //若不是websocket方式，则创建BAD_REQUEST的req，返回给客户端
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws:/" + ctx.channel() + "/websocket", null, false);


        String type = (req.getUri().split("/")[req.getUri().split("/").length - 2]);
        // 判断socket连接类型
        if (Objects.equals(type, "slide")) {
            long slideId = Long.parseLong(req.getUri().split("/")[req.getUri().split("/").length - 1]);
            ChannelSupervise.addChannelTest(ctx.channel(), slideId);
        }

        handshaker = wsFactory.newHandshaker(req);

        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
            // 连接后发送数据
//            connectSend(slideId, ctx);
        }
    }

}
