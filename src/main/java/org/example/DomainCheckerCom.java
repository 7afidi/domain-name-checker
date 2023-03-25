package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DomainCheckerCom {

    public static void main(String[] args) {
        List<String> domains = readWordsFromFile("words.txt");
        List<String> availableDomains = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < domains.size(); i++) {
            String domain = domains.get(i);
            String fullDomain = domain + ".com";
            executor.execute(new DomainCheckTask(fullDomain, i, availableDomains));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        writeToFile(availableDomains, "available_domains.txt");
    }

    public static List<String> readWordsFromFile(String fileName) {
        List<String> words = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return words;
    }

    public static boolean checkDomainAvailability(String domain) {
        try {
            Socket socket = new Socket("whois.verisign-grs.com", 43);
            String query = domain + "\r\n";
            socket.getOutputStream().write(query.getBytes("UTF-8"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response;
            while ((response = reader.readLine()) != null) {
                if (response.contains("No match for")) {
                    return true;
                }
            }
            reader.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void writeToFile(List<String> availableDomains, String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName);
            for (String domain : availableDomains) {
                writer.write(domain + "\n");
            }
            writer.close();
            System.out.println("Available domains written to " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class DomainCheckTask implements Runnable {
        private final String domain;
        private final int index;
        private final List<String> availableDomains;

        public DomainCheckTask(String domain, int index, List<String> availableDomains) {
            this.domain = domain;
            this.index = index;
            this.availableDomains = availableDomains;
        }

        @Override
        public void run() {
            boolean isAvailable = checkDomainAvailability(domain);
            if (isAvailable) {
                System.out.println("[" + index + "] " + domain + " is available!");
                availableDomains.add(domain);
            } else {
                System.out.println("[" + index + "] " + domain + " is taken!");
            }
        }
    }
}
