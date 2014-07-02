/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools;

import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.ProgressReporter;
import org.jminor.common.model.User;
import org.jminor.framework.db.DefaultEntityConnectionTest;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnections;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
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

public class EntityDataUtilTest {

  private static EntityConnection DESTINATION_CONNECTION;

  static {
    Chinook.init();
  }

  @BeforeClass
  public static void setUp() {
    try {
      final H2Database destinationDatabase = new H2Database("TempDB", "resources/demos/chinook/scripts/ddl.sql");
      DESTINATION_CONNECTION = EntityConnections.createConnection(destinationDatabase, new User("sa", ""));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @AfterClass
  public static void tearDown() {
    DESTINATION_CONNECTION.disconnect();
  }

  @Test
  public void copyEntities() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = DefaultEntityConnectionTest.CONNECTION_PROVIDER.getConnection();
    EntityDataUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 16, true, Chinook.T_ARTIST);
    EntityDataUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 16, true, Chinook.T_ALBUM);
    EntityDataUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 16, true, Chinook.T_MEDIATYPE);
    EntityDataUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 16, true, Chinook.T_GENRE);
    EntityDataUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 16, false, Chinook.T_TRACK);

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
    final EntityConnection sourceConnection = DefaultEntityConnectionTest.CONNECTION_PROVIDER.getConnection();

    final List<Entity> source = sourceConnection.selectAll(Chinook.T_PLAYLIST);
    final List<Entity.Key> dest = new ArrayList<>();
    final ProgressReporter progressReporter = new ProgressReporter() {
      @Override
      public void reportProgress(final int currentProgress) {}
    };
    EntityDataUtil.batchInsert(DESTINATION_CONNECTION, source, dest, 6, progressReporter);
    assertEquals(sourceConnection.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_PLAYLIST)),
            DESTINATION_CONNECTION.selectRowCount(EntityCriteriaUtil.criteria(Chinook.T_PLAYLIST)));

    EntityDataUtil.batchInsert(DESTINATION_CONNECTION, Collections.<Entity>emptyList(), null, 10, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void batchInsertNegativeBatchSize() throws DatabaseException {
    EntityDataUtil.batchInsert(null, null, null, -6, null);
  }
}
