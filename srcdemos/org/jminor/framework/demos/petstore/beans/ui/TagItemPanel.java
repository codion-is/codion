/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.petstore.beans.TagModel;
import org.jminor.framework.demos.petstore.model.Petstore;

import javax.swing.JPanel;
import java.awt.Dimension;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 23:36:04
 */
public class TagItemPanel extends EntityPanel {

  public TagItemPanel(final EntityModel model) {
    super(model);
  }

  /** {@inheritDoc} */
  protected JPanel initializePropertyPanel() {
    final JPanel ret = new JPanel(new FlexibleGridLayout(2,1,5,5));
    EntityComboBox box = createEntityComboBox(Petstore.TAG_ITEM_ITEM_REF);
    setDefaultFocusComponent(box);
    box.setPopupWidth(240);
    box.setPreferredSize(new Dimension(180, UiUtil.getPreferredTextFieldHeight()));
    ret.add(createControlPanel(Petstore.TAG_ITEM_ITEM_REF, box));
    box = createEntityComboBox(Petstore.TAG_ITEM_TAG_REF,
            new EntityPanelProvider("Tags", TagModel.class, TagPanel.class), false);
    ret.add(createControlPanel(Petstore.TAG_ITEM_TAG_REF, box.createPanel()));

    return ret;
  }
}
