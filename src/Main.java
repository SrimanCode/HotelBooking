import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    private static Connection conn;
    private static Scanner sc = new Scanner(System.in);

    //Helpers for input validation:
    private static String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) {
                return s;
            }
            System.out.println("Input cannot be blank. Please try again.");
        }
    }
    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (sc.hasNextInt()) {
                int value = sc.nextInt();
                sc.nextLine();
                return value;
            } else {
                System.out.println("Please enter a valid whole number.");
                sc.nextLine();
            }
        }
    }
    private static String readEmail(String prompt) {
        while (true) {
            System.out.print(prompt);
            String email = sc.nextLine().trim();
            if (email.isEmpty()) {
                System.out.println("Email cannot be blank.");
                continue;
            }
            if (!email.contains("@") || !email.contains(".")) {
                System.out.println("Please enter a valid-looking email (must contain '@' and '.').");
                continue;
            }
            return email;
        }
    }
    private static String readOptionalString(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }
    private static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                return LocalDate.parse(input); // expects YYYY-MM-DD
            } catch (Exception e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD (e.g., 2025-11-10).");
            }
        }
    }
    private static String readBookingStatus() {
        String[] allowed = { "Pending", "Confirmed", "Checked-In", "Completed", "Cancelled" };
        while (true) {
            System.out.print("New Status (Pending/Confirmed/Checked-In/Completed/Cancelled): ");
            String stat = sc.nextLine().trim();

            for (String a : allowed) {
                if (a.equalsIgnoreCase(stat)) {
                    return a;
                }
            }
            System.out.println("Invalid status. Please choose one of: Pending, Confirmed, Checked-In, Completed, Cancelled.");
        }
    }

    public static void main(String[] args) {
        try {
            connect();
            menuLoop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runSQLScript(Connection conn, String filePath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            StringBuilder sb = new StringBuilder();
            String line;

            Statement stmt = conn.createStatement();

            while ((line = reader.readLine()) != null) {

                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                sb.append(line).append(" ");
                if (line.endsWith(";")) {
                    String sql = sb.toString().trim();
                    sql = sql.substring(0, sql.length() - 1);

                    if (!sql.isBlank()) {
                        stmt.execute(sql);
                    }

                    sb.setLength(0);
                }
            }
            reader.close();
            System.out.println("SQL script loaded successfully.");

        } catch (Exception e) {
            System.out.println("Error loading SQL script: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void connect() throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream("src/app.properties"));

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        conn = DriverManager.getConnection(url, user, password);
        conn.setAutoCommit(true);
        System.out.println("Connected to DB.");
        //runSQLScript(conn, "src/create_and_populate.sql");
    }

    private static void menuLoop() {
        while (true) {
            System.out.println("\n===== HOTEL BOOKING SYSTEM =====");
            System.out.println("1. View Guests");
            System.out.println("2. View Bookings");
            System.out.println("3. View Rooms");
            System.out.println("4. View Booking Summary (VIEW)");
            System.out.println("5. Add Guest");
            System.out.println("6. Update Booking Status");
            System.out.println("7. Delete Guest");
            System.out.println("8. Transaction: Create Booking + RoomAssignment");
            System.out.println("9. Create Booking via Stored Procedure");
            System.out.println("10. Exit");
            //System.out.print("Choose option: ");

            int choice = readInt("Choose option: ");
            switch (choice) {
                case 1 -> viewGuests();
                case 2 -> viewBookings();
                case 3 -> viewRooms();
                case 4 -> viewBookingSummary();
                case 5 -> addGuest();
                case 6 -> updateBookingStatus();
                case 7 -> deleteGuest();
                case 8 -> transactionalBookingFlow();
                case 9 -> createBookingViaProcedure();   // NEW
                case 10 -> System.exit(0);
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static int safeInt() {
        while (!sc.hasNextInt()) {
            System.out.print("Invalid number. Try again: ");
            sc.next();
        }
        return sc.nextInt();
    }

    private static void viewGuests() {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM Guest");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println(
                        rs.getInt("GuestID") + " | " +
                                rs.getString("Name") + " | " +
                                rs.getString("Email") + " | " +
                                rs.getString("Phone")
                );
            }
        } catch (SQLException e) {
            System.out.println("Error fetching guests.");
        }
    }

    private static void viewBookings() {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT BookingID, StartDate, EndDate, Status, GuestID FROM Booking"
            );
            ResultSet rs = ps.executeQuery();

            System.out.println("\n=== BOOKINGS ===");
            while (rs.next()) {
                System.out.println(
                        "BookingID: " + rs.getInt("BookingID") +
                                " | GuestID: " + rs.getInt("GuestID") +
                                " | " + rs.getString("StartDate") +
                                " to " + rs.getString("EndDate") +
                                " | Status: " + rs.getString("Status")
                );
            }
        } catch (SQLException e) {
            System.out.println("Error reading bookings: " + e.getMessage());
        }
    }

    private static void viewRooms() {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT RoomID, RoomType, Price, Capacity FROM Room"
            );
            ResultSet rs = ps.executeQuery();

            System.out.println("\n=== ROOMS ===");
            while (rs.next()) {
                System.out.println(
                        rs.getInt("RoomID") + " | " +
                                rs.getString("RoomType") + " | " +
                                rs.getBigDecimal("Price") + " | " +
                                rs.getInt("Capacity")
                );
            }
        } catch (SQLException e) {
            System.out.println("Error reading rooms: " + e.getMessage());
        }
    }

    private static void viewBookingSummary() {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT BookingID, GuestID, GuestName, StartDate, EndDate, Status, StayDate, RoomID, RoomType, Price FROM vw_booking_summary ORDER BY BookingID, StayDate"
            );
            ResultSet rs = ps.executeQuery();

            System.out.println("\n=== BOOKING SUMMARY (VIEW) ===");
            while (rs.next()) {
                System.out.println(
                        "BookingID: " + rs.getInt("BookingID") +
                                " | GuestID: " + rs.getInt("GuestID") +
                                " (" + rs.getString("GuestName") + ")" +
                                " | " + rs.getString("StartDate") +
                                " to " + rs.getString("EndDate") +
                                " | Status: " + rs.getString("Status") +
                                (rs.getString("StayDate") == null ? "" :
                                        (" | StayDate: " + rs.getString("StayDate") +
                                                " | RoomID: " + rs.getInt("RoomID") +
                                                " (" + rs.getString("RoomType") + ")"))
                );
            }
        } catch (SQLException e) {
            System.out.println("Error reading booking summary: " + e.getMessage());
        }
    }

    private static void addGuest() {
        System.out.println("\n=== ADD NEW GUEST ===");
        String name = readNonEmptyString("Name: ");
        String email = readEmail("Email: ");
        String phone = readOptionalString("Phone (optional, press Enter to skip): ");

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Guest (Name, Email, Phone) VALUES (?, ?, ?)"
            );
            ps.setString(1, name);
            ps.setString(2, email);
            if (phone.isEmpty()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, phone);
            }

            ps.executeUpdate();
            System.out.println("Guest added successfully.");

        } catch (SQLIntegrityConstraintViolationException e) {
            // Unique constraint on Email most likely
            System.out.println("Error: That email is already registered for another guest. Please use a different email.");
        } catch (SQLException e) {
            System.out.println("Database error while adding guest.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    private static void updateBookingStatus() {

        System.out.println("\n=== UPDATE BOOKING STATUS ===");
        int id = readInt("Booking ID: ");
        try {
            // Check that booking exists first
            PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Booking WHERE BookingID = ?"
            );
            check.setInt(1, id);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                System.out.println("No booking found with ID " + id + ".");
                return;
            }

            String stat = readBookingStatus();  // validated and normalized

            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Booking SET Status=? WHERE BookingID=?"
            );
            ps.setString(1, stat);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Booking status updated to '" + stat + "'.");
            } else {
                System.out.println("Booking not found (no rows updated).");
            }

        } catch (SQLException e) {
            System.out.println("Database error while updating booking status.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    private static void deleteGuest() {
        System.out.println("\n=== DELETE GUEST ===");
        int id = readInt("Guest ID: ");

        try {
            // Check existence first
            PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Guest WHERE GuestID = ?"
            );
            check.setInt(1, id);
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                System.out.println("No guest found with ID " + id + ".");
                return;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Guest WHERE GuestID=?"
            );
            ps.setInt(1, id);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Guest (ID " + id + ") deleted successfully.");
            } else {
                System.out.println("Guest not deleted (no rows affected).");
            }

        } catch (SQLException e) {
            System.out.println("Error: Could not delete guest.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    private static void transactionalBookingFlow() {
        System.out.println("\n=== TRANSACTION: NEW BOOKING + ROOM ASSIGNMENT ===");

        try {
            int guest = readInt("Guest ID: ");

            // Check guest exists
            if (!existsById("Guest", "GuestID", guest)) {
                System.out.println("No guest found with ID " + guest + ". Transaction aborted.");
                return;
            }

            LocalDate start = readDate("Start Date (YYYY-MM-DD): ");
            LocalDate end   = readDate("End Date (YYYY-MM-DD): ");

            if (!start.isBefore(end)) {
                System.out.println("Error: StartDate must be before EndDate. Transaction aborted.");
                return;
            }

            int room = readInt("Room ID: ");
            if (!existsById("Room", "RoomID", room)) {
                System.out.println("No room found with ID " + room + ". Transaction aborted.");
                return;
            }

            LocalDate stayDate = readDate("StayDate (YYYY-MM-DD, must be within booking period): ");

            if (stayDate.isBefore(start) || stayDate.isAfter(end)) {
                System.out.println("Error: StayDate must be within the booking period. Transaction aborted.");
                return;
            }

            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                // Insert booking
                PreparedStatement ps1 = conn.prepareStatement(
                        "INSERT INTO Booking (StartDate, EndDate, Status, GuestID) VALUES (?, ?, 'Confirmed', ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps1.setDate(1, java.sql.Date.valueOf(start));
                ps1.setDate(2, java.sql.Date.valueOf(end));
                ps1.setInt(3, guest);
                ps1.executeUpdate();

                ResultSet keys = ps1.getGeneratedKeys();
                keys.next();
                int newBookingID = keys.getInt(1);

                // Insert room assignment (may fail if violates uq_room_stay)
                PreparedStatement ps2 = conn.prepareStatement(
                        "INSERT INTO RoomAssignment (StayDate, BookingID, RoomID) VALUES (?, ?, ?)"
                );
                ps2.setDate(1, java.sql.Date.valueOf(stayDate));
                ps2.setInt(2, newBookingID);
                ps2.setInt(3, room);
                ps2.executeUpdate();

                conn.commit();
                System.out.println("Transaction successful! BookingID = " + newBookingID);

            } catch (SQLIntegrityConstraintViolationException e) {
                // This will catch things like the UNIQUE(RoomID, StayDate) violation or FK issues
                conn.rollback();

                String msg = e.getMessage();
                if (msg != null && msg.contains("uq_room_stay")) {
                    System.out.println("Error: That room is already assigned on " + stayDate +
                            ". Please choose a different room or date.");
                } else {
                    System.out.println("Transaction failed due to a constraint violation.");
                    System.out.println("Details: " + e.getMessage());
                }

            } catch (SQLException e) {
                conn.rollback();
                System.out.println("Database error during transaction. Rolling back...");
                System.out.println("Details: " + e.getMessage());
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }

        } catch (SQLException e) {
            System.out.println("Unexpected database error.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    private static void createBookingViaProcedure() {
        System.out.println("\n=== CREATE BOOKING VIA STORED PROCEDURE ===");
        try {
            int guestId = readInt("Guest ID: ");
            if (!existsById("Guest", "GuestID", guestId)) {
                System.out.println("No guest found with ID " + guestId + ".");
                return;
            }

            LocalDate start = readDate("Start Date (YYYY-MM-DD): ");
            LocalDate end   = readDate("End Date (YYYY-MM-DD): ");

            String roomType = readOptionalString(
                    "Room type (Single/Double/Suite/Deluxe/Family) or press Enter for any: ");
            if (roomType.isBlank()) {
                roomType = null;
            }

            CallableStatement cs = conn.prepareCall("{ CALL CreateBookingWithValidation(?, ?, ?, ?) }");
            cs.setInt(1, guestId);
            cs.setDate(2, java.sql.Date.valueOf(start));
            cs.setDate(3, java.sql.Date.valueOf(end));
            if (roomType == null) {
                cs.setNull(4, Types.VARCHAR);
            } else {
                cs.setString(4, roomType);
            }

            cs.execute();
            System.out.println("Stored procedure executed successfully (booking created and assigned).");

        } catch (SQLException e) {
            // Custom errors from SIGNAL in the procedure use SQLSTATE '45000'
            System.out.println("Error while calling stored procedure.");
            System.out.println("Details: " + e.getMessage());
        }
    }

    // helper to check if a row exists by ID
    private static boolean existsById(String table, String idCol, int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + idCol + " = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }
}