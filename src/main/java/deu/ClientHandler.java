package deu;

import deu.controller.SystemController;
import deu.moniter.LogManager;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        
         String clientIP = (socket.getInetAddress() != null) ? socket.getInetAddress().getHostAddress() : "Unknown";
        
        try {
            // 순서 주의: 반드시 OutputStream을 먼저 생성
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // flush header
            in = new ObjectInputStream(socket.getInputStream());
            
            while(true){
            // 요청 수신
            Object request = in.readObject();
            
            LogManager.logRequest(clientIP, request);

            // 요청 처리
            Object response = new SystemController().handle(request);

            // 응답 송신
            out.writeObject(response);
            out.flush();
            }

        } catch (Exception e) {
//            System.err.println("[ClientHandler] 통신 중 오류 발생:");
//            e.printStackTrace();
// 에러 로그도 LogManager를 통해 기록
            //LogManager.log("[ERROR] 통신 중 오류 (" + clientIP + "): " + e.getMessage());
            // e.printStackTrace(); // 필요 시 주석 해제
        } finally {
            // 명시적으로 자원 정리
            try {
                if (in != null) in.close();
            } catch (IOException ignored) {}
            try {
                if (out != null) out.close();
            } catch (IOException ignored) {}
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}