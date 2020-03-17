/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class AlbumEditPanel extends EntityEditPanel {

  public AlbumEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(ALBUM_ARTIST_FK);

    createForeignKeyLookupField(ALBUM_ARTIST_FK).setColumns(18);
    createTextField(ALBUM_TITLE).setColumns(18);

    final JPanel inputPanel = new JPanel(new GridLayout(2, 1, 5, 5));
    inputPanel.add(createPropertyPanel(ALBUM_ARTIST_FK));
    inputPanel.add(createPropertyPanel(ALBUM_TITLE));

    setLayout(new BorderLayout(5, 5));

    final JPanel inputBasePanel = new JPanel(new BorderLayout(5, 5));
    inputBasePanel.add(inputPanel, BorderLayout.NORTH);

    final CoverArtPanel coverArtPanel = new CoverArtPanel(getEditModel().value(ALBUM_COVER));

    add(inputBasePanel, BorderLayout.WEST);
    add(coverArtPanel, BorderLayout.CENTER);
  }
}
