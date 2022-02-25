/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntitySearchField;

import java.awt.Dimension;

import static is.codion.framework.demos.chinook.domain.Chinook.Track;
import static javax.swing.SortOrder.ASCENDING;

final class TrackSelectionProvider extends EntitySearchField.TableSelectionProvider {

  TrackSelectionProvider(EntitySearchModel trackSearchModel) {
    super(trackSearchModel);
    SwingEntityTableModel tableModel = getTable().getModel();
    tableModel.getColumnModel().setColumns(Track.ARTIST_DENORM, Track.ALBUM_FK, Track.NAME);
    tableModel.getSortModel().setSortOrder(Track.ARTIST_DENORM, ASCENDING);
    tableModel.getSortModel().addSortOrder(Track.ALBUM_FK, ASCENDING);
    tableModel.getSortModel().addSortOrder(Track.NAME, ASCENDING);
    setPreferredSize(new Dimension(500, 300));
  }
}
