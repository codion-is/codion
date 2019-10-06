/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.tutorial;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;

import java.sql.Types;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.jminor.framework.domain.Entities.getKeys;
import static org.jminor.framework.domain.Properties.*;

/**
 * A tutorial class for demonstration purposes.
 */
public final class ChinookTutorial {

  /** The domain class, which contains the domain model definition */
  public static final class Chinook extends Domain {

    //string constants for the table ('T_' prefix) and for each column
    private static final String T_ARTIST = "chinook.artist";
    private static final String ARTIST_ID = "artistid";
    private static final String ARTIST_NAME = "name";

    //string constants for the table ('T_' prefix), for each column
    //and one for the foreign key relation
    private static final String T_ALBUM = "chinook.album";
    private static final String ALBUM_ALBUMID = "albumid";
    private static final String ALBUM_TITLE = "title";
    private static final String ALBUM_ARTISTID = "artistid";
    private static final String ALBUM_ARTISTID_FK = "artist_fk";

    public Chinook() {
      //define an entity based on the table 'chinook.artist'
      define(T_ARTIST,
              primaryKeyProperty(ARTIST_ID),
              columnProperty(ARTIST_NAME, Types.VARCHAR, "Name")
                      .setNullable(false).setMaxLength(120))
              .setKeyGenerator(automaticKeyGenerator("chinook.artist"))
              .setStringProvider(new Domain.StringProvider(ARTIST_NAME))
              .setSmallDataset(true)
              .setCaption("Album");

      //define an entity based on the table 'chinook.album'
      define(T_ALBUM,
              primaryKeyProperty(ALBUM_ALBUMID),
              columnProperty(ALBUM_TITLE, Types.VARCHAR, "Title")
                      .setNullable(false).setMaxLength(160),
              foreignKeyProperty(ALBUM_ARTISTID_FK, "Artist", T_ARTIST,
                      columnProperty(ALBUM_ARTISTID))
                      .setNullable(false))
              .setKeyGenerator(automaticKeyGenerator("chinook.album"))
              .setStringProvider(new StringProvider()
                      .addValue(ALBUM_ARTISTID_FK).addText(" - ").addValue(ALBUM_TITLE))
              .setCaption("Album");
    }
  }

  /**
   * Demonstrates how a {@link EntityConnection} is created and used
   * to select Entity instances.
   * @see #createDatabase()
   * @see #createUser()
   * @throws DatabaseException in case of a exception
   */
  private static void selectingEntities() throws DatabaseException {
    //initialize a connection provider, this class is responsible
    //for supplying a valid connection or throwing an exception
    //in case a connection can not be established
    final EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(createDatabase())
                    .setDomainClassName(Chinook.class.getName())
                    .setUser(createUser());

    //fetch the connection from the provider, note that the provider always
    //returns the same connection or a new one if the previous one has been
    //disconnected or has become invalid for some reason
    final EntityConnection connection = connectionProvider.getConnection();

    //select the artist Metallica by name, the selectSingle() method
    //throws RecordNotFoundException if no record is found and a regular
    //DatabaseException with a relevant message if more than one are found
    final Entity metallica = connection.selectSingle(Chinook.T_ARTIST,
            Chinook.ARTIST_NAME, "Metallica");

    //select all albums by Metallica, by using selectMany() with the
    //Metallica Entity as condition value, basically asking for the
    //records where the given foreign key references that specific Entity
    //selectMany() returns an empty list if none are found
    final List<Entity> albums = connection.selectMany(Chinook.T_ALBUM,
            Chinook.ALBUM_ARTISTID_FK, metallica);
    albums.forEach(System.out::println);

    //for more complex queries we use a EntitySelectCondition, provided
    //by a EntityConditions instance based on the domain model
    final EntityConditions conditions = connectionProvider.getConditions();
    //we create a select condition, where we specify the id of the entity
    //we're selecting, the id of the property we're searching, the type
    //of condition and the value. We also set the order by clause.
    final EntitySelectCondition artistsStartingWithAnCondition =
            conditions.selectCondition(Chinook.T_ARTIST,
                    Chinook.ARTIST_NAME, Condition.Type.LIKE, "An%");
    //and finally we set the order by clause
    artistsStartingWithAnCondition.setOrderBy(
            Domain.orderBy().ascending(Chinook.ARTIST_NAME));

    final List<Entity> artistsStartingWithAn =
            connection.selectMany(artistsStartingWithAnCondition);
    artistsStartingWithAn.forEach(System.out::println);

    //create a select condition
    final EntitySelectCondition albumsByArtistsStartingWithAnCondition =
            conditions.selectCondition(Chinook.T_ALBUM,
                    Chinook.ALBUM_ARTISTID_FK, Condition.Type.LIKE, artistsStartingWithAn);
    albumsByArtistsStartingWithAnCondition.setOrderBy(
            Domain.orderBy()
                    .ascending(Chinook.ALBUM_ARTISTID)
                    .descending(Chinook.ALBUM_TITLE));

    final List<Entity> albumsByArtistsStartingWithAn =
            connection.selectMany(albumsByArtistsStartingWithAnCondition);
    albumsByArtistsStartingWithAn.forEach(System.out::println);

    //disconnects the underlying connection
    connectionProvider.disconnect();
  }

  private static void modifyingEntities() throws DatabaseException {
    final EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(createDatabase())
                    .setDomainClassName(Chinook.class.getName())
                    .setUser(createUser());

    final EntityConnection connection = connectionProvider.getConnection();
    final Domain domain = connectionProvider.getDomain();

    //lets create a new artist
    final Entity myBand = domain.entity(Chinook.T_ARTIST);
    //and give the band a name
    myBand.put(Chinook.ARTIST_NAME, "My band name");

    //we start a transaction
    connection.beginTransaction();

    //we insert the Entity, the insert() method returns a List
    //containing the primary keys of the inserted records, but
    //we ignore those since we don't need them right now.
    //Note that the primary key of the entity instance is populated
    //during insert, that's because we're running with a local connection,
    //with a remote connections you have to select the entity
    //after insert to get an instance containing the generated key value
    connection.insert(singletonList(myBand));

    //now for our first album
    final Entity album = domain.entity(Chinook.T_ALBUM);
    //set the album artist by setting the artist foreign key to my band
    album.put(Chinook.ALBUM_ARTISTID_FK, myBand);
    //and set the title
    album.put(Chinook.ALBUM_TITLE, "My first album");

    //and insert the album
    connection.insert(singletonList(album));

    //and finally we commit
    connection.commitTransaction();

    //lets rename our album and our band as well
    myBand.put(Chinook.ARTIST_NAME, "A proper name");
    album.put(Chinook.ALBUM_TITLE, "A proper title");

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

    connectionProvider.disconnect();
  }

  /** Configures the embedded in-memory H2 database */
  private static void configureDatabase() {
    // Configure the datababase
    Database.DATABASE_TYPE.set(Database.Type.H2.toString());
    Database.DATABASE_EMBEDDED_IN_MEMORY.set(true);
    Database.DATABASE_INIT_SCRIPT.set("demos/chinook/src/main/sql/create_schema.sql");
  }

  /** @return an embedded in-memory H2 database instance */
  private static Database createDatabase() {
    configureDatabase();
    // Creates an instance based on the configuration values
    return Databases.getInstance();
  }

  /** @return a test User */
  private static User createUser() {
    return new User("scott", "tiger".toCharArray());
  }

  public static void main(final String[] args) throws DatabaseException {
    selectingEntities();
    modifyingEntities();
  }
}
