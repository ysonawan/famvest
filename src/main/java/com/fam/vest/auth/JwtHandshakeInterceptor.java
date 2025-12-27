package com.fam.vest.auth;

import com.fam.vest.pojo.StompPrincipal;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            String query = servletRequest.getServletRequest().getQueryString();

            if (query != null) {
                // parse token param from query string
                Map<String, String> queryParams = Arrays.stream(query.split("&"))
                        .map(s -> s.split("="))
                        .filter(arr -> arr.length == 2)
                        .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));

                String token = queryParams.get("token");
                String username = jwtUtil.extractUsername(token);
                if (token != null && jwtUtil.isTokenValid(token, username)) {
                    attributes.put("user", new StompPrincipal(username));
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception ex) {
    }
}
