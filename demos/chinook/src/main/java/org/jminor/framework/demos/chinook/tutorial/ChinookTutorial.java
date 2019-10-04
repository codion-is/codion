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

import static org.jminor.framework.domain.Properties.*;

/**
 * A tutorial class for demonstration purposes.
 */
public final class ChinookTutorial {

  /** The domain class, which contains the domain model definition */
  public static final class Chinook extends Domain {

    /*String constants for the table ('T_' prefix) and for each column*/
    private static final String T_ARTIST = "chinook.artist";
    private static final String ARTIST_ID = "artistid";
    private static final String ARTIST_NAME = "name";

    /*String constants for the table ('T_' prefix), for each column
      and one for the foreign key relation*/
    private static final String T_ALBUM = "chinook.album";
    private static final String ALBUM_ALBUMID = "albumid";
    private static final String ALBUM_TITLE = "title";
    private static final String ALBUM_ARTISTID = "artistid";
    private static final String ALBUM_ARTISTID_FK = "artist_fk";

    public Chinook() {
      /* Define an entity based on the table 'chinook.artist' */
      define(T_ARTIST,
              primaryKeyProperty(ARTIST_ID),
              columnProperty(ARTIST_NAME, Types.VARCHAR, "Name")
                      .setNullable(false).setMaxLength(120))
              .setKeyGenerator(automaticKeyGenerator("chinook.artist"))
              .setStringProvider(new Domain.StringProvider(ARTIST_NAME))
              .setSmallDataset(true)
              .setCaption("Album");

      /* Define an entity based on the table 'chinook.album' */
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
   * to select and manipulate Entity instances.
   * @see #createDatabase()
   * @see #createUser()
   * @throws DatabaseException in case of a exception
   */
  private static void entities() throws DatabaseException {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(createDatabase())
            .setDomainClassName(Chinook.class.getName())
            .setUser(createUser());

    final EntityConnection connection = connectionProvider.getConnection();

    final Entity metallica = connection.selectSingle(Chinook.T_ARTIST, Chinook.ARTIST_NAME, "Metallica");

    final List<Entity> albums = connection.selectMany(Chinook.T_ALBUM, Chinook.ALBUM_ARTISTID_FK, metallica);
    albums.forEach(System.out::println);

    final EntityConditions conditions = connectionProvider.getConditions();
    final EntitySelectCondition artistsStartingWithAnCondition = conditions.selectCondition(Chinook.T_ARTIST,
            Chinook.ARTIST_NAME, Condition.Type.LIKE, "An%")
            .setOrderBy(Domain.orderBy().ascending(Chinook.ARTIST_NAME));

    final List<Entity> artistsStartingWithAn = connection.selectMany(artistsStartingWithAnCondition);

    artistsStartingWithAn.forEach(System.out::println);

    connection.disconnect();
  }

  /** Configures the embedded in-memory H2 database */
  private static void configureDatabase() {
    // Configure the datababase
    Database.DATABASE_TYPE.set(Database.Type.H2.toString());
    Database.DATABASE_EMBEDDED_IN_MEMORY.set(true);
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");
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
    entities();
  }
}
