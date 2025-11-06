## 프로젝트 구조
```
├── controller/                # 도메인별 요청 처리
│   ├── SystemController.java  # 요청 분기 라우팅
│   └── business/              # 각 도메인 컨트롤러
├── service/                   # 비즈니스 로직 처리 계층
├── repository/                # YAML 파일 접근 계층
├── model/                     # DTO, Entity 클래스 정의
└── ServerMain.java            # 서버 진입점
```
## 실행 방법

### 주의사항
1. Maven pom.xml에 코드가 작성되어 있어 자동으로 프로젝트 실행하면 라이브러리를 전부 가져옵니다.
2. 원드라이브나 구글드라이브에서 넣고 프로젝트 실행하면 못 불러옵니다.
3. 일반적인 로컬 디스크에 프로젝트를 진행해서 열어주세요 (모든 IDE 동일).

### 요구사항
- Java 21 이상
- Maven 3.x 이상

### 실행 명령어
```
mvn clean package
java -jar target/DeuLectureRoomServer-1.0.0.jar
```
- 서버 기본 포트: 9999
- 데이터 저장 경로: ./data/

### 데이터 파일 조작 방법
 - src/main/resources/data 폴더 내 json을 수정하면 동적으로 반영됩니다.
 - lectures.yaml 파일에서는 startTime, endTime을 분단위로 작성해도 반올림 되어 적용됩니다.
 - users.yaml 파일에서 사용자를 수동으로 추가해서 관리자를 추가할 수 있습니다.
    - number: 란에 m을 붙이면 관리자 페이지를 이용하게 할 수 있습니다. (회원가입 시 관리자로 가입불가)
