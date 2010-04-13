/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.ui.input.InputValueProvider;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class for editing a property value for one or more entities at a time.
 */
public class PropertyEditPanel extends JPanel {

  private final Event evtButtonClicked = new Event();

  private final InputValueProvider inputManager;

  private JButton okButton;
  private int buttonValue = -Integer.MAX_VALUE;

  /**
   * Instantiates a new PropertyEditPanel
   * @param property the property to edit
   */
  public PropertyEditPanel(final Property property) {
    this(property, null);
  }

  /**
   * Instantiates a new PropertyEditPanel
   * @param property the property to edit
   * @param inputManager the InputManager to use, if no InputManager is specified a default one is used
   */
  public PropertyEditPanel(final Property property, final InputValueProvider inputManager) {
    if (property == null)
      throw new IllegalArgumentException("Property must be specified");

    this.inputManager = inputManager;
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

  public Event eventButtonClicked() {
    return evtButtonClicked;
  }

  protected void initUI(final String propertyID) {
    setLayout(new BorderLayout(5,5));
    setBorder(BorderFactory.createTitledBorder(propertyID));
    add(inputManager.getInputComponent(), BorderLayout.CENTER);
    final JPanel btnBase = new JPanel(new FlowLayout(FlowLayout.CENTER));
    btnBase.add(createButtonPanel());
    add(btnBase, BorderLayout.SOUTH);
  }

  private JPanel createButtonPanel() {
    final JPanel panel = new JPanel(new GridLayout(1,2,5,5));
    panel.add(okButton = createButton(Messages.get(Messages.OK), Messages.get(Messages.OK_MNEMONIC), JOptionPane.OK_OPTION));
    panel.add(createButton(Messages.get(Messages.CANCEL), Messages.get(Messages.CANCEL_MNEMONIC), JOptionPane.CANCEL_OPTION));

    return panel;
  }

  private JButton createButton(final String caption, final String mnemonic, final int option) {
    final JButton button = new JButton(new AbstractAction(caption) {
      public void actionPerformed(final ActionEvent e) {
        buttonValue = option;
        evtButtonClicked.fire();
      }
    });
    button.setMnemonic(mnemonic.charAt(0));

    return button;
  }

  /**
   * A InputManager implementation for Entity values.
   */
  public static class EntityInputProvider extends InputValueProvider {
    public EntityInputProvider(final Property.ForeignKeyProperty foreignKeyProperty, final EntityEditModel editModel,
                               final Entity currentValue) {
      super(createEntityField(foreignKeyProperty, editModel, currentValue));
    }

    @Override
    public Object getValue() {
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
        if (model.getNullValueString() == null)
          model.setNullValueString("-");
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
}
