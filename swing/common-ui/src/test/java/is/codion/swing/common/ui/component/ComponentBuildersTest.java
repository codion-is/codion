/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TemporalInputPanel;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.UpdateOn;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class ComponentBuildersTest {

  @Test
  public void integerField() {
    final Value<Integer> value = Value.value(42);
    final ComponentValue<Integer, IntegerField> componentValue =
            ComponentBuilders.integerFieldBuilder().buildComponentValue();
    componentValue.link(value);
    assertEquals(componentValue.getComponent().getText(), "42");
  }

  @Test
  public void longField() {
    final Value<Long> value = Value.value(42L);
    final ComponentValue<Long, LongField> componentValue =
            ComponentBuilders.longFieldBuilder().buildComponentValue();
    componentValue.link(value);
    assertEquals(componentValue.getComponent().getText(), "42");
  }

  @Test
  public void doubleField() {
    final Value<Double> value = Value.value(42.2);
    final ComponentValue<Double, DoubleField> componentValue =
            ComponentBuilders.doubleFieldBuilder().buildComponentValue();
    componentValue.link(value);
    assertEquals(componentValue.getComponent().getNumber(), value.get());
  }

  @Test
  public void bigDecimalField() {
    final Value<BigDecimal> value = Value.value(BigDecimal.valueOf(42.2));
    final ComponentValue<BigDecimal, BigDecimalField> componentValue =
            ComponentBuilders.bigDecimalFieldBuilder().buildComponentValue();
    componentValue.link(value);
    assertEquals(componentValue.getComponent().getNumber(), value.get());
  }

  @Test
  public void localTimeField() {
    final Value<LocalTime> value = Value.value(LocalTime.now());
    final ComponentValue<LocalTime, TemporalField<LocalTime>> componentValue =
            ComponentBuilders.localTimeFieldBuilder("HH:mm").buildComponentValue();
    componentValue.link(value);
    assertEquals(componentValue.get(), value.get().truncatedTo(ChronoUnit.MINUTES));
  }

  @Test
  public void localDateField() {
    final Value<LocalDate> value = Value.value(LocalDate.now());
    final ComponentValue<LocalDate, TemporalField<LocalDate>> componentValue =
            ComponentBuilders.localDateFieldBuilder("dd-MM-yyyy").buildComponentValue();
    componentValue.link(value);
    assertEquals(componentValue.get(), value.get());
  }

  @Test
  public void localDateTimeField() {
    final Value<LocalDateTime> value = Value.value(LocalDateTime.now());
    final ComponentValue<LocalDateTime, TemporalField<LocalDateTime>> componentValue =
            ComponentBuilders.localDateTimeFieldBuilder("dd-MM-yyyy HH:mm").buildComponentValue();
    componentValue.link(value);
    assertEquals(componentValue.get(), value.get().truncatedTo(ChronoUnit.MINUTES));
  }

  @Test
  public void offsetDateTimeField() {
    final Value<OffsetDateTime> value = Value.value(OffsetDateTime.now());
    final ComponentValue<OffsetDateTime, TemporalField<OffsetDateTime>> componentValue =
            ComponentBuilders.offsetDateTimeFieldBuilder("yyyy-MM-dd'T'HH:mm:ss.SSSZ").buildComponentValue();
    componentValue.link(value);
//    assertEquals(componentValue.get(), value.get().truncatedTo(ChronoUnit.MINUTES));
  }

  @Test
  public void temporalInputPanel() {
    final Value<LocalDate> value = Value.value(LocalDate.now());
    final ComponentValue<LocalDate, TemporalInputPanel<LocalDate>> componentValue =
            ComponentBuilders.temporalInputPanelBuiler(LocalDate.class)
            .dateTimePattern("dd-MM-yyyy")
            .buildComponentValue();
    componentValue.link(value);
    assertEquals(componentValue.get(), value.get());
  }

  @Test
  public void checkBox() {
    final Value<Boolean> value = Value.value(true, false);
    final ComponentValue<Boolean, JCheckBox> componentValue = ComponentBuilders.checkBoxBuilder()
            .transferFocusOnEnter(true).buildComponentValue();
    componentValue.link(value);
    final JCheckBox box = componentValue.getComponent();
    assertTrue(box.isSelected());
    assertTrue(value.get());

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(value.get());

    value.set(true);
    assertTrue(box.isSelected());
  }

  @Test
  public void toggleButton() {
    final Value<Boolean> value = Value.value(true, false);
    final ComponentValue<Boolean, JToggleButton> componentValue = ComponentBuilders.toggleButtonBuilder()
            .transferFocusOnEnter(true).buildComponentValue();
    componentValue.link(value);
    final JToggleButton box = componentValue.getComponent();
    assertTrue(box.isSelected());
    assertTrue(value.get());

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(value.get());

    value.set(true);
    assertTrue(box.isSelected());
  }

  @Test
  public void nullableCheckBox() {
    final Value<Boolean> value = Value.value(true);
    final ComponentValue<Boolean, JCheckBox> componentValue = ComponentBuilders.checkBoxBuilder()
            .transferFocusOnEnter(true).nullable(true).buildComponentValue();
    componentValue.link(value);
    final NullableCheckBox box = (NullableCheckBox) componentValue.getComponent();
    assertTrue(box.isSelected());
    assertTrue(value.get());

    box.getMouseListeners()[1].mouseClicked(null);

    assertNull(box.getState());
    assertNull(value.get());

    value.set(false);
    assertFalse(box.isSelected());
  }

  @Test
  public void booleanComboBox() {
    final Value<Boolean> value = Value.value(true);
    final ComponentValue<Boolean, SteppedComboBox<Item<Boolean>>> componentValue =
            ComponentBuilders.booleanComboBoxBuilder(new BooleanComboBoxModel())
            .transferFocusOnEnter(true).buildComponentValue();
    componentValue.link(value);
    final BooleanComboBoxModel boxModel = (BooleanComboBoxModel)
            componentValue.getComponent().getModel();
    assertTrue(boxModel.getSelectedValue().getValue());
    boxModel.setSelectedItem(null);
    assertNull(value.get());

    value.set(false);
    assertFalse(boxModel.getSelectedValue().getValue());
  }

  @Test
  public void valueListComboBox() {
    final List<Item<Integer>> items = asList(item(0, "0"), item(1, "1"),
          item(2, "2"), item(3, "3"));
    final Value<Integer> value = Value.value();
    final ComponentValue<Integer, SteppedComboBox<Item<Integer>>> componentValue =
            ComponentBuilders.valueListComboBoxBuilder(items)
            .transferFocusOnEnter(true)
            .nullable(true).buildComponentValue();
    componentValue.link(value);
    final JComboBox<Item<Integer>> comboBox = componentValue.getComponent();
    final ItemComboBoxModel<Integer> model = (ItemComboBoxModel<Integer>) comboBox.getModel();
    assertEquals(0, model.indexOf(null));
    assertTrue(model.containsItem(Item.item(null)));

    assertNull(value.get());
    comboBox.setSelectedItem(1);
    assertEquals(1, value.get());
    comboBox.setSelectedItem(2);
    assertEquals(2, value.get());
    comboBox.setSelectedItem(3);
    assertEquals(3, value.get());
    comboBox.setSelectedItem(4);//does not exist
    assertEquals(3, value.get());
  }

  @Test
  public void comboBox() {
    final DefaultComboBoxModel<Integer> boxModel = new DefaultComboBoxModel<>(new Integer[] {0, 1, 2, 3});
    final Value<Integer> value = Value.value();
    final ComponentValue<Integer, SteppedComboBox<Integer>> componentValue =
            ComponentBuilders.comboBoxBuilder(Integer.class, boxModel)
            .transferFocusOnEnter(true).buildComponentValue();
    componentValue.link(value);
    final JComboBox<Integer> box = componentValue.getComponent();

    assertNull(value.get());
    box.setSelectedItem(1);
    assertEquals(1, value.get());
    box.setSelectedItem(2);
    assertEquals(2, value.get());
    box.setSelectedItem(3);
    assertEquals(3, value.get());
    box.setSelectedItem(4);//does not exist
    assertEquals(3, value.get());
  }

  @Test
  public void textField() {
    final Value<String> value = Value.value();
    final ComponentValue<String, JTextField> componentValue =
            ComponentBuilders.textFieldBuilder(String.class)
            .columns(10).upperCase().selectAllOnFocusGained().buildComponentValue();
    componentValue.link(value);
    final JTextField field = componentValue.getComponent();
    field.setText("hello");
    assertEquals("HELLO", value.get());
  }

  @Test
  public void textArea() {
    final Value<String> value = Value.value();
    final ComponentValue<String, JTextArea> componentValue = ComponentBuilders.textAreaBuilder()
            .transferFocusOnEnter(true).rows(4).columns(2).updateOn(UpdateOn.KEYSTROKE).lineWrap(true).wrapStyleWord(true)
            .buildComponentValue();
    componentValue.link(value);
    final JTextArea textArea = componentValue.getComponent();
    textArea.setText("hello");
    assertEquals("hello", value.get());
  }

  @Test
  public void textInputPanel() {
    final Value<String> value = Value.value();
    final ComponentValue<String, TextInputPanel> componentValue =
            ComponentBuilders.textInputPanelBuilder()
            .transferFocusOnEnter(true).columns(10).buttonFocusable(true).updateOn(UpdateOn.KEYSTROKE).buildComponentValue();
    componentValue.link(value);
    final TextInputPanel inputPanel = componentValue.getComponent();
    inputPanel.setText("hello");
    assertEquals("hello", value.get());
  }

  @Test
  public void formattedTextField() {
    final Value<String> value = Value.value();
    final ComponentValue<String, JFormattedTextField> componentValue =
            ComponentBuilders.formattedTextFieldBuilder()
            .formatMask("##:##").valueContainsLiterals(true).columns(6).updateOn(UpdateOn.KEYSTROKE)
            .focusLostBehaviour(JFormattedTextField.COMMIT).buildComponentValue();
    componentValue.link(value);
    final JFormattedTextField field = componentValue.getComponent();
    field.setText("1234");
    assertEquals("12:34", value.get());
  }
}
