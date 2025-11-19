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

//디버그 모드 - 상세 출력할 수 있도록

public class DebugState implements ServerState {
    @Override
    public void log(String message){
        // 시간 정보를 포함해서 모든 로그 출력
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[DEBUG " + time + "] " + message);
    }
    
}
