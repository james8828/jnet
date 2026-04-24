package com.jnet.anno.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Knife4j API 文档响应过滤器
 * 自动解码 Base64 编码的 API 文档响应
 *
 * @author JNet Team
 */
@Slf4j
@Component
@Order(1)
public class Knife4jApiDocFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String uri = httpRequest.getRequestURI();
        
        // 只拦截 /v3/api-docs 端点
        if (uri.contains("/v3/api-docs") && !uri.endsWith(".json")) {
            ResponseWrapper wrappedResponse = new ResponseWrapper(httpResponse);
            chain.doFilter(request, wrappedResponse);
            
            byte[] originalContent = wrappedResponse.getCaptureAsBytes();
            String originalText = new String(originalContent, StandardCharsets.UTF_8);
            
            // 检查是否是 Base64 编码的字符串（以引号开头和结尾）
            if (originalText.startsWith("\"") && originalText.endsWith("\"")) {
                try {
                    String base64Content = originalText.substring(1, originalText.length() - 1);
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Content);
                    String decodedJson = new String(decodedBytes, StandardCharsets.UTF_8);
                    
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    httpResponse.setCharacterEncoding("UTF-8");
                    httpResponse.setContentLength(decodedJson.getBytes(StandardCharsets.UTF_8).length);
                    httpResponse.getWriter().write(decodedJson);
                    httpResponse.getWriter().flush();
                    
                    log.debug("Decoded Base64 API docs, length: {}", decodedJson.length());
                } catch (Exception e) {
                    log.error("Failed to decode Base64 response", e);
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    httpResponse.setCharacterEncoding("UTF-8");
                    httpResponse.getWriter().write(originalText);
                    httpResponse.getWriter().flush();
                }
            } else {
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.setCharacterEncoding("UTF-8");
                httpResponse.getWriter().write(originalText);
                httpResponse.getWriter().flush();
            }
        } else {
            chain.doFilter(request, response);
        }
    }
    
    private static class ResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream capture;
        private ServletOutputStream output;
        private PrintWriter writer;
        
        public ResponseWrapper(HttpServletResponse response) {
            super(response);
            this.capture = new ByteArrayOutputStream();
        }
        
        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (writer != null) {
                throw new IllegalStateException("getWriter() already called");
            }
            if (output == null) {
                output = new ServletOutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        capture.write(b);
                    }
                    
                    @Override
                    public boolean isReady() {
                        return true;
                    }
                    
                    @Override
                    public void setWriteListener(WriteListener listener) {
                    }
                };
            }
            return output;
        }
        
        @Override
        public PrintWriter getWriter() throws IOException {
            if (output != null) {
                throw new IllegalStateException("getOutputStream() already called");
            }
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(capture, getCharacterEncoding()));
            }
            return writer;
        }
        
        @Override
        public void flushBuffer() throws IOException {
            if (writer != null) writer.flush();
            if (output != null) output.flush();
        }
        
        public byte[] getCaptureAsBytes() throws IOException {
            if (writer != null) writer.flush();
            if (output != null) output.flush();
            return capture.toByteArray();
        }
    }
}
