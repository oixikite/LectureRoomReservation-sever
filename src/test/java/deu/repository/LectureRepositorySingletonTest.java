/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.repository;

/**
 *
 * @author scq37
 */

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LectureRepository의 싱글톤 패턴(Singleton Pattern) 무결성 검증 테스트
 * 이 테스트 클래스는 다음 두 가지 핵심 원칙을 검증
 * 1. 객체 동일성(Identity): getInstance()를 통해 반환된 인스턴스가 메모리상에서 동일한 객체인가?
 * 2. 생성자 은닉(Information Hiding): 생성자가 private으로 선언되어 외부 생성이 차단되었는가? (Reflection 활용)
 */

public class LectureRepositorySingletonTest {
 @Test
    @DisplayName("싱글톤 원칙 1: getInstance()를 여러 번 호출해도 항상 동일한 객체(주소)를 반환해야 한다.")
    void testSingletonIdentity() {
        System.out.println("\n=== [Test 1] 싱글톤 인스턴스 동일성(Identity) 검사 시작 ===");
        
        // given
        LectureRepository instance1 = LectureRepository.getInstance();
        LectureRepository instance2 = LectureRepository.getInstance();

        // 시각화: 객체의 고유 해시코드(메모리 주소 기반 ID)를 출력
        System.out.println("1. 첫 번째 호출 객체 주소(Hash): " + System.identityHashCode(instance1));
        System.out.println("2. 두 번째 호출 객체 주소(Hash): " + System.identityHashCode(instance2));

        // when & then
        if (instance1 == instance2) {
            System.out.println(">> 검증 성공: 두 변수가 가리키는 메모리 주소가 완벽히 일치합니다.");
        } else {
            System.out.println(">> 검증 실패: 두 변수가 서로 다른 객체를 가리킵니다.");
        }

        assertSame(instance1, instance2, "두 인스턴스는 동일한 참조(Reference)여야 합니다.");
        System.out.println("=== [Test 1] 종료 ===\n");
    }

    @Test
    @DisplayName("싱글톤 원칙 2: 생성자는 private이어야 하며, 외부에서 new로 생성할 수 없어야 한다.")
    void testConstructorIsPrivate() throws NoSuchMethodException {
        System.out.println("\n=== [Test 2] 생성자 은닉성(Private) 검사 시작 ===");

        // given
        Constructor<LectureRepository> constructor = LectureRepository.class.getDeclaredConstructor();
        
        // 시각화: 생성자의 접근 제어자 확인
        String modifiers = Modifier.toString(constructor.getModifiers());
        System.out.println("1. 생성자의 접근 제어자: [" + modifiers + "]");

        // when & then
        boolean isPrivate = Modifier.isPrivate(constructor.getModifiers());
        
        if (isPrivate) {
            System.out.println(">> 검증 성공: 생성자가 private으로 보호되어 있습니다.");
        } else {
            System.out.println(">> 검증 실패: 생성자가 공개되어 있습니다 (public/protected 등).");
        }

        assertTrue(isPrivate, "생성자는 반드시 private이어야 합니다.");
        System.out.println("=== [Test 2] 종료 ===\n");
    }   
}
