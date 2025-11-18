/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.moniter;

/**
 *
 * @author scq37
 */

//관리자 모드
public class LogManager {
    private static ServerState state = new NormalState(); // 기본값

    public static void setState(ServerState newState) {
        state = newState;
        System.out.println("\n>>> 모니터링 모드 변경: " + newState.getClass().getSimpleName() + "\n");
    }

    public static void log(String message) {
        state.log(message);
    }
}
