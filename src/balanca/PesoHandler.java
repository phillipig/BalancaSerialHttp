/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package balanca;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Phillipi
 */
public class PesoHandler implements HttpHandler {
    
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    
    private String porta; //{"COM1","COM2","COM3","COM4"};
    private int baudRate; //{2400, 4800, 9600, 1200, 19200};
    private int databits; //{SerialPort.DATABITS_7, SerialPort.DATABITS_8};
    private int paridade; //{SerialPort.PARITY_NONE, SerialPort.PARITY_ODD, SerialPort.PARITY_EVEN, SerialPort.PARITY_SPACE};
    
    @Override
    public void handle(HttpExchange he) {
        try {
            if (he.getRequestMethod().equalsIgnoreCase("POST")) {
                Headers requestHeaders = he.getRequestHeaders();
                int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
                byte[] rawContent = new byte[contentLength];
                try (InputStream is = he.getRequestBody()) {
                    is.read(rawContent);
                }
                
                String content = new String(rawContent, CHARSET);
                String responseBody = peso(content);
                final byte[] rawResponseBody = responseBody.getBytes(CHARSET);

                Headers responseHeaders = he.getResponseHeaders();
                responseHeaders.set("Content-Type", String.format("application/json; charset=%s", CHARSET));
                responseHeaders.set("Access-Control-Allow-Origin", "*");
                responseHeaders.set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");
                responseHeaders.set("Access-Control-Allow-Credentials", "true");
                responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
                he.sendResponseHeaders(HttpURLConnection.HTTP_OK, rawResponseBody.length);
                try (OutputStream os = he.getResponseBody()) {
                    os.write(rawResponseBody);
                }
                he.close();
            }else{
                he.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
                he.close();
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    private String peso(String corpo){
        try{
            JSONObject json = new JSONObject(corpo);
            porta = json.getString("porta");
            baudRate = json.getInt("baudRate");
            databits = json.getInt("databits");
            paridade = json.getInt("paridade");
            byte[] peso = serialJSSC();
            JSONObject res = new JSONObject();
            if(peso != null){
                res.put("peso", bytesParaDouble(peso));
            }else{
                res.put("peso", -1);
            }
            return res.toString();
        }catch(JSONException ex){
            JSONObject res = new JSONObject();
            res.put("erro", ex.getMessage());
            return res.toString();
        }
    }
    
    public byte[] serialJSSC(){
        SerialPort serialPort = new SerialPort(porta);
        try {
            System.out.println("Porta Aberta: "+serialPort.openPort());
            System.out.println("Parametros: "+serialPort.setParams(baudRate, databits, SerialPort.STOPBITS_1, paridade));
            System.out.println("<ENQ>: "+serialPort.writeBytes(new byte[]{0x05}));
            byte[] buffer = serialPort.readBytes(7, 3000);
            System.out.println(new String(buffer));
            System.out.println("Porta Fechada: "+serialPort.closePort());
            return buffer;
        }catch (SerialPortTimeoutException | SerialPortException ex) {
            System.out.println(ex);
            try {
                System.out.println("Porta Fechada: "+serialPort.closePort());
            } catch (SerialPortException ex1) {
                System.out.println(ex1);
            }
            return null;
        }
    }
    
    private double bytesParaDouble(byte[] lista){
        String peso = bytesParaString(lista);
        int t = peso.length();
        try{
            return Double.valueOf(peso.substring(0, t-4)+"."+peso.substring(t-4, t));
        }catch(NumberFormatException ex){
            System.out.println("Erro ao converter peso para double.\n"+ex.getMessage());
            if(peso.contains("IIIII")) return -2;
            if(peso.contains("NNNNN")) return -3;
            if(peso.contains("SSSSS")) return -4;
            return -1;
        }
    }
    
    private String bytesParaString(byte[] lista){
        char[] retornoChar = new char[lista.length];
        for (int i=1; i<lista.length-1; i++) retornoChar[i] = (char) lista[i];
        return new String(retornoChar);
    }
}