package deu;

import deu.model.dto.request.command.LectureCommandRequest;
import deu.model.dto.request.data.lecture.LectureFilterRequest;
import deu.model.dto.response.LectureListResponse;
import deu.model.entity.Lecture;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * 서버에 "강의실 강의 조회" 명령을 보내는 간단한 클라이언트 테스트 클래스.
 */
public class LectureClientTest {
    public static void main(String[] args) {
        String host = "127.0.0.1";  // 서버 IP
        int port = 8080;            // 서버 포트

        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // ✅ 요청 데이터 구성
            LectureFilterRequest filter = new LectureFilterRequest(
                    2025, "FIRST", "정보관", "9", "911"
            );

            // ✅ 명령 생성 (SystemController의 switch문과 정확히 일치해야 함)
            LectureCommandRequest request = new LectureCommandRequest("강의실 강의 조회", filter);

            // ✅ 요청 전송
            System.out.println("[CLIENT] 요청 전송 → " + filter.getBuilding() + " " + filter.getFloor() + "층 " + filter.getLectureroom() + "호");
            out.writeObject(request);
            out.flush();

            // ✅ 응답 수신
            Object response = in.readObject();

            // ✅ 응답 타입 검증
            if (response instanceof LectureListResponse res) {
                System.out.println("[CLIENT] 서버 응답 수신 성공!");
                System.out.println("상태: " + res.getStatus());
                System.out.println("메시지: " + res.getMessage());
                System.out.println("총 강의 수: " + res.getTotalCount());

                List<Lecture> lectures = res.getLectures();
                for (Lecture lec : lectures) {
                    System.out.printf(" - %s | %s | %s %s~%s%n",
                            lec.getId(),
                            lec.getTitle(),
                            lec.getDay(),
                            lec.getStartTime(),
                            lec.getEndTime());
                }

            } else {
                System.out.println("[CLIENT] ⚠ 예상치 못한 응답 타입: " + response.getClass().getName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
