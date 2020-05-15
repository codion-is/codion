/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.model;

import dev.codion.common.db.database.Databases;
import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.model.table.ColumnConditionModel;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.demos.chinook.domain.Chinook;
import dev.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import dev.codion.framework.domain.entity.Entity;

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
