/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.domain;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Random;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ChinookTest extends EntityTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  public ChinookTest() {
    super(ChinookImpl.class.getName());
  }

  @Test
  public void album() throws Exception {
    testEntity(T_ALBUM);
  }

  @Test
  public void albumCoverArt() throws Exception {
    final Entity artist = initializeReferenceEntity(T_ARTIST, emptyMap());
    final HashMap<String, Entity> foreignKeyReferences = new HashMap<>();
    final EntityConnection connection = getConnection();

    foreignKeyReferences.put(T_ARTIST, connection.selectSingle(
            connection.insert(singletonList(artist)).get(0)));

    final Entity album = connection.selectSingle(
            connection.insert(singletonList(
                    initializeTestEntity(T_ALBUM, foreignKeyReferences))).get(0));

    final byte[] coverart = new byte[1024];
    new Random().nextBytes(coverart);

    connection.writeBlob(album.getKey(), ALBUM_COVERART, coverart);

    final byte[] bytes = connection.readBlob(album.getKey(), ALBUM_COVERART);

    assertArrayEquals(coverart, bytes);
  }

  @Test
  public void artist() throws Exception {
    testEntity(T_ARTIST);
  }

  @Test
  public void customer() throws Exception {
    testEntity(T_CUSTOMER);
  }

  @Test
  public void employee() throws Exception {
    testEntity(T_EMPLOYEE);
  }

  @Test
  public void genre() throws Exception {
    testEntity(T_GENRE);
  }

  @Test
  public void invoce() throws Exception {
    testEntity(T_INVOICE);
  }

  @Test
  public void invoiceLine() throws Exception {
    testEntity(T_INVOICELINE);
  }

  @Test
  public void mediaType() throws Exception {
    testEntity(T_MEDIATYPE);
  }

  @Test
  public void playlist() throws Exception {
    testEntity(T_PLAYLIST);
  }

  @Test
  public void playlistTrack() throws Exception {
    testEntity(T_PLAYLISTTRACK);
  }

  @Test
  public void track() throws Exception {
    testEntity(T_TRACK);
  }

  @Override
  protected User getTestUser() throws CancelException {
    return UNIT_TEST_USER;
  }
}
