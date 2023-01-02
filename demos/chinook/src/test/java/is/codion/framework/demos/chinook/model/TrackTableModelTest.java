/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
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
  void raisePriceOfSelected() throws DatabaseException {
    EntityConnectionProvider connectionProvider = createConnectionProvider();

    Entity masterOfPuppets = connectionProvider.connection()
            .selectSingle(Album.TITLE, "Master Of Puppets");

    TrackTableModel trackTableModel = new TrackTableModel(connectionProvider);
    trackTableModel.tableConditionModel().conditionModel(Track.ALBUM_FK)
            .setEqualValue(masterOfPuppets);

    trackTableModel.refresh();
    assertEquals(8, trackTableModel.getRowCount());

    trackTableModel.selectionModel().selectAll();
    trackTableModel.raisePriceOfSelected(BigDecimal.ONE);

    trackTableModel.items().forEach(track ->
            assertEquals(BigDecimal.valueOf(1.99), track.get(Track.UNITPRICE)));
  }

  private EntityConnectionProvider createConnectionProvider() {
    return LocalEntityConnectionProvider.builder()
            .domainClassName(ChinookImpl.class.getName())
            .user(User.parse("scott:tiger"))
            .build();
  }
}
