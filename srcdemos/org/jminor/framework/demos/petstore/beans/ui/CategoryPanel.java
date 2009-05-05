/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.petstore.beans.ProductModel;
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
public class CategoryPanel extends EntityPanel {

  public CategoryPanel(final EntityModel model) {
    super(model);
  }

  /** {@inheritDoc} */
  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(ProductModel.class, ProductPanel.class));
  }

  /** {@inheritDoc} */
  @Override
  protected JPanel initializePropertyPanel() {
    final JPanel ret = new JPanel(new FlexibleGridLayout(2,2,5,5));
    JTextField txt = createTextField(Petstore.CATEGORY_ID);
    setDefaultFocusComponent(txt);
    txt.setColumns(10);
    ret.add(createControlPanel(Petstore.CATEGORY_ID, txt));
    txt = createTextField(Petstore.CATEGORY_NAME);
    txt.setColumns(10);
    ret.add(createControlPanel(Petstore.CATEGORY_NAME, txt));
    ret.add(createControlPanel(Petstore.CATEGORY_DESCRIPTION, createTextField(Petstore.CATEGORY_DESCRIPTION)));
    ret.add(createControlPanel(Petstore.CATEGORY_IMAGE_URL, createTextField(Petstore.CATEGORY_IMAGE_URL)));

    return ret;
  }

  @Override
  protected double getDetailSplitPaneResizeWeight() {
    return 0.2;
  }
}
