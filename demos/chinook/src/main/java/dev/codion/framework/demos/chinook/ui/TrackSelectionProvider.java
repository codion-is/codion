/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.ui;

import dev.codion.framework.model.EntityLookupModel;
import dev.codion.swing.framework.model.SwingEntityTableModel;
import dev.codion.swing.framework.ui.EntityLookupField;

import java.awt.Dimension;

import static dev.codion.common.model.table.SortingDirective.ASCENDING;
import static dev.codion.framework.demos.chinook.domain.Chinook.*;

final class TrackSelectionProvider extends EntityLookupField.TableSelectionProvider {

  TrackSelectionProvider(final EntityLookupModel trackLookupModel) {
    super(trackLookupModel);
    final SwingEntityTableModel tableModel = getTable().getModel();
    tableModel.setColumns(TRACK_ARTIST_DENORM, TRACK_ALBUM_FK, TRACK_NAME);
    tableModel.setSortingDirective(TRACK_ARTIST_DENORM, ASCENDING);
    tableModel.addSortingDirective(TRACK_ALBUM_FK, ASCENDING);
    tableModel.addSortingDirective(TRACK_NAME, ASCENDING);
    setPreferredSize(new Dimension(500, 300));
  }
}
