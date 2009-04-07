/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.petstore.beans.ItemModel;
import org.jminor.framework.demos.petstore.model.Petstore;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 14:05:58
 */
public class ProductPanel extends EntityPanel {

  /** {@inheritDoc} */
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(ItemModel.class, ItemPanel.class));
  }

  /** {@inheritDoc} */
  protected JPanel initializePropertyPanel() {
    final JPanel ret = new JPanel(new FlexibleGridLayout(3,1,5,5));
    final EntityComboBox box = createEntityComboBox(Petstore.PRODUCT_CATEGORY_REF);
    setDefaultFocusComponent(box);
    ret.add(createControlPanel(Petstore.PRODUCT_CATEGORY_REF, box));
    ret.add(createControlPanel(Petstore.PRODUCT_NAME, createTextField(Petstore.PRODUCT_NAME)));
    final JTextField txt = createTextField(Petstore.PRODUCT_DESCRIPTION);
    txt.setColumns(16);
    ret.add(createControlPanel(Petstore.PRODUCT_DESCRIPTION, txt));

    return ret;
  }
}