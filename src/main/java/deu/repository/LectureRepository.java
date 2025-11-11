package deu.repository;

import deu.model.entity.Lecture;
import deu.model.enums.Semester;
import lombok.Getter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 강의 정보를 YAML 파일로 관리하는 저장소 클래스
 * 싱글톤 패턴을 사용하며, 파일이 없을 경우 resources에서 복사하여 생성한다.
 * @author oixikite
 * @modifier oxxultus
 * @since 2025.05.16
 */
public class LectureRepository {
    // 외부에서 접근하는 싱글톤 인스턴스
    // Singleton instance
    @Getter
    private static final LectureRepository instance = new LectureRepository();

    // 강의 리스트
    private final List<Lecture> lectureList = new ArrayList<>();
    
    //연도,학기 기본값 설정
    private static final int DEFAULT_YEAR = 2025;
    private static final Semester DEFAULT_SEMESTER = Semester.FIRST;

    // YAML 파일 경로 (JAR 또는 IDE 실행 경로 기준)
    private final String FILE_PATH = System.getProperty("user.dir") + File.separator + "data" + File.separator + "lectures.yaml";

    // SnakeYAML 객체
    private final Yaml yaml;

    // 강의 데이터를 감싸는 내부 클래스 - 아래 형식을 유지하기 위해 사용한다.
    public static class LectureWrapper {
        public List<Lecture> lectures;

        public LectureWrapper() {
            this.lectures = new ArrayList<>();
        }
    }

    // 생성자: YAML 설정 및 파일 로딩
    private LectureRepository() {
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        // SnakeYAML 2.x 대응: 클래스 태그 제거용 Representer
        Representer representer = new Representer(options);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        representer.addClassTag(LectureWrapper.class, Tag.MAP);
        representer.addClassTag(Lecture.class, Tag.MAP);

        yaml = new Yaml(representer, options);

        loadAllFromFile();
    }

    // 파일 저장
    private void saveAllToFile() {
        File file = new File(FILE_PATH);
        File parentDir = file.getParentFile();

        if (!parentDir.exists()) {
            boolean dirCreated = parentDir.mkdirs();
            if (dirCreated) {
                System.out.println("[LectureRepository] 디렉토리 생성됨: " + parentDir.getAbsolutePath());
            } else {
                System.err.println("[LectureRepository] 디렉토리 생성 실패: " + parentDir.getAbsolutePath());
            }
        }

        try (Writer writer = new FileWriter(file)) {
            LectureWrapper wrapper = new LectureWrapper();
            wrapper.lectures = lectureList;
            yaml.dump(wrapper, writer);
            System.out.println("[LectureRepository] 파일 저장 완료: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[LectureRepository] 파일 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 파일에서 불러오기
    private void loadAllFromFile() {
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            System.out.println("[LectureRepository] 파일이 없어 리소스에서 복사합니다: " + file.getAbsolutePath());

            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                if (dirCreated) {
                    System.out.println("[LectureRepository] 디렉토리 생성됨: " + parentDir.getAbsolutePath());
                } else {
                    System.err.println("[LectureRepository] 디렉토리 생성 실패: " + parentDir.getAbsolutePath());
                }
            }

            try (InputStream resourceInput = getClass().getResourceAsStream("/data/lectures.yaml");
                 OutputStream output = new FileOutputStream(file)) {

                if (resourceInput == null) {
                    System.err.println("[LectureRepository] resources/data/lectures.yaml 리소스가 없습니다.");
                    return;
                }

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = resourceInput.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                System.out.println("[LectureRepository] 리소스 파일 복사 완료");

            } catch (IOException e) {
                System.err.println("[LectureRepository] 리소스 파일 복사 중 오류: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        try (InputStream input = new FileInputStream(file)) {
            LectureWrapper wrapper = yaml.loadAs(input, LectureWrapper.class);
            if (wrapper != null && wrapper.lectures != null) {
                lectureList.clear();
                lectureList.addAll(wrapper.lectures);
                System.out.println("[LectureRepository] 파일 로딩 완료: " + file.getAbsolutePath());
                System.out.println("[LectureRepository] 불러온 강의 수: " + lectureList.size());
            } else {
                System.out.println("[LectureRepository] 파일은 있으나 강의 데이터가 비어있습니다.");
            }
        } catch (IOException e) {
            System.err.println("[LectureRepository] 파일 로딩 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        
        //누락 필드 보정(연도,학기)
        boolean patched = false;
        for (Lecture l : lectureList) {
            if (l.getYear() == null || l.getYear() == 0) {
                l.setYear(DEFAULT_YEAR);
                patched = true;
            }
            if (l.getSemester() == null) {
                l.setSemester(DEFAULT_SEMESTER);
                patched = true;
            }
        }
        if (patched) {
            System.out.println("[LectureRepository] 누락된 year/semester 보정 후 파일에 반영합니다.");
            saveAllToFile(); // 기존 YAML을 덮어써서 year/semester가 추가되도록 함
        }
    }

    // 강의 저장 (수정 포함) - 기존코드
//    public String save(Lecture lecture) {
//        if (lecture == null || lecture.getId() == null || lecture.getId().isBlank()) {
//            return "400"; // 잘못된 요청
//        }
//
//        deleteById(lecture.getId());
//        lectureList.add(lecture);
//        saveAllToFile();
//        return "200";
//    }
     public String save(Lecture lecture) {
        if (lecture == null || lecture.getId() == null || lecture.getId().isBlank()) return "400";
        // 안전장치: 저장 시에도 기본값 주입
        if (lecture.getYear() == null || lecture.getYear() == 0) lecture.setYear(DEFAULT_YEAR);
        if (lecture.getSemester() == null) lecture.setSemester(DEFAULT_SEMESTER);

        deleteById(lecture.getId());
        lectureList.add(lecture);
        saveAllToFile();
        return "200";
    }
    

    // 강의 삭제
    public String deleteById(String id) {
        boolean removed = lectureList.removeIf(l -> l.getId().equals(id));
        saveAllToFile();
        return removed ? "200" : "404";
    }

    // 강의 존재 여부
    public String existsById(String id) {
        return lectureList.stream().anyMatch(l -> l.getId().equals(id)) ? "200" : "404";
    }

    // 강의 ID로 조회
    public Optional<Lecture> findById(String id) {
        return lectureList.stream().filter(l -> l.getId().equals(id)).findFirst();
    }

    // 전체 강의 리스트 반환
    public List<Lecture> findAll() {
        loadAllFromFile();
        return new ArrayList<>(lectureList);
    }

    // 강의명 + 교수명으로 ID 조회
    public Optional<String> findIdByLectureNameAndProfessor(String title, String professor) {
        return lectureList.stream()
                .filter(l -> l.getTitle().equals(title) && l.getProfessor().equals(professor))
                .map(Lecture::getId)
                .findFirst();
    }
    
    //특정 학년도.학기 기반 조회 및 검증
    public List<Lecture> findAllByYearAndSemester(int year, Semester semester) {
    List<Lecture> result = new ArrayList<>();
    for (Lecture l : lectureList) {
        if (l.getYear() == year && l.getSemester() == semester) {
            result.add(l);
        }
    }
    return result;
}
    //건물/층/강의실 + 학년도/학기 필터 조회
    public List<Lecture> findRoomLectures(int year, Semester semester,
                                      String building, String floor, String lectureroom) {
    List<Lecture> result = new ArrayList<>();
    for (Lecture l : lectureList) {
        if (l.getYear() == year && l.getSemester() == semester
                && building.equals(l.getBuilding())
                && floor.equals(l.getFloor())
                && lectureroom.equals(l.getLectureroom())) {
            result.add(l);
        }
    }
    return result;
}
    // 시각 "HH:mm" 문자열 비교용: startA < endB && startB < endA 면 겹침
private static boolean timeOverlaps(String startA, String endA, String startB, String endB) {
    return startA.compareTo(endB) < 0 && startB.compareTo(endA) < 0;
}

/** 
 * 동일 강의실/요일/학년/학기에서 시간 겹침 존재 여부
 * newLecture와 id가 같은 기존 항목은 제외(수정 시 자기 자신 제외)
 */
public boolean hasTimeConflict(Lecture newLecture) {
    for (Lecture l : lectureList) {
        if (l.getId() != null && l.getId().equals(newLecture.getId())) continue;
        if (l.getYear() == newLecture.getYear()
                && l.getSemester() == newLecture.getSemester()
                && l.getBuilding().equals(newLecture.getBuilding())
                && l.getFloor().equals(newLecture.getFloor())
                && l.getLectureroom().equals(newLecture.getLectureroom())
                && l.getDay().equals(newLecture.getDay())) {
            if (timeOverlaps(l.getStartTime(), l.getEndTime(),
                             newLecture.getStartTime(), newLecture.getEndTime())) {
                return true;
            }
        }
    }
    return false;
}




}