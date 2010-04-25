/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.model.ChangeValueMapEditModel;
import org.jminor.common.ui.ChangeValueMapEditPanel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.PLAYLISTTRACK_PLAYLISTID_FK;
import static org.jminor.framework.demos.chinook.domain.Chinook.PLAYLISTTRACK_TRACKID_FK;

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
  protected ChangeValueMapEditPanel initializeEditPanel(final ChangeValueMapEditModel editModel) {
    return new EntityEditPanel((EntityEditModel) editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new GridLayout(2, 1, 5, 5));
        final JComboBox boxPlaylist = createEntityComboBox(PLAYLISTTRACK_PLAYLISTID_FK);
        setDefaultFocusComponent(boxPlaylist);
        final JTextField txtTrack = createEntityLookupField(PLAYLISTTRACK_TRACKID_FK);
        txtTrack.setColumns(30);
        add(createPropertyPanel(PLAYLISTTRACK_PLAYLISTID_FK, boxPlaylist));
        add(createPropertyPanel(PLAYLISTTRACK_TRACKID_FK, txtTrack));
      }
    };
  }
}