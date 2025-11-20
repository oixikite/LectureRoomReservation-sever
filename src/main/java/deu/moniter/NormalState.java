/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.moniter;

/**
 *
 * @author scq37
 */
//ConcreteState(구체적 상태) - 실제 동작(로깅 여부 판단, 필터링)을 수행하는 곳. 여기에 if문이 있어야 
//일반 모드 - 일단은 에러만 출력하도록

public class NormalState implements ServerState {
    
    @Override
    public void logRequest(String clientIP, Object request) {
        // Normal 모드에서는 정상 요청 로그를 남기지 않음. (무시)
    }
    
    @Override
    public void log(String message){
        //[ERRor] 태그가 붙은 중요 매시지만 출력
        if (message.startsWith("[ERROR]") || message.contains("Exception")) {
            System.out.println(" " + message);
        }
    }
}
