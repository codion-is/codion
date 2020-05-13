/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.framework.model.EntityLookupModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityLookupField;

import java.awt.Dimension;

import static org.jminor.common.model.table.SortingDirective.ASCENDING;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

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
