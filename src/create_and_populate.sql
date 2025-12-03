USE cs157a2;

DROP TABLE IF EXISTS RoomMaintenance;
DROP TABLE IF EXISTS RoomAssignment;
DROP TABLE IF EXISTS Invoice;
DROP TABLE IF EXISTS Booking;
DROP TABLE IF EXISTS Room;
DROP TABLE IF EXISTS Guest;

CREATE TABLE Guest (
    GuestID INT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Email VARCHAR(100) NOT NULL UNIQUE,
    Phone VARCHAR(20)
);

CREATE TABLE Room (
    RoomID INT AUTO_INCREMENT PRIMARY KEY,
    RoomType ENUM('Single', 'Double', 'Suite', 'Deluxe', 'Family') NOT NULL,
    Price DECIMAL(10,2) NOT NULL,
    Capacity INT NOT NULL
);

CREATE TABLE Booking (
    BookingID INT AUTO_INCREMENT PRIMARY KEY,
    StartDate DATE NOT NULL,
    EndDate DATE NOT NULL,
    Status ENUM('Pending', 'Confirmed', 'Checked-In', 'Completed', 'Cancelled') DEFAULT 'Pending',
    GuestID INT NOT NULL,
    CONSTRAINT chk_booking_dates CHECK (StartDate < EndDate),
    CONSTRAINT fk_booking_guest
        FOREIGN KEY (GuestID) REFERENCES Guest(GuestID)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE Invoice (
    InvoiceID INT AUTO_INCREMENT PRIMARY KEY,
    Amount DECIMAL(10,2) NOT NULL,
    IssueDate DATE NOT NULL,
    PaymentStatus ENUM('Unpaid', 'Paid', 'Overdue') DEFAULT 'Unpaid',
    BookingID INT NOT NULL,
    CONSTRAINT fk_invoice_booking
        FOREIGN KEY (BookingID) REFERENCES Booking(BookingID)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE RoomAssignment (
    AssignmentID INT AUTO_INCREMENT PRIMARY KEY,
    StayDate DATE NOT NULL,
    BookingID INT NOT NULL,
    RoomID INT NOT NULL,
    -- No double-sell: a given room can only be assigned once per date
    CONSTRAINT uq_room_stay UNIQUE (RoomID, StayDate),
    CONSTRAINT fk_assignment_booking
        FOREIGN KEY (BookingID) REFERENCES Booking(BookingID)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_assignment_room
        FOREIGN KEY (RoomID) REFERENCES Room(RoomID)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE RoomMaintenance (
    MaintenanceNo INT NOT NULL,
    RoomID INT NOT NULL,
    Date DATE NOT NULL,
    Description VARCHAR(255),
    StaffName VARCHAR(100),
    PRIMARY KEY (MaintenanceNo, RoomID),
    CONSTRAINT fk_maintenance_room
        FOREIGN KEY (RoomID) REFERENCES Room(RoomID)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- Indexes
CREATE INDEX idx_booking_guest      ON Booking(GuestID);
CREATE INDEX idx_invoice_booking    ON Invoice(BookingID);
CREATE INDEX idx_assignment_booking ON RoomAssignment(BookingID);
CREATE INDEX idx_assignment_room    ON RoomAssignment(RoomID);
CREATE INDEX idx_maintenance_room   ON RoomMaintenance(RoomID);
CREATE INDEX idx_booking_dates      ON Booking(StartDate, EndDate);
CREATE INDEX idx_room_type          ON Room(RoomType);

-- Trigger
CREATE TRIGGER trg_booking_after_insert_invoice
AFTER INSERT ON Booking
FOR EACH ROW
INSERT INTO Invoice (Amount, IssueDate, PaymentStatus, BookingID)
VALUES (0.00, CURDATE(), 'Unpaid', NEW.BookingID);

-- View
DROP VIEW IF EXISTS vw_booking_summary;
CREATE VIEW vw_booking_summary AS
SELECT
  b.BookingID,
  g.GuestID,
  g.Name AS GuestName,
  b.StartDate,
  b.EndDate,
  b.Status,
  ra.StayDate,
  ra.RoomID,
  r.RoomType,
  r.Price
FROM Booking b
JOIN Guest g ON g.GuestID = b.GuestID
LEFT JOIN RoomAssignment ra ON ra.BookingID = b.BookingID
LEFT JOIN Room r ON r.RoomID = ra.RoomID;

-- Initial Data
INSERT INTO Guest (Name, Email, Phone) VALUES
('Ada Lovelace', 'ada@example.com', '555-1000'),
('Alan Turing', 'alan@example.com', '555-2000');

INSERT INTO Room (RoomType, Price, Capacity) VALUES
('Deluxe', 150.00, 2),
('Suite', 250.00, 3);

INSERT INTO Booking (StartDate, EndDate, Status, GuestID) VALUES
('2025-11-10', '2025-11-12', 'Confirmed', 1),
('2025-11-11', '2025-11-13', 'Pending', 2);

INSERT INTO RoomAssignment (StayDate, BookingID, RoomID) VALUES
('2025-11-10', 1, 1),
('2025-11-11', 1, 1),
('2025-11-11', 2, 2);

INSERT INTO RoomMaintenance (RoomID, MaintenanceNo, Date, Description, StaffName) VALUES
(1, 1, '2025-11-09', 'Deep clean', 'Chris'),
(2, 1, '2025-11-10', 'HVAC check', 'Sam');
