package deu;

import java.net.ServerSocket;
import java.net.Socket;

import deu.repository.LectureRepository;
import deu.model.enums.Semester;
import deu.model.entity.Lecture;
import java.util.List;

public class ServerMain {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("================================================================================");
            System.out.println("");
            System.out.println("    서버 실행 중... 포트 번호: 8080");
            System.out.println("");
            System.out.println("================================================================================");
            
             // [TEST] 연/학기+강의실 필터 동작 확인  - 기능 구현 끝나면 삭제할 예정
        LectureRepository repo = LectureRepository.getInstance();
        List<Lecture> test = repo.findRoomLectures(2025, Semester.FIRST, "정보관", "9", "911"); // ← 테스트 조건
        System.out.println("[TEST] 2025-1학기 정보관 9층 911 강의 수 = " + test.size());
        for (Lecture lec : test) {
            System.out.println(" - " + lec.getId() + " | " + lec.getTitle() + " | " 
                               + lec.getDay() + " " + lec.getStartTime() + "~" + lec.getEndTime());
        }
        
        // [TEST-STEP1] AdminLectureService 간단 검증  - 기능 구현 끝나면 삭제
{
    deu.service.AdminLectureService svc = new deu.service.AdminLectureService();

    // 1) 조회
    var list = svc.listByRoom(2025, Semester.FIRST, "정보관", "9", "911");
    System.out.println("[ADMIN-TEST] 2025-1 정보관 9층 911 개수 = " + list.size());

    // 2) 저장(수정/신규) 샘플 - 기존 항목과 겹치지 않는 시간으로 테스트
    Lecture tmp = new Lecture();
    tmp.setId("TEST-NEW-001");
    tmp.setTitle("테스트과목");
    tmp.setBuilding("정보관");
    tmp.setFloor("9");
    tmp.setLectureroom("911");
    tmp.setProfessor("관리자");
    tmp.setDay("FRIDAY");
    tmp.setStartTime("08:00");
    tmp.setEndTime("08:50");
    tmp.setYear(2025);
    tmp.setSemester(Semester.FIRST);

    var r1 = svc.upsert(tmp);
    System.out.println("[ADMIN-TEST] upsert 결과 = " + r1.code() + " / " + r1.message());

    // 3) 삭제
    var r2 = svc.delete("TEST-NEW-001");
    System.out.println("[ADMIN-TEST] delete 결과 = " + r2.code() + " / " + r2.message());
}


            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new ClientHandler(client)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }      
    }   
}
