package deu.repository;

import deu.model.entity.RoomReservation;
import lombok.Getter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {

    private static final String FILE_PATH =
            System.getProperty("user.dir")
                    + File.separator + "data"
                    + File.separator + "reservations.yaml";

    @Getter
    private static final ReservationRepository instance = new ReservationRepository();

    private final List<RoomReservation> roomReservationList = new ArrayList<>();
    private final Yaml yaml;

    // Wrapper 클래스 (YAML 상단에 키 유지)
    public static class RoomReservationWrapper {
        public List<RoomReservation> reservations = new ArrayList<>();
    }

    private ReservationRepository() {
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Representer representer = new Representer(options);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        representer.addClassTag(RoomReservationWrapper.class, Tag.MAP);
        representer.addClassTag(RoomReservation.class, Tag.MAP);

        this.yaml = new Yaml(representer, options);

        createDataDirectoryIfNeeded();
        loadFromFile();
    }

    // 디렉토리 생성
    private void createDataDirectoryIfNeeded() {
        File file = new File(FILE_PATH);
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    // 예약 저장
    public void save(RoomReservation reservation) {
        roomReservationList.add(reservation);
        saveToFile();
    }

    // 예약 삭제 (객체 기준)
    public void delete(RoomReservation reservation) {
        roomReservationList.remove(reservation);
        saveToFile();
    }

    // 예약 ID로 삭제
    public boolean deleteById(String id) {
        boolean result = roomReservationList.removeIf(r -> r.getId().equals(id));
        if (result) saveToFile();
        return result;
    }

    // 예약 ID로 조회
    public RoomReservation findById(String id) {
        return roomReservationList.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // 사용자 ID로 전체 예약 조회 (대소문자 무시)
    public List<RoomReservation> findByUser(String userId) {

        if (userId == null) return new ArrayList<>();

        String target = userId.trim().toLowerCase();

        List<RoomReservation> results = new ArrayList<>();
        for (RoomReservation r : roomReservationList) {
            if (r.getNumber() != null &&
                    r.getNumber().trim().toLowerCase().equals(target)) {
                results.add(r);
            }
        }
        return results;
    }

    // 모든 예약 반환
    public List<RoomReservation> findAll() {
        return new ArrayList<>(roomReservationList);
    }

    // (사용 안 하는 중복 체크)
    public boolean isDuplicate(String date, String startTime, String lectureRoom) {
        for (RoomReservation r : roomReservationList) {
            if (r.getDate().equals(date)
                    && r.getStartTime().equals(startTime)
                    && r.getLectureRoom().equals(lectureRoom)) {
                return true;
            }
        }
        return false;
    }

    // 전체 저장
    public void saveToFile() {
        createDataDirectoryIfNeeded();

        try (Writer writer = new FileWriter(FILE_PATH)) {
            RoomReservationWrapper wrapper = new RoomReservationWrapper();
            wrapper.reservations = roomReservationList;
            yaml.dump(wrapper, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 초기 로딩
    private void loadFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (InputStream input = new FileInputStream(file)) {
            RoomReservationWrapper wrapper = yaml.loadAs(input, RoomReservationWrapper.class);
            if (wrapper != null && wrapper.reservations != null) {
                roomReservationList.clear();
                roomReservationList.addAll(wrapper.reservations);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 테스트용: 전체 예약 삭제
    public void clear() {
        roomReservationList.clear();
        saveToFile();
    }

    // ==================================================================================================
    // 🔥 백업 기능: reservations.yaml → 지정된 backup 파일로 복사
    // ==================================================================================================
    public boolean exportBackup(String backupFilePath) {
        File source = new File(FILE_PATH);
        File target = new File(backupFilePath);

        try {
            // 폴더 없으면 생성
            if (target.getParentFile() != null) {
                target.getParentFile().mkdirs();
            }

            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(target)) {

                byte[] buffer = new byte[1024];
                int length;

                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                System.out.println("[ReservationRepository] 백업 성공 → " + backupFilePath);
                return true;
            }

        } catch (Exception e) {
            System.err.println("[ReservationRepository] 백업 실패: " + e.getMessage());
            return false;
        }
    }

    // ==================================================================================================
    // 🔥 복구 기능: backup 파일 → reservations.yaml 덮어쓰기 + 메모리 재로딩
    // ==================================================================================================
    public boolean importBackup(String backupFilePath) {
        File source = new File(backupFilePath);
        File target = new File(FILE_PATH);

        try {
            if (!source.exists()) {
                System.err.println("[ReservationRepository] 복구 실패: 백업 파일이 존재하지 않습니다.");
                return false;
            }

            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(target)) {

                byte[] buffer = new byte[1024];
                int length;

                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }

            // 복구 후 메모리 재로딩
            loadFromFile();
            System.out.println("[ReservationRepository] 복구 성공 ← " + backupFilePath);
            return true;

        } catch (Exception e) {
            System.err.println("[ReservationRepository] 복구 실패: " + e.getMessage());
            return false;
        }
    }
}
