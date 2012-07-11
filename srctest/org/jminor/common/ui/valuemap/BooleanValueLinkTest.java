/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ValueMapEditModel;
import org.jminor.framework.client.model.DefaultEntityModel;
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
    final ValueMapEditModel<String, Object> model = new DefaultEntityModel(Petstore.T_ITEM, EntityConnectionImplTest.CONNECTION_PROVIDER).getEditModel();
    final JCheckBox chkBox = new JCheckBox();
    final ButtonModel buttonModel = chkBox.getModel();
    new BooleanValueLink<String>(buttonModel, model, Petstore.ITEM_DISABLED);
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
