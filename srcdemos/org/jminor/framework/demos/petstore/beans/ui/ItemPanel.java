/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.client.ui.EntityUiUtil;
import org.jminor.framework.demos.petstore.beans.AddressModel;
import org.jminor.framework.demos.petstore.beans.ContactInfoModel;
import org.jminor.framework.demos.petstore.beans.TagItemModel;
import org.jminor.framework.demos.petstore.domain.Petstore;

import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 14:05:58
 */
public class ItemPanel extends EntityPanel {

  public ItemPanel(final EntityModel model) {
    super(model, "Item", true, false, false, HIDDEN);
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(TagItemModel.class, TagItemPanel.class));
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new FlexibleGridLayout(3,3,5,5));
        EntityComboBox box = createEntityComboBox(Petstore.ITEM_PRODUCT_FK);
        setDefaultFocusComponent(box);
        add(createPropertyPanel(Petstore.ITEM_PRODUCT_FK, box));
        JTextField txt = createTextField(Petstore.ITEM_NAME);
        txt.setColumns(12);
        add(createPropertyPanel(Petstore.ITEM_NAME, txt));
        txt = createTextField(Petstore.ITEM_DESCRIPTION);
        txt.setColumns(16);
        add(createPropertyPanel(Petstore.ITEM_DESCRIPTION, txt));
        add(createPropertyPanel(Petstore.ITEM_PRICE, createTextField(Petstore.ITEM_PRICE)));
        box = createEntityComboBox(Petstore.ITEM_C0NTACT_INFO_FK);
        box.setPopupWidth(200);
        box.setPreferredSize(UiUtil.getPreferredTextFieldSize());
        add(createPropertyPanel(Petstore.ITEM_C0NTACT_INFO_FK, EntityUiUtil.createEntityComboBoxNewRecordPanel(box,
                new EntityPanelProvider(ContactInfoModel.class, ContactInfoPanel.class), false)));
        box = createEntityComboBox(Petstore.ITEM_ADDRESS_FK);
        box.setPopupWidth(200);
        box.setPreferredSize(UiUtil.getPreferredTextFieldSize());
        add(createPropertyPanel(Petstore.ITEM_ADDRESS_FK, EntityUiUtil.createEntityComboBoxNewRecordPanel(box,
                new EntityPanelProvider(AddressModel.class, AddressPanel.class), false)));
        add(createPropertyPanel(Petstore.ITEM_IMAGE_URL, createTextField(Petstore.ITEM_IMAGE_URL)));
        add(createPropertyPanel(Petstore.ITEM_IMAGE_THUMB_URL, createTextField(Petstore.ITEM_IMAGE_THUMB_URL)));
        add(createPropertyPanel(Petstore.ITEM_DISABLED, createCheckBox(Petstore.ITEM_DISABLED, null, false)));
      }
    };
  }
}