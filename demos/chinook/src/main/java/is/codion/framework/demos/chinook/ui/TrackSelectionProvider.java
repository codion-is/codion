/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.model.EntityLookupModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityLookupField;

import java.awt.Dimension;

import static is.codion.common.model.table.SortingDirective.ASCENDING;
import static is.codion.framework.demos.chinook.domain.Chinook.Track;

final class TrackSelectionProvider extends EntityLookupField.TableSelectionProvider {

  TrackSelectionProvider(final EntityLookupModel trackLookupModel) {
    super(trackLookupModel);
    final SwingEntityTableModel tableModel = getTable().getModel();
    tableModel.setColumns(Track.ARTIST_DENORM, Track.ALBUM_FK, Track.NAME);
    tableModel.setSortingDirective(Track.ARTIST_DENORM, ASCENDING);
    tableModel.addSortingDirective(Track.ALBUM_FK, ASCENDING);
    tableModel.addSortingDirective(Track.NAME, ASCENDING);
    setPreferredSize(new Dimension(500, 300));
  }
}
