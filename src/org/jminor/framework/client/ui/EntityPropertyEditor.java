/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.common.model.formats.LongMediumDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.combobox.BooleanComboBoxModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

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
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * A class for editing a property value for one or more entities at a time
 */
public class EntityPropertyEditor extends JPanel {

  public final Event evtButtonClicked = new Event("EntityPropertyEditor.evtButtonClicked");

  private final JComponent field;
  private final Property property;
  private transient final InputManager inputManager;

  private JButton okButton;
  private int buttonValue = -Integer.MAX_VALUE;

  /**
   * Instantiates a new EntityPropertyEditor
   * @param property the property to edit
   * @param entities the entities
   * @throws UserException in case of an exception
   */
  public EntityPropertyEditor(final Property property, final List<Entity> entities) throws UserException {
    this(property, entities, null);
  }

  /**
   * Instantiates a new EntityPropertyEditor
   * @param property the property to edit
   * @param entities the entities
   * @param entityModel an EntityModel instance used in case of an Property.EntityProperty being edited,
   * it provides both the IEntityDbProvider as well as the EntityComboBoxModel used in that case
   * @throws UserException in case of an exception
   */
  public EntityPropertyEditor(final Property property, final List<Entity> entities,
                              final EntityModel entityModel) throws UserException {
    this(property, entities, entityModel, null);
  }

  /**
   * Instantiates a new EntityPropertyEditor
   * @param property the property to edit
   * @param entities the entities
   * @param entityModel an EntityModel instance used in case of an Property.EntityProperty being edited,
   * it provides both the IEntityDbProvider as well as the EntityComboBoxModel used in that case
   * @param inputManager the InputManager to use
   * @throws UserException in case of an exception
   */
  public EntityPropertyEditor(final Property property, final List<Entity> entities,
                              final EntityModel entityModel, final InputManager inputManager) throws UserException {
    if (property instanceof Property.EntityProperty && entityModel == null)
      throw new IllegalArgumentException("No EntityModel instance provided for entity property editor");

    this.inputManager = inputManager;
    this.property = property;
    final Collection<Object> values = EntityUtil.getPropertyValues(entities, property.propertyID);
    this.field = getInputField(entityModel, values.size() == 1 ? values.iterator().next() : null);
    if (this.field instanceof JTextField)
      FrameworkUiUtil.addLookupDialog((JTextField) this.field, entityModel.getEntityID(), property,
              entityModel.getDbConnectionProvider());
    initUI(property.getCaption());
  }

  /**
   * @return true if the edit has been accepted
   */
  public boolean isEditAccepted() {
    return buttonValue == JOptionPane.OK_OPTION;
  }

  /**
   * @return the OK button
   */
  public JButton getOkButton() {
    return okButton;
  }

  /**
   * @return the value specified by the input component of this EntityPropertyEditor
   * @throws org.jminor.common.model.UserException in case of exception
   */
  public Object getValue() throws UserException {
    if (this.inputManager != null)
      return inputManager.getValue();

    switch (property.getPropertyType()) {
      case LONG_DATE:
      case SHORT_DATE:
        try {
          final String dateText = ((DateInputPanel)field).inputField.getText();
          if (!dateText.contains("_"))
            return new Timestamp(((DateInputPanel)field).maskFormat.parse(dateText).getTime());
          else
            return null;
        }
        catch (ParseException e) {
          throw new UserException("Wrong date format "
                  + ((DateInputPanel)field).maskFormat.toPattern() + " expected");
        }
      case DOUBLE:
        return ((DoubleField)field).getDouble();
      case INT:
        return ((IntField)field).getInt();
      case BOOLEAN:
        return ((JComboBox) field).getSelectedItem();
      case CHAR: {
        final String txt = ((TextInputPanel)field).getText();
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

        return ((TextInputPanel)field).getText();
      }
    }
  }

  protected JComponent getInputField(final EntityModel entityModel, final Object currentValue) throws UserException {
    if (inputManager != null)
      return inputManager.getInputComponent();

    switch (property.getPropertyType()) {
      case LONG_DATE:
        return FrameworkUiUtil.createDateChooserPanel((Date) currentValue, new LongMediumDateFormat());
      case SHORT_DATE:
        return FrameworkUiUtil.createDateChooserPanel((Date) currentValue, new ShortDashDateFormat());
      case DOUBLE:
        final DoubleField dfield = new DoubleField();
        if (currentValue != null)
          dfield.setDouble((Double) currentValue);
        return dfield;
      case INT:
        final IntField ifield = new IntField();
        if (currentValue != null)
          ifield.setInt((Integer) currentValue);
        return ifield;
      case BOOLEAN:
        final JComboBox ret = new JComboBox(new BooleanComboBoxModel());
        if (currentValue != null)
          ret.setSelectedItem(currentValue);
        return ret;
      case ENTITY:
        return createEntityField(entityModel, currentValue);
      default:
        return createTextInputPanel(currentValue);
    }
  }

  protected void initUI(final String propertyID) {
    setLayout(new BorderLayout(5,5));
    setBorder(BorderFactory.createTitledBorder(propertyID));
    add(this.field, BorderLayout.CENTER);
    final JPanel btnBase = new JPanel(new FlowLayout(FlowLayout.CENTER));
    btnBase.add(createButtonPanel());
    add(btnBase, BorderLayout.SOUTH);
  }

  private JComponent createEntityField(final EntityModel entityModel, final Object currentValue) throws UserException {
    final Property.EntityProperty entityProperty = (Property.EntityProperty) property;
    if (!EntityRepository.get().isLargeDataset(entityProperty.referenceEntityID)) {
      final EntityComboBoxModel model = entityModel.createEntityComboBoxModel(entityProperty);
      model.refresh();
      if (currentValue != null)
        model.setSelectedItem(currentValue);

      return new JComboBox(model);
    }
    else {
      final String[] searchPropertyIds = EntityRepository.get().getEntitySearchPropertyIDs(entityProperty.referenceEntityID);
      List<Property> searchProperties;
      if (searchPropertyIds != null) {
        searchProperties = EntityRepository.get().getProperties(entityProperty.referenceEntityID, searchPropertyIds);
      }
      else {//use all string properties
        final Collection<Property> properties =
                EntityRepository.get().getDatabaseProperties(entityProperty.referenceEntityID);
        searchProperties = new ArrayList<Property>();
        for (final Property property : properties)
          if (property.getPropertyType() == Type.STRING)
            searchProperties.add(property);
      }
      if (searchProperties.size() == 0)
        throw new RuntimeException("No searchable properties found for entity: " + entityProperty.referenceEntityID);

      final EntityLookupField field = new EntityLookupField(entityModel.createEntityLookupModel(
              ((Property.EntityProperty) property).referenceEntityID, null, searchProperties));
      if (currentValue != null)
        field.getModel().setSelectedEntity((Entity) currentValue);

      return field;
    }
  }

  private JPanel createButtonPanel() {
    final JPanel ret = new JPanel(new GridLayout(1,2,5,5));
    ret.add(okButton = createButton(Messages.get(Messages.OK), Messages.get(Messages.OK_MNEMONIC), JOptionPane.OK_OPTION));
    ret.add(createButton(Messages.get(Messages.CANCEL), Messages.get(Messages.CANCEL_MNEMONIC), JOptionPane.CANCEL_OPTION));

    return ret;
  }

  private JButton createButton(final String caption, final String mnemonic, final int option) {
    final JButton ret = new JButton(new AbstractAction(caption){
      public void actionPerformed(final ActionEvent e) {
        buttonValue = option;
        evtButtonClicked.fire();
      }
    });
    ret.setMnemonic(mnemonic.charAt(0));

    return ret;
  }

  private TextInputPanel createTextInputPanel(final Object currentValue) {
    final JTextField txtField = new JTextField(currentValue != null ? currentValue.toString() : "");
    txtField.setColumns(16);
    return new TextInputPanel(txtField, property.getCaption());
  }

  public static abstract class InputManager {
    private final JComponent inputComponent;

    public InputManager(final JComponent inputComponent) {
      this.inputComponent = inputComponent;
    }

    public JComponent getInputComponent() {
      return this.inputComponent;
    }

    protected abstract Object getValue();
  }
}
