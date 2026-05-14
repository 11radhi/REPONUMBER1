package com.mavi;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/auth/*")

public class AuthenticationServlet extends HttpServlet {
	@Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
}
	
	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setAccessControlHeaders(response);
        response.setContentType("application/json");
        
        String pathInfo = request.getPathInfo();
        StringBuilder jsonBuffer = new StringBuilder();
        String line;
        
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonBuffer.append(line);
            }
        }

        String json = jsonBuffer.toString();
        
        try (Connection conn = DBconnection.getConnection()) {
            if ("/signup".equals(pathInfo)) {
                // Crude JSON parsing tool alternative for low-dependency setups
                String name = extractJsonValue(json, "name");
                String email = extractJsonValue(json, "email");
                String password = extractJsonValue(json, "password");

                String sql = "INSERT INTO platform_users (full_name, email, password_string) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, name);
                    stmt.setString(2, email);
                    stmt.setString(3, password);
                    stmt.executeUpdate();
                    response.getWriter().write("{\"success\":true,\"message\":\"Account registered successfully.\"}");
                }
            } else if ("/login".equals(pathInfo)) {
                String email = extractJsonValue(json, "email");
                String password = extractJsonValue(json, "password");

                String sql = "SELECT full_name FROM platform_users WHERE email = ? AND password_string = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, email);
                    stmt.setString(2, password);
                    ResultSet rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        response.getWriter().write("{\"success\":true,\"token\":\"session-active-token\",\"user\":\"" + rs.getString("full_name") + "\"}");
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"success\":false,\"message\":\"Invalid email or security passkey.\"}");
                    }
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"message\":\"Database transaction failure: " + e.getMessage() + "\"}");
        }
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern) + pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private void setAccessControlHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}
