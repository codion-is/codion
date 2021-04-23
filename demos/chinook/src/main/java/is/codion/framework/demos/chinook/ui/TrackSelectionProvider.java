/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntitySearchField;

import java.awt.Dimension;

import static is.codion.common.model.table.SortingDirective.ASCENDING;
import static is.codion.framework.demos.chinook.domain.Chinook.Track;

final class TrackSelectionProvider extends EntitySearchField.TableSelectionProvider {

  TrackSelectionProvider(final EntitySearchModel trackSearchModel) {
    super(trackSearchModel);
    final SwingEntityTableModel tableModel = getTable().getModel();
    tableModel.getColumnModel().setColumns(Track.ARTIST_DENORM, Track.ALBUM_FK, Track.NAME);
    tableModel.getSortModel().setSortingDirective(Track.ARTIST_DENORM, ASCENDING);
    tableModel.getSortModel().addSortingDirective(Track.ALBUM_FK, ASCENDING);
    tableModel.getSortModel().addSortingDirective(Track.NAME, ASCENDING);
    setPreferredSize(new Dimension(500, 300));
  }
}
