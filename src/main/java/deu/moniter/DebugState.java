/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.moniter;

/**
 *
 * @author scq37
 */

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import deu.model.dto.request.command.UserCommandRequest;

//ConcreteState(구체적 상태) - 실제 동작(로깅 여부 판단, 필터링)을 수행하는 곳. 여기에 if문이 있어야 
//디버그 모드 - 상세 출력할 수 있도록

public class DebugState implements ServerState {
    
    @Override
    public void logRequest(String clientIP, Object request) {
        String logType = request.getClass().getSimpleName();
        boolean isNoise = false;
        
        
        // =======================================================
        // [필터링 로직]
        // =======================================================
        
        // (1) 알림, 예약 조회 등 기본 소음 필터링
        if (logType.contains("Notification") || logType.contains("Reservation")) {
            isNoise = true;
        } 
        // (2) UserCommandRequest 세부 필터링
        else if (request instanceof UserCommandRequest) {
            UserCommandRequest uReq = (UserCommandRequest) request;
            String command = uReq.getType(); 

            // "동시접속자" 또는 "사용자 이름 반환"은 소음으로 간주
            if ("동시접속자".equals(command) || "사용자 이름 반환".equals(command)) {
                isNoise = true;
            } else {
                // 로그인, 회원가입 등은 중요하니까 구체적으로 기록
                logType = "UserReq(" + command + ")";
            }
        }

        // (3) 소음이 아닐 때만 출력
        if (!isNoise) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.println("[DEBUG " + time + "] [요청 처리] " + clientIP + " : " + logType);
        }
    }
    
    @Override
    public void log(String message){
        // 시간 정보를 포함해서 모든 로그 출력
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[DEBUG " + time + "] " + message);
    }
    
}
