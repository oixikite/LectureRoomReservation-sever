/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.moniter;

/**
 *
 * @author scq37
 */
//Context 에 해당
//관리자 모드
public class LogManager {
    private static ServerState state = new NormalState(); // 기본값

    public static void setState(ServerState newState) {
        state = newState;
        System.out.println("\n>>> 모니터링 모드 변경: " + newState.getClass().getSimpleName() + "\n");
    }
    
    // 핸들러가 호출할 메서드 (객체를 넘김)
    public static void logRequest(String clientIP, Object request) {
        state.logRequest(clientIP, request);
    }

    public static void log(String message) {
        state.log(message);
    }
}
