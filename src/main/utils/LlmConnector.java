package main.utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LlmConnector {

    private static final String SERVER_URL = "http://localhost:5000/ask";

    public static String getResponse(String role, String context, String instruction) {
        try{
            //Configurare conexiune
            URL url = new URL(SERVER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            //Construire JSON folosind JSONObject pentru a fi clar ce se intampla
            JSONObject jsonInput = new JSONObject();
            jsonInput.put("agent_role", role);
            jsonInput.put("context", context);
            jsonInput.put("instruction", instruction);

            //Trimite Request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            //Citire Raspuns
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                // Parsam raspunsul primit de la Python
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.getString("status").equals("success")) {
                    return jsonResponse.getString("reply");
                } else {
                    return "Eroare de la serverul Python.";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Nu pot contacta 'creierul' (Serverul Python e oprit?)";
        }
    }
}
