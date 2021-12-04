/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static is.codion.framework.demos.chinook.domain.Chinook.Track;
import static java.util.Arrays.asList;

public final class TrackTableModel extends SwingEntityTableModel {

  public TrackTableModel(final EntityConnectionProvider connectionProvider) {
    super(Track.TYPE, connectionProvider);
    setEditable(true);
  }

  public void raisePriceOfSelected(final BigDecimal increase) throws DatabaseException {
    if (getSelectionModel().isSelectionNotEmpty()) {
      Collection<Long> trackIds = Entity.get(Track.ID, getSelectionModel().getSelectedItems());
      List<Entity> result = getConnectionProvider().getConnection()
              .executeFunction(Track.RAISE_PRICE, asList(trackIds, increase));
      replaceEntities(result);
    }
  }
}
