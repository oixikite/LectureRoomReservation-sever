package deu;

import deu.controller.SystemController;
import deu.model.dto.request.command.UserCommandRequest;
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
            
            // 요청 수신
            Object request = in.readObject();
            
           // ==================================================================
                // [모니터링 필터링 로직]
                // ==================================================================
                String logType = request.getClass().getSimpleName();
                boolean isNoise = false;

                // (1) 알림, 예약 조회 등 기본 소음 필터링
                if (logType.contains("Notification") || logType.contains("Reservation")) {
                    isNoise = true;
                }
                // (2) "동시 접속자 수" 확인(UserCommandRequest) 필터링
                else if (request instanceof UserCommandRequest) {
                    UserCommandRequest uReq = (UserCommandRequest) request;
                    
                    // 요청 타입이 "동시접속자"라면 소음으로 간주
                    if ("동시접속자".equals(uReq.getType())) {
                        isNoise = true;
                    } else {
                        // 로그인, 회원가입 등은 중요하니까 구체적으로 기록
                        logType = "UserReq(" + uReq.getType() + ")";
                    }
                }

                // 소음이 아닐 때만 로그 출력
                if (!isNoise) {
                    LogManager.log("[요청 처리] " + clientIP + " : " + logType);
                }
                // ==================================================================

            // 요청 처리
            Object response = new SystemController().handle(request);

            // 응답 송신
            out.writeObject(response);
            out.flush();

        } catch (Exception e) {
//            System.err.println("[ClientHandler] 통신 중 오류 발생:");
//            e.printStackTrace();
// 에러 로그도 LogManager를 통해 기록
            LogManager.log("[ERROR] 통신 중 오류 (" + clientIP + "): " + e.getMessage());
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