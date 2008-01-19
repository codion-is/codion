/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.State;
import org.jminor.common.model.formats.AbstractDateMaskFormat;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.Property;

import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public abstract class EntityBindingFactory extends JPanel {

  /**
   * @return Value for property 'model'.
   */
  public abstract EntityModel getModel();

  public JPanel getControlPanel(final String propertyName, final JComponent inputComponent) {
    return getControlPanel(propertyName, inputComponent, true);
  }

  public JPanel getControlPanel(final String propertyName, final JComponent inputComponent,
                                final boolean labelOnTop) {
    return getControlPanel(propertyName, inputComponent, labelOnTop, 0, 0);
  }

  public JPanel getControlPanel(final String propertyName, final JComponent inputComponent,
                                final boolean labelOnTop, final int hgap, final int vgap) {
    return getControlPanel(propertyName, inputComponent, labelOnTop, hgap, vgap, JLabel.LEADING);
  }

  public JPanel getControlPanel(final String propertyName, final JComponent inputComponent,
                                final boolean labelOnTop, final int hgap, final int vgap, final int labelAlignment) {
    final JPanel ret = new JPanel(labelOnTop ?
            new GridLayout(2, 1, hgap, vgap) : new FlowLayout(FlowLayout.LEADING, hgap, vgap));
    ret.add(createLabel(propertyName, labelAlignment));
    ret.add(inputComponent);

    return ret;
  }

  protected final JTextArea createTextArea(final String propertyName) {
    return createTextArea(propertyName, -1, -1);
  }

  protected final JTextArea createTextArea(final String propertyName, final int rows, final int columns) {
    return FrameworkUiUtil.createTextArea(Entity.repository.getProperty(getModel().getEntityID(), propertyName),
            getModel(), rows, columns);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final String propertyName,
                                                             final AbstractDateMaskFormat dateMaskFormat) {
    return createDateFieldPanel(propertyName, dateMaskFormat, true);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final String propertyName,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton) {
    return createDateFieldPanel(propertyName, dateMaskFormat, includeButton, null);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final String propertyName,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton,
                                                             final State enabledState) {
    return createDateFieldPanel(Entity.repository.getProperty(getModel().getEntityID(), propertyName),
            dateMaskFormat, includeButton, enabledState);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final Property property,
                                                             final AbstractDateMaskFormat dateMaskFormat) {
    return createDateFieldPanel(property, dateMaskFormat, true);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final Property property,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton) {
    return createDateFieldPanel(property, dateMaskFormat, includeButton, null);
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final Property property,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton,
                                                             final State enabledState) {
    return createDateFieldPanel(property, dateMaskFormat, includeButton, enabledState, getModel());
  }

  protected final UiUtil.DateInputPanel createDateFieldPanel(final Property property,
                                                             final AbstractDateMaskFormat dateMaskFormat,
                                                             final boolean includeButton,
                                                             final State enabledState,
                                                             final EntityModel entityModel) {
    return FrameworkUiUtil.createDateFieldPanel(property, entityModel, dateMaskFormat,
            LinkType.READ_WRITE, includeButton, enabledState);
  }

  protected final JTextField createTextField(final String propertyName) {
    return createTextField(propertyName, LinkType.READ_WRITE);
  }

  protected final JTextField createTextField(final String propertyName, final LinkType linkType) {
    return createTextField(propertyName, linkType, true);
  }

  protected final JTextField createTextField(final String propertyName, final LinkType linkType,
                                             final boolean immediateUpdate) {
    return createTextField(propertyName, linkType, immediateUpdate, null);
  }

  protected final JTextField createTextField(final String propertyName, final LinkType linkType,
                                             final boolean immediateUpdate, final String formatString) {
    return createTextField(propertyName, linkType, immediateUpdate, formatString, null);
  }

  protected final JTextField createTextField(final String propertyName, final LinkType linkType,
                                             final boolean immediateUpdate, final String formatString,
                                             final State enabledState) {
    return createTextField(propertyName, linkType, immediateUpdate, formatString, enabledState, false);
  }

  protected final JTextField createTextField(final String propertyName, final LinkType linkType,
                                             final boolean immediateUpdate, final String formatString,
                                             final State enabledState, final boolean valueIncludesLiteralCharacters) {
    return createTextField(Entity.repository.getProperty(getModel().getEntityID(), propertyName),
            linkType, formatString, immediateUpdate, enabledState, valueIncludesLiteralCharacters);
  }

  protected final JTextField createTextField(final Property property) {
    return createTextField(property, LinkType.READ_WRITE);
  }

  protected final JTextField createTextField(final Property property, final LinkType linkType) {
    return createTextField(property, linkType, null, true);
  }

  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String formatString, final boolean immediateUpdate) {
    return createTextField(property, linkType, formatString, immediateUpdate, null);
  }

  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String formatString, final boolean immediateUpdate,
                                             final State enabledState) {
    return createTextField(property, linkType, formatString, immediateUpdate, enabledState, false);
  }

  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String formatString, final boolean immediateUpdate,
                                             final State enabledState, final boolean valueContainsLiteralCharacters) {
    return FrameworkUiUtil.createTextField(property, getModel(), linkType, formatString, immediateUpdate,
            null, enabledState, valueContainsLiteralCharacters);
  }

  protected final JCheckBox createCheckBox(final String propertyName) {
    return createCheckBox(Entity.repository.getProperty(getModel().getEntityID(), propertyName));
  }

  protected final JCheckBox createCheckBox(final String propertyName, final State enabledState) {
    return createCheckBox(propertyName, enabledState, true);
  }

  protected final JCheckBox createCheckBox(final String propertyName, final State enabledState,
                                           final boolean includeCaption) {
    return createCheckBox(Entity.repository.getProperty(getModel().getEntityID(), propertyName), enabledState, includeCaption);
  }

  protected final JCheckBox createCheckBox(final Property property) {
    return createCheckBox(property, null);
  }

  protected final JCheckBox createCheckBox(final Property property, final State enabledState) {
    return createCheckBox(property, enabledState, true);
  }

  protected final JCheckBox createCheckBox(final Property property, final State enabledState,
                                           final boolean includeCaption) {
    return FrameworkUiUtil.createCheckBox(property, getModel(), enabledState, includeCaption);
  }

  protected final JComboBox createBooleanComboBox(final String propertyName) {
    return createBooleanComboBox(propertyName, null);
  }

  protected final JComboBox createBooleanComboBox(final String propertyName, final State enabledState) {
    return createBooleanComboBox(Entity.repository.getProperty(getModel().getEntityID(), propertyName),enabledState);
  }

  protected final JComboBox createBooleanComboBox(final Property property) {
    return createBooleanComboBox(property, null);
  }

  protected final JComboBox createBooleanComboBox(final Property property, final State enabledState) {
    return FrameworkUiUtil.createBooleanComboBox(property, getModel(), enabledState);
  }

  protected final SteppedComboBox createComboBox(final String propertyName, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(propertyName, comboBoxModel, maximumMatch, null);
  }

  protected final SteppedComboBox createComboBox(final String propertyName, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final State enabledState) {
    return createComboBox(Entity.repository.getProperty(getModel().getEntityID(), propertyName),
            comboBoxModel, maximumMatch, enabledState);
  }

  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(property, comboBoxModel, maximumMatch, null);
  }

  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final State enabledState) {
    final SteppedComboBox ret = FrameworkUiUtil.createComboBox(property, getModel(), comboBoxModel, enabledState);
    if (maximumMatch)
      MaximumMatch.enable(ret);

    return ret;
  }

  protected final SteppedComboBox createEditableComboBox(final String propertyName, final ComboBoxModel comboBoxModel) {
    return createEditableComboBox(propertyName, comboBoxModel, null);
  }

  protected final SteppedComboBox createEditableComboBox(final String propertyName, final ComboBoxModel comboBoxModel,
                                                         final State enabledState) {
    return createEditableComboBox(Entity.repository.getProperty(getModel().getEntityID(), propertyName),
            comboBoxModel, enabledState);
  }

  protected final SteppedComboBox createEditableComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                         final State enabledState) {
    return FrameworkUiUtil.createComboBox(property, getModel(), comboBoxModel, enabledState, true);
  }

  protected final SteppedComboBox createPropertyComboBox(final String propertyName) {
    return createPropertyComboBox(propertyName, null);
  }

  protected final SteppedComboBox createPropertyComboBox(final String propertyName, final State state) {
    return createPropertyComboBox(propertyName, state, null);
  }

  protected final SteppedComboBox createPropertyComboBox(final String propertyName, final State state,
                                                         final Object nullValue) {
    return createPropertyComboBox(propertyName, state, nullValue, false);
  }

  protected final SteppedComboBox createPropertyComboBox(final String propertyName, final State state,
                                                         final Object nullValue, final boolean editable) {
    return createPropertyComboBox(Entity.repository.getProperty(getModel().getEntityID(), propertyName),
            state, nullValue, editable);
  }

  protected final SteppedComboBox createPropertyComboBox(final Property property) {
    return createPropertyComboBox(property, null);
  }

  protected final SteppedComboBox createPropertyComboBox(final Property property, final State state) {
    return createPropertyComboBox(property, state, null);
  }

  protected final SteppedComboBox createPropertyComboBox(final Property property, final State state,
                                                         final Object nullValue) {
    return createPropertyComboBox(property, state, nullValue, false);
  }

  protected final SteppedComboBox createPropertyComboBox(final Property property, final State state,
                                                         final Object nullValue, final boolean editable) {
    return FrameworkUiUtil.createPropertyComboBox(property, getModel(), null, state, nullValue, editable);
  }

  protected final EntityComboBox createEntityComboBox(final String propertyName) {
    return createEntityComboBox(propertyName, null);
  }

  protected final EntityComboBox createEntityComboBox(final String propertyName, final State enabledState) {
    return createEntityComboBox((Property.EntityProperty)
            Entity.repository.getProperty(getModel().getEntityID(), propertyName), enabledState);
  }

  protected final JTextField createEntityField(final String propertyName) {
    return createEntityField(Entity.repository.getProperty(getModel().getEntityID(), propertyName));
  }

  protected final JPanel createEntityFieldPanel(final String propertyName, final EntityTableModel lookupModel) {
    return createEntityFieldPanel((Property.EntityProperty)
            Entity.repository.getProperty(getModel().getEntityID(), propertyName), lookupModel);
  }

  protected final EntityComboBox createEntityComboBox(final Property.EntityProperty property) {
    return createEntityComboBox(property, null);
  }

  protected final EntityComboBox createEntityComboBox(final Property.EntityProperty property, final State enabledState) {
    return createEntityComboBox(property, null, false, enabledState);
  }

  protected final EntityComboBox createEntityComboBox(final String propertyName,
                                                      final EntityPanelInfo appInfo,
                                                      final boolean newButtonFocusable) {
    return createEntityComboBox((Property.EntityProperty) Entity.repository.getProperty(getModel().getEntityID(),
            propertyName), appInfo, newButtonFocusable, null);
  }

  protected final EntityComboBox createEntityComboBox(final Property.EntityProperty property,
                                                      final EntityPanelInfo appInfo,
                                                      final boolean newButtonFocusable, final State enabledState) {
    return FrameworkUiUtil.createEntityComboBox(property, getModel(), appInfo, newButtonFocusable, enabledState);
  }

  protected final JTextField createEntityField(final Property property) {
    return FrameworkUiUtil.createEntityField(property, getModel());
  }

  protected final JPanel createEntityFieldPanel(final Property.EntityProperty property, final EntityTableModel lookupModel) {
    return FrameworkUiUtil.createEntityFieldPanel(property, getModel(), lookupModel);
  }

  protected final JLabel createLabel(final String propertyName) {
    return createLabel(propertyName, JLabel.LEFT);
  }

  protected final JLabel createLabel(final String propertyName, final int horizontalAlignment) {
    final String text = Entity.repository.getProperty(getModel().getEntityID(), propertyName).getCaption();
    if (text == null || text.length() == 0)
      throw new IllegalArgumentException("Cannot create a label for property: " + propertyName + ", no caption");

    return new JLabel(text, horizontalAlignment);
  }
}
