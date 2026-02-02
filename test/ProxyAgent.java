package test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;

public class ProxyAgent implements ClassFileTransformer {

    public static void premain(String agentArgs, Instrumentation inst) {
        startProxy();
        inst.addTransformer(new ProxyAgent());
    }

    public static void log(String msg) {
        System.out.println("[NoChatRestrictions] " + msg);
    }

    public static void startProxy() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8888), 0);
            server.createContext("/", new ProxyHandler());
            server.setExecutor(null);
            server.start();
            log("Started proxy");
        } catch (Exception e) {
            log("Proxy failed: " + e.getMessage());
        }
    }

    static class ProxyHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if (path.contains("/player/attributes") || path.startsWith("/privileges")) {
                log("Blocked " + path);
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            forwardRequest(exchange);
        }

        public void forwardRequest(HttpExchange exchange) throws IOException {
            HttpURLConnection c = null;
            try {
                URL targetUrl = new URL("https://api.minecraftservices.com" + exchange.getRequestURI().toString());
                c = (HttpURLConnection) targetUrl.openConnection();
                
                String method = exchange.getRequestMethod();
                c.setRequestMethod(method);
        
                for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
                    String key = header.getKey();
                    if (key != null && !key.equalsIgnoreCase("Host") && !key.equalsIgnoreCase("Content-Length")) {
                        c.setRequestProperty(key, String.join(",", header.getValue()));
                    }
                }
        
                if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("HEAD")) {
                    c.setDoOutput(true);
                    try (InputStream is = exchange.getRequestBody(); 
                         OutputStream os = c.getOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                    }
                }
        
                int responseCode = c.getResponseCode();
                
                InputStream respStream = (responseCode >= 400) ? c.getErrorStream() : c.getInputStream();
        
                exchange.getResponseHeaders().clear();
                for (Map.Entry<String, List<String>> header : c.getHeaderFields().entrySet()) {
                    String key = header.getKey();
                    if (key != null && !key.equalsIgnoreCase("Transfer-Encoding") && !key.equalsIgnoreCase("Content-Length")) {
                        exchange.getResponseHeaders().put(key, header.getValue());
                    }
                }
        
                if (respStream == null) {
                    exchange.sendResponseHeaders(responseCode, -1);
                } else {
                    exchange.sendResponseHeaders(responseCode, 0); 
                    try (OutputStream os = exchange.getResponseBody()) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = respStream.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                    }
                    respStream.close();
                }
        
            } catch (Exception e) {
                log(e.getMessage());
                try { 
                    exchange.sendResponseHeaders(502, -1); 
                } catch (Exception ignored) {

                }
            } finally {
                if (c != null) {
                    c.disconnect();
                }
                exchange.close();
            }
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className != null && className.equals("com/mojang/authlib/yggdrasil/YggdrasilEnvironment")) {
            return replace(classfileBuffer);
        }
        return null;
    }

    public static byte[] replace(byte[] buffer) {
        byte[] target = "https://api.minecraftservices.com".getBytes();
        byte[] replacement = "http://aaaaaaaaaaa.localhost:8888".getBytes();
        
        for (int i = 0; i <= buffer.length - target.length; i++) {
            boolean match = true;
            for (int j = 0; j < target.length; j++) {
                if (buffer[i + j] != target[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                System.arraycopy(replacement, 0, buffer, i, replacement.length);
                log("Code modification successful!");
            }
        }
        return buffer;
    }
}
