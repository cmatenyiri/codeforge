package com.codeforge.config;

import com.codeforge.realtime.SubscriptionGuard;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Live updates: STOMP over WebSocket (with SockJS as the fallback), relayed to
 * RabbitMQ.
 *
 * <p>Screens that used to re-read the server on a timer — the contest lobby and
 * pages, the standings, a rejudge's progress, an interview's session — now
 * subscribe to a topic and re-read only when told something changed. See
 * {@link com.codeforge.realtime.Topics} for what there is to subscribe to.
 *
 * <p>The broker is RabbitMQ rather than Spring's in-memory one because there
 * may be more than one instance of this backend. An in-memory broker only
 * reaches the sessions connected to the instance that published; with the relay
 * every instance hands its sessions' subscriptions to RabbitMQ, which delivers a
 * message to all of them whichever instance sent it.
 *
 * <p>The endpoint sits behind the same security filter chain as the REST API,
 * so the handshake is authenticated by the JWT cookie like any other request.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties properties;
    private final CorsProperties corsProperties;
    private final SubscriptionGuard subscriptionGuard;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // The same allowlist as the REST API: a cross-origin page must
                // not be able to open a session on a visitor's cookie.
                .setAllowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        WebSocketProperties.Relay relay = properties.relay();
        registry.enableStompBrokerRelay("/topic")
                // The JVM's resolver rather than Netty's own, which on macOS
                // needs a native library and complains at every start without it.
                .setTcpClient(new ReactorNettyTcpClient<>(
                        client -> client.host(relay.host())
                                .port(relay.port())
                                .resolver(DefaultAddressResolverGroup.INSTANCE),
                        new StompReactorNettyCodec()))
                .setClientLogin(relay.login())
                .setClientPasscode(relay.passcode())
                .setSystemLogin(relay.login())
                .setSystemPasscode(relay.passcode())
                .setVirtualHost(relay.virtualHost());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(subscriptionGuard);
    }
}
