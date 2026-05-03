package com.example.pfesalesapp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class HomeController {

    // 🔹 Home page
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // 🔹 Upload + Send to Python + Show result
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {

        if (file.isEmpty()) {
            model.addAttribute("error", "No file selected ❌");
            return "result";
        }

        try {
            // Save file
            String uploadDir = System.getProperty("java.io.tmpdir") + "uploads/";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filePath = uploadDir + file.getOriginalFilename();
            java.io.File dest = new java.io.File(filePath);
            file.transferTo(dest);

            // Send file to Python
            java.net.URL url = new java.net.URL("http://localhost:5000/analyze");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            java.io.OutputStream outputStream = conn.getOutputStream();
            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(outputStream, "UTF-8"), true);

            writer.append("--" + boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getOriginalFilename() + "\"\r\n");
            writer.append("Content-Type: text/csv\r\n\r\n").flush();

            java.nio.file.Files.copy(dest.toPath(), outputStream);
            outputStream.flush();

            writer.append("\r\n").flush();
            writer.append("--" + boundary + "--").append("\r\n").flush();

            // Read response from Python
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Send result to HTML page
            model.addAttribute("result", response.toString());

            return "result";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "result";
        }
    }
}