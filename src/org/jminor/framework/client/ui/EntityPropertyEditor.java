/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.common.model.formats.LongMediumDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.combobox.BooleanComboBoxModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.Date;

public class EntityPropertyEditor extends JPanel {

  public final Event evtButtonClicked = new Event("EntityPropertyEditor.evtButtonClicked");

  private final JComponent field;
  private final Property property;
  private final Object currentValue;
  private transient final InputManager inputManager;
  private transient final IEntityDbProvider dbProvider;

  private JButton okButton;
  private int buttonValue = -Integer.MAX_VALUE;

  public EntityPropertyEditor(final Object currentValue, final Property property, final boolean multipleEntityUpdate) throws UserException {
    this(currentValue, property, null, multipleEntityUpdate);
  }

  public EntityPropertyEditor(final Object currentValue, final Property property, final IEntityDbProvider db,
                              final boolean multipleEntityUpdate) throws UserException {
    this(currentValue, property, db, null, multipleEntityUpdate);
  }

  public EntityPropertyEditor(final Object currentValue, final Property property, final IEntityDbProvider dbProvider,
                              final InputManager inputManager, final boolean multipleEntityUpdate) throws UserException {
    if (property instanceof Property.EntityProperty && dbProvider == null)
      throw new IllegalArgumentException("No db object provided for entity property editor");

    this.inputManager = inputManager;
    this.dbProvider = dbProvider;
    this.property = property;
    this.currentValue = currentValue;
    this.field = getInputField(!multipleEntityUpdate && !EntityUtil.isValueNull(property.getPropertyType(), currentValue));
    initUI(property.getCaption());
  }

  /**
   * @return Value for property 'buttonValue'.
   */
  public int getButtonValue() {
    return buttonValue;
  }

  /**
   * @return Value for property 'okButton'.
   */
  public JButton getOkButton() {
    return okButton;
  }

  /**
   * @return Value for property 'value'.
   * @throws org.jminor.common.model.UserException in case of exception
   */
  public Object getValue() throws UserException {
    if (this.inputManager != null)
      return inputManager.getValue();

    switch (property.getPropertyType()) {
      case LONG_DATE:
      case SHORT_DATE:
        try {
          final String dateText = ((UiUtil.DateInputPanel)field).inputField.getText();
          if (!dateText.contains("_"))
            return ((UiUtil.DateInputPanel)field).maskFormat.parse(dateText);
          else
            return null;
        }
        catch (ParseException e) {
          throw new UserException("Wrong date format "
                  + ((UiUtil.DateInputPanel)field).maskFormat.toPattern() + " expected");
        }
      case DOUBLE:
        return ((DoubleField)field).getDouble();
      case INT:
        return ((IntField)field).getInteger();
      case BOOLEAN:
        return ((JComboBox) field).getSelectedItem();
      case CHAR: {
        final String txt = ((JTextField)field).getText();
        if (txt.length() > 0)
          return txt.charAt(0);

        return null;
      }
      case ENTITY:
        final JComboBox box = (JComboBox)field;
        if (box.getSelectedIndex() == 0)
          return null;

        return box.getSelectedItem();
      default: {
        if (field instanceof JComboBox)
          return ((JComboBox)field).getSelectedItem();

        return ((JTextField)field).getText();
      }
    }
  }

  protected JComponent getInputField(final boolean setCurrentValue) throws UserException {
    if (inputManager != null)
      return inputManager.getInputComponent();

    switch (property.getPropertyType()) {
      case LONG_DATE:
        return FrameworkUiUtil.createDateChooserPanel(setCurrentValue ? (Date) currentValue : null,
                new LongMediumDateFormat());
      case SHORT_DATE: {
        return FrameworkUiUtil.createDateChooserPanel(setCurrentValue ? (Date) currentValue : null,
                new ShortDashDateFormat());
      }
      case DOUBLE:
        final DoubleField dfield = new DoubleField();
        if (setCurrentValue)
          dfield.setDouble((Double) currentValue);
        return dfield;
      case INT:
        final IntField ifield = new IntField();
        if (setCurrentValue)
          ifield.setInt((Integer) currentValue);
        return ifield;
      case BOOLEAN:
        final JComboBox ret = new JComboBox(new BooleanComboBoxModel());
        if (setCurrentValue)
          ret.setSelectedItem(currentValue);
        return ret;
      case ENTITY: {
        final EntityComboBoxModel model = new EntityComboBoxModel(dbProvider,
                ((Property.EntityProperty) property).referenceEntityID, true, "N/A");
        model.refresh();
        if (setCurrentValue)
          model.setSelectedItem(currentValue);
        return new JComboBox(model);
      }
      default: {
        return new JTextField(setCurrentValue ? (currentValue != null ? currentValue.toString() : "") : "");
      }
    }
  }

  protected void initUI(final String propertyID) {
    setLayout(new BorderLayout(5,5));
    setBorder(BorderFactory.createTitledBorder(propertyID));
    add(this.field, BorderLayout.CENTER);
    final JPanel btnBase = new JPanel(new FlowLayout(FlowLayout.CENTER));
    btnBase.add(getButtonPanel());
    add(btnBase, BorderLayout.SOUTH);
  }

  private JPanel getButtonPanel() {
    final JPanel ret = new JPanel(new GridLayout(1,2,5,5));
    ret.add(okButton = getButton(Messages.get(Messages.OK), JOptionPane.OK_OPTION));
    ret.add(getButton(Messages.get(Messages.CANCEL), JOptionPane.CANCEL_OPTION));

    return ret;
  }

  private JButton getButton(final String caption, final int option) {
    return new JButton(new AbstractAction(caption){
      public void actionPerformed(final ActionEvent e) {
        buttonValue = option;
        evtButtonClicked.fire();
      }
    });
  }

  public static abstract class InputManager {
    private final JComponent inputComponent;

    public InputManager(JComponent inputComponent) {
      this.inputComponent = inputComponent;
    }

    public JComponent getInputComponent() {
      return this.inputComponent;
    }

    protected abstract Object getValue();
  }
}
