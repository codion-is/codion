/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_TAG;

import javax.swing.JTextField;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 14:05:58
 */
public class TagPanel extends EntityEditPanel {

  public TagPanel(final EntityEditModel model) {
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