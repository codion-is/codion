/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.event.Event;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.textfield.SizedDocument;
import is.codion.swing.common.ui.component.textfield.TextInputPanel;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;

public class StringValuesTest {

  private String stringValue;
  private final Event<String> stringValueChangedEvent = Event.event();

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(String stringValue) {
    this.stringValue = stringValue;
    stringValueChangedEvent.onEvent();
  }

  @Test
  void valueLink() {
    Value<String> textValue = Value.value("start");
    textValue.addValidator(text -> {
      if (text != null && text.equals("nono")) {
        throw new IllegalArgumentException();
      }
    });
    JTextField textField = new JTextField();
    Value<String> textFieldValue = ComponentValues.textComponent(textField);
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
  void nullInitialValue() throws Exception {
    stringValue = null;
    JTextField textField = new JTextField();
    Value<String> stringPropertyValue = Value.propertyValue(this, "stringValue",
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

    JTextField textField2 = new JTextField();
    this.stringValue = "test";
    ComponentValues.textComponent(textField2)
            .link(Value.propertyValue(this, "stringValue", String.class, stringValueChangedEvent));
    assertEquals("test", textField2.getText());
  }

  @Test
  void nonNullInitialValue() throws Exception {
    stringValue = "name";
    JTextField textField = new JTextField();
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
  void textValueField() {
    final String value = "hello";

    SizedDocument document = new SizedDocument();
    document.setMaximumLength(5);

    TextInputPanel inputPanel = TextInputPanel.builder(new JTextField(document, value, 0))
            .dialogTitle("none").build();

    ComponentValue<String, TextInputPanel> componentValue = inputPanel.componentValue();
    assertEquals(value, componentValue.get());

    document.setMaximumLength(10);

    componentValue = inputPanel.componentValue();
    assertEquals(value, componentValue.get());

    inputPanel.setText("");

    componentValue = inputPanel.componentValue();
    assertNull(componentValue.get());

    componentValue.getComponent().setText("tester");
    assertEquals("tester", componentValue.get());

    componentValue.getComponent().setText("");
    assertNull(componentValue.get());

    assertThrows(IllegalArgumentException.class, () -> inputPanel.componentValue()
            .getComponent().setText("asdfasdfasdfasdfasdf"));
  }

  @Test
  void textValue() {
    JTextField textField = new JTextField();

    ComponentValue<String, JTextField> value = ComponentValues.textComponent(textField);

    assertNull(value.get());
    textField.setText("hello there");
    assertEquals("hello there", value.get());
    textField.setText("");
    assertNull(value.get());

    value.set("hi");
    assertEquals("hi", textField.getText());
  }

  @Test
  void characterValue() {
    ComponentValue<Character, JTextField> value = ComponentValues.characterTextField(new JTextField());
    assertNull(value.get());
    value.getComponent().setText("2");
    assertEquals('2', value.get());
    value.set(null);
    assertTrue(value.getComponent().getText().isEmpty());
  }
}
