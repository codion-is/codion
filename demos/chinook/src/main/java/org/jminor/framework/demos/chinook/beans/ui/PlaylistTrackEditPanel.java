/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JTextField;
import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.PLAYLISTTRACK_PLAYLISTID_FK;
import static org.jminor.framework.demos.chinook.domain.Chinook.PLAYLISTTRACK_TRACKID_FK;

public class PlaylistTrackEditPanel extends EntityEditPanel {

  public PlaylistTrackEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(PLAYLISTTRACK_PLAYLISTID_FK);
    createForeignKeyComboBox(PLAYLISTTRACK_PLAYLISTID_FK);
    final JTextField txtTrack = createForeignKeyLookupField(PLAYLISTTRACK_TRACKID_FK);
    txtTrack.setColumns(30);

    setLayout(new GridLayout(2, 1, 5, 5));
    addPropertyPanel(PLAYLISTTRACK_PLAYLISTID_FK);
    addPropertyPanel(PLAYLISTTRACK_TRACKID_FK);
  }
}