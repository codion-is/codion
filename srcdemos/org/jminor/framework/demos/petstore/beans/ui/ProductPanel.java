/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.petstore.beans.ItemModel;
import org.jminor.framework.demos.petstore.domain.Petstore;

import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 14:05:58
 */
public class ProductPanel extends EntityPanel {

  public ProductPanel(final EntityModel model) {
    super(model, "Product", true, false, false, EMBEDDED, true);
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(ItemModel.class, ItemPanel.class));
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new FlexibleGridLayout(3,1,5,5));
        final EntityComboBox box = createEntityComboBox(Petstore.PRODUCT_CATEGORY_FK);
        setDefaultFocusComponent(box);
        add(createPropertyPanel(Petstore.PRODUCT_CATEGORY_FK, box));
        add(createPropertyPanel(Petstore.PRODUCT_NAME, createTextField(Petstore.PRODUCT_NAME)));
        final JTextField txt = createTextField(Petstore.PRODUCT_DESCRIPTION);
        txt.setColumns(16);
        add(createPropertyPanel(Petstore.PRODUCT_DESCRIPTION, txt));
      }
    };
  }

  @Override
  protected double getDetailSplitPaneResizeWeight() {
    return 0.3;
  }
}