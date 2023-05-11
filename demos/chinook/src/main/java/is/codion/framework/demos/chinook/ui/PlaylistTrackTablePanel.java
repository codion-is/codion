/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntitySearchField;
import is.codion.swing.framework.ui.EntityTablePanel;

public final class PlaylistTrackTablePanel extends EntityTablePanel {

  public PlaylistTrackTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    configureTrackConditionPanel();
    setUpdateSelectedComponentFactory(PlaylistTrack.TRACK_FK, new TrackComponentFactory());
  }

  private void configureTrackConditionPanel() {
    EntitySearchField equalField = (EntitySearchField) conditionPanel().conditionPanel(PlaylistTrack.TRACK_FK).equalField();
    equalField.setSelectionProvider(new TrackSelectionProvider(equalField.model()));
  }
}
