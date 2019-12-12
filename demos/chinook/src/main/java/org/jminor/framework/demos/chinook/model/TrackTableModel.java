/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import java.math.BigDecimal;
import java.util.List;

import static org.jminor.framework.demos.chinook.domain.Chinook.F_RAISE_PRICE;
import static org.jminor.framework.demos.chinook.domain.Chinook.TRACK_TRACKID;

public class TrackTableModel extends SwingEntityTableModel {

  public TrackTableModel(final EntityConnectionProvider connectionProvider) {
    super(Chinook.T_TRACK, connectionProvider);
  }

  public void raisePriceOfSelected(final BigDecimal increase) throws DatabaseException {
    final List<Long> trackIds = Entities.getValues(TRACK_TRACKID,
            getSelectionModel().getSelectedItems());

    final List<Entity> result = getConnectionProvider().getConnection()
            .executeFunction(F_RAISE_PRICE, trackIds, increase);
    replaceEntities(result);
  }
}
