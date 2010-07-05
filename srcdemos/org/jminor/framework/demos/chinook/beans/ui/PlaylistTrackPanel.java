/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.PLAYLISTTRACK_PLAYLISTID_FK;
import static org.jminor.framework.demos.chinook.domain.Chinook.PLAYLISTTRACK_TRACKID_FK;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.GridLayout;

public class PlaylistTrackPanel extends EntityEditPanel {

  public PlaylistTrackPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusComponentKey(PLAYLISTTRACK_PLAYLISTID_FK);
    final JComboBox boxPlaylist = createEntityComboBox(PLAYLISTTRACK_PLAYLISTID_FK);
    final JTextField txtTrack = createEntityLookupField(PLAYLISTTRACK_TRACKID_FK);
    txtTrack.setColumns(30);

    setLayout(new GridLayout(2, 1, 5, 5));
    add(createPropertyPanel(PLAYLISTTRACK_PLAYLISTID_FK, boxPlaylist));
    add(createPropertyPanel(PLAYLISTTRACK_TRACKID_FK, txtTrack));
  }
}