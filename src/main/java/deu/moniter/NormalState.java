/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.moniter;

/**
 *
 * @author scq37
 */

//일반 모드 - 일단은 에러만 출력하도록

public class NormalState implements ServerState {
    @Override
    public void log(String message){
        //[ERRor] 태그가 붙은 중요 매시지만 출력
        if (message.startsWith("[ERROR]") || message.contains("Exception")) {
            System.out.println(" " + message);
        }
    }
}
