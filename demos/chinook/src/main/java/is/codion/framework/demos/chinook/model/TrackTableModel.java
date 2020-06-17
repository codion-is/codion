/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Functions;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.math.BigDecimal;
import java.util.List;

import static is.codion.framework.demos.chinook.domain.Chinook.Track;

public class TrackTableModel extends SwingEntityTableModel {

  public TrackTableModel(final EntityConnectionProvider connectionProvider) {
    super(Track.TYPE, connectionProvider);
    setEditable(true);
  }

  public void raisePriceOfSelected(final BigDecimal increase) throws DatabaseException {
    List<Long> trackIds = Entities.getValues(Track.ID,
            getSelectionModel().getSelectedItems());

    List<Entity> result = getConnectionProvider().getConnection()
            .executeFunction(Functions.RAISE_PRICE, trackIds, increase);
    replaceEntities(result);
  }
}
