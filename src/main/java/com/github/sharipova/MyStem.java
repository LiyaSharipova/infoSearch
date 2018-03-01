package com.github.sharipova;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class MyStem {

    public static String stem(String word) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder("./mystem",
                "-ln","-d","-e", "utf-8",
                "--format", "text");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String result;

        try (OutputStream in = process.getOutputStream();
             InputStream out = process.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(out, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(in, StandardCharsets.UTF_8))) {
            writer.write(word + "\n");
            writer.flush();
            result = reader.readLine();
        }
        return result;
    }

}