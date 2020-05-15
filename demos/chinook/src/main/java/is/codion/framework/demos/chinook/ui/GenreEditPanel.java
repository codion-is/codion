/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.ui;

import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;

import static dev.codion.framework.demos.chinook.domain.Chinook.GENRE_NAME;
import static dev.codion.swing.common.ui.layout.Layouts.gridLayout;

public class GenreEditPanel extends EntityEditPanel {

  public GenreEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(GENRE_NAME);

    createTextField(GENRE_NAME).setColumns(12);

    setLayout(gridLayout(1, 1));
    addPropertyPanel(GENRE_NAME);
  }
}