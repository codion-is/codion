/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Track.RaisePriceParameters;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static is.codion.framework.demos.chinook.domain.Chinook.Track;

public final class TrackTableModel extends SwingEntityTableModel {

  public TrackTableModel(EntityConnectionProvider connectionProvider) {
    super(Track.TYPE, connectionProvider);
    setEditable(true);
  }

  public void raisePriceOfSelected(BigDecimal increase) throws DatabaseException {
    if (selectionModel().isSelectionNotEmpty()) {
      Collection<Long> trackIds = Entity.get(Track.ID, selectionModel().getSelectedItems());
      List<Entity> result = connectionProvider().connection()
              .executeFunction(Track.RAISE_PRICE, new RaisePriceParameters(trackIds, increase));
      replaceEntities(result);
    }
  }
}
