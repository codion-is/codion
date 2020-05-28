/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.tutorial;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.EntitySelectCondition;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.Property;

import java.sql.Types;
import java.util.List;

import static is.codion.common.db.Operator.LIKE;
import static is.codion.framework.db.condition.Conditions.selectCondition;
import static is.codion.framework.demos.chinook.tutorial.EntitiesTutorial.Chinook.*;
import static is.codion.framework.domain.entity.Entities.getKeys;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class EntitiesTutorial {

  /** The domain class, which contains the domain model definition */
  public static final class Chinook extends Domain {

    //string constant for the table entityId ('T_' prefix)
    //and a Attribute for each column
    public static final String T_ARTIST = "chinook.artist";
    public static final Attribute<Integer> ARTIST_ID = attribute("artistid");
    public static final Attribute<String> ARTIST_NAME = attribute("name");

    //string constants for the table entityId ('T_' prefix),
    //and a Attribute for each column and one for the foreign key relation
    public static final String T_ALBUM = "chinook.album";
    public static final Attribute<Integer> ALBUM_ALBUMID = attribute("albumid");
    public static final Attribute<String> ALBUM_TITLE = attribute("title");
    public static final Attribute<Integer> ALBUM_ARTISTID = attribute("artistid");
    public static final Attribute<Entity> ALBUM_ARTIST_FK = attribute("artist_fk");

    public Chinook() {
      //create properties for the columns in the table 'chinook.artist'
      Property.Builder artistId = primaryKeyProperty(ARTIST_ID, Types.INTEGER);
      Property.Builder artistName = columnProperty(ARTIST_NAME, Types.VARCHAR, "Name");
      artistName.nullable(false).maximumLength(120);

      //define an entity based on the table 'chinook.artist',
      //with the above properties
      define(T_ARTIST, artistId, artistName)
              .keyGenerator(automatic("chinook.artist"))
              .stringProvider(new StringProvider(ARTIST_NAME))
              .smallDataset(true)
              .caption("Artist");

      //create properties for the columns in the table 'chinook.album'
      Property.Builder albumId = primaryKeyProperty(ALBUM_ALBUMID, Types.INTEGER);
      Property.Builder albumTitle = columnProperty(ALBUM_TITLE, Types.VARCHAR, "Title");
      albumTitle.nullable(false).maximumLength(160);
      //we wrap the actual 'artistid' column property in a foreign key
      //referencing the entity identified by T_ARTIST
      Property.Builder albumArtist =
              foreignKeyProperty(ALBUM_ARTIST_FK, "Artist", T_ARTIST,
                      columnProperty(ALBUM_ARTISTID, Types.INTEGER));
      albumArtist.nullable(false);

      //define an entity based on the table 'chinook.album',
      //with the above properties
      define(T_ALBUM, albumId, albumTitle, albumArtist)
              .keyGenerator(automatic("chinook.album"))
              .stringProvider(new StringProvider()
                      .addValue(ALBUM_ARTIST_FK)
                      .addText(" - ")
                      .addValue(ALBUM_TITLE))
              .caption("Album");
    }
  }

  /**
   * Demonstrates how to use a {@link EntityConnection} to select Entity instances.
   * @throws DatabaseException in case of an exception
   */
  private static void selectingEntities(EntityConnectionProvider connectionProvider) throws DatabaseException {
    //fetch the connection from the provider, note that the provider always
    //returns the same connection or a new one if the previous one has been
    //disconnected or has become invalid for some reason
    EntityConnection connection = connectionProvider.getConnection();

    //select the artist Metallica by name, the selectSingle() method
    //throws a RecordNotFoundException if no record is found and a
    //MultipleRecordsFoundException if more than one are found
    Entity metallica = connection.selectSingle(T_ARTIST, ARTIST_NAME, "Metallica");

    //select all albums by Metallica, by using select() with the
    //Metallica Entity as condition value, basically asking for the
    //records where the given foreign key references that specific Entity
    //select() returns an empty list if none are found
    List<Entity> albums = connection.select(T_ALBUM, ALBUM_ARTIST_FK, metallica);

    albums.forEach(System.out::println);

    //for more complex queries we use a EntitySelectCondition, provided
    //by the Conditions factory class.
    //we create a select condition, where we specify the id of the entity
    //we're selecting, the id of the property we're searching by, the type
    //of condition and the value.
    EntitySelectCondition artistsCondition = selectCondition(T_ARTIST, ARTIST_NAME, LIKE, "An%");
    //and we set the order by clause
    artistsCondition.setOrderBy(orderBy().ascending(ARTIST_NAME));

    List<Entity> artistsStartingWithAn = connection.select(artistsCondition);

    artistsStartingWithAn.forEach(System.out::println);

    //create a select condition
    EntitySelectCondition albumsCondition = selectCondition(T_ALBUM, ALBUM_ARTIST_FK, LIKE, artistsStartingWithAn);
    albumsCondition.setOrderBy(orderBy().ascending(ALBUM_ARTISTID).descending(ALBUM_TITLE));

    List<Entity> albumsByArtistsStartingWithAn = connection.select(albumsCondition);

    albumsByArtistsStartingWithAn.forEach(System.out::println);
  }

  /**
   * Demonstrates how to use a {@link EntityConnection} to modify Entity instances.
   * @throws DatabaseException in case of an exception
   */
  private static void modifyingEntities(EntityConnectionProvider connectionProvider) throws DatabaseException {
    EntityConnection connection = connectionProvider.getConnection();

    //this Entities object serves as a factory for Entity instances
    Entities entities = connectionProvider.getEntities();

    //lets create a new band
    Entity myBand = entities.entity(T_ARTIST);
    //and give the band a name
    myBand.put(ARTIST_NAME, "My band name");

    //we start a transaction
    connection.beginTransaction();

    //we insert the Entity, the insert() method returns the primary key
    //of the inserted record, but we don't need it right now so we ignore it.
    //Note that because we're running with a local connection in a single VM
    //the primary key of the entity instance is populated during insert,
    //with a remote connection the insert happens in another VM, so you have
    //to select the entity after insert to get an instance containing
    //the generated key value or use the key received via the return value
    connection.insert(myBand);

    //now for our first album
    Entity album = entities.entity(T_ALBUM);
    //set the album artist by setting the artist foreign key to my band
    album.put(ALBUM_ARTIST_FK, myBand);
    //and set the title
    album.put(ALBUM_TITLE, "My first album");

    //and insert the album
    connection.insert(album);

    //and finally we commit
    connection.commitTransaction();

    //lets rename our album and our band as well
    myBand.put(ARTIST_NAME, "A proper name");
    album.put(ALBUM_TITLE, "A proper title");

    //and perform the update, note that we only have to use transactions
    //when we're performing multiple insert/update or delete calls,
    //here we're just doing one so this call automatically happens within
    //a single transaction
    connection.update(asList(myBand, album));

    //finally we clean up after ourselves by deleting our imaginary band and album,
    //note that the order of the entities matters, since we can't delete
    //the artist before the album, this method deletes records in the
    //same order as the are received
    connection.delete(getKeys(asList(album, myBand)));
  }

  public static void main(final String[] args) throws DatabaseException {
    // Configure the database
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");
    //initialize a connection provider, this class is responsible
    //for supplying a valid connection or throwing an exception
    //in case a connection can not be established
    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(Databases.getInstance())
                    .setDomainClassName(Chinook.class.getName())
                    .setUser(Users.parseUser("scott:tiger"));

    selectingEntities(connectionProvider);

    modifyingEntities(connectionProvider);

    connectionProvider.disconnect();
  }
}
