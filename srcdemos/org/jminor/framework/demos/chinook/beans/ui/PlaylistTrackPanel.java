/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.chinook.domain.Chinook;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class PlaylistTrackPanel extends EntityPanel {

  public PlaylistTrackPanel(final EntityModel model) {
    super(model, "Playlist tracks");
  }

  @Override
  protected EntityEditPanel initializeEditPanel(EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new GridLayout(2, 1, 5, 5));
        final JComboBox boxPlaylist = createEntityComboBox(Chinook.PLAYLISTTRACK_PLAYLISTID_FK);
        setDefaultFocusComponent(boxPlaylist);
        final JTextField txtTrack = createEntityLookupField(Chinook.PLAYLISTTRACK_TRACKID_FK);
        txtTrack.setColumns(35);
        add(createPropertyPanel(Chinook.PLAYLISTTRACK_PLAYLISTID_FK, boxPlaylist));
        add(createPropertyPanel(Chinook.PLAYLISTTRACK_TRACKID_FK, txtTrack));
      }
    };
  }
}