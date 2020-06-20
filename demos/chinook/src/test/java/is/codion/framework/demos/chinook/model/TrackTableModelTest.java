/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TrackTableModelTest {

  @Test
  public void raisePriceOfSelected() throws DatabaseException {
    final EntityConnectionProvider connectionProvider = createConnectionProvider();

    final Entity masterOfPuppets = connectionProvider.getConnection()
            .selectSingle(Album.TITLE, "Master Of Puppets");

    final TrackTableModel trackTableModel = new TrackTableModel(connectionProvider);
    final ColumnConditionModel<Entity, ?> albumConditionModel =
            trackTableModel.getTableConditionModel().getConditionModel(Track.ALBUM_FK);

    albumConditionModel.setEqualValue(masterOfPuppets);

    trackTableModel.refresh();
    assertEquals(8, trackTableModel.getRowCount());

    trackTableModel.getSelectionModel().selectAll();
    trackTableModel.raisePriceOfSelected(BigDecimal.ONE);

    trackTableModel.getItems().forEach(track ->
            assertEquals(BigDecimal.valueOf(1.99), track.get(Track.UNITPRICE)));
  }

  private EntityConnectionProvider createConnectionProvider() {
    return new LocalEntityConnectionProvider(Databases.getInstance())
            .setDomainClassName(ChinookImpl.class.getName())
            .setUser(Users.parseUser("scott:tiger"));
  }
}
