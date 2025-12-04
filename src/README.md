# CS157A Final Project — Hotel Booking Console App

## Database Setup

1. **Create the database**

   In the MySQL client:

  
   CREATE DATABASE IF NOT EXISTS cs157a2;
   2. **Configure the connection properties**

   Edit `src/app.properties`:

  
   db.url=jdbc:mysql://localhost:3306/cs157a2?allowPublicKeyRetrieval=true&useSSL=false
   db.user=<your_mysql_username>
   db.password=<your_mysql_password>
   
   3. **Create tables and sample data**

   In the MySQL client, select the database and run the SQL script:

  
   USE cs157a2;
   SOURCE /Users/omary/HotelBooking/src/create_and_populate.sql;
      This creates all tables, constraints, sample data, the view, trigger, and stored procedure.

---

## Running the Java Program

1. **Compile**

   From the project root (differs for each user):

  
   mkdir -p out
   javac -d out -cp "lib/*" src/Main.java
   
   2. **Run**

  
   java -cp "out:lib/*" Main
      When it starts, you should see:

   - A message indicating the database connection was successful.
   - The text-based menu for the hotel booking console application.

---

## How the Application Was Built (Step by Step)

1. **Database design**

   - Designed the schema for core tables: guests, rooms, bookings, invoices, room assignments, and room maintenance.
   - Added primary keys, foreign keys, and constraints (including a unique constraint to prevent double-booking the same room on the same date).

2. **SQL script creation**

   - Wrote `create_and_populate.sql` to:
     - Drop and recreate all tables.
     - Define constraints, indexes, a view, a trigger, and a stored procedure.
     - Insert initial sample data into the main tables.

3. **JDBC configuration**

   - Added the MySQL connector JAR (`mysql-connector-j-9.5.0.jar`) to the `lib/` folder.
   - Created `app.properties` for the JDBC URL, username, and password.
   - Implemented Java code to load these properties and open a JDBC connection to MySQL.

4. **Console menu and operations**

   - Implemented a text-based menu in `Main.java` using `Scanner` for input.
   - Added options to:
     - View data from multiple tables and a view.
     - Insert, update, and delete records using `PreparedStatement`.
     - Run a transactional workflow that uses commit and rollback.

5. **Validation and error handling**

   - Added input validation helpers for strings, integers, dates, and booking status.
   - Wrapped database operations in `try/catch` blocks to handle SQL exceptions and show clear error messages.

---

## MySQL and Connector Information

- **MySQL version**: MySQL 8.x
- **JDBC driver**: `mysql-connector-j-9.5.0.jar`