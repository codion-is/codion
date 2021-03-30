package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.item.Item;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityInputComponents.IncludeCaption;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import static org.junit.jupiter.api.Assertions.*;

public class EntityInputComponentsTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final EntityEditModel editModel = new SwingEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);
  private final EntityInputComponents inputComponents = new EntityInputComponents(editModel.getEntityDefinition());

  @Test
  public void createLabel() {
    final JLabel label = inputComponents.createLabel(TestDomain.DETAIL_STRING);
    assertEquals(DOMAIN.getEntities().getDefinition(TestDomain.T_DETAIL).getProperty(TestDomain.DETAIL_STRING).getCaption(), label.getText());
  }

  @Test
  public void createNullableCheckBoxNonNullableBooleanProperty() {
    assertThrows(IllegalArgumentException.class, () ->
            inputComponents.createNullableCheckBox(TestDomain.DETAIL_BOOLEAN,
                    editModel.value(TestDomain.DETAIL_BOOLEAN), null, IncludeCaption.YES));
  }

  @Test
  public void createCheckBox() {
    editModel.setDefaultValues();
    final JCheckBox box = inputComponents.createCheckBox(TestDomain.DETAIL_BOOLEAN, editModel.value(TestDomain.DETAIL_BOOLEAN));
    assertTrue(box.isSelected());//default value is true
    assertTrue(editModel.get(TestDomain.DETAIL_BOOLEAN));

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(editModel.get(TestDomain.DETAIL_BOOLEAN));

    editModel.put(TestDomain.DETAIL_BOOLEAN, true);
    assertTrue(box.isSelected());
  }

  @Test
  public void createNullableCheckBox() {
    editModel.setDefaultValues();
    final NullableCheckBox box = inputComponents.createNullableCheckBox(TestDomain.DETAIL_BOOLEAN_NULLABLE, editModel.value(TestDomain.DETAIL_BOOLEAN_NULLABLE), null, IncludeCaption.NO);
    assertTrue(box.isSelected());//default value is true
    assertTrue(editModel.get(TestDomain.DETAIL_BOOLEAN_NULLABLE));

    box.getMouseListeners()[1].mouseClicked(null);

    assertNull(box.getState());
    assertNull(editModel.get(TestDomain.DETAIL_BOOLEAN_NULLABLE));

    editModel.put(TestDomain.DETAIL_BOOLEAN_NULLABLE, false);
    assertFalse(box.isSelected());
  }

  @Test
  public void createBooleanComboBox() {
    editModel.setDefaultValues();
    editModel.put(TestDomain.DETAIL_BOOLEAN, true);
    final BooleanComboBoxModel boxModel = (BooleanComboBoxModel)
            inputComponents.createBooleanComboBox(TestDomain.DETAIL_BOOLEAN, editModel.value(TestDomain.DETAIL_BOOLEAN)).getModel();
    assertTrue(boxModel.getSelectedValue().getValue());
    boxModel.setSelectedItem(null);
    assertNull(editModel.get(TestDomain.DETAIL_BOOLEAN));

    editModel.put(TestDomain.DETAIL_BOOLEAN, false);
    assertFalse(boxModel.getSelectedValue().getValue());
  }

  @Test
  public void createValueListComboBox() {
    final JComboBox<Item<Integer>> box = inputComponents.createValueListComboBox(TestDomain.DETAIL_INT_VALUE_LIST,
            editModel.value(TestDomain.DETAIL_INT_VALUE_LIST));

    assertNull(editModel.get(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(1);
    assertEquals(1, editModel.get(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(2);
    assertEquals(2, editModel.get(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(3);
    assertEquals(3, editModel.get(TestDomain.DETAIL_INT_VALUE_LIST));
    box.setSelectedItem(4);//does not exist
    assertEquals(3, editModel.get(TestDomain.DETAIL_INT_VALUE_LIST));
  }

  @Test
  public void createComboBox() {
    final DefaultComboBoxModel<Integer> boxModel = new DefaultComboBoxModel<>(new Integer[] {0, 1, 2, 3});
    final JComboBox<Integer> box = inputComponents.createComboBox(TestDomain.DETAIL_INT, editModel.value(TestDomain.DETAIL_INT), boxModel);

    assertNull(editModel.get(TestDomain.DETAIL_INT));
    box.setSelectedItem(1);
    assertEquals(1, editModel.get(TestDomain.DETAIL_INT));
    box.setSelectedItem(2);
    assertEquals(2, editModel.get(TestDomain.DETAIL_INT));
    box.setSelectedItem(3);
    assertEquals(3, editModel.get(TestDomain.DETAIL_INT));
    box.setSelectedItem(4);//does not exist
    assertEquals(3, editModel.get(TestDomain.DETAIL_INT));
  }

  @Test
  public void valueListComboBox() {
    final Value<Integer> value = Value.value();
    final SteppedComboBox<Item<Integer>> comboBox = inputComponents.createValueListComboBox(TestDomain.DETAIL_INT_VALUE_LIST, value);
    final ItemComboBoxModel<Integer> model = (ItemComboBoxModel<Integer>) comboBox.getModel();
    assertEquals(0, model.indexOf(null));
    assertTrue(model.containsItem(Item.item(null)));
  }
}
