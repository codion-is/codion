/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.petstore.beans.TagItemModel;
import org.jminor.framework.demos.petstore.model.Petstore;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;

/**
 * User: Bj�rn Darri
 * Date: 24.12.2007
 * Time: 14:05:58
 */
public class TagPanel extends EntityPanel {

  /** {@inheritDoc} */
  protected List<EntityPanelInfo> getDetailPanelInfo() {
    return Arrays.asList(new EntityPanelInfo(TagItemModel.class, TagItemPanel.class));
  }

  /** {@inheritDoc} */
  protected JPanel initializePropertyPanel() {
    final JPanel ret = new JPanel(new FlexibleGridLayout(1,1,5,5));
    final JTextField txt = createTextField(Petstore.TAG_TAG);
    setDefaultFocusComponent(txt);
    txt.setColumns(16);
    ret.add(getControlPanel(Petstore.TAG_TAG, txt));

    return ret;
  }
}