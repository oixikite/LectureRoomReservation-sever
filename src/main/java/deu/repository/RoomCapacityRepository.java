package deu.repository;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 강의실별 정원(capacity)을 관리하는 리포지토리 (Singleton).
 * data/room-capacity.yaml 파일을 읽어서 메모리에 올려둔다.
 */
public class RoomCapacityRepository {

    private static final RoomCapacityRepository instance = new RoomCapacityRepository();

    // key: building|floor|room , value: capacity
    private final Map<String, Integer> capacityMap = new HashMap<>();

    private RoomCapacityRepository() {
        loadFromYaml();
    }

    public static RoomCapacityRepository getInstance() {
        return instance;
    }

    private String key(String building, String floor, String room) {
        return building + "|" + floor + "|" + room;
    }

    private void loadFromYaml() {
        try (InputStream in = getClass()
                .getClassLoader()
                .getResourceAsStream("data/room-capacity.yaml")) {

            if (in == null) {
                System.err.println("[RoomCapacityRepository] room-capacity.yaml 파일을 찾을 수 없습니다.");
                return;
            }

            LoaderOptions options = new LoaderOptions();
            Constructor constructor = new Constructor(RoomCapacityYaml.class, options);
            Yaml yaml = new Yaml(constructor);

            RoomCapacityYaml root = yaml.load(in);

            if (root != null && root.rooms != null) {
                capacityMap.clear();

                root.rooms.forEach(r -> {
                    String key = key(r.getBuilding(), String.valueOf(r.getFloor()), r.getRoom());
                    capacityMap.put(key, r.getCapacity());
                });
            }

            System.out.println("[RoomCapacityRepository] room-capacity.yaml 로드 완료");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[RoomCapacityRepository] YAML 로드 중 오류: " + e.getMessage());
        }
    }

    /**
     * 정원(capacity) 조회. 없으면 0 반환 (제한 없음 취급).
     */
    public int getCapacity(String building, String floor, String room) {
        return capacityMap.getOrDefault(key(building, floor, room), 0);
    }

    // ===== YAML 매핑용 내부 클래스들 =====

    public static class RoomCapacityYaml {
        private List<RoomCapacityItem> rooms;

        public List<RoomCapacityItem> getRooms() {
            return rooms;
        }

        public void setRooms(List<RoomCapacityItem> rooms) {
            this.rooms = rooms;
        }
    }

    public static class RoomCapacityItem {
        private String building;
        private int floor;
        private String room;
        private int capacity;

        public String getBuilding() {
            return building;
        }

        public void setBuilding(String building) {
            this.building = building;
        }

        public int getFloor() {
            return floor;
        }

        public void setFloor(int floor) {
            this.floor = floor;
        }

        public String getRoom() {
            return room;
        }

        public void setRoom(String room) {
            this.room = room;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }
    }
}
