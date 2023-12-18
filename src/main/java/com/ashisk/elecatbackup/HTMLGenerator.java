package com.ashisk.elecatbackup;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class HTMLGenerator {
    private Connection connection;

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_HOSTNAME = "localhost";
    private int port;
    private String hostname;

    public HTMLGenerator() {
        this.port = DEFAULT_PORT;
        this.hostname = DEFAULT_HOSTNAME;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String generatePluginInfoTable(String tableName, Connection connection) {
        String htmlContent = generateHTMLTable(connection, tableName);
        return htmlContent;
    }

    public String startServerAndReturnURL(String htmlContent) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(hostname, port), 0);
            String serverUrl = "http://" + hostname + ":" + port + "/";
            server.createContext("/", new HTMLHandler(htmlContent));
            server.createContext("/update", new UpdateHandler());
            server.setExecutor(null);
            server.start();
            return serverUrl;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String generateHTMLTable(Connection connection, String tableName) {
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><body><table>");

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tableName);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String pluginName = resultSet.getString("PluginName");
                String pluginVersion = resultSet.getString("PluginVersion");
                int pluginId = resultSet.getInt("PluginId");
                String pluginAuthor = resultSet.getString("PluginAuthor");
                String pluginComment = resultSet.getString("PluginComment");
                htmlBuilder.append("<tr><td>").append(pluginId).append("</td><td>").append(pluginName)
                        .append("</td><td>").append(pluginVersion).append("</td><td>").append(pluginAuthor)
                        .append("</td><td><input type='text' value='").append(pluginComment)
                        .append("' onchange='updateComment(this.value, ").append(pluginId).append(")'></td></tr>");
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        htmlBuilder.append("</table></body><script>\n" +
                "function updateComment(comment, pluginId) {\n" +
                "  var xhr = new XMLHttpRequest();\n" +
                "  xhr.open('POST', '/update', true);\n" +
                "  xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');\n" +
                "  xhr.onreadystatechange = function() {\n" +
                "    if (xhr.readyState === 4 && xhr.status === 200) {\n" +
                "      console.log('Comment updated successfully');\n" +
                "    }\n" +
                "  };\n" +
                "  xhr.send('pluginId=' + pluginId + '&pluginComment=' + encodeURIComponent(comment));\n" +
                "}\n" +
                "</script></html>");

        return htmlBuilder.toString();
    }

    static class HTMLHandler implements HttpHandler {
        private final String htmlContent;

        public HTMLHandler(String htmlContent) {
            this.htmlContent = htmlContent;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, htmlContent.getBytes().length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(htmlContent.getBytes());
            outputStream.close();
        }
    }

    class UpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                Map<String, String> requestData = parseFormData(requestBody);
                String comment = requestData.get("pluginComment");
                int pluginId = Integer.parseInt(requestData.get("pluginId"));
                updatePluginComment(pluginId, comment);
                String response = "Comment updated successfully";
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            } else {
                String response = "Invalid request";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response.getBytes());
                outputStream.close();
            }
        }

        private Map<String, String> parseFormData(String formData) {
            Map<String, String> data = new HashMap<>();
            String[] pairs = formData.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    data.put(key, value);
                }
            }
            return data;
        }

        public void updatePluginComment(int pluginId, String comment) {
            try {
                PreparedStatement statement = connection.prepareStatement("UPDATE plugin_info SET PluginComment = ? WHERE PluginId = ?");
                statement.setString(1, comment);
                statement.setInt(2, pluginId);
                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}