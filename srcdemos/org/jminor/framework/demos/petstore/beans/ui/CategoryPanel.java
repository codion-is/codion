/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.petstore.beans.ProductModel;
import org.jminor.framework.demos.petstore.domain.Petstore;

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
    super(model, "Category");
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
    final JTextField txtName = createTextField(Petstore.CATEGORY_NAME);
    setDefaultFocusComponent(txtName);
    txtName.setColumns(10);
    ret.add(createControlPanel(Petstore.CATEGORY_NAME, txtName));
    final JTextField txtDesc = createTextField(Petstore.CATEGORY_DESCRIPTION);
    txtDesc.setColumns(18);
    ret.add(createControlPanel(Petstore.CATEGORY_DESCRIPTION, txtDesc));
    ret.add(createControlPanel(Petstore.CATEGORY_IMAGE_URL, createTextField(Petstore.CATEGORY_IMAGE_URL)));

    return ret;
  }

  @Override
  protected double getDetailSplitPaneResizeWeight() {
    return 0.2;
  }
}
