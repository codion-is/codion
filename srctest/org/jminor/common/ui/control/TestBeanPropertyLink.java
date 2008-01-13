/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.Constants;
import org.jminor.common.model.Event;
import org.jminor.common.ui.ControlProvider;
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
public class TestBeanPropertyLink extends TestCase {

  private int intValue;
  private Event evtIntValueChanged = new Event("");

  private double doubleValue;
  private Event evtDoubleValueChanged = new Event("");

  private String stringValue;
  private Event evtStringValueChanged = new Event("");

  private boolean booleanValue;
  private Event evtBooleanValueChanged = new Event("");

  private String selectedItem;
  private Event evtSelectedItemChanged = new Event("");

  public TestBeanPropertyLink() {
    super("TestBeanPropertyLink");
  }

  public void testIntBeanPropertyLink() throws Exception {
    final IntField txtInt = new IntField();
    new IntBeanPropertyLink(txtInt, this, "intValue", evtIntValueChanged, "");
    assertEquals("Int value should be 0 on initialization", 0, txtInt.getInt());
    setIntValue(2);
    assertEquals("Int value should be 2", 2, txtInt.getInt());
    txtInt.setText("42");
    assertEquals("Int value should be 42", 42, getIntValue());
    txtInt.setText("");
    assertEquals("Int value should be null", Constants.INT_NULL_VALUE, getIntValue());
  }

  public void testDoubleBeanPropertyLink() throws Exception {
    final DoubleField txtDouble = new DoubleField();
    new DoubleBeanPropertyLink(txtDouble, this, "doubleValue", evtDoubleValueChanged, "");
    assertEquals("Double value should be 0 on initialization", 0.0, txtDouble.getDouble());
    setDoubleValue(2.2);
    assertEquals("Double value should be 2.2", 2.2, txtDouble.getDouble());
    txtDouble.setText("42.2");
    assertEquals("Double value should be 42.2", 42.2, getDoubleValue());
    txtDouble.setText("");
    assertEquals("Double value should be null", Constants.DOUBLE_NULL_VALUE, getDoubleValue());
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
    ControlProvider.bindToggleButtonAndProperty(checkBox, this, "booleanValue", "", evtBooleanValueChanged, null);
    assertFalse("Boolean value should be false on initialization", checkBox.isSelected());
    setBooleanValue(true);
    assertTrue("Boolean value should be true", checkBox.isSelected());
    checkBox.doClick();
    assertFalse("Boolean value should be false", isBooleanValue());
  }

  public void testSelectedItemBeanPropertyLink() throws Exception {
    final JComboBox box = new JComboBox(new String[] {"b", "d", "s"});
    new SelectedItemBeanPropertyLink(box, this, "selectedItem", String.class, evtSelectedItemChanged, "");
    assertNull("selected item should be null", getSelectedItem());
    setSelectedItem("s");
    assertEquals("selected item should be 's'", "s", box.getSelectedItem());
    box.setSelectedItem("d");
    assertEquals("selected item should be 'd'", "d", getSelectedItem());
  }

  public int getIntValue() {
    return intValue;
  }

  public void setIntValue(int intValue) {
    this.intValue = intValue;
    evtIntValueChanged.fire();
  }

  public double getDoubleValue() {
    return doubleValue;
  }

  public void setDoubleValue(double doubleValue) {
    this.doubleValue = doubleValue;
    evtDoubleValueChanged.fire();
  }

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(String stringValue) {
    this.stringValue = stringValue;
    evtStringValueChanged.fire();
  }

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(boolean booleanValue) {
    this.booleanValue = booleanValue;
    evtBooleanValueChanged.fire();
  }

  public String getSelectedItem() {
    return selectedItem;
  }

  public void setSelectedItem(String selectedItem) {
    this.selectedItem = selectedItem;
    evtSelectedItemChanged.fire();
  }
}
