create schema chinook;

CREATE TABLE Chinook.Genre
(
    GenreId INTEGER NOT NULL,
    Name VARCHAR(120) ,
    CONSTRAINT PK_Genre PRIMARY KEY (GenreId)
);

CREATE TABLE Chinook.MediaType
(
    MediaTypeId INTEGER NOT NULL,
    Name VARCHAR(120) ,
    CONSTRAINT PK_MediaType PRIMARY KEY (MediaTypeId)
);

CREATE TABLE Chinook.Artist
(
    ArtistId INTEGER NOT NULL,
    Name VARCHAR(120) ,
    CONSTRAINT PK_Artist PRIMARY KEY (ArtistId)
);

CREATE TABLE Chinook.Album
(
    AlbumId INTEGER NOT NULL,
    Title VARCHAR(160) NOT NULL,
    ArtistId INTEGER NOT NULL,
    CONSTRAINT PK_ProductItem PRIMARY KEY (AlbumId)
);

CREATE TABLE Chinook.Track
(
    TrackId INTEGER NOT NULL,
    Name VARCHAR(200) NOT NULL,
    AlbumId INTEGER ,
    MediaTypeId INTEGER NOT NULL,
    GenreId INTEGER ,
    Composer VARCHAR(220) ,
    Milliseconds INTEGER NOT NULL,
    Bytes DOUBLE ,
    UnitPrice DOUBLE NOT NULL,
    CONSTRAINT PK_Track PRIMARY KEY (TrackId)
);

CREATE TABLE Chinook.Employee
(
    EmployeeId INTEGER NOT NULL,
    LastName VARCHAR(20) NOT NULL,
    FirstName VARCHAR(20) NOT NULL,
    Title VARCHAR(30) ,
    ReportsTo INTEGER ,
    BirthDate DATE ,
    HireDate DATE ,
    Address VARCHAR(70) ,
    City VARCHAR(40) ,
    State VARCHAR(40) ,
    Country VARCHAR(40) ,
    PostalCode VARCHAR(10) ,
    Phone VARCHAR(24) ,
    Fax VARCHAR(24) ,
    Email VARCHAR(60) ,
    CONSTRAINT PK_Employee PRIMARY KEY (EmployeeId)
);

CREATE TABLE Chinook.Customer
(
    CustomerId INTEGER NOT NULL,
    FirstName VARCHAR(40) NOT NULL,
    LastName VARCHAR(20) NOT NULL,
    Company VARCHAR(80) ,
    Address VARCHAR(70) ,
    City VARCHAR(40) ,
    State VARCHAR(40) ,
    Country VARCHAR(40) ,
    PostalCode VARCHAR(10) ,
    Phone VARCHAR(24) ,
    Fax VARCHAR(24) ,
    Email VARCHAR(60) NOT NULL,
    SupportRepId INTEGER ,
    CONSTRAINT PK_Customer PRIMARY KEY (CustomerId)
);

CREATE TABLE Chinook.Invoice
(
    InvoiceId INTEGER NOT NULL,
    CustomerId INTEGER NOT NULL,
    InvoiceDate DATE NOT NULL,
    BillingAddress VARCHAR(70) ,
    BillingCity VARCHAR(40) ,
    BillingState VARCHAR(40) ,
    BillingCountry VARCHAR(40) ,
    BillingPostalCode VARCHAR(10) ,
    Total DOUBLE NOT NULL,
    CONSTRAINT PK_Invoice PRIMARY KEY (InvoiceId)
);

CREATE TABLE Chinook.InvoiceLine
(
    InvoiceLineId INTEGER NOT NULL,
    InvoiceId INTEGER NOT NULL,
    TrackId INTEGER NOT NULL,
    UnitPrice DOUBLE NOT NULL,
    Quantity INTEGER NOT NULL,
    CONSTRAINT PK_InvoiceLine PRIMARY KEY (InvoiceLineId)
);

CREATE TABLE Chinook.Playlist
(
    PlaylistId INTEGER NOT NULL,
    Name VARCHAR(120) ,
    CONSTRAINT PK_Playlist PRIMARY KEY (PlaylistId)
);

CREATE TABLE Chinook.PlaylistTrack
(
    PlaylistId INTEGER NOT NULL,
    TrackId INTEGER NOT NULL,
    CONSTRAINT PK_PlaylistTrack PRIMARY KEY (PlaylistId, TrackId)
);


/*******************************************************************************
   Create Foreign Keys
********************************************************************************/
ALTER TABLE Chinook.Album ADD CONSTRAINT FK_Artist_Album FOREIGN KEY (ArtistId) REFERENCES Chinook.Artist(ArtistId);
ALTER TABLE Chinook.Track ADD CONSTRAINT FK_Album_Track FOREIGN KEY (AlbumId) REFERENCES Chinook.Album(AlbumId);
ALTER TABLE Chinook.Track ADD CONSTRAINT FK_MediaType_Track FOREIGN KEY (MediaTypeId) REFERENCES Chinook.MediaType(MediaTypeId);
ALTER TABLE Chinook.Track ADD CONSTRAINT FK_Genre_Track FOREIGN KEY (GenreId) REFERENCES Chinook.Genre(GenreId);
ALTER TABLE Chinook.Employee ADD CONSTRAINT FK_Employee_ReportsTo FOREIGN KEY (ReportsTo) REFERENCES Chinook.Employee(EmployeeId);
ALTER TABLE Chinook.Customer ADD CONSTRAINT FK_Employee_Customer FOREIGN KEY (SupportRepId) REFERENCES Chinook.Employee(EmployeeId);
ALTER TABLE Chinook.Invoice ADD CONSTRAINT FK_Customer_Invoice FOREIGN KEY (CustomerId) REFERENCES Chinook.Customer(CustomerId);
ALTER TABLE Chinook.InvoiceLine ADD CONSTRAINT FK_ProductItem_InvoiceLine FOREIGN KEY (TrackId) REFERENCES Chinook.Track(TrackId);
ALTER TABLE Chinook.InvoiceLine ADD CONSTRAINT FK_Invoice_InvoiceLine FOREIGN KEY (InvoiceId) REFERENCES Chinook.Invoice(InvoiceId);
ALTER TABLE Chinook.PlaylistTrack ADD CONSTRAINT FK_Track_PlaylistTrack FOREIGN KEY (TrackId) REFERENCES Chinook.Track(TrackId);
ALTER TABLE Chinook.PlaylistTrack ADD CONSTRAINT FK_Playlist_PlaylistTrack FOREIGN KEY (PlaylistId) REFERENCES Chinook.Playlist(PlaylistId);

