/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.icons.Logos;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TemporalInputPanel;
import is.codion.swing.common.ui.textfield.TextInputPanel;

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
    final Font defaultFont = new JTextField().getFont();

    final Value<Integer> value = Value.value(42);

    final IntegerFieldBuilder builder = Components.integerField()
            .range(0, 100)
            .font(defaultFont.deriveFont(Font.BOLD))
            .foreground(Color.WHITE)
            .background(Color.BLACK)
            .linkedValue(value);

    final IntegerField component = builder.build();
    final ComponentValue<Integer, IntegerField> componentValue = builder.buildComponentValue();

    assertSame(component, componentValue.getComponent());

    builder.clear();

    final IntegerField nextComponent = builder.build();
    final ComponentValue<Integer, IntegerField> nextComponentValue = builder.buildComponentValue();

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
    final Value<Integer> value = Value.value(42);
    final ComponentValue<Integer, IntegerField> componentValue = Components.integerField()
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
    final Value<Long> value = Value.value(42L);
    final ComponentValue<Long, LongField> componentValue = Components.longField()
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
    final Value<Double> value = Value.value(42.2);
    final ComponentValue<Double, DoubleField> componentValue = Components.doubleField()
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
    final Value<BigDecimal> value = Value.value(BigDecimal.valueOf(42.2));
    final ComponentValue<BigDecimal, BigDecimalField> componentValue = Components.bigDecimalField()
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
    final Value<LocalTime> value = Value.value(LocalTime.now());
    final ComponentValue<LocalTime, TemporalField<LocalTime>> componentValue =
            Components.localTimeField("HH:mm")
                    .focusLostBehaviour(JFormattedTextField.COMMIT)
                    .linkedValue(value)
                    .buildComponentValue();
    assertEquals(componentValue.get(), value.get().truncatedTo(ChronoUnit.MINUTES));
  }

  @Test
  void localDateField() {
    final Value<LocalDate> value = Value.value(LocalDate.now());
    final ComponentValue<LocalDate, TemporalField<LocalDate>> componentValue =
            Components.localDateField("dd-MM-yyyy")
                    .focusLostBehaviour(JFormattedTextField.COMMIT)
                    .linkedValue(value)
                    .buildComponentValue();
    assertEquals(componentValue.get(), value.get());
  }

  @Test
  void localDateTimeField() {
    final Value<LocalDateTime> value = Value.value(LocalDateTime.now());
    final ComponentValue<LocalDateTime, TemporalField<LocalDateTime>> componentValue =
            Components.localDateTimeField("dd-MM-yyyy HH:mm")
                    .focusLostBehaviour(JFormattedTextField.COMMIT)
                    .linkedValue(value)
                    .buildComponentValue();
    assertEquals(componentValue.get(), value.get().truncatedTo(ChronoUnit.MINUTES));
  }

  @Test
  void offsetDateTimeField() {
    final Value<OffsetDateTime> value = Value.value(OffsetDateTime.now());
    final ComponentValue<OffsetDateTime, TemporalField<OffsetDateTime>> componentValue =
            Components.offsetDateTimeField("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    .focusLostBehaviour(JFormattedTextField.COMMIT)
                    .linkedValue(value)
                    .buildComponentValue();
//    assertEquals(componentValue.get(), value.get().truncatedTo(ChronoUnit.MINUTES));
  }

  @Test
  void temporalInputPanel() {
    final Value<LocalDate> value = Value.value(LocalDate.now());
    final ComponentValue<LocalDate, TemporalInputPanel<LocalDate>> componentValue =
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
    final Value<Boolean> value = Value.value(true, false);
    final ComponentValue<Boolean, JCheckBox> componentValue = Components.checkBox(value)
            .caption("caption")
            .horizontalAlignment(SwingConstants.CENTER)
            .includeCaption(true)
            .transferFocusOnEnter(true)
            .buildComponentValue();
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
  void toggleButton() {
    final Value<Boolean> value = Value.value(true, false);
    final ComponentValue<Boolean, JToggleButton> componentValue = Components.toggleButton(value)
            .caption("caption")
            .includeCaption(true)
            .transferFocusOnEnter(true)
            .buildComponentValue();
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
  void radioButton() {
    final Value<Boolean> value = Value.value(true, false);
    final ComponentValue<Boolean, JRadioButton> componentValue = Components.radioButton(value)
            .caption("caption")
            .includeCaption(true)
            .transferFocusOnEnter(true)
            .buildComponentValue();
    final JRadioButton button = componentValue.getComponent();
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
    final Value<Boolean> value = Value.value(true);
    final ComponentValue<Boolean, JCheckBox> componentValue = Components.checkBox(value)
            .transferFocusOnEnter(true)
            .nullable(true)
            .buildComponentValue();
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
  void booleanComboBox() {
    final Value<Boolean> value = Value.value(true);
    final ComponentValue<Boolean, SteppedComboBox<Item<Boolean>>> componentValue =
            Components.booleanComboBox(ItemComboBoxModel.createBooleanModel())
                    .maximumRowCount(5)
                    .transferFocusOnEnter(true)
                    .linkedValue(value)
                    .buildComponentValue();
    final ItemComboBoxModel<Boolean> boxModel =
            (ItemComboBoxModel<Boolean>) componentValue.getComponent().getModel();
    assertTrue(boxModel.getSelectedValue().getValue());
    boxModel.setSelectedItem(null);
    assertNull(value.get());

    value.set(false);
    assertFalse(boxModel.getSelectedValue().getValue());
  }

  @Test
  void itemComboBox() {
    final List<Item<Integer>> items = asList(item(0, "0"), item(1, "1"),
            item(2, "2"), item(3, "3"));
    final Value<Integer> value = Value.value();
    final ComponentValue<Integer, SteppedComboBox<Item<Integer>>> componentValue = Components.itemComboBox(items)
            .mouseWheelScrollingWithWrapAround(true)
            .transferFocusOnEnter(true)
            .sorted(true)
            .linkedValue(value)
            .nullable(true)
            .buildComponentValue();
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
  void comboBox() {
    final DefaultComboBoxModel<String> boxModel = new DefaultComboBoxModel<>(new String[] {"0", "1", "2", "3"});
    final Value<String> value = Value.value();
    final ComponentValue<String, SteppedComboBox<String>> componentValue = Components.comboBox(boxModel)
            .completionMode(Completion.Mode.NONE)//otherwise, a non-existing element can be selected, last test fails
            .editable(true)
            .orientation(ComponentOrientation.RIGHT_TO_LEFT)
            .maximumRowCount(5)
            .linkedValue(value)
            .mouseWheelScrollingWithWrapAround(true)
            .transferFocusOnEnter(true).buildComponentValue();
    final JComboBox<String> box = componentValue.getComponent();

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
    final Value<String> value = Value.value();
    final ComponentValue<String, JTextField> componentValue = Components.textField()
            .columns(10)
            .upperCase(true)
            .selectAllOnFocusGained(true)
            .action(Control.control(() -> {}))
            .lookupDialog(Collections::emptyList)
            .format(null)
            .horizontalAlignment(SwingConstants.CENTER)
            .linkedValue(value)
            .buildComponentValue();
    final JTextField field = componentValue.getComponent();
    field.setText("hello");
    assertEquals("HELLO", value.get());
  }

  @Test
  void textArea() {
    final Value<String> value = Value.value();
    final TextAreaBuilder builder = Components.textArea()
            .transferFocusOnEnter(true)
            .autoscrolls(true)
            .rowsColumns(4, 2)
            .updateOn(UpdateOn.KEYSTROKE)
            .lineWrap(true)
            .wrapStyleWord(true)
            .linkedValue(value);
    final ComponentValue<String, JTextArea> componentValue = builder
            .buildComponentValue();
    final JTextArea textArea = componentValue.getComponent();
    textArea.setText("hello");
    assertEquals("hello", value.get());
    builder.scrollPane().build();
  }

  @Test
  void textInputPanel() {
    final Value<String> value = Value.value();
    final ComponentValue<String, TextInputPanel> componentValue = Components.textInputPanel()
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
    final TextInputPanel inputPanel = componentValue.getComponent();
    inputPanel.setText("hello");
    assertEquals("hello", value.get());
  }

  @Test
  void formattedTextField() {
    final Value<String> value = Value.value();
    final ComponentValue<String, JFormattedTextField> componentValue = Components.formattedTextField()
            .formatMask("##:##")
            .valueContainsLiterals(true)
            .columns(6)
            .updateOn(UpdateOn.KEYSTROKE)
            .focusLostBehaviour(JFormattedTextField.COMMIT)
            .linkedValue(value)
            .buildComponentValue();
    final JFormattedTextField field = componentValue.getComponent();
    field.setText("1234");
    assertEquals("12:34", value.get());
  }

  @Test
  void integerSpinner() {
    final Value<Integer> value = Value.value(10);
    final ComponentValue<Integer, JSpinner> componentValue = Components.integerSpinner()
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
    final Value<Double> value = Value.value(10d);
    final ComponentValue<Double, JSpinner> componentValue = Components.doubleSpinner()
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
    final Value<String> value = Value.value();
    final ComponentValue<String, JSpinner> componentValue = Components.<String>listSpinner(new SpinnerListModel(Arrays.asList("One", "Two")))
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
    final Value<Integer> value = Value.value();
    final SpinnerListModel spinnerModel = new SpinnerListModel(asList(item(1, "One"), item(2, "Two")));
    final ComponentValue<Integer, JSpinner> componentValue = Components.<Integer>itemSpinner(spinnerModel)
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
    final Value<Integer> value = Value.value(10);
    final ComponentValue<Integer, JSlider> componentValue = Components.slider(new DefaultBoundedRangeModel(0, 0, 0, 100))
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
    final Value<String> textValue = Value.value("label");
    final ComponentValue<String, JLabel> componentValue = Components.label(textValue)
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
    final DefaultListModel<String> listModel = new DefaultListModel<>();
    listModel.addElement("one");
    listModel.addElement("two");
    listModel.addElement("three");

    final Value<String> textValue = Value.value("two");
    final ListBuilder<String> listBuilder = Components.list(listModel)
            .visibleRowCount(4)
            .layoutOrientation(JList.VERTICAL)
            .fixedCellHeight(10)
            .fixedCellWidth(10)
            .linkedValue(textValue);
    final ComponentValue<String, JList<String>> componentValue = listBuilder
            .buildComponentValue();
    assertEquals("two", componentValue.get());
    textValue.set("three");
    assertEquals("three", componentValue.get());
    listBuilder.scrollPane().build();
  }

  @Test
  void builder() {
    final JButton component = new JButton();
    final ComponentBuilder<Object, JButton, ?> builder = Components.component(component)
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
    final Value.Validator<String> validator = value -> {
      if ("test".equals(value)) {
        throw new IllegalArgumentException();
      }
    };
    assertThrows(IllegalArgumentException.class, () -> Components.textField(String.class)
            .initialValue("test")
            .validator(validator)
            .build());

    final Value<String> stringValue = Value.value("test");
    assertThrows(IllegalArgumentException.class, () -> Components.textField(String.class, stringValue)
            .validator(validator)
            .build());
    assertThrows(IllegalArgumentException.class, () -> Components.textField(String.class)
            .linkedValueObserver(stringValue.getObserver())
            .validator(validator)
            .build());
  }
}
