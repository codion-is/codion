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
import org.jminor.framework.demos.petstore.beans.AddressModel;
import org.jminor.framework.demos.petstore.beans.ContactInfoModel;
import org.jminor.framework.demos.petstore.beans.TagItemModel;
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
public class ItemPanel extends EntityPanel {

  public ItemPanel(final EntityModel model) {
    super(model, "Item", true, false, false, HIDDEN);
  }

  /** {@inheritDoc} */
  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(TagItemModel.class, TagItemPanel.class));
  }

  /** {@inheritDoc} */
  @Override
  protected JPanel initializePropertyPanel() {
    final JPanel ret = new JPanel(new FlexibleGridLayout(3,3,5,5));
    EntityComboBox box = createEntityComboBox(Petstore.ITEM_PRODUCT_FK);
    setDefaultFocusComponent(box);
    ret.add(createControlPanel(Petstore.ITEM_PRODUCT_FK, box));
    JTextField txt = createTextField(Petstore.ITEM_NAME);
    txt.setColumns(12);
    ret.add(createControlPanel(Petstore.ITEM_NAME, txt));
    txt = createTextField(Petstore.ITEM_DESCRIPTION);
    txt.setColumns(16);
    ret.add(createControlPanel(Petstore.ITEM_DESCRIPTION, txt));
    ret.add(createControlPanel(Petstore.ITEM_PRICE, createTextField(Petstore.ITEM_PRICE)));
    box = createEntityComboBox(Petstore.ITEM_C0NTACT_INFO_FK,
            new EntityPanelProvider(ContactInfoModel.class, ContactInfoPanel.class), false);
    box.setPopupWidth(200);
    box.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    ret.add(createControlPanel(Petstore.ITEM_C0NTACT_INFO_FK, box.createPanel()));
    box = createEntityComboBox(Petstore.ITEM_ADDRESS_FK,
            new EntityPanelProvider(AddressModel.class, AddressPanel.class), false);
    box.setPopupWidth(200);
    box.setPreferredSize(UiUtil.getPreferredTextFieldSize());
    ret.add(createControlPanel(Petstore.ITEM_ADDRESS_FK, box.createPanel()));
    ret.add(createControlPanel(Petstore.ITEM_IMAGE_URL, createTextField(Petstore.ITEM_IMAGE_URL)));
    ret.add(createControlPanel(Petstore.ITEM_IMAGE_THUMB_URL, createTextField(Petstore.ITEM_IMAGE_THUMB_URL)));
    ret.add(createControlPanel(Petstore.ITEM_DISABLED, createCheckBox(Petstore.ITEM_DISABLED, null, false)));

    return ret;
  }
}