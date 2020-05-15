/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.ui;

import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static dev.codion.framework.demos.chinook.domain.Chinook.*;
import static dev.codion.swing.common.ui.layout.Layouts.borderLayout;
import static dev.codion.swing.common.ui.layout.Layouts.gridLayout;

public class AlbumEditPanel extends EntityEditPanel {

  public AlbumEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(ALBUM_ARTIST_FK);

    createForeignKeyLookupField(ALBUM_ARTIST_FK).setColumns(18);
    createTextField(ALBUM_TITLE).setColumns(18);

    final JPanel inputPanel = new JPanel(gridLayout(2, 1));
    inputPanel.add(createPropertyPanel(ALBUM_ARTIST_FK));
    inputPanel.add(createPropertyPanel(ALBUM_TITLE));

    setLayout(borderLayout());

    final JPanel inputBasePanel = new JPanel(borderLayout());
    inputBasePanel.add(inputPanel, BorderLayout.NORTH);

    add(inputBasePanel, BorderLayout.WEST);
    add(new CoverArtPanel(getEditModel().value(ALBUM_COVER)), BorderLayout.CENTER);
  }
}
