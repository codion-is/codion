/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.PLAYLIST_NAME;
import static org.jminor.framework.demos.chinook.domain.Chinook.T_PLAYLISTTRACK;

import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class PlaylistPanel extends EntityPanel {

  public PlaylistPanel(final EntityModel model) {
    super(model, new PlaylistEditPanel(model.getEditModel()));
    addDetailPanel(new PlaylistTrackPanel(model.getDetailModel(T_PLAYLISTTRACK)));
  }

  static class PlaylistEditPanel extends EntityEditPanel {

    PlaylistEditPanel(final EntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      setLayout(new GridLayout(1, 1, 5, 5));
      final JTextField txtName = createTextField(PLAYLIST_NAME);
      txtName.setColumns(12);
      setInitialFocusComponent(txtName);
      add(createPropertyPanel(PLAYLIST_NAME, txtName));
    }
  }
}