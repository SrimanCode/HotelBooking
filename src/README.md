# CS157A Final Project — Hotel Booking Console App

A Java console application that uses JDBC to interact with a MySQL database for a simple hotel booking system.

- Language/Runtime: Java 17
- Database: MySQL 8.x
- JDBC Driver: mysql-connector-j 9.5.0 (provided in `lib/`)
- Project DB: `cs157a2`

---

## 1) Prerequisites

- Java 17 (check with `java -version`)
- MySQL 8.x running locally (default port 3306)
- MySQL JDBC driver JAR present in `lib/` (already included)

Optional JDBC URL flags for local MySQL without TLS:
- `?allowPublicKeyRetrieval=true&useSSL=false`

Example URL:
```
jdbc:mysql://localhost:3306/cs157a2?allowPublicKeyRetrieval=true&useSSL=false
```

---

## 2) Database Setup

- Create the database (if not present):
```sql
CREATE DATABASE IF NOT EXISTS cs157a2;
```
- Configure `src/app.properties` with your credentials:
```
db.url=jdbc:mysql://localhost:3306/cs157a2?allowPublicKeyRetrieval=true&useSSL=false
db.user=<your_user>
db.password=<your_password>
```
- On app startup, the script `src/create_and_populate.sql` is executed. It will DROP and recreate tables, then insert sample data, create a trigger, and a view.
  - If you want to preserve existing data, temporarily remove the call to `runSQLScript(...)` in `connect()` before running.

---

## 3) Build and Run

From the project root:
```bash
mkdir -p out
javac -d out -cp "lib/*" src/Main.java
java -cp "out:lib/*" Main
```
You should see "Connected to DB." and the main menu.

---

## 4) Menu Overview

- 1: View Guests
- 2: View Bookings
- 3: View Rooms
- 4: View Booking Summary (VIEW)
- 5: Add Guest
- 6: Update Booking Status
- 7: Delete Guest
- 8: Transaction: Create Booking + RoomAssignment
- 9: Exit

Notes:
- All SQL operations use `PreparedStatement`.
- The transactional workflow (option 8) spans multiple tables and uses COMMIT/ROLLBACK.

---

## 5) Schema and DB Features

Tables (created in `create_and_populate.sql`):
- Guest(GuestID, Name, Email UNIQUE, Phone)
- Room(RoomID, RoomType ENUM, Price, Capacity)
- Booking(BookingID, StartDate, EndDate, Status ENUM, GuestID FK→Guest ON DELETE CASCADE)
- Invoice(InvoiceID, Amount, IssueDate, PaymentStatus ENUM, BookingID FK→Booking ON DELETE CASCADE)
- RoomAssignment(AssignmentID, StayDate, BookingID FK→Booking ON DELETE CASCADE, RoomID FK→Room ON DELETE RESTRICT)
- RoomMaintenance((MaintenanceNo, RoomID) PK, Date, Description, StaffName, FK→Room)

Other DB objects:
- Trigger: `trg_booking_after_insert_invoice` (AFTER INSERT on Booking → inserts an Invoice row with default values)
- View: `vw_booking_summary` (joins Guest/Booking/RoomAssignment/Room for reporting)

Indexes:
- Several supporting indexes (see the SQL file) including on dates, room type, and FKs.

---

## 6) Test Plan (what to show)

Run all tests against `cs157a2`.

### A. JDBC connection and script load
- Run the app and confirm:
  - "Connected to DB."
  - "SQL script loaded successfully."

### B. SELECT (at least 3 tables)
- Menu 1: View Guests → shows seeded guests.
- Menu 2: View Bookings → shows seeded bookings.
- Menu 3: View Rooms → shows seeded rooms.

### C. INSERT (Add Guest) with validation/constraints
- Menu 5: Add Guest
  - Case 1: Valid name/email/phone → "Guest added." and appears in View Guests.
  - Case 2: Duplicate email (e.g., `ada@example.com`) → expect a DB constraint error (duplicate key).

### D. UPDATE (Booking Status)
- Menu 6: Update Booking Status
  - Enter an existing BookingID and a valid status (Pending, Confirmed, Checked-In, Completed, Cancelled) → "Booking updated."
  - If invalid BookingID → "Booking not found."

### E. DELETE (Guest) and cascade behavior
- Menu 7: Delete Guest
  - Deleting a guest with bookings will cascade delete their bookings (and thus invoices and room assignments) due to FK `ON DELETE CASCADE`.
  - Verify by viewing bookings after deletion.

### F. Transactional workflow (COMMIT and ROLLBACK)
- Menu 8: Transaction: Create Booking + RoomAssignment
  - Success (COMMIT):
    - Example inputs: GuestID=1, Start=2025-12-10, End=2025-12-12, RoomID=1, StayDate=2025-12-11
    - Expect: "Transaction successful!"
    - Verify: View Bookings shows the new booking; `Invoice` contains an entry for the new BookingID (trigger fired).
  - Failure (ROLLBACK):
    - Use an invalid RoomID (e.g., 999) or a non-existent GuestID.
    - Expect: "Transaction failed! Rolling back..." and no partial records left behind.

### G. Trigger test (independent)
- In MySQL CLI, insert a booking manually and confirm an invoice is created by the trigger:
```sql
INSERT INTO Booking (StartDate, EndDate, Status, GuestID)
VALUES ('2025-12-10', '2025-12-12', 'Confirmed', 1);
SELECT LAST_INSERT_ID() INTO @bid;
SELECT * FROM Invoice WHERE BookingID = @bid; -- expect one row
```

### H. View test
- In MySQL CLI (or via menu 4):
```sql
SELECT * FROM vw_booking_summary ORDER BY BookingID, StayDate;
```
- Expected columns: BookingID, GuestID, GuestName, StartDate, EndDate, Status, StayDate, RoomID, RoomType, Price.

---

## 7) Video Demo Flow (≤ 6 minutes)

Target length: ~5:30. Keep terminal and app window visible.

- 0:00–0:20 — Start & Connect
  - Show `app.properties` briefly.
  - Run `java -cp "out:lib/*" Main`.
  - Narrate “Connected to DB.” and “SQL script loaded successfully.”

- 0:20–1:10 — SELECTs (3 tables)
  - Menu 1: View Guests.
  - Menu 2: View Bookings.
  - Menu 3: View Rooms.

- 1:10–2:10 — INSERT + validation/constraint
  - Menu 5: Add Guest with a valid new email → “Guest added.” Show in View Guests.
  - Try Add Guest with duplicate email → show constraint error message.

- 2:10–2:40 — UPDATE
  - Menu 6: Update Booking Status to “Confirmed” (or another allowed value) for an existing BookingID.

- 2:40–3:10 — DELETE
  - Menu 7: Delete a guest without bookings (success) OR try to delete a guest with bookings to explain cascade behavior and show result.

- 3:10–4:00 — Transaction (COMMIT)
  - Menu 8: Provide valid inputs. Show “Transaction successful!”
  - Menu 2: View Bookings to confirm.

- 4:00–4:40 — Transaction (ROLLBACK)
  - Menu 8: Use invalid RoomID (e.g., 999). Show “Transaction failed! Rolling back...” Then View Bookings to confirm no partial insert.

- 4:40–5:10 — Trigger
  - In MySQL CLI: Insert a booking, then `SELECT` from Invoice by `LAST_INSERT_ID()` to show the trigger-created invoice.

- 5:10–5:40 — View
  - In MySQL CLI or Menu 4: `SELECT * FROM vw_booking_summary ORDER BY BookingID, StayDate;`
  - Briefly explain columns (GuestName, dates, room, price).

- 5:40–6:00 — Wrap
  - Mention Java 17, MySQL 8, connector version, and that all requirements are covered.

Tips:
- Have MySQL CLI pre-opened in another terminal.
- Use copy/paste for quick CLI statements.
- Keep input values ready to avoid typing delays.

---

## 8) Known Notes / Tips

- The initialization script is destructive (drops/recreates tables). Comment out the `runSQLScript(...)` call in `connect()` to avoid resetting data on every run.
- If you see "Public Key Retrieval is not allowed", add `?allowPublicKeyRetrieval=true&useSSL=false` to the JDBC URL for local dev.
- If publishing this repo, do not commit real passwords. Consider adding `src/app.example.properties` and git-ignoring `src/app.properties`.

---

## 9) Submission Package (include in zip)

Zip name: `CS157A_FinalProject_TeamGroupName.zip`

Include:
- `src/Main.java` (console app with menu, Scanner, JDBC, PreparedStatements, commit & rollback)
- `src/create_and_populate.sql` (tables, constraints, indexes, trigger, VIEW, sample data)
- `src/app.properties` (database URL, username, password)
- `src/README.md` (this file)
- Optional: `src/ai_log.md` for prompts/notes

---

## 10) Versions

- MySQL: tested with 8.x
- JDBC Driver: mysql-connector-j 9.5.0
