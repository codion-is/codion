package org.jminor.framework.client.ui.property;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.petstore.beans.ItemModel;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import junit.framework.TestCase;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;

public class BooleanPropertyLinkTest extends TestCase {

  private EntityEditModel model;

  public BooleanPropertyLinkTest() {
    model = new ItemModel(EntityDbConnectionTest.dbProvider).getEditModel();
  }

  public void test() throws Exception {
    final Property disabledProperty = EntityRepository.getProperty(Petstore.T_ITEM, Petstore.ITEM_DISABLED);
    final JCheckBox chkBox = new JCheckBox();
    final ButtonModel buttonModel = chkBox.getModel();
    new BooleanPropertyLink(buttonModel, model, disabledProperty);
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
