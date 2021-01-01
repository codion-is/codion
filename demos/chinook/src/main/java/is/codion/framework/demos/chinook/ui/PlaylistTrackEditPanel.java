/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityLookupField;

import static is.codion.swing.common.ui.Components.setPreferredHeight;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;

public class PlaylistTrackEditPanel extends EntityEditPanel {

  public PlaylistTrackEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(PlaylistTrack.PLAYLIST_FK);

    final EntityComboBox playlistComboBox = createForeignKeyComboBox(PlaylistTrack.PLAYLIST_FK);
    setPreferredHeight(playlistComboBox, getPreferredTextFieldHeight());
    final EntityLookupField trackLookupField = createForeignKeyLookupField(PlaylistTrack.TRACK_FK);
    trackLookupField.setSelectionProvider(new TrackSelectionProvider(trackLookupField.getModel()));
    trackLookupField.setColumns(30);

    setLayout(gridLayout(2, 1));
    addInputPanel(PlaylistTrack.PLAYLIST_FK);
    addInputPanel(PlaylistTrack.TRACK_FK);
  }
}