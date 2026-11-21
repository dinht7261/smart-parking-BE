# Smart Parking Backend - Spring Boot + Oracle

This is **stage 1 only**: Oracle connection and read-only parking spot APIs. It does not implement booking, QR, sessions, security, payments, sensors or physical gates yet. No frontend changes are required.

## Requirements

- Java JDK 21 (check `java -version`)
- Apache Maven 3.9+ (check `mvn -version`)
- Oracle Database server (e.g. Oracle AI Database Free, service `FREEPDB1`)
- DBeaver Community, VS Code or IntelliJ IDEA
- Internet access during first Maven dependency download

DBeaver is a **database client**, not a database server. On a typical local Oracle Free setup:
- Host `localhost`
- Port `1521`
- Service name (NOT SID) `FREEPDB1`

## 1. Create a schema via DBeaver

1. Connect to `FREEPDB1` as `SYSTEM`, using the admin password chosen while installing Oracle.
2. Open a SQL editor, execute `database/01_create_user_as_system.sql` after replacing the placeholder password. Do not use this password in published code.
3. Create a second DBeaver connection to the same service with username `SMART_PARKING` and the chosen password.
4. Execute `database/02_schema_as_smart_parking.sql` using the new connection.
5. Execute `database/03_verify.sql`: it should show 4 parking spots.

If your Oracle service isn't `FREEPDB1` (e.g. older XE uses `XEPDB1`) use the actual service shown in your Oracle installation.

## 2. Start Spring Boot (PowerShell)

```powershell
java -version
mvn -version
$env:DB_URL = 'jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
$env:DB_USER = 'SMART_PARKING'
$env:DB_PASSWORD = 'your_password_from_step_1'
mvn spring-boot:run
```

The database must be up before starting the backend. `spring.jpa.hibernate.ddl-auto=validate` checks schema and never generates tables automatically.

## 3. Test APIs

```powershell
curl.exe http://localhost:8080/api/v1/health
curl.exe http://localhost:8080/api/v1/parking-spots
curl.exe http://localhost:8080/api/v1/parking-spots/availability
```

Expected availability on the initial sample data:

```json
{"total":4,"available":4,"reserved":0,"occupied":0,"maintenance":0}
```

## Architecture

```
src/main/java/com/smartparking/backend/
  SmartParkingBackendApplication.java
  health/HealthController.java
  spot/{ParkingSpot,ParkingSpotStatus,ParkingSpotRepository,ParkingSpotService,ParkingSpotController,ParkingSpotResponse,AvailabilityResponse}.java
src/main/resources/application.yml
database/{01_create_user_as_system,02_schema_as_smart_parking,03_verify}.sql
```

### Important
- No hard-coded database secret in `application.yml`.
- No `SYSTEM` / `SYS` account in the backend.
- Do not expose these unauthenticated APIs on a public network: authorization is not implemented yet.
- The read-only endpoints calculate availability from `PARKING_SPOTS.STATUS`; when a booking is approved later, the spot becomes `RESERVED`, and a booked vehicle's arrival becomes `OCCUPIED` without a second decrement.
- This starter was generated without running an Oracle server or full Maven build in the author's environment; verify locally and share errors for adjustment.
