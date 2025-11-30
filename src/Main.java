import java.sql.*;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    private static Connection conn;
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            connect();
            menuLoop();
        } catch (Exception e) {
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
    }

    private static void menuLoop() {
        while (true) {
            System.out.println("\n===== HOTEL BOOKING SYSTEM =====");
            System.out.println("1. View Guests");
            System.out.println("2. View Bookings");
            System.out.println("3. Add Guest");
            System.out.println("4. Update Booking Status");
            System.out.println("5. Delete Guest");
            System.out.println("6. Transaction: Create Booking + RoomAssignment");
            System.out.println("7. Exit");
            System.out.print("Choose option: ");

            int choice = safeInt();
            switch (choice) {
                case 1 -> viewGuests();
                case 2 -> viewBookings();
                case 3 -> addGuest();
                case 4 -> updateBookingStatus();
                case 5 -> deleteGuest();
                case 6 -> transactionalBookingFlow();
                case 7 -> System.exit(0);
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

    private static void addGuest() {
        sc.nextLine();
        System.out.print("Name: ");
        String name = sc.nextLine();
        System.out.print("Email: ");
        String email = sc.nextLine();
        System.out.print("Phone: ");
        String phone = sc.nextLine();

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Guest (Name, Email, Phone) VALUES (?, ?, ?)"
            );
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.executeUpdate();

            System.out.println("Guest added.");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void updateBookingStatus() {
        System.out.print("Booking ID: ");
        int id = safeInt();
        sc.nextLine();
        System.out.print("New Status (Pending/Confirmed/Cancelled): ");
        String stat = sc.nextLine();

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Booking SET Status=? WHERE BookingID=?"
            );
            ps.setString(1, stat);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("Booking updated.");
            else
                System.out.println("Booking not found.");

        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
        }
    }

    private static void deleteGuest() {
        System.out.print("Guest ID: ");
        int id = safeInt();

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Guest WHERE GuestID=?"
            );
            ps.setInt(1, id);
            int rows = ps.executeUpdate();

            if (rows > 0)
                System.out.println("Guest deleted.");
            else
                System.out.println("Guest not found.");
        } catch (SQLException e) {
            System.out.println("Error: Cannot delete guest. Likely has bookings.");
        }
    }

    private static void transactionalBookingFlow() {
        try {
            System.out.print("Guest ID: ");
            int guest = safeInt();

            System.out.print("Start Date (YYYY-MM-DD): ");
            String sd = sc.next();

            System.out.print("End Date (YYYY-MM-DD): ");
            String ed = sc.next();

            System.out.print("Room ID: ");
            int room = safeInt();

            System.out.print("StayDate: ");
            String stay = sc.next();


            LocalDate start = LocalDate.parse(sd);
            LocalDate end = LocalDate.parse(ed);
            LocalDate stayDate = LocalDate.parse(stay);

            if (!start.isBefore(end)) {
                System.out.println("Error: StartDate must be before EndDate. Transaction aborted.");
                return;
            }

            if (stayDate.isBefore(start) || stayDate.isAfter(end)) {
                System.out.println("Error: StayDate must be within the booking period. Transaction aborted.");
                return;
            }

            conn.setAutoCommit(false);

            // Insert booking
            PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO Booking (StartDate, EndDate, Status, GuestID) VALUES (?, ?, 'Confirmed', ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps1.setString(1, sd);
            ps1.setString(2, ed);
            ps1.setInt(3, guest);
            ps1.executeUpdate();

            ResultSet keys = ps1.getGeneratedKeys();
            keys.next();
            int newBookingID = keys.getInt(1);

            // Insert room assignment
            PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO RoomAssignment (StayDate, BookingID, RoomID) VALUES (?, ?, ?)"
            );
            ps2.setString(1, stay);
            ps2.setInt(2, newBookingID);
            ps2.setInt(3, room);
            ps2.executeUpdate();

            // Commit all changes
            conn.commit();
            conn.setAutoCommit(true);

            System.out.println("Transaction successful!");

        } catch (SQLException e) {
            System.out.println("Transaction failed! Rolling back...");
            try { conn.rollback(); } catch (SQLException ex) {}
            System.out.println("Error: " + e.getMessage());
        }
    }

}