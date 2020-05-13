/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.model;

import dev.codion.common.db.exception.DatabaseException;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.chinook.domain.Chinook;
import dev.codion.framework.domain.entity.Entities;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.swing.framework.model.SwingEntityTableModel;

import java.math.BigDecimal;
import java.util.List;

import static dev.codion.framework.demos.chinook.domain.Chinook.F_RAISE_PRICE;
import static dev.codion.framework.demos.chinook.domain.Chinook.TRACK_TRACKID;

public class TrackTableModel extends SwingEntityTableModel {

  public TrackTableModel(final EntityConnectionProvider connectionProvider) {
    super(Chinook.T_TRACK, connectionProvider);
    setEditable(true);
  }

  public void raisePriceOfSelected(final BigDecimal increase) throws DatabaseException {
    final List<Long> trackIds = Entities.getValues(TRACK_TRACKID,
            getSelectionModel().getSelectedItems());

    final List<Entity> result = getConnectionProvider().getConnection()
            .executeFunction(F_RAISE_PRICE, trackIds, increase);
    replaceEntities(result);
  }
}
