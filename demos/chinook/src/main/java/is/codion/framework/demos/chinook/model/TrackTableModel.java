/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Track.RaisePriceParameters;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.math.BigDecimal;
import java.util.Collection;

import static is.codion.framework.demos.chinook.domain.Chinook.Track;

public final class TrackTableModel extends SwingEntityTableModel {

  public TrackTableModel(EntityConnectionProvider connectionProvider) {
    super(Track.TYPE, connectionProvider);
    editable().set(true);
  }

  public void raisePriceOfSelected(BigDecimal increase) throws DatabaseException {
    if (selectionModel().selectionNotEmpty().get()) {
      Collection<Long> trackIds = Entity.values(Track.ID, selectionModel().getSelectedItems());
      Collection<Entity> result = connectionProvider().connection()
              .execute(Track.RAISE_PRICE, new RaisePriceParameters(trackIds, increase));
      replace(result);
    }
  }
}
