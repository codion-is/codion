/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.swing.framework.model.EntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JTextField;
import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.GENRE_NAME;

public class GenreEditPanel extends EntityEditPanel {

  public GenreEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(GENRE_NAME);
    final JTextField txtName = createTextField(GENRE_NAME);
    txtName.setColumns(12);

    setLayout(new GridLayout(1, 1, 5, 5));
    addPropertyPanel(GENRE_NAME);
  }
}