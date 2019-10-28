/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.PLAYLISTTRACK_PLAYLIST_FK;
import static org.jminor.framework.demos.chinook.domain.Chinook.PLAYLISTTRACK_TRACK_FK;

public class PlaylistTrackEditPanel extends EntityEditPanel {

  public PlaylistTrackEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(PLAYLISTTRACK_PLAYLIST_FK);

    createForeignKeyComboBox(PLAYLISTTRACK_PLAYLIST_FK);
    createForeignKeyLookupField(PLAYLISTTRACK_TRACK_FK).setColumns(30);

    setLayout(new GridLayout(2, 1, 5, 5));
    addPropertyPanel(PLAYLISTTRACK_PLAYLIST_FK);
    addPropertyPanel(PLAYLISTTRACK_TRACK_FK);
  }
}