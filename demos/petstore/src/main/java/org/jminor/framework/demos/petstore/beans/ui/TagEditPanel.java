/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JTextField;

import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_TAG;

public class TagEditPanel extends EntityEditPanel {

  public TagEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(1,1,5,5));
    final JTextField txt = createTextField(TAG_TAG);
    setInitialFocusComponent(txt);
    txt.setColumns(16);
    addPropertyPanel(TAG_TAG);
  }
}