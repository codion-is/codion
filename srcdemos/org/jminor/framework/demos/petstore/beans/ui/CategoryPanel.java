/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.petstore.beans.ProductModel;
import org.jminor.framework.demos.petstore.domain.Petstore;

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

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(ProductModel.class, ProductPanel.class));
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new FlexibleGridLayout(2,2,5,5));
        final JTextField txtName = createTextField(Petstore.CATEGORY_NAME);
        setDefaultFocusComponent(txtName);
        txtName.setColumns(10);
        add(createPropertyPanel(Petstore.CATEGORY_NAME, txtName));
        final JTextField txtDesc = createTextField(Petstore.CATEGORY_DESCRIPTION);
        txtDesc.setColumns(18);
        add(createPropertyPanel(Petstore.CATEGORY_DESCRIPTION, txtDesc));
        add(createPropertyPanel(Petstore.CATEGORY_IMAGE_URL, createTextField(Petstore.CATEGORY_IMAGE_URL)));
      }
    };
  }

  @Override
  protected double getDetailSplitPaneResizeWeight() {
    return 0.2;
  }
}
