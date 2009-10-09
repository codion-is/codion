/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.combobox.BooleanComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * A class for editing a property value for one or more entities at a time
 */
public class PropertyEditPanel extends JPanel {

  public final Event evtButtonClicked = new Event();

  private final InputManager inputManager;

  private JButton okButton;
  private int buttonValue = -Integer.MAX_VALUE;

  /**
   * Instantiates a new PropertyEditPanel
   * @param property the property to edit
   * @param entities the entities
   */
  public PropertyEditPanel(final Property property, final List<Entity> entities) {
    this(property, entities, null);
  }

  /**
   * Instantiates a new PropertyEditPanel
   * @param property the property to edit
   * @param entities the entities
   * @param editModel an EntityEditModel instance used in case of an Property.ForeignKeyProperty being edited,
   * it provides both the EntityDbProvider as well as the EntityComboBoxModel used in that case
   */
  public PropertyEditPanel(final Property property, final List<Entity> entities, final EntityEditModel editModel) {
    this(property, entities, editModel, null);
  }

  /**
   * Instantiates a new PropertyEditPanel
   * @param property the property to edit
   * @param entities the entities
   * @param editModel an EntityEditModel instance used in case of an Property.ForeignKeyProperty being edited,
   * it provides both the EntityDbProvider as well as the EntityComboBoxModel used in that case
   * @param inputManager the InputManager to use, if no InputManager is specified a default one is used
   */
  public PropertyEditPanel(final Property property, final List<Entity> entities, final EntityEditModel editModel,
                           final InputManager inputManager) {
    if (property == null)
      throw new IllegalArgumentException("Property must be specified");
    if (property instanceof Property.ForeignKeyProperty && editModel == null)
      throw new IllegalArgumentException("No EntityModel instance provided for foreign key property editor");

    this.inputManager = inputManager != null ? inputManager : initializeInputManager(property, editModel, entities);
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
   * @return the value specified by the input component of this PropertyEditPanel
   */
  public Object getValue() {
    return inputManager.getValue();
  }

  protected void initUI(final String propertyID) {
    setLayout(new BorderLayout(5,5));
    setBorder(BorderFactory.createTitledBorder(propertyID));
    add(inputManager.getInputComponent(), BorderLayout.CENTER);
    final JPanel btnBase = new JPanel(new FlowLayout(FlowLayout.CENTER));
    btnBase.add(createButtonPanel());
    add(btnBase, BorderLayout.SOUTH);
  }

  protected InputManager initializeInputManager(final Property property, final EntityEditModel editModel,
                                                final List<Entity> entities) {
    final Collection<Object> values = EntityUtil.getPropertyValues(entities, property.getPropertyID());
    final Object currentValue = values.size() == 1 ? values.iterator().next() : null;
    switch (property.getPropertyType()) {
      case TIMESTAMP:
        return new DateInputManager((Date) currentValue, Configuration.getDefaultTimestampFormat());
      case DATE:
        return new DateInputManager((Date) currentValue, Configuration.getDefaultDateFormat());
      case DOUBLE:
        return new DoubleInputManager((Double) currentValue);
      case INT:
        return new IntInputManager((Integer) currentValue);
      case BOOLEAN:
        return new BooleanInputManager((Boolean) currentValue);
      case STRING:
        return new TextInputManager(property, editModel, (String) currentValue);
      case ENTITY:
        return new EntityInputManager((Property.ForeignKeyProperty) property, editModel, (Entity) currentValue);
    }

    throw new IllegalArgumentException("Unsupported property type: " + property.getPropertyType());
  }

  private JPanel createButtonPanel() {
    final JPanel ret = new JPanel(new GridLayout(1,2,5,5));
    ret.add(okButton = createButton(Messages.get(Messages.OK), Messages.get(Messages.OK_MNEMONIC), JOptionPane.OK_OPTION));
    ret.add(createButton(Messages.get(Messages.CANCEL), Messages.get(Messages.CANCEL_MNEMONIC), JOptionPane.CANCEL_OPTION));

    return ret;
  }

  private JButton createButton(final String caption, final String mnemonic, final int option) {
    final JButton ret = new JButton(new AbstractAction(caption) {
      public void actionPerformed(final ActionEvent e) {
        buttonValue = option;
        evtButtonClicked.fire();
      }
    });
    ret.setMnemonic(mnemonic.charAt(0));

    return ret;
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

  public static class DateInputManager extends InputManager {
    public DateInputManager(final Date currentValue, final SimpleDateFormat dateFormat) {
      super(EntityUiUtil.createDateInputPanel(currentValue, dateFormat));
    }

    @Override
    protected Object getValue() {
      try {
        final String dateText = ((DateInputPanel) getInputComponent()).getInputField().getText();
        if (!dateText.contains("_"))
          return new Timestamp(((DateInputPanel) getInputComponent()).getDateFormat().parse(dateText).getTime());
        else
          return null;
      }
      catch (ParseException e) {
        throw new RuntimeException("Wrong date format "
                + ((DateInputPanel) getInputComponent()).getDateFormat().toPattern() + " expected");
      }
    }
  }

  public static class DoubleInputManager extends InputManager {
    public DoubleInputManager(final Double currentValue) {
      super(new DoubleField());
      if (currentValue != null)
        ((DoubleField) getInputComponent()).setDouble(currentValue);
    }

    @Override
    protected Object getValue() {
      return ((DoubleField) getInputComponent()).getDouble();
    }
  }

  public static class IntInputManager extends InputManager {
    public IntInputManager(final Integer currentValue) {
      super(new IntField());
      if (currentValue != null)
        ((IntField) getInputComponent()).setInt(currentValue);
    }

    @Override
    protected Object getValue() {
      return ((IntField) getInputComponent()).getInt();
    }
  }

  public static class BooleanInputManager extends InputManager {
    public BooleanInputManager(final Boolean currentValue) {
      super(new JComboBox(new BooleanComboBoxModel()));
      if (currentValue != null)
        ((JComboBox) getInputComponent()).setSelectedItem(currentValue);
    }

    @Override
    protected Object getValue() {
      return ((ItemComboBoxModel.Item) ((JComboBox) getInputComponent()).getModel().getSelectedItem()).getItem();
    }
  }

  public static class EntityInputManager extends InputManager {
    public EntityInputManager(final Property.ForeignKeyProperty foreignKeyProperty, final EntityEditModel editModel,
                              final Entity currentValue) {
      super(createEntityField(foreignKeyProperty, editModel, currentValue));
    }

    @Override
    protected Object getValue() {
      if (getInputComponent() instanceof JComboBox) {
        if (((JComboBox) getInputComponent()).getSelectedIndex() == 0)
          return null;

        return ((JComboBox) getInputComponent()).getSelectedItem();
      }
      else {//EntityLookupField
        final EntityLookupField lookupField = (EntityLookupField) getInputComponent();
        if (lookupField.getModel().getSelectedEntities().size() == 0)
          return null;

        return lookupField.getModel().getSelectedEntities().get(0);
      }
    }

    private static JComponent createEntityField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                final EntityEditModel editModel, final Object currentValue) {
      if (!EntityRepository.isLargeDataset(foreignKeyProperty.getReferencedEntityID())) {
        final EntityComboBoxModel model = editModel.createEntityComboBoxModel(foreignKeyProperty);
        if (model.getNullValueItem() == null)
          model.setNullValueItem("-");
        model.refresh();
        if (currentValue != null)
          model.setSelectedItem(currentValue);

        return new JComboBox(model);
      }
      else {
        final String[] searchPropertyIds = EntityRepository.getEntitySearchPropertyIDs(foreignKeyProperty.getReferencedEntityID());
        List<Property> searchProperties;
        if (searchPropertyIds != null) {
          searchProperties = EntityRepository.getProperties(foreignKeyProperty.getReferencedEntityID(), searchPropertyIds);
        }
        else {//use all string properties
          final Collection<Property> properties =
                  EntityRepository.getDatabaseProperties(foreignKeyProperty.getReferencedEntityID());
          searchProperties = new ArrayList<Property>();
          for (final Property property : properties)
            if (property.getPropertyType() == Type.STRING)
              searchProperties.add(property);
        }
        if (searchProperties.size() == 0)
          throw new RuntimeException("No searchable properties found for entity: " + foreignKeyProperty.getReferencedEntityID());

        final EntityLookupField field = new EntityLookupField(editModel.createEntityLookupModel(
                foreignKeyProperty.getReferencedEntityID(), null, searchProperties));
        if (currentValue != null)
          field.getModel().setSelectedEntity((Entity) currentValue);

        return field;
      }
    }
  }

  public static class TextInputManager extends InputManager {
    public TextInputManager(final Property property, final EntityEditModel editModel, final String currentValue) {
      super(createTextInputPanel(property, editModel, currentValue));
    }

    @Override
    protected Object getValue() {
      return ((TextInputPanel) getInputComponent()).getText();
    }

    private static TextInputPanel createTextInputPanel(final Property property, final EntityEditModel editModel,
                                                       final Object currentValue) {
      final JTextField txtField = new JTextField(currentValue != null ? currentValue.toString() : "");
      txtField.setColumns(16);
      EntityUiUtil.addLookupDialog(txtField, editModel.getEntityID(), property, editModel.getDbProvider());

      return new TextInputPanel(txtField, property.getCaption());
    }
  }
}
