# Smart Parking Backend — Spring Boot + Oracle

Stage 1 starter: Oracle database connectivity and **read-only** parking spot REST APIs. This does not yet include login, bookings, QR check-in, parking sessions, payments, hardware or security.

## Requirements
- JDK 21
- Apache Maven 3.9+
- Oracle Database Free (or another Oracle database instance)
- DBeaver for executing SQL and inspecting tables
- IntelliJ IDEA (open `pom.xml` as a Maven project)

DBeaver is a database client, **not** the database server.

## Database setup (DBeaver)

For a typical local Oracle Free installation use:
- Host: `localhost`
- Port: `1521`
- Connection type: **Service name**, service: `FREEPDB1`

1. Connect as `SYSTEM` to the `FREEPDB1` **PDB**, not the root container.
2. In `database/01_create_user_as_system.sql` replace the example password with your own; run that script.
3. Connect as `SMART_PARKING` with the newly created password.
4. Run `database/02_schema_as_smart_parking.sql` (on an empty schema only).
5. Run `database/03_verify.sql`. Four parking spots should appear.

If your Oracle installation has a different service name, update the JDBC URL accordingly.

## Running in IntelliJ

Configure the project to use JDK 21 and allow Maven to import dependencies. Set the `DB_PASSWORD` environment variable in IntelliJ's **Run → Edit Configurations** for `SmartParkingBackendApplication` (and optionally `DB_URL`, `DB_USER`, `SERVER_PORT`). Run the application.

Alternatively in PowerShell:
```powershell
$env:DB_URL='jdbc:oracle:thin:@//localhost:1521/FREEPDB1'
$env:DB_USER='SMART_PARKING'
$env:DB_PASSWORD='your_actual_password'
mvn spring-boot:run
```

## API endpoints
- `GET http://localhost:8080/api/v1/health` — verifies Oracle with `SELECT 1 FROM DUAL`
- `GET http://localhost:8080/api/v1/parking-spots` — lists parking spots
- `GET http://localhost:8080/api/v1/parking-spots/availability` — gives aggregate counts

With the provided four-row seed data, availability should return:
```json
{"total":4,"available":4,"reserved":0,"occupied":0,"maintenance":0}
```

The number of available spots is calculated from `PARKING_SPOTS.STATUS`. Booking approval will eventually move `AVAILABLE → RESERVED`; an approved vehicle's QR check-in will move `RESERVED → OCCUPIED` **without decrementing the count twice**.

## Security notes
- Never commit an Oracle password or `.env`. The `.gitignore` excludes local secret files.
- `.env.example` is sample documentation, not automatically read by Spring Boot.
- `spring.jpa.hibernate.ddl-auto=validate` checks the existing schema rather than creating it.
- Do not expose this starter publicly: authentication and authorization have not been implemented yet.

**Status:** GitHub source upload completed; Maven build and live Oracle integration have not yet been verified.
