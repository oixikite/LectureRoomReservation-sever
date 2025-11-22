/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.repository;

/**
 *
 * @author scq37
 */

import deu.model.entity.Lecture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 싱글톤 및 동시성 제어(Synchronized) 검증 전용 테스트  (안전성 테스트)
 */

public class LectureRepositoryConcurrencyTest {
    @Test
    @DisplayName("멀티스레드 환경: 100개의 스레드가 동시에 저장 요청을 보내도 데이터 누락이 없어야 한다.")
    void testConcurrentSave() throws InterruptedException {
        // given
        LectureRepository repo = LectureRepository.getInstance();
        int threadCount = 100;
        
        // 스레드 풀 생성 (32개 스레드로 100개 작업 처리)
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 테스트 전 데이터 개수 파악
        int initialCount = repo.findAll().size();

        // when
        for (int i = 0; i < threadCount; i++) {
            int index = i;
            executorService.submit(() -> {
                try {
                    Lecture lecture = new Lecture();
                    lecture.setId("CONCURRENT_TEST_" + index);
                    lecture.setTitle("동시성테스트강의");
                    repo.save(lecture); // 동시 다발적 호출
                } finally {
                    latch.countDown(); // 작업 완료 알림
                }
            });
        }

        latch.await(); // 100개 작업이 끝날 때까지 대기

        // then
        int finalCount = repo.findAll().size();
        
        // 데이터 정리 (테스트용 데이터 삭제)
        for (int i = 0; i < threadCount; i++) {
            repo.deleteById("CONCURRENT_TEST_" + i);
        }

        // 검증: 만약 싱글톤 동기화가 깨졌다면 finalCount는 (initial + 100)보다 작게 나옴
        assertEquals(initialCount + threadCount, finalCount, 
            "동시성 제어가 완벽하다면 요청한 100개가 모두 저장되어야 합니다.");
    }
    
}
