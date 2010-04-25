/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.petstore.beans.ItemModel;
import org.jminor.framework.demos.petstore.domain.Petstore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;

public class BooleanPropertyLinkTest {

  private ChangeValueMapEditModel<String, Object> model;

  public BooleanPropertyLinkTest() {
    model = new ItemModel(EntityDbConnectionTest.DB_PROVIDER).getEditModel();
  }

  @Test
  public void test() throws Exception {
    final JCheckBox chkBox = new JCheckBox();
    final ButtonModel buttonModel = chkBox.getModel();
    new BooleanPropertyLink(buttonModel, model, Petstore.ITEM_DISABLED);
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
