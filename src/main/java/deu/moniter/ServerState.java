/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package deu.moniter;

/**
 *
 * @author scq37
 */

//인터페이스에 해당  - 모든 상태가 공통으로 구현해야 할 메서드 정의 
public interface ServerState { 
    //요청 객체를 통째로 받아서 상태가 알아서 판단하게 함
    void logRequest(String clientIP, Object request);
    
    void log(String message);
}
