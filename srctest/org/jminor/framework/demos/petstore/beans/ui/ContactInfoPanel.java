/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelInfo;
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
public class ContactInfoPanel extends EntityPanel {

  /** {@inheritDoc} */
  protected List<EntityPanelInfo> getDetailPanelInfo() {
    return Arrays.asList(new EntityPanelInfo(ItemModel.class, ItemPanel.class));
  }

  /** {@inheritDoc} */
  protected JPanel initializePropertyPanel() {
    final JPanel ret = new JPanel(new FlexibleGridLayout(3,1,5,5));
    JTextField txt = createTextField(Petstore.SELLER_CONTACT_INFO_LAST_NAME);
    setDefaultFocusComponent(txt);
    txt.setColumns(10);
    ret.add(getControlPanel(Petstore.SELLER_CONTACT_INFO_LAST_NAME, txt));
    ret.add(getControlPanel(Petstore.SELLER_CONTACT_INFO_FIRST_NAME, createTextField(Petstore.SELLER_CONTACT_INFO_FIRST_NAME)));
    ret.add(getControlPanel(Petstore.SELLER_CONTACT_INFO_EMAIL, createTextField(Petstore.SELLER_CONTACT_INFO_EMAIL)));

    return ret;
  }
}