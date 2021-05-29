/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.tutorial;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;

import java.util.List;

import static is.codion.framework.db.condition.Conditions.where;
import static is.codion.framework.demos.chinook.tutorial.EntitiesTutorial.Chinook.Album;
import static is.codion.framework.demos.chinook.tutorial.EntitiesTutorial.Chinook.Artist;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.Entity.getPrimaryKeys;
import static is.codion.framework.domain.entity.KeyGenerator.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class EntitiesTutorial {

  /** The domain class, which contains the domain model definition */
  public static final class Chinook extends DefaultDomain {

    // DomainType for identifying this domain model
    static final DomainType DOMAIN = domainType(Chinook.class);

    // EntityType constant for the table entityType
    // and an Attribute for each column
    public interface Artist {
      EntityType<Entity> TYPE = DOMAIN.entityType("chinook.artist");

      Attribute<Integer> ID = TYPE.integerAttribute("artistid");
      Attribute<String> NAME = TYPE.stringAttribute("name");
    }

    // EntityType constant for the table entityType and an Attribute
    // for each column and one for the foreign key relation
    public interface Album {
      EntityType<Entity> TYPE = DOMAIN.entityType("chinook.album");

      Attribute<Integer> ID = TYPE.integerAttribute("albumid");
      Attribute<String> TITLE = TYPE.stringAttribute("title");
      Attribute<Integer> ARTIST_ID = TYPE.integerAttribute("artistid");

      // create a foreign key attribute referencing the Artist.TYPE, via the Album.ARTIST_ID attribute
      ForeignKey ARTIST_FK = TYPE.foreignKey("artist_fk", ARTIST_ID, Artist.ID);
    }

    public Chinook() {
      super(DOMAIN);
      // create properties for the columns in the table 'chinook.artist'
      Property.Builder<Integer> artistId = primaryKeyProperty(Artist.ID);

      Property.Builder<String> artistName = columnProperty(Artist.NAME, "Name");

      artistName.nullable(false).maximumLength(120);

      // define an entity based on the table 'chinook.artist',
      // with the above properties
      define(Artist.TYPE, artistId, artistName)
              .keyGenerator(automatic("chinook.artist"))
              .stringFactory(stringFactory(Artist.NAME))
              .smallDataset()
              .caption("Artist");

      // create properties for the columns in the table 'chinook.album'
      Property.Builder<Integer> albumId = primaryKeyProperty(Album.ID);

      Property.Builder<String> albumTitle = columnProperty(Album.TITLE, "Title");

      albumTitle.nullable(false).maximumLength(160);

      Property.Builder<Integer> albumArtistId = columnProperty(Album.ARTIST_ID);

      albumId.nullable(false);

      Property.Builder<Entity> albumArtist =
              foreignKeyProperty(Album.ARTIST_FK, "Artist");

      // define an entity based on the table 'chinook.album',
      // with the above properties
      define(Album.TYPE, albumId, albumTitle, albumArtistId, albumArtist)
              .keyGenerator(automatic("chinook.album"))
              .stringFactory(stringFactory()
                      .value(Album.ARTIST_FK)
                      .text(" - ")
                      .value(Album.TITLE))
              .caption("Album");
    }
  }

  /**
   * Demonstrates how to use a {@link EntityConnection} to select Entity instances.
   * @throws DatabaseException in case of an exception
   */
  private static void selectingEntities(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // fetch the connection from the provider, note that the provider always
    // returns the same connection or a new one if the previous one has been
    // disconnected or has become invalid for some reason
    EntityConnection connection = connectionProvider.getConnection();

    // select the artist Metallica by name, the selectSingle() method
    // throws a RecordNotFoundException if no record is found and a
    // MultipleRecordsFoundException if more than one are found
    Entity metallica = connection.selectSingle(Artist.NAME, "Metallica");

    // select all albums by Metallica, by using select() with the
    // Metallica Entity as condition value, basically asking for the
    // records where the given foreign key references that specific Entity
    // select() returns an empty list if none are found
    List<Entity> albums = connection.select(Album.ARTIST_FK, metallica);

    albums.forEach(System.out::println);

    // for more complex queries we use a SelectCondition, provided by the Condition class.
    // we create a condition, where we specify the the attribute we're
    // searching by, the type of condition and the value.
    SelectCondition artistsCondition = where(Artist.NAME).equalTo("An%").asSelectCondition();

    // and we set the order by clause
    artistsCondition.orderBy(orderBy().ascending(Artist.NAME));

    List<Entity> artistsStartingWithAn = connection.select(artistsCondition);

    artistsStartingWithAn.forEach(System.out::println);

    // create a select condition
    SelectCondition albumsCondition = where(Album.ARTIST_FK).equalTo(artistsStartingWithAn).asSelectCondition();
    albumsCondition.orderBy(orderBy().ascending(Album.ARTIST_ID).descending(Album.TITLE));

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

    // lets create a new band
    Entity myBand = entities.entity(Artist.TYPE);

    // and give the band a name
    myBand.put(Artist.NAME, "My band name");

    // we start a transaction
    connection.beginTransaction();

    // we insert the Entity, the insert() method returns the primary key
    // of the inserted record, but we don't need it right now so we ignore it.
    // Note that because we're running with a local connection in a single VM
    // the primary key of the entity instance is populated during insert,
    // with a remote connection the insert happens in another VM, so you have
    // to select the entity after insert to get an instance containing
    // the generated key value or use the key received via the return value
    connection.insert(myBand);

    // now for our first album
    Entity album = entities.entity(Album.TYPE);

    // set the album artist by setting the artist foreign key to my band
    album.put(Album.ARTIST_FK, myBand);

    // and set the title
    album.put(Album.TITLE, "My first album");

    // and insert the album
    connection.insert(album);

    // and finally we commit
    connection.commitTransaction();

    // lets rename our album and our band as well
    myBand.put(Artist.NAME, "A proper name");
    album.put(Album.TITLE, "A proper title");

    // and perform the update, note that we only have to use transactions
    // when we're performing multiple insert/update or delete calls,
    // here we're just doing one so this call automatically happens within
    // a single transaction
    connection.update(asList(myBand, album));

    // finally we clean up after ourselves by deleting our imaginary band and album,
    // note that the order of the entities matters, since we can't delete
    // the artist before the album, this method deletes records in the
    // same order as the are received
    connection.delete(getPrimaryKeys(asList(album, myBand)));
  }

  public static void main(final String[] args) throws DatabaseException {
    // Configure the database
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPTS.set("src/main/sql/create_schema.sql");
    // initialize a connection provider, this class is responsible
    // for supplying a valid connection or throwing an exception
    // in case a connection can not be established
    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(DatabaseFactory.getDatabase())
                    .setDomainClassName(Chinook.class.getName())
                    .setUser(User.parseUser("scott:tiger"));

    selectingEntities(connectionProvider);

    modifyingEntities(connectionProvider);

    connectionProvider.close();
  }
}
