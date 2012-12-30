/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;
import org.jminor.common.model.checkbox.TristateButtonModel;
import org.jminor.common.model.combobox.BooleanComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;
import org.jminor.common.model.valuemap.ValueMapEditModel;
import org.jminor.common.model.valuemap.ValueMapValue;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.checkbox.TristateCheckBox;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.AbstractValueLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.ValueLinks;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.input.InputProviderPanel;
import org.jminor.common.ui.textfield.DocumentSizeFilter;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.common.ui.textfield.SizedDocument;
import org.jminor.common.ui.valuemap.ValueLinkValidators;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.DefaultEntityLookupModel;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.EntityDataProvider;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.text.AbstractDocument;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A static utility class concerned with UI related tasks.
 */
public final class EntityUiUtil {

  private static final String PROPERTY_PARAM_NAME = "property";
  private static final String EDIT_MODEL_PARAM_NAME = "editModel";

  private EntityUiUtil() {}

  /**
   * Shows a dialog for selecting the root logging level.
   * @param dialogParent the component serving as a dialog parent
   */
  public static void setLoggingLevel(final JComponent dialogParent) {
    final DefaultComboBoxModel model = new DefaultComboBoxModel(
            new Object[] {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR});
    final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    model.setSelectedItem(rootLogger.getLevel());
    JOptionPane.showMessageDialog(dialogParent, new JComboBox(model),
            FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL), JOptionPane.QUESTION_MESSAGE);
    rootLogger.setLevel((Level) model.getSelectedItem());
  }

  /**
   * Creates an Action for viewing an image based on the entity selected in a EntityTablePanel.
   * The action shows an image found at the path specified by the value of the given propertyID.
   * If no entity is selected or the image path value is null no action is performed.
   * @param tablePanel the EntityTablePanel the table panel
   * @param imagePathPropertyID the ID of the property specifying the image path
   * @return an Action for viewing an image based on the selected entity in a EntityTablePanel
   * @see UiUtil#showImage(String, javax.swing.JComponent)
   */
  public static Action initializeViewImageAction(final EntityTablePanel tablePanel, final String imagePathPropertyID) {
    Util.rejectNullValue(tablePanel, "tablePanel");
    Util.rejectNullValue(imagePathPropertyID, "imagePathPropertyID");
    return new AbstractAction() {
      @Override
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

  /**
   * Performs a lookup for the given entity type, using a EntityLookupField displayed
   * in a dialog, using the default search properties for the given entityID.
   * @param entityID the entityID of the entity to perform a lookup for
   * @param connectionProvider the connection provider
   * @param singleSelection if true only a single entity can be selected
   * @param dialogParent the component serving as the dialog parent
   * @param lookupCaption the caption for the lookup field, used as a caption for the dialog as well
   * @return the selected entities or an empty collection in case a selection was not performed
   * @see EntityLookupField
   * @see Entities#getSearchProperties(String)
   */
  public static Collection<Entity> lookupEntities(final String entityID, final EntityConnectionProvider connectionProvider,
                                                  final boolean singleSelection, final JComponent dialogParent,
                                                  final String lookupCaption) {
    return lookupEntities(entityID, connectionProvider, singleSelection, dialogParent, lookupCaption, lookupCaption);
  }

  /**
   * Performs a lookup for the given entity type, using a EntityLookupField displayed
   * in a dialog, using the default search properties for the given entityID.
   * @param entityID the entityID of the entity to perform a lookup for
   * @param connectionProvider the connection provider
   * @param singleSelection if true only a single entity can be selected
   * @param dialogParent the component serving as the dialog parent
   * @param lookupCaption the caption for the lookup field
   * @param dialogTitle the title to display on the dialog
   * @return the selected entities or an empty collection in case a selection was not performed
   * @see EntityLookupField
   * @see Entities#getSearchProperties(String)
   */
  public static Collection<Entity> lookupEntities(final String entityID, final EntityConnectionProvider connectionProvider,
                                                  final boolean singleSelection, final JComponent dialogParent,
                                                  final String lookupCaption, final String dialogTitle) {
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(entityID, connectionProvider,
            Entities.getSearchProperties(entityID));
    if (singleSelection) {
      lookupModel.setMultipleSelectionAllowed(false);
    }
    final InputProviderPanel inputPanel = new InputProviderPanel(lookupCaption, new EntityLookupProvider(lookupModel, null));
    UiUtil.showInDialog(UiUtil.getParentWindow(dialogParent), inputPanel, true, dialogTitle,
            null, inputPanel.getOkButton(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted()) {
      return lookupModel.getSelectedEntities();
    }

    return Collections.emptyList();
  }

  public static Collection<Entity> selectEntities(final EntityTableModel lookupModel, final JComponent dialogOwner,
                                                  final boolean singleSelection, final String dialogTitle) throws CancelException {
    return selectEntities(lookupModel, dialogOwner, singleSelection, dialogTitle, null);
  }

  public static Collection<Entity> selectEntities(final EntityTableModel lookupModel, final JComponent dialogOwner,
                                                  final boolean singleSelection, final String dialogTitle,
                                                  final Dimension preferredSize) throws CancelException {
    Util.rejectNullValue(lookupModel, "lookupModel");
    final Collection<Entity> selected = new ArrayList<Entity>();
    final JDialog dialog = new JDialog(UiUtil.getParentWindow(dialogOwner), dialogTitle);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        final List<Entity> entities = lookupModel.getSelectedItems();
        for (final Entity entity : entities) {
          selected.add(entity);
        }
        dialog.dispose();
      }
    };
    final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        selected.add(null);//hack to indicate cancel
        dialog.dispose();
      }
    };

    final EntityTablePanel entityTablePanel = new EntityTablePanel(lookupModel);
    entityTablePanel.initializePanel();
    entityTablePanel.addTableDoubleClickListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        if (!entityTablePanel.getEntityTableModel().isSelectionEmpty()) {
          okAction.actionPerformed(null);
        }
      }
    });
    entityTablePanel.setSearchPanelVisible(true);
    if (singleSelection) {
      entityTablePanel.getJTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    final Action searchAction = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        lookupModel.refresh();
        if (lookupModel.getRowCount() > 0) {
          lookupModel.setSelectedIndexes(Arrays.asList(0));
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
    UiUtil.addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, cancelAction);
    entityTablePanel.getJTable().getInputMap(
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    dialog.setLayout(new BorderLayout());
    if (preferredSize != null) {
      entityTablePanel.setPreferredSize(preferredSize);
    }
    dialog.add(entityTablePanel, BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(UiUtil.createFlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(btnSearch);
    buttonPanel.add(btnOk);
    buttonPanel.add(btnCancel);
    dialog.getRootPane().setDefaultButton(btnOk);
    dialog.add(buttonPanel, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(dialogOwner);
    dialog.setModal(true);
    dialog.setResizable(true);
    dialog.setVisible(true);

    if (selected.isEmpty() || (selected.size() == 1 && selected.contains(null))) {
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
    final JLabel label = new JLabel(property.getCaption(), horizontalAlignment);
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
    checkProperty(property, editModel);
    if (!property.isBoolean()) {
      throw new IllegalArgumentException("Boolean property required for createCheckBox");
    }

    final JCheckBox checkBox = includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox();
    ValueLinks.toggleValueLink(checkBox.getModel(),
            new EditModelValue(editModel, property.getPropertyID()), null, LinkType.READ_WRITE, null);
    UiUtil.linkToEnabledState(enabledState, checkBox);
    if (property.getDescription() != null) {
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
    checkProperty(property, editModel);
    if (!property.isBoolean() && property.isNullable()) {
      throw new IllegalArgumentException("Nullable boolean property required for createTristateCheckBox");
    }

    final TristateCheckBox checkBox = new TristateCheckBox(includeCaption ? property.getCaption() : null);
    ValueLinks.tristateValueLink((TristateButtonModel) checkBox.getModel(), new EditModelValue(editModel, property.getPropertyID()), LinkType.READ_WRITE, null);
    UiUtil.linkToEnabledState(enabledState, checkBox);
    if (property.getDescription() != null) {
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
    checkProperty(foreignKeyProperty, editModel);
    final EntityComboBoxModel boxModel = editModel.initializeEntityComboBoxModel(foreignKeyProperty);
    boxModel.refresh();
    final EntityComboBox comboBox = new EntityComboBox(boxModel);
    ValueLinks.selectedItemValueLink(comboBox, new EditModelValue(editModel, foreignKeyProperty.getPropertyID()), LinkType.READ_WRITE);
    UiUtil.linkToEnabledState(enabledState, comboBox);
    MaximumMatch.enable(comboBox);
    if (foreignKeyProperty.getDescription() != null) {
      comboBox.setToolTipText(foreignKeyProperty.getDescription());
    }
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      //getEditor().getEditorComponent() only required because the combo box is editable, due to MaximumMatch.enable() above
      UiUtil.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
    }

    return comboBox;
  }

  public static EntityFieldPanel createEntityFieldPanel(final Property.ForeignKeyProperty foreignKeyProperty,
                                                        final EntityEditModel editModel, final EntityTableModel lookupModel) {
    checkProperty(foreignKeyProperty, editModel);

    return new EntityFieldPanel(foreignKeyProperty, editModel, lookupModel);
  }

  public static JTextField createEntityField(final Property.ForeignKeyProperty foreignKeyProperty,
                                             final EntityEditModel editModel) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(foreignKeyProperty, editModel);
    final JTextField textField = new JTextField();
    textField.setEditable(false);
    textField.setFocusable(false);
    if (foreignKeyProperty.getDescription() != null) {
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
    return createEntityLookupField(foreignKeyProperty, editModel, (StateObserver) null);
  }

  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel, final String... searchPropertyIDs) {
    return createEntityLookupField(foreignKeyProperty, editModel, null, searchPropertyIDs);
  }

  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel, final StateObserver enabledState) {
    final Collection<String> searchPropertyIDs = Entities.getSearchPropertyIDs(foreignKeyProperty.getReferencedEntityID());
    if (searchPropertyIDs.isEmpty()) {
      throw new IllegalArgumentException("No default search properties specified for entity: " + foreignKeyProperty.getReferencedEntityID()
              + ", unable to create EntityLookupField, you must specify the searchPropertyIDs");
    }

    return createEntityLookupField(foreignKeyProperty, editModel, enabledState, searchPropertyIDs.toArray(new String[searchPropertyIDs.size()]));
  }

  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel, final StateObserver enabledState,
                                                          final String... searchPropertyIDs) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(foreignKeyProperty, editModel);
    if (searchPropertyIDs == null || searchPropertyIDs.length == 0) {
      throw new IllegalArgumentException("No search properties specified for entity lookup field: " + foreignKeyProperty.getReferencedEntityID());
    }

    final EntityLookupModel lookupModel = editModel.initializeEntityLookupModel(foreignKeyProperty);
    final EntityLookupField lookupField = new EntityLookupField(lookupModel);

    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      lookupField.setTransferFocusOnEnter();
    }
    new LookupValueLink(lookupField.getModel(), editModel, foreignKeyProperty.getPropertyID());
    UiUtil.linkToEnabledState(enabledState, lookupField);
    if (foreignKeyProperty.getDescription() != null) {
      lookupField.setToolTipText(foreignKeyProperty.getDescription());
    }
    UiUtil.selectAllOnFocusGained(lookupField);

    return lookupField;
  }

  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel) {
    return createValueListComboBox(property, editModel, true, null);
  }

  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel,
                                                        final StateObserver enabledState) {
    return createValueListComboBox(property, editModel, true, enabledState);
  }

  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel,
                                                        final boolean sortItems) {
    return createValueListComboBox(property, editModel, sortItems, null);
  }

  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel,
                                                        final boolean sortItems, final StateObserver enabledState) {
    final ItemComboBoxModel model;
    if (sortItems) {
      model = new ItemComboBoxModel(property.getValues());
    }
    else {
      model = new ItemComboBoxModel(null, property.getValues());
    }
    final SteppedComboBox comboBox = createComboBox(property, editModel, model, enabledState);
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
    checkProperty(property, editModel);
    final SteppedComboBox comboBox = new SteppedComboBox(model);
    comboBox.setEditable(editable);
    ValueLinks.selectedItemValueLink(comboBox, new EditModelValue(editModel, property.getPropertyID()), LinkType.READ_WRITE);
    UiUtil.linkToEnabledState(enabledState, comboBox);
    if (property.getDescription() != null) {
      comboBox.setToolTipText(property.getDescription());
    }
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      UiUtil.transferFocusOnEnter(comboBox);
    }

    return comboBox;
  }

  public static DateInputPanel createDateInputPanel(final Property property, final EntityEditModel editModel,
                                                    final LinkType linkType, final boolean includeButton) {
    return createDateInputPanel(property, editModel, linkType, includeButton, null);
  }

  public static DateInputPanel createDateInputPanel(final Property property, final EntityEditModel editModel,
                                                    final LinkType linkType, final boolean includeButton,
                                                    final StateObserver enabledState) {
    Util.rejectNullValue(property,PROPERTY_PARAM_NAME);
    if (!property.isTime()) {
      throw new IllegalArgumentException("Property " + property + " is not a date property");
    }

    final JFormattedTextField field = (JFormattedTextField) createTextField(property, editModel, linkType,
            DateUtil.getDateMask((SimpleDateFormat) property.getFormat()), true, enabledState);
    final DateInputPanel panel = new DateInputPanel(field, (SimpleDateFormat) property.getFormat(), includeButton, enabledState);
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
    checkProperty(property, editModel);
    if (!property.isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string property");
    }

    final JTextArea textArea = rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea();
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    if (property.getMaxLength() > 0) {
      ((AbstractDocument) textArea.getDocument()).setDocumentFilter(new DocumentSizeFilter(property.getMaxLength()));
    }

    ValueLinks.textValueLink(textArea, new EditModelValue(editModel, property.getPropertyID()), linkType, null, true);
    ValueLinkValidators.addValidator(property.getPropertyID(), textArea, editModel);
    if (property.getDescription() != null) {
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
    return createTextField(property, editModel, linkType, formatMaskString, immediateUpdate, enabledState, false);
  }

  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final StateObserver enabledState,
                                           final boolean valueContainsLiteralCharacters) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    Util.rejectNullValue(linkType, "linkType");
    checkProperty(property, editModel);
    final JTextField textField = initializeTextField(property, editModel, enabledState, formatMaskString, valueContainsLiteralCharacters);
    final String propertyID = property.getPropertyID();
    if (property.isString()) {
      if (formatMaskString != null) {
        ValueLinks.formattedTextValueLink((JFormattedTextField) textField, new EditModelValue(editModel, propertyID), linkType, null);
      }
      else {
        ValueLinks.textValueLink(textField, new EditModelValue(editModel, propertyID), linkType, null, immediateUpdate);
      }
    }
    else if (property.isInteger()) {
      ValueLinks.intValueLink((IntField) textField, new EditModelValue(editModel, propertyID), linkType, false, (NumberFormat) property.getFormat());
    }
    else if (property.isDouble()) {
      ValueLinks.doubleValueLink((DoubleField) textField, new EditModelValue(editModel, propertyID), linkType, false, (NumberFormat) property.getFormat());
    }
    else if (property.isDate()) {
      ValueLinks.dateValueLink((JFormattedTextField) textField, new EditModelValue(editModel, propertyID), linkType, (SimpleDateFormat) property.getFormat(), false);
    }
    else if (property.isTimestamp()) {
      ValueLinks.dateValueLink((JFormattedTextField) textField, new EditModelValue(editModel, propertyID), linkType, (SimpleDateFormat) property.getFormat(), true);
    }
    else {
      throw new IllegalArgumentException("Not a text based property: " + property);
    }
    if (property.isString() && formatMaskString != null) {
      ValueLinkValidators.addFormattedValidator(property.getPropertyID(), textField, editModel);
    }
    else {
      ValueLinkValidators.addValidator(property.getPropertyID(), textField, editModel);
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
                                                       final EventObserver refreshEvent, final StateObserver enabledState) {
    return createPropertyComboBox(propertyID, editModel, refreshEvent, enabledState, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent, final StateObserver enabledState,
                                                       final String nullValue) {
    return createPropertyComboBox(Entities.getColumnProperty(editModel.getEntityID(), propertyID),
            editModel, refreshEvent, enabledState, nullValue);
  }

  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel) {
    return createPropertyComboBox(property, editModel, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent) {
    return createPropertyComboBox(property, editModel, refreshEvent, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent, final StateObserver enabledState) {
    return createPropertyComboBox(property, editModel, refreshEvent, enabledState, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent, final StateObserver enabledState,
                                                       final String nullValue) {
    return createPropertyComboBox(property, editModel, refreshEvent, enabledState, nullValue, false);
  }

  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final EventObserver refreshEvent, final StateObserver enabledState,
                                                       final String nullValue, final boolean editable) {
    final SteppedComboBox comboBox = createComboBox(property, editModel,
            editModel.initializePropertyComboBoxModel(property, refreshEvent, nullValue), enabledState, editable);
    if (!editable) {
      MaximumMatch.enable(comboBox);
    }

    return comboBox;
  }

  public static JPanel createLookupFieldPanel(final EntityLookupField lookupField, final EntityTableModel tableModel) {
    Util.rejectNullValue(lookupField, "lookupField");
    Util.rejectNullValue(tableModel, "tableModel");
    if (!lookupField.getModel().getEntityID().equals(tableModel.getEntityID())) {
      throw new IllegalArgumentException("Entity type mismatch: " + lookupField.getModel().getEntityID()
              + ", should be: " + tableModel.getEntityID());
    }
    final JButton btn = new JButton(new AbstractAction("...") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        try {
          lookupField.getModel().setSelectedEntities(selectEntities(tableModel, lookupField,
                  true, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY), null));
        }
        catch (CancelException ignored) {}
      }
    });
    btn.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);

    final JPanel panel = new JPanel(new BorderLayout(0, 0));
    panel.add(lookupField, BorderLayout.CENTER);
    panel.add(btn, BorderLayout.EAST);

    return panel;
  }

  /**
   * Creates a new JButton which shows the edit panel provided by <code>panelProvider</code> and if an insert is performed
   * selects the new entity in the <code>lookupField</code>.
   * @param comboBox the combo box in which to select the new entity, if any
   * @param panelProvider the EntityPanelProvider for providing the EntityEditPanel to use for creating the new entity
   * @return the JButton
   */
  public static JButton createNewEntityButton(final EntityComboBox comboBox, final EntityPanelProvider panelProvider) {
    return new JButton(new CreateEntityAction(comboBox, panelProvider));
  }

  /**
   * Creates a new JButton which shows the edit panel provided by <code>panelProvider</code> and if an insert is performed
   * selects the new entity in the <code>lookupField</code>.
   * @param lookupField the lookup field in which to select the new entity, if any
   * @param panelProvider the EntityPanelProvider for providing the EntityEditPanel to use for creating the new entity
   * @return the JButton
   */
  public static JButton createNewEntityButton(final EntityLookupField lookupField, final EntityPanelProvider panelProvider) {
    return new JButton(new CreateEntityAction(lookupField, panelProvider));
  }

  public static JPanel createEntityComboBoxPanel(final EntityComboBox entityComboBox, final EntityPanelProvider panelProvider,
                                                 final boolean newRecordButtonTakesFocus) {
    return createEastButtonPanel(entityComboBox, new CreateEntityAction(entityComboBox, panelProvider), newRecordButtonTakesFocus);
  }

  public static JPanel createEntityComboBoxFilterPanel(final EntityComboBox entityComboBox, final String foreignKeyPropertyID,
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

  private static JTextField initializeTextField(final Property property, final EntityEditModel editModel,
                                                final StateObserver enabledState, final String formatMaskString,
                                                final boolean valueContainsLiteralCharacters) {
    final JTextField field;
    if (property.isInteger()) {
      field = new IntField();
    }
    else if (property.isDouble()) {
      field = new DoubleField();
      if (property.getMaximumFractionDigits() > 0) {
        ((DoubleField) field).setMaximumFractionDigits(property.getMaximumFractionDigits());
      }
    }
    else if (property.isTime()) {
      field = UiUtil.createFormattedField(DateUtil.getDateMask((SimpleDateFormat) property.getFormat()));
    }
    else if (property.isString()) {
      if (formatMaskString == null) {
        field = new JTextField(new SizedDocument(), "", 0);
      }
      else {
        field = UiUtil.createFormattedField(formatMaskString, valueContainsLiteralCharacters);
      }
    }
    else {
      throw new IllegalArgumentException("Unable to create text field for property type: " + property.getType());
    }

    UiUtil.linkToEnabledState(enabledState, field);
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter(field);
    }
    if (property.getDescription() != null) {
      field.setToolTipText(property.getDescription());
    }
    if (property.getMaxLength() > 0 && field.getDocument() instanceof SizedDocument) {
      ((SizedDocument) field.getDocument()).setMaxLength(property.getMaxLength());
    }
    if (property instanceof Property.ColumnProperty) {
      UiUtil.addLookupDialog(field, editModel.getValueProvider(property));
    }

    return field;
  }

  private static void checkProperty(final Property property, final EntityEditModel editModel) {
    if (!property.getEntityID().equals(editModel.getEntityID())) {
      throw new IllegalArgumentException("Entity type mismatch: " + property.getEntityID() + ", should be: " + editModel.getEntityID());
    }
  }

  private static final class EditModelValue extends ValueMapValue<String, Object> {
    private EditModelValue(final ValueMapEditModel<String, Object> editModel, final String key) {
      super(editModel, key);
    }
  }

  /**
   * A class for linking an EntityLookupModel to a EntityEditModel foreign key property value.
   */
  public static final class LookupValueLink extends AbstractValueLink<Object> {

    private final EntityLookupModel lookupModel;

    /**
     * Instantiates a new LookupModelValueLink
     * @param lookupModel the lookup model to link
     * @param editModel the EntityEditModel instance
     * @param foreignKeyPropertyID the foreign key property ID to link
     */
    public LookupValueLink(final EntityLookupModel lookupModel, final EntityEditModel editModel,
                           final String foreignKeyPropertyID) {
      super(new EditModelValue(editModel, foreignKeyPropertyID), LinkType.READ_WRITE);
      this.lookupModel = lookupModel;
      updateUI();
      lookupModel.addSelectedEntitiesListener(new EventAdapter() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred() {
          updateModel();
        }
      });
    }

    @Override
    protected Object getUIValue() {
      final Collection<Entity> selectedEntities = lookupModel.getSelectedEntities();
      return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
    }

    @Override
    protected void setUIValue(final Object value) {
      final List<Entity> valueList = new ArrayList<Entity>();
      if (!isModelValueNull()) {
        valueList.add((Entity) value);
      }
      lookupModel.setSelectedEntities(valueList);
    }
  }

  public static final class EntityFieldPanel extends JPanel {

    private final JTextField textField;

    public EntityFieldPanel(final Property.ForeignKeyProperty foreignKeyProperty,
                            final EntityEditModel editModel, final EntityTableModel lookupModel) {
      super(UiUtil.createBorderLayout());
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
        @Override
        public void actionPerformed(final ActionEvent e) {
          try {
            final Collection<Entity> selected = selectEntities(lookupModel, textField,
                    true, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY), null);
            editModel.setValue(foreignKeyProperty.getPropertyID(), !selected.isEmpty() ? selected.iterator().next() : null);
          }
          catch (CancelException ignored) {}
        }
      });
      btn.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);

      add(textField, BorderLayout.CENTER);
      add(btn, BorderLayout.EAST);
    }
  }

  private static final class CreateEntityAction extends AbstractAction {

    private final JComponent component;
    private final EntityDataProvider dataProvider;
    private final EntityPanelProvider panelProvider;
    private final List<Entity> lastInsertedEntities = new ArrayList<Entity>();

    private CreateEntityAction(final JComponent component, final EntityPanelProvider panelProvider) {
      super("", Images.loadImage(Images.IMG_ADD_16));
      this.component = component;
      if (component instanceof EntityComboBox) {
        this.dataProvider = ((EntityComboBox) component).getModel();
      }
      else if (component instanceof EntityLookupField) {
        this.dataProvider = ((EntityLookupField) component).getModel();
      }
      else {
        throw new IllegalArgumentException("EntityComboBox or EntityLookupField expected, got: " + component);
      }
      this.panelProvider = panelProvider;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      final EntityEditPanel editPanel = panelProvider.createEditPanel(dataProvider.getConnectionProvider());
      editPanel.initializePanel();
      editPanel.getEditModel().addAfterInsertListener(new EventAdapter<EntityEditModel.InsertEvent>() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred(final EntityEditModel.InsertEvent eventInfo) {
          lastInsertedEntities.clear();
          lastInsertedEntities.addAll(eventInfo.getInsertedEntities());
        }
      });
      final JOptionPane pane = new JOptionPane(editPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
      final JDialog dialog = pane.createDialog(component, panelProvider.getCaption());
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      UiUtil.addInitialFocusHack(editPanel, new InitialFocusAction(editPanel));
      dialog.setVisible(true);
      if (pane.getValue() != null && pane.getValue().equals(0)) {
        final boolean insertPerformed = editPanel.insert();//todo exception during insert, f.ex validation failure not handled
        if (insertPerformed && !lastInsertedEntities.isEmpty()) {
          if (dataProvider instanceof EntityComboBoxModel) {
            ((EntityComboBoxModel) dataProvider).refresh();
            ((EntityComboBoxModel) dataProvider).setSelectedItem(lastInsertedEntities.get(0));
          }
          else if (dataProvider instanceof EntityLookupModel) {
            ((EntityLookupModel) dataProvider).setSelectedEntities(lastInsertedEntities);
          }
        }
      }
      component.requestFocusInWindow();
    }
  }

  private static final class InitialFocusAction extends AbstractAction {

    private final EntityEditPanel editPanel;

    private InitialFocusAction(final EntityEditPanel editPanel) {
      this.editPanel = editPanel;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      editPanel.setInitialFocus();
    }
  }
}
