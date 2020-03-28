/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.model;

import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;
import org.jminor.framework.domain.entity.Entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TrackTableModelTest {

  @Test
  public void raisePriceOfSelected() throws DatabaseException {
    final EntityConnectionProvider connectionProvider = createConnectionProvider();

    final Entity masterOfPuppets = connectionProvider.getConnection()
            .selectSingle(Chinook.T_ALBUM, Chinook.ALBUM_TITLE, "Master Of Puppets");

    final TrackTableModel trackTableModel = new TrackTableModel(connectionProvider);
    final ColumnConditionModel albumConditionModel =
            trackTableModel.getConditionModel().getPropertyConditionModel(Chinook.TRACK_ALBUM_FK);

    albumConditionModel.setLikeValue(masterOfPuppets);

    trackTableModel.refresh();
    assertEquals(8, trackTableModel.getRowCount());

    trackTableModel.getSelectionModel().selectAll();
    trackTableModel.raisePriceOfSelected(BigDecimal.ONE);

    trackTableModel.getItems().forEach(track ->
            assertEquals(BigDecimal.valueOf(1.99), track.getBigDecimal(Chinook.TRACK_UNITPRICE)));
  }

  private EntityConnectionProvider createConnectionProvider() {
    return new LocalEntityConnectionProvider(Databases.getInstance())
            .setDomainClassName(ChinookImpl.class.getName())
            .setUser(Users.parseUser("scott:tiger"));
  }
}
