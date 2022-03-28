/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.component.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.textfield.BigDecimalField;
import is.codion.swing.common.ui.component.textfield.DoubleField;
import is.codion.swing.common.ui.component.textfield.IntegerField;
import is.codion.swing.common.ui.component.textfield.LongField;
import is.codion.swing.common.ui.component.textfield.TemporalField;
import is.codion.swing.common.ui.component.textfield.TemporalInputPanel;
import is.codion.swing.common.ui.component.textfield.TextInputPanel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.icons.Logos;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerListModel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class ComponentsTest {

  @Test
  void clear() {
    Font defaultFont = new JTextField().getFont();

    Value<Integer> value = Value.value(42);

    IntegerFieldBuilder builder = Components.integerField()
            .range(0, 100)
            .font(defaultFont.deriveFont(Font.BOLD))
            .foreground(Color.WHITE)
            .background(Color.BLACK)
            .linkedValue(value);

    IntegerField component = builder.build();
    ComponentValue<Integer, IntegerField> componentValue = builder.buildComponentValue();

    assertSame(component, componentValue.getComponent());

    builder.clear();

    IntegerField nextComponent = builder.build();
    ComponentValue<Integer, IntegerField> nextComponentValue = builder.buildComponentValue();

    assertSame(nextComponent, nextComponentValue.getComponent());

    assertNotSame(component, nextComponent);
    assertNotSame(componentValue, nextComponentValue);

    value.set(20);

    assertEquals(20, component.getInteger());
    assertEquals(20, nextComponent.getInteger());
    assertEquals(20, componentValue.get());
    assertEquals(20, nextComponentValue.get());

    nextComponentValue.set(25);

    assertEquals(25, component.getInteger());
    assertEquals(25, nextComponent.getInteger());
    assertEquals(25, componentValue.get());
    assertEquals(25, nextComponentValue.get());

    assertTrue(component.getFont().isBold());
    assertTrue(nextComponent.getFont().isBold());

    assertEquals(Color.WHITE, component.getForeground());
    assertEquals(Color.BLACK, component.getBackground());
  }

  @Test
  void integerField() {
    Value<Integer> value = Value.value(42);
    ComponentValue<Integer, IntegerField> componentValue = Components.integerField()
            .range(0, 100)
            .font(Font.getFont("arial"))
            .minimumHeight(10)
            .minimumWidth(10)
            .foreground(Color.WHITE)
            .background(Color.BLACK)
            .linkedValue(value)
            .buildComponentValue();
    assertEquals(componentValue.getComponent().getText(), "42");
  }

  @Test
  void longField() {
    Value<Long> value = Value.value(42L);
    ComponentValue<Long, LongField> componentValue = Components.longField()
            .range(0, 100)
            .groupingSeparator('.')
            .maximumHeight(10)
            .maximumWidth(10)
            .linkedValue(value)
            .buildComponentValue();
    assertEquals(componentValue.getComponent().getText(), "42");
  }

  @Test
  void doubleField() {
    Value<Double> value = Value.value(42.2);
    ComponentValue<Double, DoubleField> componentValue = Components.doubleField()
            .range(0, 100)
            .maximumFractionDigits(2)
            .groupingSeparator('.')
            .decimalSeparator(',')
            .minimumSize(new Dimension(10, 10))
            .maximumSize(new Dimension(10, 10))
            .linkedValue(value)
            .buildComponentValue();
    assertEquals(componentValue.getComponent().getNumber(), value.get());
  }

  @Test
  void bigDecimalField() {
    Value<BigDecimal> value = Value.value(BigDecimal.valueOf(42.2));
    ComponentValue<BigDecimal, BigDecimalField> componentValue = Components.bigDecimalField()
            .maximumFractionDigits(2)
            .groupingSeparator('.')
            .decimalSeparator(',')
            .maximumSize(new Dimension(10, 10))
            .linkedValue(value)
            .buildComponentValue();
    assertEquals(componentValue.getComponent().getNumber(), value.get());
  }

  @Test
  void localTimeField() {
    Value<LocalTime> value = Value.value(LocalTime.now());
    ComponentValue<LocalTime, TemporalField<LocalTime>> componentValue =
            Components.localTimeField("HH:mm")
                    .focusLostBehaviour(JFormattedTextField.COMMIT)
                    .linkedValue(value)
                    .buildComponentValue();
    assertEquals(componentValue.get(), value.get().truncatedTo(ChronoUnit.MINUTES));
  }

  @Test
  void localDateField() {
    Value<LocalDate> value = Value.value(LocalDate.now());
    ComponentValue<LocalDate, TemporalField<LocalDate>> componentValue =
            Components.localDateField("dd-MM-yyyy")
                    .focusLostBehaviour(JFormattedTextField.COMMIT)
                    .linkedValue(value)
                    .buildComponentValue();
    assertEquals(componentValue.get(), value.get());
  }

  @Test
  void localDateTimeField() {
    Value<LocalDateTime> value = Value.value(LocalDateTime.now());
    ComponentValue<LocalDateTime, TemporalField<LocalDateTime>> componentValue =
            Components.localDateTimeField("dd-MM-yyyy HH:mm")
                    .focusLostBehaviour(JFormattedTextField.COMMIT)
                    .linkedValue(value)
                    .buildComponentValue();
    assertEquals(componentValue.get(), value.get().truncatedTo(ChronoUnit.MINUTES));
  }

  @Test
  void offsetDateTimeField() {
    Value<OffsetDateTime> value = Value.value(OffsetDateTime.now());
    ComponentValue<OffsetDateTime, TemporalField<OffsetDateTime>> componentValue =
            Components.offsetDateTimeField("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    .focusLostBehaviour(JFormattedTextField.COMMIT)
                    .linkedValue(value)
                    .buildComponentValue();
//    assertEquals(componentValue.get(), value.get().truncatedTo(ChronoUnit.MINUTES));
  }

  @Test
  void temporalInputPanel() {
    Value<LocalDate> value = Value.value(LocalDate.now());
    ComponentValue<LocalDate, TemporalInputPanel<LocalDate>> componentValue =
            Components.temporalInputPanel(LocalDate.class, "dd-MM-yyyy")
                    .columns(8)
                    .selectAllOnFocusGained(true)
                    .updateOn(UpdateOn.KEYSTROKE)
                    .linkedValue(value)
                    .buildComponentValue();
    assertEquals(componentValue.get(), value.get());
  }

  @Test
  void button() {
    Components.button()
            .action(Control.control(() -> {}))
            .preferredSize(new Dimension(10, 10))
            .build();
  }

  @Test
  void checkBox() {
    Value<Boolean> value = Value.value(true, false);
    ComponentValue<Boolean, JCheckBox> componentValue = Components.checkBox(value)
            .caption("caption")
            .horizontalAlignment(SwingConstants.CENTER)
            .includeCaption(true)
            .transferFocusOnEnter(true)
            .buildComponentValue();
    JCheckBox box = componentValue.getComponent();
    assertTrue(box.isSelected());
    assertTrue(value.get());

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(value.get());

    value.set(true);
    assertTrue(box.isSelected());
  }

  @Test
  void toggleButton() {
    Value<Boolean> value = Value.value(true, false);
    ComponentValue<Boolean, JToggleButton> componentValue = Components.toggleButton(value)
            .caption("caption")
            .includeCaption(true)
            .transferFocusOnEnter(true)
            .buildComponentValue();
    JToggleButton box = componentValue.getComponent();
    assertTrue(box.isSelected());
    assertTrue(value.get());

    box.doClick();

    assertFalse(box.isSelected());
    assertFalse(value.get());

    value.set(true);
    assertTrue(box.isSelected());
  }

  @Test
  void radioButton() {
    Value<Boolean> value = Value.value(true, false);
    ComponentValue<Boolean, JRadioButton> componentValue = Components.radioButton(value)
            .caption("caption")
            .includeCaption(true)
            .transferFocusOnEnter(true)
            .buildComponentValue();
    JRadioButton button = componentValue.getComponent();
    assertTrue(button.isSelected());
    assertTrue(value.get());

    button.doClick();

    assertFalse(button.isSelected());
    assertFalse(value.get());

    value.set(true);
    assertTrue(button.isSelected());
  }

  @Test
  void nullableCheckBox() {
    Value<Boolean> value = Value.value(true);
    ComponentValue<Boolean, JCheckBox> componentValue = Components.checkBox(value)
            .transferFocusOnEnter(true)
            .nullable(true)
            .buildComponentValue();
    NullableCheckBox box = (NullableCheckBox) componentValue.getComponent();
    assertTrue(box.isSelected());
    assertTrue(value.get());

    box.getMouseListeners()[1].mouseClicked(null);

    assertNull(box.getState());
    assertNull(value.get());

    value.set(false);
    assertFalse(box.isSelected());
  }

  @Test
  void booleanComboBox() {
    Value<Boolean> value = Value.value(true);
    ComponentValue<Boolean, JComboBox<Item<Boolean>>> componentValue =
            Components.booleanComboBox(ItemComboBoxModel.createBooleanModel())
                    .maximumRowCount(5)
                    .transferFocusOnEnter(true)
                    .linkedValue(value)
                    .buildComponentValue();
    ItemComboBoxModel<Boolean> boxModel =
            (ItemComboBoxModel<Boolean>) componentValue.getComponent().getModel();
    assertTrue(boxModel.getSelectedValue().getValue());
    boxModel.setSelectedItem(null);
    assertNull(value.get());

    value.set(false);
    assertFalse(boxModel.getSelectedValue().getValue());
  }

  @Test
  void itemComboBox() {
    List<Item<Integer>> items = asList(item(0, "0"), item(1, "1"),
            item(2, "2"), item(3, "3"));
    Value<Integer> value = Value.value();
    ComponentValue<Integer, JComboBox<Item<Integer>>> componentValue = Components.itemComboBox(items)
            .mouseWheelScrollingWithWrapAround(true)
            .transferFocusOnEnter(true)
            .sorted(true)
            .linkedValue(value)
            .nullable(true)
            .buildComponentValue();
    componentValue.link(value);
    JComboBox<Item<Integer>> comboBox = componentValue.getComponent();
    ItemComboBoxModel<Integer> model = (ItemComboBoxModel<Integer>) comboBox.getModel();
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
  void comboBox() {
    DefaultComboBoxModel<String> boxModel = new DefaultComboBoxModel<>(new String[] {"0", "1", "2", "3"});
    Value<String> value = Value.value();
    ComponentValue<String, JComboBox<String>> componentValue = Components.comboBox(boxModel)
            .completionMode(Completion.Mode.NONE)//otherwise, a non-existing element can be selected, last test fails
            .editable(true)
            .orientation(ComponentOrientation.RIGHT_TO_LEFT)
            .maximumRowCount(5)
            .linkedValue(value)
            .mouseWheelScrollingWithWrapAround(true)
            .transferFocusOnEnter(true).buildComponentValue();
    JComboBox<String> box = componentValue.getComponent();

    assertNull(value.get());
    box.setSelectedItem("1");
    assertEquals("1", value.get());
    box.setSelectedItem("2");
    assertEquals("2", value.get());
    box.setSelectedItem("3");
    assertEquals("3", value.get());
    box.setSelectedItem("4");//does not exist, but editable
    assertEquals("4", value.get());
  }

  @Test
  void textField() {
    Value<String> value = Value.value();
    ComponentValue<String, JTextField> componentValue = Components.textField()
            .columns(10)
            .upperCase(true)
            .selectAllOnFocusGained(true)
            .action(Control.control(() -> {}))
            .lookupDialog(Collections::emptyList)
            .format(null)
            .horizontalAlignment(SwingConstants.CENTER)
            .linkedValue(value)
            .buildComponentValue();
    JTextField field = componentValue.getComponent();
    field.setText("hello");
    assertEquals("HELLO", value.get());
  }

  @Test
  void textArea() {
    Value<String> value = Value.value();
    TextAreaBuilder builder = Components.textArea()
            .transferFocusOnEnter(true)
            .autoscrolls(true)
            .rowsColumns(4, 2)
            .updateOn(UpdateOn.KEYSTROKE)
            .lineWrap(true)
            .wrapStyleWord(true)
            .linkedValue(value);
    ComponentValue<String, JTextArea> componentValue = builder
            .buildComponentValue();
    JTextArea textArea = componentValue.getComponent();
    textArea.setText("hello");
    assertEquals("hello", value.get());
    builder.scrollPane().build();
  }

  @Test
  void textInputPanel() {
    Value<String> value = Value.value();
    ComponentValue<String, TextInputPanel> componentValue = Components.textInputPanel()
            .transferFocusOnEnter(true)
            .columns(10)
            .buttonFocusable(true)
            .upperCase(false)
            .lowerCase(true)
            .selectAllOnFocusGained(true)
            .textAreaSize(new Dimension(100, 100))
            .maximumLength(100)
            .caption("caption")
            .dialogTitle("title")
            .updateOn(UpdateOn.KEYSTROKE)
            .linkedValue(value)
            .buildComponentValue();
    TextInputPanel inputPanel = componentValue.getComponent();
    inputPanel.setText("hello");
    assertEquals("hello", value.get());
  }

  @Test
  void formattedTextField() {
    Value<String> value = Value.value();
    ComponentValue<String, JFormattedTextField> componentValue = Components.formattedTextField()
            .formatMask("##:##")
            .valueContainsLiterals(true)
            .columns(6)
            .updateOn(UpdateOn.KEYSTROKE)
            .focusLostBehaviour(JFormattedTextField.COMMIT)
            .linkedValue(value)
            .buildComponentValue();
    JFormattedTextField field = componentValue.getComponent();
    field.setText("1234");
    assertEquals("12:34", value.get());
  }

  @Test
  void integerSpinner() {
    Value<Integer> value = Value.value(10);
    ComponentValue<Integer, JSpinner> componentValue = Components.integerSpinner()
            .minimum(0)
            .maximum(100)
            .stepSize(10)
            .mouseWheelScrolling(true)
            .transferFocusOnEnter(true)
            .linkedValue(value)
            .buildComponentValue();
    assertEquals(10, componentValue.get());
    value.set(50);
    assertEquals(50, componentValue.get());
  }

  @Test
  void doubleSpinner() {
    Value<Double> value = Value.value(10d);
    ComponentValue<Double, JSpinner> componentValue = Components.doubleSpinner()
            .minimum(0d)
            .maximum(100d)
            .stepSize(10d)
            .columns(5)
            .mouseWheelScrollingReversed(true)
            .transferFocusOnEnter(true)
            .linkedValue(value)
            .buildComponentValue();
    assertEquals(10d, componentValue.get());
    value.set(50d);
    assertEquals(50d, componentValue.get());
  }

  @Test
  void listSpinner() {
    Value<String> value = Value.value();
    ComponentValue<String, JSpinner> componentValue = Components.<String>listSpinner(new SpinnerListModel(Arrays.asList("One", "Two")))
            .columns(5)
            .horizontalAlignment(SwingConstants.CENTER)
            .mouseWheelScrolling(true)
            .editable(false)
            .transferFocusOnEnter(true)
            .linkedValue(value)
            .buildComponentValue();
    assertEquals("One", componentValue.get());
    value.set("Two");
    assertEquals("Two", componentValue.get());
  }

  @Test
  void itemSpinner() {
    Value<Integer> value = Value.value();
    SpinnerListModel spinnerModel = new SpinnerListModel(asList(item(1, "One"), item(2, "Two")));
    ComponentValue<Integer, JSpinner> componentValue = Components.<Integer>itemSpinner(spinnerModel)
            .columns(5)
            .mouseWheelScrolling(true)
            .editable(false)
            .transferFocusOnEnter(true)
            .linkedValue(value)
            .buildComponentValue();
    assertEquals(1, componentValue.get());
    value.set(2);
    assertEquals(2, componentValue.get());
  }

  @Test
  void slider() {
    Value<Integer> value = Value.value(10);
    ComponentValue<Integer, JSlider> componentValue = Components.slider(new DefaultBoundedRangeModel(0, 0, 0, 100))
            .snapToTicks(true)
            .paintTrack(true)
            .paintTicks(true)
            .paintLabels(true)
            .inverted(false)
            .minorTickSpacing(1)
            .majorTickSpacing(10)
            .mouseWheelScrollingReversed(true)
            .linkedValue(value)
            .orientation(SwingConstants.VERTICAL)
            .buildComponentValue();
    assertEquals(10, componentValue.get());
    value.set(50);
    assertEquals(50, componentValue.get());
  }

  @Test
  void label() {
    Value<String> textValue = Value.value("label");
    ComponentValue<String, JLabel> componentValue = Components.label(textValue)
            .icon(Logos.logoTransparent())
            .displayedMnemonic('l')
            .labelFor(new JButton())
            .buildComponentValue();
    assertEquals("label", componentValue.getComponent().getText());
    textValue.set("hello");
    assertEquals("hello", componentValue.getComponent().getText());
  }

  @Test
  void list() {
    DefaultListModel<String> listModel = new DefaultListModel<>();
    listModel.addElement("one");
    listModel.addElement("two");
    listModel.addElement("three");

    Value<String> textValue = Value.value("two");
    ListBuilder<String> listBuilder = Components.list(listModel)
            .visibleRowCount(4)
            .layoutOrientation(JList.VERTICAL)
            .fixedCellHeight(10)
            .fixedCellWidth(10)
            .linkedValue(textValue);
    ComponentValue<String, JList<String>> componentValue = listBuilder
            .buildComponentValue();
    assertEquals("two", componentValue.get());
    textValue.set("three");
    assertEquals("three", componentValue.get());
    listBuilder.scrollPane().build();
  }

  @Test
  void builder() {
    JButton component = new JButton();
    ComponentBuilder<Object, JButton, ?> builder = Components.component(component)
            .clientProperty("Key", "Value");
    assertThrows(UnsupportedOperationException.class, builder::buildComponentValue);
    builder.initialValue(1);
    assertEquals("Value", component.getClientProperty("Key"));
  }

  @Test
  void valueLocked() {
    assertThrows(IllegalStateException.class, () -> Components.textField(Value.value())
            .linkedValue(Value.value()));
    assertThrows(IllegalStateException.class, () -> Components.textField(Value.value())
            .linkedValueObserver(Value.value()));
  }

  @Test
  void validatorInvalidValue() {
    Value.Validator<String> validator = value -> {
      if ("test".equals(value)) {
        throw new IllegalArgumentException();
      }
    };
    assertThrows(IllegalArgumentException.class, () -> Components.textField(String.class)
            .initialValue("test")
            .validator(validator)
            .build());

    Value<String> stringValue = Value.value("test");
    assertThrows(IllegalArgumentException.class, () -> Components.textField(String.class, stringValue)
            .validator(validator)
            .build());
    assertThrows(IllegalArgumentException.class, () -> Components.textField(String.class)
            .linkedValueObserver(stringValue.getObserver())
            .validator(validator)
            .build());
  }

  @Test
  void addConstraintsComponent() {
    assertThrows(IllegalArgumentException.class, () -> Components.panel()
            .add(new JLabel(), new JLabel()));
  }
}
