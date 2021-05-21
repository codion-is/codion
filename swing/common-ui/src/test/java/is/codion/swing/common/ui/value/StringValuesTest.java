/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.event.Event;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TextInputPanel;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public class StringValuesTest {

  private String stringValue;
  private final Event<String> stringValueChangedEvent = Event.event();

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(final String stringValue) {
    this.stringValue = stringValue;
    stringValueChangedEvent.onEvent();
  }

  @Test
  public void valueLink() {
    final Value<String> textValue = Value.value("start");
    textValue.addValidator(text -> {
      if (text != null && text.equals("nono")) {
        throw new IllegalArgumentException();
      }
    });
    final JTextField textField = new JTextField();
    final Value<String> textFieldValue = ComponentValues.textComponent(textField);
    textFieldValue.link(textValue);

    assertEquals("start", textFieldValue.get());

    textFieldValue.set("testing");
    assertEquals("testing", textValue.get());

    assertThrows(IllegalArgumentException.class, () -> textFieldValue.set("nono"));
    assertEquals("testing", textFieldValue.get());

    textValue.set("hello");
    assertEquals("hello", textFieldValue.get());

    assertThrows(IllegalArgumentException.class, () -> textValue.set("nono"));
  }

  @Test
  public void nullInitialValue() throws Exception {
    stringValue = null;
    final JTextField textField = new JTextField();
    final Value<String> stringPropertyValue = Value.propertyValue(this, "stringValue",
            String.class, stringValueChangedEvent);
    ComponentValues.textComponent(textField).link(stringPropertyValue);
    assertNull(this.stringValue);
    assertEquals("", textField.getText());
    setStringValue("hello");
    assertEquals("hello", textField.getText());
    textField.setText("42");
    assertEquals("42", this.stringValue);
    textField.setText("");
    assertNull(this.stringValue);

    final JTextField textField2 = new JTextField();
    this.stringValue = "test";
    ComponentValues.textComponent(textField2)
            .link(Value.propertyValue(this, "stringValue", String.class, stringValueChangedEvent));
    assertEquals("test", textField2.getText());
  }

  @Test
  public void nonNullInitialValue() throws Exception {
    stringValue = "name";
    final JTextField textField = new JTextField();
    ComponentValues.textComponent(textField)
            .link(Value.propertyValue(this, "stringValue", String.class, stringValueChangedEvent));
    assertEquals("name", textField.getText());
    textField.setText("darri");
    assertFalse(getStringValue().isEmpty());
    assertEquals("darri", getStringValue());
    textField.setText("");
    assertNull(getStringValue());
    setStringValue("Björn");
    assertEquals("Björn", textField.getText());
  }

  @Test
  public void textValueField() {
    final String value = "hello";

    SizedDocument document = new SizedDocument();
    document.setMaximumLength(5);

    TextInputPanel inputPanel = TextInputPanel.builder(new JTextField(document, value, 0))
            .dialogTitle("none").build();

    ComponentValue<String, TextInputPanel> componentValue = ComponentValues.textInputPanel(inputPanel);
    assertEquals(value, componentValue.get());

    document.setMaximumLength(10);

    componentValue = ComponentValues.textInputPanel(inputPanel);
    assertEquals(value, componentValue.get());

    inputPanel.setText("");

    componentValue = ComponentValues.textInputPanel(inputPanel);
    assertNull(componentValue.get());

    componentValue.getComponent().setText("tester");
    assertEquals("tester", componentValue.get());

    componentValue.getComponent().setText("");
    assertNull(componentValue.get());

    assertThrows(IllegalArgumentException.class, () -> ComponentValues.textInputPanel(inputPanel)
            .getComponent().setText("asdfasdfasdfasdfasdf"));
  }

  @Test
  public void textValue() {
    final JTextField textField = new JTextField();

    final ComponentValue<String, JTextField> value = ComponentValues.textComponent(textField);

    assertNull(value.get());
    textField.setText("hello there");
    assertEquals("hello there", value.get());
    textField.setText("");
    assertNull(value.get());

    value.set("hi");
    assertEquals("hi", textField.getText());
  }

  @Test
  public void characterValue() {
    final ComponentValue<Character, JTextField> value = ComponentValues.characterTextField(new JTextField());
    assertNull(value.get());
    value.getComponent().setText("2");
    assertEquals('2', value.get());
    value.set(null);
    assertTrue(value.getComponent().getText().isEmpty());
  }
}
