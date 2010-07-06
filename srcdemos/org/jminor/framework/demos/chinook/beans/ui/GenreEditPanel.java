/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.GENRE_NAME;

import javax.swing.JTextField;
import java.awt.GridLayout;

public class GenreEditPanel extends EntityEditPanel {

  public GenreEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusComponentKey(GENRE_NAME);
    final JTextField txtName = createTextField(GENRE_NAME);
    txtName.setColumns(12);

    setLayout(new GridLayout(1, 1, 5, 5));
    add(createPropertyPanel(GENRE_NAME, txtName));
  }
}