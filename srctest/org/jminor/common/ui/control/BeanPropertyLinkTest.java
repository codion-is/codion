/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;

import junit.framework.TestCase;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * User: Björn Darri
 * Date: 13.1.2008
 * Time: 12:15:13
 */
public class BeanPropertyLinkTest extends TestCase {

  private Integer intValue;
  private Event evtIntValueChanged = new Event("");

  private Double doubleValue;
  private Event evtDoubleValueChanged = new Event("");

  private String stringValue;
  private Event evtStringValueChanged = new Event("");

  private boolean booleanValue;
  private Event evtBooleanValueChanged = new Event("");

  private String selectedItem;
  private Event evtSelectedItemChanged = new Event("");

  public void testIntBeanPropertyLink() throws Exception {
    final IntField txtInt = new IntField();
    new IntBeanPropertyLink(txtInt, this, "intValue", evtIntValueChanged, "");
    assertNull("Int value should be null on initialization", txtInt.getInt());
    setIntValue(2);
    assertEquals("Int value should be 2", 2, (int) txtInt.getInt());
    txtInt.setText("42");
    assertEquals("Int value should be 42", 42, (int) getIntValue());
    txtInt.setText("");
    assertNull("Int value should be null", getIntValue());
  }

  public void testDoubleBeanPropertyLink() throws Exception {
    final DoubleField txtDouble = new DoubleField();
    txtDouble.setDecimalSymbol(DoubleField.POINT);
    new DoubleBeanPropertyLink(txtDouble, this, "doubleValue", evtDoubleValueChanged, "");
    assertNull("Double value should be null on initialization", txtDouble.getDouble());
    setDoubleValue(2.2);
    assertEquals("Double value should be 2.2", 2.2, txtDouble.getDouble());
    txtDouble.setText("42.2");
    assertEquals("Double value should be 42.2", 42.2, getDoubleValue());
    txtDouble.setText("");
    assertNull("Double value should be null", getDoubleValue());
  }

  public void testStringBeanPropertyLink() throws Exception {
    final JTextField txtString = new JTextField();
    new TextBeanPropertyLink(txtString, this, "stringValue", String.class, evtStringValueChanged, "");
    assertEquals("String value should be empty on initialization", "", txtString.getText());
    setStringValue("hello");
    assertEquals("String value should be 'hello'", "hello", txtString.getText());
    txtString.setText("42");
    assertEquals("String value should be 42", "42", getStringValue());
    txtString.setText("");
    assertEquals("String value should be empty", "", getStringValue());
  }

  public void testToggleBeanPropertyLink() throws Exception {
    final JCheckBox checkBox = new JCheckBox();
    ControlProvider.bindToggleButtonAndProperty(checkBox, this, "booleanValue", "", evtBooleanValueChanged);
    assertFalse("Boolean value should be false on initialization", checkBox.isSelected());
    setBooleanValue(true);
    assertTrue("Boolean value should be true", checkBox.isSelected());
    checkBox.doClick();
    assertFalse("Boolean value should be false", isBooleanValue());
  }

  public void testSelectedItemBeanPropertyLink() throws Exception {
    final JComboBox box = new JComboBox(new String[] {"b", "d", "s"});
    new SelectedItemBeanPropertyLink(box, this, "selectedItem", String.class, evtSelectedItemChanged, null);
    assertNull("selected item should be null", getSelectedItem());
    setSelectedItem("s");
    assertEquals("selected item should be 's'", "s", box.getSelectedItem());
    box.setSelectedItem("d");
    assertEquals("selected item should be 'd'", "d", getSelectedItem());
  }

  public Integer getIntValue() {
    return intValue;
  }

  public void setIntValue(final Integer intValue) {
    this.intValue = intValue;
    evtIntValueChanged.fire();
  }

  public Double getDoubleValue() {
    return doubleValue;
  }

  public void setDoubleValue(final Double doubleValue) {
    this.doubleValue = doubleValue;
    evtDoubleValueChanged.fire();
  }

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(final String stringValue) {
    this.stringValue = stringValue;
    evtStringValueChanged.fire();
  }

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(final boolean booleanValue) {
    this.booleanValue = booleanValue;
    evtBooleanValueChanged.fire();
  }

  public String getSelectedItem() {
    return selectedItem;
  }

  public void setSelectedItem(final String selectedItem) {
    this.selectedItem = selectedItem;
    evtSelectedItemChanged.fire();
  }
}
