/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;
import org.jminor.common.model.checkbox.TristateButtonModel;
import org.jminor.common.model.combobox.BooleanComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.checkbox.TristateCheckBox;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.common.ui.textfield.TextFieldPlus;
import org.jminor.common.ui.valuemap.AbstractValueMapLink;
import org.jminor.common.ui.valuemap.BooleanValueLink;
import org.jminor.common.ui.valuemap.ComboBoxValueLink;
import org.jminor.common.ui.valuemap.DateValueLink;
import org.jminor.common.ui.valuemap.DoubleValueLink;
import org.jminor.common.ui.valuemap.FormattedValueLink;
import org.jminor.common.ui.valuemap.IntValueLink;
import org.jminor.common.ui.valuemap.TextValueLink;
import org.jminor.common.ui.valuemap.TristateValueLink;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.event.InsertListener;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.apache.log4j.Level;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A static utility class concerned with UI related tasks.
 */
public final class EntityUiUtil {

  private static final String PROPERTY_PARAM_NAME = "property";
  private static final String EDIT_MODEL_PARAM_NAME = "editModel";

  private EntityUiUtil() {}

  public static void setLoggingLevel(final JComponent dialogParent) {
    final DefaultComboBoxModel model = new DefaultComboBoxModel(
            new Object[] {Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG});
    model.setSelectedItem(Util.getLoggingLevel());
    JOptionPane.showMessageDialog(dialogParent, new JComboBox(model),
            FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL), JOptionPane.QUESTION_MESSAGE);
    Util.setLoggingLevel((Level) model.getSelectedItem());
  }

  public static AbstractAction initializeViewImageAction(final EntityTablePanel tablePanel, final String imagePathPropertyID) {
    Util.rejectNullValue(tablePanel, "tablePanel");
    Util.rejectNullValue(imagePathPropertyID, "imagePathPropertyID");
    return new AbstractAction() {
      public void actionPerformed(final ActionEvent e) {
        try {
          final EntityTableModel tableModel = tablePanel.getEntityTableModel();
          if (!tableModel.isSelectionEmpty()) {
            final Entity selected = tableModel.getSelectedItem();
            if (!selected.isValueNull(imagePathPropertyID)) {
              UiUtil.showImage(selected.getStringValue(imagePathPropertyID), tablePanel);
            }
          }
        }
        catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    };
  }

  public static List<Entity> selectEntities(final EntityTableModel lookupModel, final Window owner,
                                            final boolean singleSelection, final String dialogTitle) throws CancelException {
    return selectEntities(lookupModel, owner, singleSelection, dialogTitle, null);
  }

  public static List<Entity> selectEntities(final EntityTableModel lookupModel, final Window owner,
                                            final boolean singleSelection, final String dialogTitle,
                                            final Dimension preferredSize) throws CancelException {
    Util.rejectNullValue(lookupModel, "lookupModel");
    final List<Entity> selected = new ArrayList<Entity>();
    final JDialog dialog = new JDialog(owner, dialogTitle);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
      public void actionPerformed(final ActionEvent e) {
        final List<Entity> entities = lookupModel.getSelectedItems();
        for (final Entity entity : entities) {
          selected.add(entity);
        }
        dialog.dispose();
      }
    };
    final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
      public void actionPerformed(final ActionEvent e) {
        selected.add(null);//hack to indicate cancel
        dialog.dispose();
      }
    };

    final EntityTablePanel entityTablePanel = new EntityTablePanel(lookupModel);
    entityTablePanel.initializePanel();
    entityTablePanel.addTableDoubleClickListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (!entityTablePanel.getEntityTableModel().isSelectionEmpty()) {
          okAction.actionPerformed(e);
        }
      }
    });
    entityTablePanel.setSearchPanelVisible(true);
    if (singleSelection) {
      entityTablePanel.getJTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    final Action searchAction = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(final ActionEvent e) {
        lookupModel.refresh();
        if (lookupModel.getRowCount() > 0) {
          lookupModel.setSelectedItemIndexes(Arrays.asList(0));
          entityTablePanel.getJTable().requestFocusInWindow();
        }
        else {
          JOptionPane.showMessageDialog(UiUtil.getParentWindow(entityTablePanel),
                  FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CRITERIA));
        }
      }
    };

    final JButton btnOk  = new JButton(okAction);
    final JButton btnCancel = new JButton(cancelAction);
    final JButton btnSearch = new JButton(searchAction);
    final String cancelMnemonic = Messages.get(Messages.CANCEL_MNEMONIC);
    final String okMnemonic = Messages.get(Messages.OK_MNEMONIC);
    final String searchMnemonic = FrameworkMessages.get(FrameworkMessages.SEARCH_MNEMONIC);
    btnOk.setMnemonic(okMnemonic.charAt(0));
    btnCancel.setMnemonic(cancelMnemonic.charAt(0));
    btnSearch.setMnemonic(searchMnemonic.charAt(0));
    UiUtil.addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, cancelAction);
    entityTablePanel.getJTable().getInputMap(
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    dialog.setLayout(new BorderLayout());
    if (preferredSize != null) {
      entityTablePanel.setPreferredSize(preferredSize);
    }
    dialog.add(entityTablePanel, BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
    buttonPanel.add(btnSearch);
    buttonPanel.add(btnOk);
    buttonPanel.add(btnCancel);
    dialog.getRootPane().setDefaultButton(btnOk);
    dialog.add(buttonPanel, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setModal(true);
    dialog.setResizable(true);
    dialog.setVisible(true);

    if (selected.size() == 1 && selected.contains(null)) {
      throw new CancelException();
    }
    else {
      return selected;
    }
  }

  /**
   * Creates a JLabel with a caption from the given property, using the default label text alignment
   * @param property the property for which to create the label
   * @return a JLabel for the given property
   * @see org.jminor.framework.Configuration#DEFAULT_LABEL_TEXT_ALIGNMENT
   */
  public static JLabel createLabel(final Property property) {
    return createLabel(property, Configuration.getIntValue(Configuration.DEFAULT_LABEL_TEXT_ALIGNMENT));
  }

  /**
   * Creates a JLabel with a caption from the given property
   * @param property the property for which to create the label
   * @param horizontalAlignment the horizontal text alignment
   * @return a JLabel for the given property
   */
  public static JLabel createLabel(final Property property, final int horizontalAlignment) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    final String caption = property.getCaption();
    Util.rejectNullValue(caption, "property.caption");
    final JLabel label = new JLabel(caption, horizontalAlignment);
    if (property.getMnemonic() != null) {
      label.setDisplayedMnemonic(property.getMnemonic());
    }

    return label;
  }

  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel) {
    return createCheckBox(property, editModel, null);
  }

  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel,
                                         final StateObserver enabledState) {
    return createCheckBox(property, editModel, enabledState, true);
  }

  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel,
                                         final StateObserver enabledState, final boolean includeCaption) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    if (!property.isBoolean()) {
      throw new RuntimeException("Boolean property required for createCheckBox");
    }

    final JCheckBox checkBox = includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox();
    new BooleanValueLink<String>(checkBox.getModel(), editModel, property.getPropertyID());
    UiUtil.linkToEnabledState(enabledState, checkBox);
    if (property.hasDescription()) {
      checkBox.setToolTipText(property.getDescription());
    }
    else {
      checkBox.setToolTipText(property.getCaption());
    }
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter(checkBox);
    }

    return checkBox;
  }

  public static TristateCheckBox createTristateCheckBox(final Property property, final EntityEditModel editModel,
                                                        final StateObserver enabledState, final boolean includeCaption) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    if (!property.isBoolean() && property.isNullable()) {
      throw new RuntimeException("Nullable boolean property required for createTristateCheckBox");
    }

    final TristateCheckBox checkBox = new TristateCheckBox(includeCaption ? property.getCaption() : null);
    new TristateValueLink<String>((TristateButtonModel) checkBox.getModel(), editModel, property.getPropertyID());
    UiUtil.linkToEnabledState(enabledState, checkBox);
    if (property.hasDescription()) {
      checkBox.setToolTipText(property.getDescription());
    }
    else {
      checkBox.setToolTipText(property.getCaption());
    }
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter(checkBox);
    }

    return checkBox;
  }

  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityEditModel editModel) {
    return createBooleanComboBox(property, editModel, null);
  }

  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityEditModel editModel,
                                                      final StateObserver enabledState) {
    final SteppedComboBox box = createComboBox(property, editModel, new BooleanComboBoxModel(), enabledState);
    box.setPopupWidth(40);

    return box;
  }

  public static EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                    final EntityEditModel editModel) {
    return createEntityComboBox(foreignKeyProperty, editModel, null);
  }

  public static EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                    final EntityEditModel editModel, final StateObserver enabledState) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    final EntityComboBoxModel boxModel = editModel.initializeEntityComboBoxModel(foreignKeyProperty);
    final EntityComboBox comboBox = new EntityComboBox(boxModel);
    new EntityComboBoxValueLink(comboBox, editModel, foreignKeyProperty);
    UiUtil.linkToEnabledState(enabledState, comboBox);
    MaximumMatch.enable(comboBox);
    if (foreignKeyProperty.hasDescription()) {
      comboBox.setToolTipText(foreignKeyProperty.getDescription());
    }
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
    }

    return comboBox;
  }

  public static EntityFieldPanel createEntityFieldPanel(final Property.ForeignKeyProperty foreignKeyProperty,
                                                        final EntityEditModel editModel, final EntityTableModel lookupModel) {
    return new EntityFieldPanel(foreignKeyProperty, editModel, lookupModel);
  }

  public static JTextField createEntityField(final Property.ForeignKeyProperty foreignKeyProperty,
                                             final EntityEditModel editModel) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    final JTextField textField = new JTextField();
    textField.setEditable(false);
    if (foreignKeyProperty.hasDescription()) {
      textField.setToolTipText(foreignKeyProperty.getDescription());
    }
    editModel.addValueListener(foreignKeyProperty.getPropertyID(), new ValueChangeListener() {
      @Override
      public void valueChanged(final ValueChangeEvent event) {
        textField.setText(event.getNewValue() == null ? "" : event.getNewValue().toString());
      }
    });

    return textField;
  }

  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel) {
    final Collection<String> searchPropertyIDs = Entities.getEntitySearchPropertyIDs(foreignKeyProperty.getReferencedEntityID());
    if (searchPropertyIDs.isEmpty()) {
      throw new RuntimeException("No default search properties specified for entity: " + foreignKeyProperty.getReferencedEntityID()
              + ", unable to create EntityLookupField, you must specify the searchPropertyIDs");
    }

    return createEntityLookupField(foreignKeyProperty, editModel, searchPropertyIDs.toArray(new String[searchPropertyIDs.size()]));
  }

  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel, final String... searchPropertyIDs) {
    return createEntityLookupField(foreignKeyProperty, editModel, null, searchPropertyIDs);
  }

  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel,
                                                          final Criteria additionalSearchCriteria,
                                                          final String... searchPropertyIDs) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    if (searchPropertyIDs == null || searchPropertyIDs.length == 0) {
      throw new RuntimeException("No search properties specified for entity lookup field: " + foreignKeyProperty.getReferencedEntityID());
    }
    final List<Property.ColumnProperty> searchProperties = Entities.getSearchProperties(
            foreignKeyProperty.getReferencedEntityID(), Arrays.asList(searchPropertyIDs));

    final EntityLookupField lookupField =
            new EntityLookupField(editModel.createEntityLookupModel(foreignKeyProperty.getReferencedEntityID(),
                    searchProperties, additionalSearchCriteria));
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      lookupField.setTransferFocusOnEnter();
    }
    new LookupValueLink(lookupField.getModel(), editModel, foreignKeyProperty.getPropertyID());
    if (foreignKeyProperty.hasDescription()) {
      lookupField.setToolTipText(foreignKeyProperty.getDescription());
    }
    UiUtil.selectAllOnFocusGained(lookupField);

    return lookupField;
  }

  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel) {
    return createValueListComboBox(property, editModel, null);
  }

  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel,
                                                        final StateObserver enabledState) {
    final SteppedComboBox comboBox = createComboBox(property, editModel, new ItemComboBoxModel<Object>(property.getValues()), enabledState);
    MaximumMatch.enable(comboBox);

    return comboBox;
  }

  public static SteppedComboBox createComboBox(final Property property, final EntityEditModel editModel,
                                               final ComboBoxModel model, final StateObserver enabledState) {
    return createComboBox(property, editModel, model, enabledState, false);
  }

  public static SteppedComboBox createComboBox(final Property property, final EntityEditModel editModel,
                                               final ComboBoxModel model, final StateObserver enabledState,
                                               final boolean editable) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    final SteppedComboBox comboBox = new SteppedComboBox(model);
    comboBox.setEditable(editable);
    new EntityComboBoxValueLink(comboBox, editModel, property);
    UiUtil.linkToEnabledState(enabledState, comboBox);
    if (property.hasDescription()) {
      comboBox.setToolTipText(property.getDescription());
    }
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      UiUtil.transferFocusOnEnter(comboBox);
    }

    return comboBox;
  }

  public static DateInputPanel createDateInputPanel(final Property property, final EntityEditModel editModel,
                                                    final SimpleDateFormat dateFormat, final LinkType linkType,
                                                    final boolean includeButton) {
    return createDateInputPanel(property, editModel, dateFormat, linkType, includeButton, null);
  }

  public static DateInputPanel createDateInputPanel(final Property property, final EntityEditModel editModel,
                                                    final SimpleDateFormat dateFormat, final LinkType linkType,
                                                    final boolean includeButton, final StateObserver enabledState) {
    Util.rejectNullValue(property,PROPERTY_PARAM_NAME);
    if (!property.isTime()) {
      throw new IllegalArgumentException("Property " + property + " is not a date property");
    }

    final JFormattedTextField field = (JFormattedTextField) createTextField(property, editModel, linkType,
            DateUtil.getDateMask(dateFormat), true, dateFormat, enabledState);
    final DateInputPanel panel = new DateInputPanel(field, dateFormat, includeButton, enabledState);
    if (panel.getButton() != null && Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter(panel.getButton());
    }

    return panel;
  }

  public static TextInputPanel createTextInputPanel(final Property property, final EntityEditModel editModel,
                                                    final LinkType linkType, final boolean immediateUpdate,
                                                    final boolean buttonFocusable) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    final JTextField field = createTextField(property, editModel, linkType, null, immediateUpdate);
    final TextInputPanel panel = new TextInputPanel(field, property.getCaption(), null, buttonFocusable);
    panel.setMaxLength(property.getMaxLength());
    if (panel.getButton() != null && Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter(panel.getButton());//todo
    }

    return panel;
  }

  public static JTextArea createTextArea(final Property property, final EntityEditModel editModel,
                                         final LinkType linkType) {
    return createTextArea(property, editModel, linkType, -1, -1);
  }

  public static JTextArea createTextArea(final Property property, final EntityEditModel editModel,
                                         final LinkType linkType, final int rows, final int columns) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    if (!property.isString()) {
      throw new RuntimeException("Cannot create a text area for a non-string property");
    }

    final JTextArea textArea = rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea();
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);

    new TextValueLink<String>(textArea, editModel, property.getPropertyID(), true, linkType);
    if (property.hasDescription()) {
      textArea.setToolTipText(property.getDescription());
    }

    return textArea;
  }

  public static JTextField createTextField(final Property property, final EntityEditModel editModel) {
    return createTextField(property, editModel, LinkType.READ_WRITE, null, true);
  }

  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate) {
    return createTextField(property, editModel, linkType, formatMaskString, immediateUpdate, null);
  }

  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final StateObserver enabledState) {
    return createTextField(property, editModel, linkType, formatMaskString, immediateUpdate, null, enabledState);
  }

  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final SimpleDateFormat dateFormat,
                                           final StateObserver enabledState) {
    return createTextField(property, editModel, linkType, formatMaskString, immediateUpdate, dateFormat,
            enabledState, false);
  }

  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final SimpleDateFormat dateFormat,//todo dateFormat?
                                           final StateObserver enabledState, final boolean valueContainsLiteralCharacters) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    Util.rejectNullValue(linkType, "linkType");
    final JTextField textField = initTextField(property, editModel, enabledState, formatMaskString, valueContainsLiteralCharacters);
    final String propertyID = property.getPropertyID();
    if (property.isString()) {
      if (formatMaskString != null) {
        new FormattedValueLink<String>((JFormattedTextField) textField, editModel, propertyID, null, immediateUpdate, linkType);
      }
      else {
        new TextValueLink<String>(textField, editModel, propertyID, immediateUpdate, linkType);
      }
    }
    else if (property.isInteger()) {
      new IntValueLink<String>((IntField) textField, editModel, propertyID, immediateUpdate, linkType);
    }
    else if (property.isDouble()) {
      new DoubleValueLink<String>((DoubleField) textField, editModel, propertyID, immediateUpdate, linkType);
    }
    else if (property.isDate()) {
      new DateValueLink<String>((JFormattedTextField) textField, editModel, propertyID, linkType, dateFormat, false);
    }
    else if (property.isTimestamp()) {
      new DateValueLink<String>((JFormattedTextField) textField, editModel, propertyID, linkType, dateFormat, true);
    }
    else {
      throw new IllegalArgumentException("Not a text based property: " + property);
    }

    return textField;
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityEditModel editModel) {
    return createPropertyComboBox(propertyID, editModel, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent) {
    return createPropertyComboBox(propertyID, editModel, refreshEvent, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent, final StateObserver state) {
    return createPropertyComboBox(propertyID, editModel, refreshEvent, state, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent, final StateObserver state,
                                                       final String nullValue) {
    return createPropertyComboBox(Entities.getColumnProperty(editModel.getEntityID(), propertyID),
            editModel, refreshEvent, state, nullValue);
  }

  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel) {
    return createPropertyComboBox(property, editModel, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent) {
    return createPropertyComboBox(property, editModel, refreshEvent, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent, final StateObserver state) {
    return createPropertyComboBox(property, editModel, refreshEvent, state, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent, final StateObserver state,
                                                       final String nullValue) {
    return createPropertyComboBox(property, editModel, refreshEvent, state, nullValue, false);
  }


  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent, final StateObserver state,
                                                       final String nullValue, final boolean editable) {
    final SteppedComboBox comboBox = createComboBox(property, editModel,
            editModel.initializePropertyComboBoxModel(property, refreshEvent, nullValue), state, editable);
    if (!editable) {
      MaximumMatch.enable(comboBox);
    }

    return comboBox;
  }

  public static JPanel createLookupFieldPanel(final EntityLookupField lookupField, final EntityTableModel tableModel) {
    Util.rejectNullValue(lookupField, "lookupField");
    Util.rejectNullValue(tableModel, "tableModel");
    final JButton btn = new JButton(new AbstractAction("...") {
      public void actionPerformed(final ActionEvent e) {
        try {
          lookupField.getModel().setSelectedEntities(selectEntities(tableModel, UiUtil.getParentWindow(lookupField),
                  true, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY), null));
        }
        catch (CancelException ex) {/**/}
      }
    });
    btn.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);

    final JPanel panel = new JPanel(new BorderLayout(5,0));
    panel.add(lookupField, BorderLayout.CENTER);
    panel.add(btn, BorderLayout.EAST);

    return panel;
  }

  public static JPanel createEntityComboBoxNewRecordPanel(final EntityComboBox entityComboBox,
                                                          final EntityPanelProvider panelProvider,
                                                          final boolean newRecordButtonTakesFocus) {
    return createEastButtonPanel(entityComboBox, new NewRecordAction(entityComboBox, panelProvider), newRecordButtonTakesFocus);
  }

  public static JPanel createEntityComboBoxFilterPanel(final EntityComboBox entityComboBox,
                                                       final String foreignKeyPropertyID,
                                                       final boolean filterButtonTakesFocus) {
    return createEastButtonPanel(entityComboBox, entityComboBox.createForeignKeyFilterAction(foreignKeyPropertyID),
            filterButtonTakesFocus);
  }

  private static JPanel createEastButtonPanel(final JComponent centerComponent, final Action buttonAction,
                                              final boolean buttonFocusable) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JButton button = new JButton(buttonAction);
    button.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
    button.setFocusable(buttonFocusable);

    panel.add(centerComponent, BorderLayout.CENTER);
    panel.add(button, BorderLayout.EAST);

    return panel;
  }

  private static JTextField initTextField(final Property property, final EntityEditModel editModel,
                                          final StateObserver enabledState, final String formatMaskString,
                                          final boolean valueContainsLiteralCharacters) {
    final JTextField field;
    if (property.isInteger()) {
      field = new IntField(0);
    }
    else if (property.isDouble()) {
      field = new DoubleField(0);
    }
    else if (property.isTime()) {
      field = UiUtil.createFormattedField(formatMaskString, true);
    }
    else if (property.isString()) {
      field = formatMaskString == null ? new TextFieldPlus() : UiUtil.createFormattedField(formatMaskString, valueContainsLiteralCharacters);
    }
    else {
      throw new RuntimeException("Unable to create text field for property type: " + property.getType());
    }

    UiUtil.linkToEnabledState(enabledState, field);
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter(field);
    }
    if (property.hasDescription()) {
      field.setToolTipText(property.getDescription());
    }
    if (field instanceof TextFieldPlus && property.getMaxLength() > 0) {
      ((TextFieldPlus) field).setMaxLength(property.getMaxLength());
    }
    if (property instanceof Property.ColumnProperty) {
      UiUtil.addLookupDialog(field, editModel.getValueProvider(property));
    }

    return field;
  }

  private static final class NewRecordAction extends AbstractAction {

    private final EntityComboBox comboBox;
    private final EntityPanelProvider panelProvider;

    private NewRecordAction(final EntityComboBox comboBox, final EntityPanelProvider panelProvider) {
      super("", Images.loadImage(Images.IMG_ADD_16));
      this.comboBox = comboBox;
      this.panelProvider = panelProvider;
    }

    public void actionPerformed(final ActionEvent e) {
      final EntityPanel entityPanel = panelProvider.createInstance(comboBox.getModel().getDbProvider());
      entityPanel.initializePanel();
      final List<Entity.Key> insertedPrimaryKeys = new ArrayList<Entity.Key>();
      entityPanel.getModel().getEditModel().addAfterInsertListener(new NewRecordListener(insertedPrimaryKeys));
      final Window parentWindow = UiUtil.getParentWindow(comboBox);
      final String caption = panelProvider.getCaption() == null || panelProvider.getCaption().equals("") ?
              entityPanel.getCaption() : panelProvider.getCaption();
      final JDialog dialog = new JDialog(parentWindow, caption);
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setLayout(new BorderLayout());
      dialog.add(entityPanel, BorderLayout.CENTER);
      final JButton btnClose = initializeOkButton(entityPanel.getModel().getTableModel(), dialog, insertedPrimaryKeys);
      final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      buttonPanel.add(btnClose);
      dialog.add(buttonPanel, BorderLayout.SOUTH);
      dialog.pack();
      dialog.setLocationRelativeTo(parentWindow);
      dialog.setModal(true);
      dialog.setResizable(true);
      dialog.setVisible(true);
    }

  private static final class NewRecordListener extends InsertListener {

    private final Collection<Entity.Key> insertKeys;

    private NewRecordListener(final Collection<Entity.Key> insertKeys) {
      this.insertKeys = insertKeys;
    }

    @Override
    protected void inserted(final InsertEvent event) {
      insertKeys.clear();
      insertKeys.addAll(event.getInsertedKeys());
    }
  }

    private JButton initializeOkButton(final EntityTableModel tableModel, final JDialog dialog,
                                       final List<Entity.Key> lastInsertedPrimaryKeys) {
      final JButton button = new JButton(new AbstractAction(Messages.get(Messages.OK)) {
        public void actionPerformed(final ActionEvent e) {
          comboBox.getModel().refresh();
          if (lastInsertedPrimaryKeys != null && !lastInsertedPrimaryKeys.isEmpty()) {
            comboBox.getModel().setSelectedEntityByPrimaryKey(lastInsertedPrimaryKeys.get(0));
          }
          else {
            final Entity selectedEntity = tableModel.getSelectedItem();
            if (selectedEntity != null) {
              comboBox.getModel().setSelectedItem(selectedEntity);
            }
          }
          dialog.dispose();
        }
      });
      button.setMnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0));

      return button;
    }
  }

  public static class EntityComboBoxValueLink extends ComboBoxValueLink<String> {
    public EntityComboBoxValueLink(final JComboBox comboBox, final EntityEditModel editModel,
                                   final Property property) {
      super(comboBox, editModel, property.getPropertyID(), LinkType.READ_WRITE,
              Util.rejectNullValue(property, PROPERTY_PARAM_NAME).isString());
    }
  }

  /**
   * A class for linking an EntityLookupModel to a EntityEditModel foreign key property value.
   */
  public static final class LookupValueLink extends AbstractValueMapLink<String, Object> {

    private final EntityLookupModel lookupModel;

    /**
     * Instantiates a new LookupModelValueLink
     * @param lookupModel the lookup model to link
     * @param editModel the EntityEditModel instance
     * @param foreignKeyPropertyID the foreign key property ID to link
     */
    public LookupValueLink(final EntityLookupModel lookupModel, final EntityEditModel editModel,
                           final String foreignKeyPropertyID) {
      super(editModel, foreignKeyPropertyID, LinkType.READ_WRITE);
      this.lookupModel = lookupModel;
      updateUI();
      lookupModel.addSelectedEntitiesListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          updateModel();
        }
      });
    }

    @Override
    protected Object getUIValue() {
      final List<Entity> selectedEntities = lookupModel.getSelectedEntities();
      return selectedEntities.isEmpty() ? null : selectedEntities.get(0);
    }

    @Override
    protected void setUIValue(final Object value) {
      final List<Entity> valueList = new ArrayList<Entity>();
      if (getModelValue() != null) {
        valueList.add((Entity) value);
      }
      lookupModel.setSelectedEntities(valueList);
    }
  }

  public static final class EntityFieldPanel extends JPanel {

    private final JTextField textField;

    public EntityFieldPanel(final Property.ForeignKeyProperty foreignKeyProperty,
                            final EntityEditModel editModel, final EntityTableModel lookupModel) {
      super(new BorderLayout(5,5));
      Util.rejectNullValue(lookupModel, "lookupModel");
      textField = createEntityField(foreignKeyProperty, editModel);
      initializeUI(foreignKeyProperty, editModel, lookupModel);
      addFocusListener(new FocusAdapter() {
        @Override
        public void focusGained(final FocusEvent e) {
          textField.requestFocusInWindow();
        }
      });
    }

    public JTextField getTextField() {
      return textField;
    }

    private void initializeUI(final Property.ForeignKeyProperty foreignKeyProperty,
                              final EntityEditModel editModel, final EntityTableModel lookupModel) {
      final JButton btn = new JButton(new AbstractAction("...") {
        public void actionPerformed(final ActionEvent e) {
          try {
            final List<Entity> selected = EntityUiUtil.selectEntities(lookupModel, UiUtil.getParentWindow(textField),
                    true, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY), null);
            editModel.setValue(foreignKeyProperty.getPropertyID(), !selected.isEmpty() ? selected.get(0) : null);
          }
          catch (CancelException ex) {/**/}
        }
      });
      btn.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);

      add(textField, BorderLayout.CENTER);
      add(btn, BorderLayout.EAST);
    }
  }
}
