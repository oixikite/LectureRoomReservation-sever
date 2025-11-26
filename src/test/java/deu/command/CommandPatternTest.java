package deu.command;

import deu.model.entity.RoomReservation;
import deu.repository.ReservationRepository;

import org.junit.jupiter.api.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CommandPatternTest {

    private static final String BACKUP_PATH =
            System.getProperty("user.dir") + "/data/test-backup.yaml";

    private ReservationRepository repo;

    @BeforeEach
    void setUp() {
        repo = ReservationRepository.getInstance();
        repo.clear(); // 테스트 전 Repository 초기화
    }

    @AfterEach
    void cleanUp() {
        File file = new File(BACKUP_PATH);
        if (file.exists()) file.delete(); // 테스트 후 파일 정리
    }

    // ============================================================
    // 1) BackupCommand execute() 동작 테스트
    // ============================================================
    @Test
    void testBackupCommandExecute() {
        // given
        BackupCommand command = new BackupCommand(repo, BACKUP_PATH);

        // when
        command.execute();

        // then
        File backupFile = new File(BACKUP_PATH);
        assertTrue(backupFile.exists(), "❌ 백업 파일이 생성되지 않았습니다.");
    }

    // ============================================================
    // 2) RestoreCommand execute() 동작 테스트
    // ============================================================
    @Test
    void testRestoreCommandExecute() {
        // given: 예약 한 개 생성
        RoomReservation r = RoomReservation.builder()
                .id("A1")
                .title("Test")
                .buildingName("정보관")
                .floor("9")
                .lectureRoom("911")
                .date(LocalDate.now().toString())
                .startTime("09:00")
                .endTime("10:00")
                .number("S1234")
                .status("승인")
                .build();

        repo.save(r);

        // (1) 백업 실행
        BackupCommand backupCmd = new BackupCommand(repo, BACKUP_PATH);
        backupCmd.execute();

        // (2) 원본 repo 초기화
        repo.clear();
        assertEquals(0, repo.findAll().size(), "❌ clear() 실패 - 비워지지 않음");

        // (3) 복구 실행
        RestoreCommand restoreCmd = new RestoreCommand(repo, BACKUP_PATH);
        restoreCmd.execute();

        // then
        List<RoomReservation> list = repo.findAll();
        assertEquals(1, list.size(), "❌ 복구 후 데이터 개수가 1이 아님");
        assertEquals("A1", list.get(0).getId(), "❌ 복구된 예약 ID 불일치");
    }

    // ============================================================
    // 3) Command 인터페이스 execute() 구조 동작 테스트
    // ============================================================
    @Test
    void testCommandInterfaceExecute() {
        Command cmd = new BackupCommand(repo, BACKUP_PATH);

        assertDoesNotThrow(
                cmd::execute,
                "❌ Command.execute() 실행 중 예외 발생"
        );
    }
}
