/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package balanca;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 *
 * @author Phillipi
 */
public class Balanca {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(9600), 0);
            httpServer.createContext("/", new PesoHandler());
            httpServer.setExecutor(Executors.newSingleThreadExecutor());
            httpServer.start();
            System.out.println("Leitor de Balança v1.0\nPhillipi O. Giobini");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
