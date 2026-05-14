package com.mavi;

import java.io.IOException;
import java.io.BufferedReader;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;


@WebServlet("/api/services/*")
public class ServiceOperationsServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    // READ Operations
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setAccessControlHeaders(response);
        response.setContentType("application/json");
        StringBuilder jsonResult = new StringBuilder("[");
        
        try (Connection conn = DBconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM system_services");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                jsonResult.append(String.format(
                    "{\"id\":%d,\"title\":\"%s\",\"category\":\"%s\",\"description\":\"%s\",\"slaPercentage\":%.2f},",
                    rs.getInt("id"), rs.getString("title"), rs.getString("category"), 
                    rs.getString("description").replace("\"", "\\\""), rs.getDouble("sla_percentage")
                ));
            }
            if (jsonResult.length() > 1) jsonResult.setLength(jsonResult.length() - 1);
            jsonResult.append("]");
            response.getWriter().write(jsonResult.toString());
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // CREATE Operation
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setAccessControlHeaders(response);
        String json = readPayload(request);
        
        String sql = "INSERT INTO system_services (title, category, description, sla_percentage) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, extractJsonValue(json, "title"));
            stmt.setString(2, extractJsonValue(json, "category"));
            stmt.setString(3, extractJsonValue(json, "description"));
            stmt.setDouble(4, Double.parseDouble(extractJsonValue(json, "slaPercentage")));
            stmt.executeUpdate();
            response.setStatus(HttpServletResponse.SC_CREATED);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // UPDATE Operation
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setAccessControlHeaders(response);
        String pathInfo = request.getPathInfo();
        String id = pathInfo.substring(1);
        String json = readPayload(request);

        String sql = "UPDATE system_services SET title=?, category=?, description=?, sla_percentage=? WHERE id=?";
        try (Connection conn = DBconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, extractJsonValue(json, "title"));
            stmt.setString(2, extractJsonValue(json, "category"));
            stmt.setString(3, extractJsonValue(json, "description"));
            stmt.setDouble(4, Double.parseDouble(extractJsonValue(json, "slaPercentage")));
            stmt.setInt(5, Integer.parseInt(id));
            stmt.executeUpdate();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE Operation
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setAccessControlHeaders(response);
        String pathInfo = request.getPathInfo();
        String id = pathInfo.substring(1);

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM system_services WHERE id=?")) {
            stmt.setInt(1, Integer.parseInt(id));
            stmt.executeUpdate();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String readPayload(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if(start == -1) {
            pattern = "\"" + key + "\":";
            start = json.indexOf(pattern) + pattern.length();
            int end = json.indexOf(",", start);
            if(end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
        start += pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private void setAccessControlHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}

