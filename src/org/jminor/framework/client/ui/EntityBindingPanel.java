/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.State;
import org.jminor.common.model.formats.DateMaskFormat;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.model.EntityRepository;
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

public abstract class EntityBindingPanel extends JPanel {

  /**
   * @return the EntityModel instance to bind controls to
   */
  public abstract EntityModel getModel();

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * The default layout of the resulting panel is with the label on top and <code>inputComponent</code> below.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @return a panel containing a label and a component
   */
  protected final JPanel createControlPanel(final String propertyID, final JComponent inputComponent) {
    return createControlPanel(propertyID, inputComponent, true);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @return a panel containing a label and a component
   */
  protected final JPanel createControlPanel(final String propertyID, final JComponent inputComponent,
                                            final boolean labelOnTop) {
    return createControlPanel(propertyID, inputComponent, labelOnTop, 0, 0);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @param hgap the horizontal gap between components
   * @param vgap the vertical gap between components
   * @return a panel containing a label and a component
   */
  protected final JPanel createControlPanel(final String propertyID, final JComponent inputComponent,
                                            final boolean labelOnTop, final int hgap, final int vgap) {
    return createControlPanel(propertyID, inputComponent, labelOnTop, hgap, vgap, JLabel.LEADING);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @param hgap the horizontal gap between components
   * @param vgap the vertical gap between components
   * @param labelAlignment the text alignment to use for the label
   * @return a panel containing a label and a component
   */
  protected final JPanel createControlPanel(final String propertyID, final JComponent inputComponent,
                                            final boolean labelOnTop, final int hgap, final int vgap,
                                            final int labelAlignment) {
    return createControlPanel(createLabel(propertyID, labelAlignment), inputComponent, labelOnTop, hgap, vgap);
  }

  /**
   * Creates a panel containing a label component and the <code>inputComponent</code>.
   * @param labelComponent the label component
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @param hgap the horizontal gap between components
   * @param vgap the vertical gap between components
   * @return a panel containing a label and a component
   */
  protected final JPanel createControlPanel(final JComponent labelComponent, final JComponent inputComponent,
                                            final boolean labelOnTop, final int hgap, final int vgap) {
    final JPanel ret = new JPanel(labelOnTop ?
            new GridLayout(2, 1, hgap, vgap) : new FlowLayout(FlowLayout.LEADING, hgap, vgap));
    if (labelComponent instanceof JLabel)
      ((JLabel)labelComponent).setLabelFor(inputComponent);
    ret.add(labelComponent);
    ret.add(inputComponent);

    return ret;
  }

  /**
   * Creates a JTextArea component bound to the property identified by <code>propertyID </code>.
   * @param propertyID the ID of the property to bind
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyID) {
    return createTextArea(propertyID, -1, -1);
  }

  /**
   * Creates a JTextArea component bound to the property identified by <code>propertyID </code>.
   * @param propertyID the ID of the property to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyID, final int rows, final int columns) {
    return FrameworkUiUtil.createTextArea(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            getModel(), rows, columns);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID) {
    return createTextInputPanel(propertyID, LinkType.READ_WRITE);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID, final LinkType linkType) {
    return createTextInputPanel(propertyID, linkType, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @param linkType the property LinkType
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID, final LinkType linkType,
                                                      final boolean immediateUpdate) {
    return createTextInputPanel(propertyID, linkType, immediateUpdate, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param buttonFocusable specifies whether the edit button should be focusable.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID, final LinkType linkType,
                                                      final boolean immediateUpdate, final boolean buttonFocusable) {
    return createTextInputPanel(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID), linkType,
            immediateUpdate, buttonFocusable);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final Property property, final LinkType linkType,
                                                      final boolean immediateUpdate) {
    return createTextInputPanel(property, linkType, immediateUpdate, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param buttonFocusable specifies whether the edit button should be focusable.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final Property property, final LinkType linkType,
                                                      final boolean immediateUpdate, final boolean buttonFocusable) {
    return new TextInputPanel(createTextField(property, linkType, null, immediateUpdate), property.getCaption(),
            null, buttonFocusable);
  }

  /**
   * Creates a new DateInputPanel using the default short date format, bound to the property
   * identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @return a DateInputPanel using the default short date format
   * @see org.jminor.framework.Configuration#DEFAULT_SHORT_DATE_FORMAT
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID) {
    return createDateInputPanel(propertyID, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateMaskFormat the format to use for masking the input field
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final DateMaskFormat dateMaskFormat) {
    return createDateInputPanel(propertyID, dateMaskFormat, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel using the default short date format
   * @see org.jminor.framework.Configuration#DEFAULT_SHORT_DATE_FORMAT
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final boolean includeButton) {
    return createDateInputPanel(propertyID,
            new DateMaskFormat((String) Configuration.getValue(Configuration.DEFAULT_SHORT_DATE_FORMAT)),
            includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateMaskFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final DateMaskFormat dateMaskFormat,
                                                      final boolean includeButton) {
    return createDateInputPanel(propertyID, dateMaskFormat, includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateMaskFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final DateMaskFormat dateMaskFormat,
                                                      final boolean includeButton, final State enabledState) {
    return createDateInputPanel(propertyID, dateMaskFormat, includeButton, enabledState, LinkType.READ_WRITE);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateMaskFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param linkType the property link type
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final DateMaskFormat dateMaskFormat,
                                                      final boolean includeButton, final State enabledState,
                                                      final LinkType linkType) {
    return createDateInputPanel(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            dateMaskFormat, includeButton, enabledState, linkType);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateMaskFormat the format to use for masking the input field
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final DateMaskFormat dateMaskFormat) {
    return createDateInputPanel(property, dateMaskFormat, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateMaskFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final DateMaskFormat dateMaskFormat,
                                                      final boolean includeButton) {
    return createDateInputPanel(property, dateMaskFormat, includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateMaskFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final DateMaskFormat dateMaskFormat,
                                                      final boolean includeButton, final State enabledState) {
    return createDateInputPanel(property, dateMaskFormat, includeButton, enabledState, LinkType.READ_WRITE);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateMaskFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param linkType the property link type
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final DateMaskFormat dateMaskFormat,
                                                      final boolean includeButton, final State enabledState,
                                                      final LinkType linkType) {
    return FrameworkUiUtil.createDateInputPanel(property, getModel(), dateMaskFormat, linkType, includeButton, enabledState);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID) {
    return createTextField(propertyID, LinkType.READ_WRITE);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final LinkType linkType) {
    return createTextField(propertyID, linkType, true);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate) {
    return createTextField(propertyID, linkType, immediateUpdate, null);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate, final String maskString) {
    return createTextField(propertyID, linkType, immediateUpdate, maskString, null);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate, final String maskString,
                                             final State enabledState) {
    return createTextField(propertyID, linkType, immediateUpdate, maskString, enabledState, false);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @param valueIncludesLiteralCharacters only applicable if <code>maskString</code> is specified
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final LinkType linkType,
                                             final boolean immediateUpdate, final String maskString,
                                             final State enabledState, final boolean valueIncludesLiteralCharacters) {
    return createTextField(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            linkType, maskString, immediateUpdate, enabledState, valueIncludesLiteralCharacters);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property) {
    return createTextField(property, LinkType.READ_WRITE);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param linkType the property link type
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final LinkType linkType) {
    return createTextField(property, linkType, null, true);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String maskString, final boolean immediateUpdate) {
    return createTextField(property, linkType, maskString, immediateUpdate, null);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the ID of the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String maskString, final boolean immediateUpdate,
                                             final State enabledState) {
    return createTextField(property, linkType, maskString, immediateUpdate, enabledState, false);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param linkType the property link type
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @param valueIncludesLiteralCharacters only applicable if <code>maskString</code> is specified
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final LinkType linkType,
                                             final String maskString, final boolean immediateUpdate,
                                             final State enabledState, final boolean valueIncludesLiteralCharacters) {
    return FrameworkUiUtil.createTextField(property, getModel(), linkType, maskString, immediateUpdate,
            null, enabledState, valueIncludesLiteralCharacters);
  }

  /**
   * Creates a JCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyID) {
    return createCheckBox(propertyID, null);
  }

  /**
   * Creates a JCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyID, final State enabledState) {
    return createCheckBox(propertyID, enabledState, true);
  }

  /**
   * Creates a JCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyID, final State enabledState,
                                           final boolean includeCaption) {
    return createCheckBox(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID), enabledState, includeCaption);
  }

  /**
   * Creates a JCheckBox bound to the given property
   * @param property the property to bind
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final Property property) {
    return createCheckBox(property, null);
  }

  /**
   * Creates a JCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final Property property, final State enabledState) {
    return createCheckBox(property, enabledState, true);
  }

  /**
   * Creates a JCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final Property property, final State enabledState,
                                           final boolean includeCaption) {
    return FrameworkUiUtil.createCheckBox(property, getModel(), enabledState, includeCaption);
  }

  /**
   * Create a JComboBox for the property identified by <code>propertyID</code>, containing
   * values for the boolean values: true, false, null
   * @param propertyID the ID of the property to bind
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final String propertyID) {
    return createBooleanComboBox(propertyID, null);
  }

  /**
   * Create a JComboBox for the property identified by <code>propertyID</code>, containing
   * values for the boolean values: true, false, null
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final String propertyID, final State enabledState) {
    return createBooleanComboBox(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),enabledState);
  }

  /**
   * Create a JComboBox for the given property, containing
   * values for the boolean values: true, false, null
   * @param property the property to bind
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final Property property) {
    return createBooleanComboBox(property, null);
  }

  /**
   * Create a JComboBox for the given property, containing
   * values for the boolean values: true, false, null
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final Property property, final State enabledState) {
    return FrameworkUiUtil.createBooleanComboBox(property, getModel(), enabledState);
  }

  /**
   * Creates a SteppedComboBox bound to the property identitied by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(propertyID, comboBoxModel, maximumMatch, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identitied by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound the the property
   * @see MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final State enabledState) {
    return createComboBox(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            comboBoxModel, maximumMatch, enabledState);
  }

/**
   * Creates a SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(property, comboBoxModel, maximumMatch, null);
  }

  /**
   * Creates a SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound the the property
   * @see MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final State enabledState) {
    final SteppedComboBox ret = FrameworkUiUtil.createComboBox(property, getModel(), comboBoxModel, enabledState);
    if (maximumMatch)
      MaximumMatch.enable(ret);

    return ret;
  }

  /**
   * Creates an editable SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final String propertyID, final ComboBoxModel comboBoxModel) {
    return createEditableComboBox(propertyID, comboBoxModel, null);
  }

  /**
   * Creates an editable SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param enabledState a state for controlling the enabled state of the component
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                         final State enabledState) {
    return createEditableComboBox(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            comboBoxModel, enabledState);
  }

  /**
   * Creates an editable SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param enabledState a state for controlling the enabled state of the component
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                         final State enabledState) {
    return FrameworkUiUtil.createComboBox(property, getModel(), comboBoxModel, enabledState, true);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID) {
    return createPropertyComboBox(propertyID, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final State enabledState) {
    return createPropertyComboBox(propertyID, enabledState, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param nullValue the value used to represent a null value, shown at the top of the combo box value list
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final State enabledState,
                                                         final Object nullValue) {
    return createPropertyComboBox(propertyID, enabledState, nullValue, false);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param nullValue the value used to represent a null value, shown at the top of the combo box value list
   * @param editable true if the combo box should be editable, only works with combo boxes based on Type.STRING properties
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final State enabledState,
                                                         final Object nullValue, final boolean editable) {
    return createPropertyComboBox(EntityRepository.get().getProperty(getModel().getEntityID(), propertyID),
            enabledState, nullValue, editable);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property property) {
    return createPropertyComboBox(property, null);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property property, final State enabledState) {
    return createPropertyComboBox(property, enabledState, null);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param nullValue the value used to represent a null value, shown at the top of the combo box value list
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property property, final State enabledState,
                                                         final Object nullValue) {
    return createPropertyComboBox(property, enabledState, nullValue, false);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param nullValue the value used to represent a null value, shown at the top of the combo box value list
   * @param editable true if the combo box should be editable, only works with combo boxes based on Type.STRING properties
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property property, final State enabledState,
                                                         final Object nullValue, final boolean editable) {
    return FrameworkUiUtil.createPropertyComboBox(property, getModel(), null, enabledState, nullValue, editable);
  }

  /**
   * Creates a EntityComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return a EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final String propertyID) {
    return createEntityComboBox(propertyID, null);
  }

  /**
   * Creates a EntityComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final String propertyID, final State enabledState) {
    return createEntityComboBox((Property.EntityProperty)
            EntityRepository.get().getProperty(getModel().getEntityID(), propertyID), enabledState);
  }

  /**
   * Creates an EntityLookupField bound to the property identified by <code>propertyID</code>, the property
   * must be an Property.EntityProperty
   * @param propertyID the ID of the property to bind
   * @param searchPropertyIDs the IDs of the properties to use in the lookup
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final String propertyID, final String... searchPropertyIDs) {
    final Property.EntityProperty property = EntityRepository.get().getEntityProperty(getModel().getEntityID(), propertyID);
    return createEntityLookupField(property, searchPropertyIDs);
  }

  /**
   * Creates an EntityLookupField bound to the given entity property
   * @param property the property to bind
   * @param searchPropertyIDs the IDs of the properties to use in the lookup
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final Property.EntityProperty property, final String... searchPropertyIDs) {
    return FrameworkUiUtil.createEntityLookupField(property, getModel(), searchPropertyIDs);
  }

  /**
   * Creates an EntityComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param newRecordPanelProvider an EntityPanelProvider specifying the EntityPanel/EntityModel
   * combination used to create new instances of the entity this EntityComboBox is based on
   * @param newButtonFocusable if true then the button included on the panel provided by this
   * EntityComboBox is focusable
   * @return an EntityComboBox bound to the property
   * @see org.jminor.framework.client.ui.EntityComboBox#createPanel()
   */
  protected final EntityComboBox createEntityComboBox(final String propertyID,
                                                      final EntityPanelProvider newRecordPanelProvider,
                                                      final boolean newButtonFocusable) {
    return createEntityComboBox(EntityRepository.get().getEntityProperty(getModel().getEntityID(),
            propertyID), newRecordPanelProvider, newButtonFocusable, null);
  }

  /**
   * Creates an EntityComboBox bound to the given entity property
   * @param property the property to bind
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final Property.EntityProperty property) {
    return createEntityComboBox(property, null);
  }

  /**
   * Creates an EntityComboBox bound to the given entity property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final Property.EntityProperty property, final State enabledState) {
    return createEntityComboBox(property, null, false, enabledState);
  }

  /**
   * Creates an EntityComboBox bound to the given entity property
   * @param property the property to bind
   * @param newRecordPanelProvider an EntityPanelProvider specifying the EntityPanel/EntityModel
   * combination used to create new instances of the entity this EntityComboBox is based on
   * @param newButtonFocusable if true then the button included on the panel provided by this
   * EntityComboBox is focusable
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityComboBox bound to the property
   * @see org.jminor.framework.client.ui.EntityComboBox#createPanel()
   */
  protected final EntityComboBox createEntityComboBox(final Property.EntityProperty property,
                                                      final EntityPanelProvider newRecordPanelProvider,
                                                      final boolean newButtonFocusable, final State enabledState) {
    return FrameworkUiUtil.createEntityComboBox(property, getModel(), newRecordPanelProvider, newButtonFocusable, enabledState);
  }

  /**
   * Creates an uneditable JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createEntityField(final String propertyID) {
    return createEntityField(EntityRepository.get().getEntityProperty(getModel().getEntityID(), propertyID));
  }

  /**
   * Creates an uneditable JTextField bound to the given property
   * @param property the property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createEntityField(final Property.EntityProperty property) {
    return FrameworkUiUtil.createEntityField(property, getModel());
  }

  /**
   * Creates a JPanel containing an uneditable JTextField bound to the property identified by <code>propertyID</code>
   * and a button for selecting an Entity to set as the property value
   * @param propertyID the ID of the property to bind
   * @param lookupModel an EntityTableModel to use when looking up entities
   * @return an uneditable JTextField bound to the property
   */
  protected final JPanel createEntityFieldPanel(final String propertyID, final EntityTableModel lookupModel) {
    return createEntityFieldPanel((Property.EntityProperty)
            EntityRepository.get().getProperty(getModel().getEntityID(), propertyID), lookupModel);
  }

  /**
   * Creates a JPanel containing an uneditable JTextField bound to the given property identified
   * and a button for selecting an Entity to set as the property value
   * @param property the property to bind
   * @param lookupModel an EntityTableModel to use when looking up entities
   * @return an uneditable JTextField bound to the property
   */
  protected final JPanel createEntityFieldPanel(final Property.EntityProperty property, final EntityTableModel lookupModel) {
    return FrameworkUiUtil.createEntityFieldPanel(property, getModel(), lookupModel);
  }

  /**
   * Creates a JLabel with a caption from the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property from which to retrieve the caption
   * @return a JLabel for the given property
   */
  protected final JLabel createLabel(final String propertyID) {
    return createLabel(propertyID, JLabel.LEFT);
  }

  /**
   * Creates a JLabel with a caption from the given property identified by <code>propertyID</code>
   * @param propertyID the ID of the property from which to retrieve the caption
   * @param horizontalAlignment the horizontal text alignment
   * @return a JLabel for the given property
   */
  protected final JLabel createLabel(final String propertyID, final int horizontalAlignment) {
    final String text = EntityRepository.get().getProperty(getModel().getEntityID(), propertyID).getCaption();
    if (text == null || text.length() == 0)
      throw new IllegalArgumentException("Cannot create a label for property: " + propertyID + ", no caption");

    return new JLabel(text, horizontalAlignment);
  }
}
