package deu.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B파트 - 예약 로직/규칙 판단에서
 * 세미나/보강 목적 판별 유틸 메서드에 대한 단위 테스트.
 *
 * - private static boolean isSeminarOrMakeupPurpose(String purpose)
 *   메서드를 리플렉션으로 호출해서 검증한다.
 */
class ReservationServicePatternTest {

    /**
     * private static boolean isSeminarOrMakeupPurpose(String purpose)
     * 메서드를 리플렉션으로 꺼내오는 헬퍼.
     */
    private boolean callIsSeminarOrMakeupPurpose(String purpose) throws Exception {
        Method m = ReservationService.class
                .getDeclaredMethod("isSeminarOrMakeupPurpose", String.class);
        m.setAccessible(true); // private 메서드 접근 허용
        return (boolean) m.invoke(null, purpose); // static 메서드라 this = null
    }

    @Test
    @DisplayName("목적에 '세미나' 또는 '보강'이 포함되면 true를 반환해야 한다")
    void seminarOrMakeupPurpose_ShouldReturnTrue_WhenKeywordIncluded() throws Exception {
        assertTrue(callIsSeminarOrMakeupPurpose("세미나 발표"));
        assertTrue(callIsSeminarOrMakeupPurpose("보강 수업"));
        assertTrue(callIsSeminarOrMakeupPurpose(" [세미나] 알고리즘 스터디 "));
        assertTrue(callIsSeminarOrMakeupPurpose("네트워크 보강"));
    }

    @Test
    @DisplayName("목적이 null, 공백이거나 다른 용도면 false를 반환해야 한다")
    void seminarOrMakeupPurpose_ShouldReturnFalse_WhenInvalidOrOtherPurpose() throws Exception {
        assertFalse(callIsSeminarOrMakeupPurpose(null));
        assertFalse(callIsSeminarOrMakeupPurpose(""));
        assertFalse(callIsSeminarOrMakeupPurpose("   "));
        assertFalse(callIsSeminarOrMakeupPurpose("팀 프로젝트 회의"));
        assertFalse(callIsSeminarOrMakeupPurpose("수업 준비"));
    }
}
