/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.ProgressReporter;
import org.jminor.common.model.User;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.local.LocalEntityConnectionTest;
import org.jminor.framework.db.local.LocalEntityConnections;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Entity;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EntityConnectionUtilTest {

  private static EntityConnection DESTINATION_CONNECTION;

  static {
    Chinook.init();
  }

  @BeforeClass
  public static void setUp() {
    try {
      final H2Database destinationDatabase = new H2Database("TempDB", "resources/demos/chinook/scripts/ddl.sql");
      DESTINATION_CONNECTION = LocalEntityConnections.createConnection(destinationDatabase, new User("sa", ""));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @AfterClass
  public static void tearDown() {
    DESTINATION_CONNECTION.disconnect();
  }

  @Test
  public void copyEntities() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = LocalEntityConnectionTest.CONNECTION_PROVIDER.getConnection();
    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 16, true, Chinook.T_ARTIST);
    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 16, true, Chinook.T_ALBUM);
    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 16, true, Chinook.T_MEDIATYPE);
    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 16, true, Chinook.T_GENRE);
    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 16, false, Chinook.T_TRACK);

    assertEquals(sourceConnection.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_ARTIST)),
            DESTINATION_CONNECTION.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_ARTIST)));
    assertEquals(sourceConnection.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_ALBUM)),
            DESTINATION_CONNECTION.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_ALBUM)));
    assertEquals(sourceConnection.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_MEDIATYPE)),
            DESTINATION_CONNECTION.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_MEDIATYPE)));
    assertEquals(sourceConnection.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_GENRE)),
            DESTINATION_CONNECTION.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_GENRE)));
    assertEquals(sourceConnection.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_TRACK)),
            DESTINATION_CONNECTION.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_TRACK)));
  }

  @Test
  public void batchInsert() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = LocalEntityConnectionTest.CONNECTION_PROVIDER.getConnection();

    final List<Entity> source = sourceConnection.selectAll(Chinook.T_PLAYLIST);
    final List<Entity.Key> dest = new ArrayList<>();
    final ProgressReporter progressReporter = new ProgressReporter() {
      @Override
      public void reportProgress(final int currentProgress) {}
    };
    EntityConnectionUtil.batchInsert(DESTINATION_CONNECTION, source, dest, 6, progressReporter);
    assertEquals(sourceConnection.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_PLAYLIST)),
            DESTINATION_CONNECTION.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_PLAYLIST)));

    EntityConnectionUtil.batchInsert(DESTINATION_CONNECTION, Collections.<Entity>emptyList(), null, 10, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void batchInsertNegativeBatchSize() throws DatabaseException {
    EntityConnectionUtil.batchInsert(null, null, null, -6, null);
  }
}
