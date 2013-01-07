/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.EditModelValue;
import org.jminor.common.ui.LinkType;
import org.jminor.common.ui.ValueLinks;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.petstore.domain.Petstore;

import org.junit.Test;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BooleanValueLinkTest {

  @Test
  public void test() throws Exception {
    Petstore.init();
    final EntityEditModel model = new DefaultEntityEditModel(Petstore.T_ITEM, EntityConnectionImplTest.CONNECTION_PROVIDER);
    final JCheckBox chkBox = new JCheckBox();
    final ButtonModel buttonModel = chkBox.getModel();
    ValueLinks.toggleValueLink(buttonModel, new EditModelValue<String, Boolean>(model, Petstore.ITEM_DISABLED), LinkType.READ_WRITE);
    assertFalse(buttonModel.isSelected());
    model.setValue(Petstore.ITEM_DISABLED, true);
    assertTrue(buttonModel.isSelected());
    chkBox.setSelected(false);
    assertFalse((Boolean) model.getValue(Petstore.ITEM_DISABLED));
    buttonModel.setSelected(true);
    assertTrue((Boolean) model.getValue(Petstore.ITEM_DISABLED));
    chkBox.doClick();
    assertFalse((Boolean) model.getValue(Petstore.ITEM_DISABLED));
    chkBox.doClick();
    assertTrue((Boolean) model.getValue(Petstore.ITEM_DISABLED));
    model.setValue(Petstore.ITEM_DISABLED, null);
    assertFalse(chkBox.isSelected());
  }
}
