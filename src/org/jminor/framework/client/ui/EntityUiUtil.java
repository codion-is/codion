/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.Values;
import org.jminor.common.model.checkbox.TristateButtonModel;
import org.jminor.common.model.combobox.BooleanComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.model.valuemap.EditModelValues;
import org.jminor.common.model.valuemap.ValueChange;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.ValueLinks;
import org.jminor.common.ui.checkbox.TristateCheckBox;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
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
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.text.AbstractDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A static utility class concerned with UI related tasks.
 */
public final class EntityUiUtil {

  private static final String PROPERTY_PARAM_NAME = "property";
  private static final String EDIT_MODEL_PARAM_NAME = "editModel";
  private static final int BOOLEAN_COMBO_BOX_POPUP_WIDTH = 40;
  private static final Color INVALID_COLOR = Color.RED;

  private EntityUiUtil() {}

  /**
   * Shows a dialog for selecting the root logging level.
   * @param dialogParent the component serving as a dialog parent
   */
  public static void setLoggingLevel(final JComponent dialogParent) {
    final DefaultComboBoxModel<Level> model = new DefaultComboBoxModel<>(
            new Level[] {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR});
    final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    model.setSelectedItem(rootLogger.getLevel());
    JOptionPane.showMessageDialog(dialogParent, new JComboBox<>(model),
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
          if (!tableModel.getSelectionModel().isSelectionEmpty()) {
            final Entity selected = tableModel.getSelectionModel().getSelectedItem();
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
    UiUtil.displayInDialog(dialogParent, inputPanel, dialogTitle, true, inputPanel.getOkButton(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted()) {
      return lookupModel.getSelectedEntities();
    }

    return Collections.emptyList();
  }

  /**
   * Displays a entity table in a dialog for selecting one or more entities
   * @param lookupModel the table model on which to base the table panel
   * @param dialogOwner the dialog owner
   * @param singleSelection if true then only a single item can be selected
   * @param dialogTitle the dialog title
   * @return a Collection containing the selected entities
   * @throws CancelException in case the user cancels the operation
   */
  public static Collection<Entity> selectEntities(final EntityTableModel lookupModel, final JComponent dialogOwner,
                                                  final boolean singleSelection, final String dialogTitle) throws CancelException {
    return selectEntities(lookupModel, dialogOwner, singleSelection, dialogTitle, null);
  }

  /**
   * Displays a entity table in a dialog for selecting one or more entities
   * @param lookupModel the table model on which to base the table panel
   * @param dialogOwner the dialog owner
   * @param singleSelection if true then only a single item can be selected
   * @param dialogTitle the dialog title
   * @param preferredSize the preferred size of the dialog
   * @return a Collection containing the selected entities
   * @throws CancelException in case the user cancels the operation
   */
  public static Collection<Entity> selectEntities(final EntityTableModel lookupModel, final JComponent dialogOwner,
                                                  final boolean singleSelection, final String dialogTitle,
                                                  final Dimension preferredSize) throws CancelException {
    Util.rejectNullValue(lookupModel, "lookupModel");
    final Collection<Entity> selected = new ArrayList<>();
    final JDialog dialog = new JDialog(UiUtil.getParentWindow(dialogOwner), dialogTitle);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        final List<Entity> entities = lookupModel.getSelectionModel().getSelectedItems();
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
    entityTablePanel.addTableDoubleClickListener(new EventListener() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        if (!lookupModel.getSelectionModel().isSelectionEmpty()) {
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
          lookupModel.getSelectionModel().setSelectedIndexes(Arrays.asList(0));
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
   * @see org.jminor.framework.Configuration#LABEL_TEXT_ALIGNMENT
   */
  public static JLabel createLabel(final Property property) {
    return createLabel(property, Configuration.getIntValue(Configuration.LABEL_TEXT_ALIGNMENT));
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

  /**
   * Creates a JCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param editModel the edit model to bind with the value
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a boolean property
   */
  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel) {
    return createCheckBox(property, editModel, null);
  }

  /**
   * Creates a JCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the checkbox
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a boolean property
   */
  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel,
                                         final StateObserver enabledState) {
    return createCheckBox(property, editModel, enabledState, true);
  }

  /**
   * Creates a JCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the checkbox
   * @param includeCaption if true then the property caption is included as the checkbox text
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a boolean property
   */
  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel,
                                         final StateObserver enabledState, final boolean includeCaption) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    checkProperty(property, editModel);
    if (!property.isBoolean()) {
      throw new IllegalArgumentException("Boolean property required for createCheckBox");
    }

    final JCheckBox checkBox = includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox();
    ValueLinks.toggleValueLink(checkBox.getModel(),
            EditModelValues.<Boolean>value(editModel, property.getPropertyID()), false);
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

  /**
   * Creates a TristateCheckBox based on the given boolean property
   * @param property the property on which value to base the checkbox
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the checkbox
   * @param includeCaption if true then the property caption is included as the checkbox text
   * @return a check box based on the given property
   * @throws IllegalArgumentException in case the property is not a nullable boolean property
   */
  public static TristateCheckBox createTristateCheckBox(final Property property, final EntityEditModel editModel,
                                                        final StateObserver enabledState, final boolean includeCaption) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(property, editModel);
    if (!property.isBoolean() && property.isNullable()) {
      throw new IllegalArgumentException("Nullable boolean property required for createTristateCheckBox");
    }

    final TristateCheckBox checkBox = new TristateCheckBox(includeCaption ? property.getCaption() : null);
    ValueLinks.tristateValueLink((TristateButtonModel) checkBox.getModel(),
            EditModelValues.<Boolean>value(editModel, property.getPropertyID()), false);
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

  /**
   * Creates a combobox containing the values (null, yes, no) based on the given boolean property
   * @param property the property on which to base the combobox
   * @param editModel the edit model to bind with the value
   * @return a SteppedComboBox based on the given boolean property
   */
  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityEditModel editModel) {
    return createBooleanComboBox(property, editModel, null);
  }

  /**
   * Creates a combobox containing the values (null, yes, no) based on the given boolean property
   * @param property the property on which to base the combobox
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the combobox
   * @return a SteppedComboBox based on the given boolean property
   */
  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityEditModel editModel,
                                                      final StateObserver enabledState) {
    final SteppedComboBox box = createComboBox(property, editModel, new BooleanComboBoxModel(), enabledState);
    box.setPopupWidth(BOOLEAN_COMBO_BOX_POPUP_WIDTH);

    return box;
  }

  /**
   * Creates EntityComboBox based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the combobox
   * @param editModel the edit model to bind with the value
   * @return a EntityComboBox based on the given foreign key property
   */
  public static EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                    final EntityEditModel editModel) {
    return createEntityComboBox(foreignKeyProperty, editModel, null);
  }

  /**
   * Creates EntityComboBox based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the combobox
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the combobox
   * @return a EntityComboBox based on the given foreign key property
   */
  public static EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                    final EntityEditModel editModel, final StateObserver enabledState) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(foreignKeyProperty, editModel);
    final EntityComboBoxModel boxModel = editModel.getEntityComboBoxModel(foreignKeyProperty);
    boxModel.refresh();
    final EntityComboBox comboBox = new EntityComboBox(boxModel);
    ValueLinks.selectedItemValueLink(comboBox, EditModelValues.value(editModel, foreignKeyProperty.getPropertyID()));
    UiUtil.linkToEnabledState(enabledState, comboBox);
    MaximumMatch.enable(comboBox);
    comboBox.setToolTipText(foreignKeyProperty.getDescription());
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      //getEditor().getEditorComponent() only required because the combo box is editable, due to MaximumMatch.enable() above
      UiUtil.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
    }

    return comboBox;
  }

  public static JTextField createEntityField(final Property.ForeignKeyProperty foreignKeyProperty,
                                             final EntityEditModel editModel) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(foreignKeyProperty, editModel);
    final JTextField textField = new JTextField();
    textField.setEditable(false);
    textField.setFocusable(false);
    textField.setToolTipText(foreignKeyProperty.getDescription());
    editModel.addValueListener(foreignKeyProperty.getPropertyID(), new EventInfoListener<ValueChange>() {
      @Override
      public void eventOccurred(final ValueChange info) {
        textField.setText(info.getNewValue() == null ? "" : info.getNewValue().toString());
      }
    });
    return textField;
  }

  /**
   * Creates a EntityLookupField based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the lookup model
   * @param editModel the edit model to bind with the value
   * @return a lookup model based on the given foreign key property
   */
  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel) {
    return createEntityLookupField(foreignKeyProperty, editModel, (StateObserver) null);
  }

  /**
   * Creates a EntityLookupField based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the lookup model
   * @param editModel the edit model to bind with the value
   * @param searchPropertyIDs the propertyIDs to use when searching via this lookup field
   * @return a lookup model based on the given foreign key property
   */
  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel, final String... searchPropertyIDs) {
    return createEntityLookupField(foreignKeyProperty, editModel, null, searchPropertyIDs);
  }

  /**
   * Creates a EntityLookupField based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the lookup model
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the lookup field
   * @return a lookup model based on the given foreign key property
   */
  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel, final StateObserver enabledState) {
    final Collection<String> searchPropertyIDs = Entities.getSearchPropertyIDs(foreignKeyProperty.getReferencedEntityID());
    if (searchPropertyIDs.isEmpty()) {
      throw new IllegalArgumentException("No default search properties specified for entity: " + foreignKeyProperty.getReferencedEntityID()
              + ", unable to create EntityLookupField, you must specify the searchPropertyIDs");
    }

    return createEntityLookupField(foreignKeyProperty, editModel, enabledState, searchPropertyIDs.toArray(new String[searchPropertyIDs.size()]));
  }

  /**
   * Creates a EntityLookupField based on the given foreign key property
   * @param foreignKeyProperty the foreign key property on which entity to base the lookup model
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the lookup field
   * @param searchPropertyIDs the propertyIDs to use when searching via this lookup field
   * @return a lookup model based on the given foreign key property
   */
  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel, final StateObserver enabledState,
                                                          final String... searchPropertyIDs) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(foreignKeyProperty, editModel);
    if (searchPropertyIDs == null || searchPropertyIDs.length == 0) {
      throw new IllegalArgumentException("No search properties specified for entity lookup field: " + foreignKeyProperty.getReferencedEntityID());
    }

    final EntityLookupModel lookupModel = editModel.getEntityLookupModel(foreignKeyProperty);
    final EntityLookupField lookupField = new EntityLookupField(lookupModel);

    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      lookupField.setTransferFocusOnEnter();
    }
    Values.link(EditModelValues.<Entity>value(editModel, foreignKeyProperty.getPropertyID()),
            new LookupUIValue(lookupField.getModel()));
    UiUtil.linkToEnabledState(enabledState, lookupField);
    lookupField.setToolTipText(foreignKeyProperty.getDescription());
    UiUtil.selectAllOnFocusGained(lookupField);

    return lookupField;
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel) {
    return createValueListComboBox(property, editModel, true, null);
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the combo box
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel,
                                                        final StateObserver enabledState) {
    return createValueListComboBox(property, editModel, true, enabledState);
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param sortItems if true then the items are sorted
   * @return a combo box based on the given values
   */
  public static SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final EntityEditModel editModel,
                                                        final boolean sortItems) {
    return createValueListComboBox(property, editModel, sortItems, null);
  }

  /**
   * Creates a combo box based on the values in the given value list property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param sortItems if true then the items are sorted
   * @param enabledState the state controlling the enabled state of the combo box
   * @return a combo box based on the given values
   */
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

  /**
   * Creates a combo box based on the given combo box model
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param model the combo box model
   * @param enabledState the state controlling the enabled state of the combo box
   * @return a combo box based on the given model
   */
  public static SteppedComboBox createComboBox(final Property property, final EntityEditModel editModel,
                                               final ComboBoxModel model, final StateObserver enabledState) {
    return createComboBox(property, editModel, model, enabledState, false);
  }

  /**
   * Creates a combo box based on the given combo box model
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param model the combo box model
   * @param enabledState the state controlling the enabled state of the combo box
   * @param editable if true then the combo box is made editable
   * @return a combo box based on the given model
   */
  public static SteppedComboBox createComboBox(final Property property, final EntityEditModel editModel,
                                               final ComboBoxModel model, final StateObserver enabledState,
                                               final boolean editable) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    checkProperty(property, editModel);
    final SteppedComboBox comboBox = new SteppedComboBox(model);
    comboBox.setEditable(editable);
    ValueLinks.selectedItemValueLink(comboBox, EditModelValues.value(editModel, property.getPropertyID()));
    UiUtil.linkToEnabledState(enabledState, comboBox);
    comboBox.setToolTipText(property.getDescription());
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      UiUtil.transferFocusOnEnter(comboBox);
    }

    return comboBox;
  }

  /**
   * Creates a panel with a date input field and a button for opening a date input dialog
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the value is read only
   * @param includeButton if true then a button for opening a date input dialog is included
   * @return a date input panel
   */
  public static DateInputPanel createDateInputPanel(final Property property, final EntityEditModel editModel,
                                                    final boolean readOnly, final boolean includeButton) {
    return createDateInputPanel(property, editModel, readOnly, includeButton, null);
  }

  /**
   * Creates a panel with a date input field and a button for opening a date input dialog
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the value is read only
   * @param includeButton if true then a button for opening a date input dialog is included
   * @param enabledState the state controlling the enabled state of the panel
   * @return a date input panel
   */
  public static DateInputPanel createDateInputPanel(final Property property, final EntityEditModel editModel,
                                                    final boolean readOnly, final boolean includeButton,
                                                    final StateObserver enabledState) {
    Util.rejectNullValue(property,PROPERTY_PARAM_NAME);
    if (!property.isDateOrTime()) {
      throw new IllegalArgumentException("Property " + property + " is not a date or time property");
    }

    final JFormattedTextField field = (JFormattedTextField) createTextField(property, editModel, readOnly,
            DateUtil.getDateMask((SimpleDateFormat) property.getFormat()), true, enabledState);
    final DateInputPanel panel = new DateInputPanel(field, (SimpleDateFormat) property.getFormat(), includeButton, enabledState);
    if (panel.getButton() != null && Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter(panel.getButton());
    }

    return panel;
  }

  /**
   * Creates a panel with a text field and a button for opening a dialog with a text area
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the value is read only
   * @param immediateUpdate if true then the value is committed on each keystroke, otherwise on focus lost
   * @param buttonFocusable if true then the dialog button is focusable
   * @return a text input panel
   */
  public static TextInputPanel createTextInputPanel(final Property property, final EntityEditModel editModel,
                                                    final boolean readOnly, final boolean immediateUpdate,
                                                    final boolean buttonFocusable) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    final JTextField field = createTextField(property, editModel, readOnly, null, immediateUpdate);
    final TextInputPanel panel = new TextInputPanel(field, property.getCaption(), null, buttonFocusable);
    panel.setMaxLength(property.getMaxLength());
    if (panel.getButton() != null && Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter(panel.getButton());//todo
    }

    return panel;
  }

  /**
   * Creates a text area based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the value is read only
   * @return a text area
   */
  public static JTextArea createTextArea(final Property property, final EntityEditModel editModel,
                                         final boolean readOnly) {
    return createTextArea(property, editModel, readOnly, -1, -1, null);
  }

  /**
   * Creates a text area based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param readOnly if true then the value is read only
   * @param rows the number of rows
   * @param columns the number of columns
   * @return a text area
   */
  public static JTextArea createTextArea(final Property property, final EntityEditModel editModel,
                                         final boolean readOnly, final int rows, final int columns,
                                         final StateObserver enabledState) {
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
    if (enabledState != null) {
      UiUtil.linkToEnabledState(enabledState, textArea);
    }

    ValueLinks.textValueLink(textArea, EditModelValues.<String>value(editModel, property.getPropertyID()), null, true, readOnly);
    ValueLinkValidators.addValidator(property.getPropertyID(), textArea, editModel);
    textArea.setToolTipText(property.getDescription());

    return textArea;
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final EntityEditModel editModel) {
    return createTextField(property, editModel, false, null, true);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param immediateUpdate if true then the value is committed on each keystroke, otherwise on focus lost
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final boolean readOnly, final String formatMaskString,
                                           final boolean immediateUpdate) {
    return createTextField(property, editModel, readOnly, formatMaskString, immediateUpdate, null);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param immediateUpdate if true then the value is committed on each keystroke, otherwise on focus lost
   * @param enabledState the state controlling the enabled state of the panel
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final boolean readOnly, final String formatMaskString,
                                           final boolean immediateUpdate, final StateObserver enabledState) {
    return createTextField(property, editModel, readOnly, formatMaskString, immediateUpdate, enabledState, false);
  }

  /**
   * Creates a text field based on the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param formatMaskString if specified the resulting text field is a JFormattedField with this mask
   * @param immediateUpdate if true then the value is committed on each keystroke, otherwise on focus lost
   * @param enabledState the state controlling the enabled state of the panel
   * @param valueContainsLiteralCharacters whether or not the value should contain any literal characters
   * associated with a the format mask
   * @return a text field for the given property
   */
  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final boolean readOnly, final String formatMaskString,
                                           final boolean immediateUpdate, final StateObserver enabledState,
                                           final boolean valueContainsLiteralCharacters) {
    Util.rejectNullValue(property, PROPERTY_PARAM_NAME);
    Util.rejectNullValue(editModel, EDIT_MODEL_PARAM_NAME);
    checkProperty(property, editModel);
    final JTextField textField = initializeTextField(property, editModel, enabledState, formatMaskString, valueContainsLiteralCharacters);
    final String propertyID = property.getPropertyID();
    if (property.isString()) {
      ValueLinks.textValueLink(textField, EditModelValues.<String>value(editModel, propertyID), null, immediateUpdate, readOnly);
    }
    else if (property.isInteger()) {
      ValueLinks.intValueLink((IntField) textField, EditModelValues.<Integer>value(editModel, propertyID),
              (NumberFormat) property.getFormat(), false, readOnly);
    }
    else if (property.isDouble()) {
      ValueLinks.doubleValueLink((DoubleField) textField, EditModelValues.<Double>value(editModel, propertyID),
              (NumberFormat) property.getFormat(), false, readOnly);
    }
    else if (property.isDateOrTime()) {
      ValueLinks.dateValueLink((JFormattedTextField) textField, EditModelValues.<Date>value(editModel, propertyID),
              readOnly, (SimpleDateFormat) property.getFormat(), property.getType());
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

  /**
   * Creates a combo box based on the values of the given property
   * @param propertyID the propertyID
   * @param editModel the edit model to bind with the value
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityEditModel editModel) {
    return createPropertyComboBox(propertyID, editModel, null);
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param propertyID the propertyID
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the panel
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityEditModel editModel,
                                                       final StateObserver enabledState) {
    return createPropertyComboBox(Entities.getColumnProperty(editModel.getEntityID(), propertyID),
            editModel, enabledState);
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel) {
    return createPropertyComboBox(property, editModel, null);
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the panel
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final StateObserver enabledState) {
    return createPropertyComboBox(property, editModel, enabledState, false);
  }

  /**
   * Creates a combo box based on the values of the given property
   * @param property the property
   * @param editModel the edit model to bind with the value
   * @param enabledState the state controlling the enabled state of the panel
   * @param editable if true then the combo box will be editable
   * @return a combo box based on the property values
   */
  public static SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final EntityEditModel editModel,
                                                       final StateObserver enabledState, final boolean editable) {
    final SteppedComboBox comboBox = createComboBox(property, editModel, editModel.getPropertyComboBoxModel(property), enabledState, editable);
    if (!editable) {
      MaximumMatch.enable(comboBox);
    }

    return comboBox;
  }

  /**
   * Creates a panel containing a EntityLookupField and a button for opening a entity table panel for
   * selecting entities
   * @param lookupField the lookup field
   * @param tableModel the table model
   * @return a lookup field panel
   */
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

  /**
   * Creates a panel containing an EntityComboBox and a button for creating a new entity for that combo box
   * @param entityComboBox the combo box
   * @param panelProvider the EntityPanelProvider to use when creating a panel for inserting a new record
   * @param newRecordButtonTakesFocus if true then the new record button is focusable
   * @return a panel with a combo box and a button
   */
  public static JPanel createEntityComboBoxPanel(final EntityComboBox entityComboBox, final EntityPanelProvider panelProvider,
                                                 final boolean newRecordButtonTakesFocus) {
    return createEastButtonPanel(entityComboBox, new CreateEntityAction(entityComboBox, panelProvider), newRecordButtonTakesFocus);
  }

  /**
   * Creates a panel containing an EntityLookupField and a button for creating a new entity for that field
   * @param entityLookupField the lookup field
   * @param panelProvider the EntityPanelProvider to use when creating a panel for inserting a new record
   * @param newRecordButtonTakesFocus if true then the new record button is focusable
   * @return a panel with a lookup field and a button
   */
  public static JPanel createEntityLookupFieldPanel(final EntityLookupField entityLookupField, final EntityPanelProvider panelProvider,
                                                    final boolean newRecordButtonTakesFocus) {
    return createEastButtonPanel(entityLookupField, new CreateEntityAction(entityLookupField, panelProvider), newRecordButtonTakesFocus);
  }

  /**
   * Creates a panel containing an EntityComboBox and a button for filtering that combo box based on a foreign key
   * @param entityComboBox the combo box
   * @param foreignKeyPropertyID the foreign key to base the filtering on
   * @param filterButtonTakesFocus if true then the filter button is focusable
   * @return a panel with a combo box and a button
   */
  public static JPanel createEntityComboBoxFilterPanel(final EntityComboBox entityComboBox, final String foreignKeyPropertyID,
                                                       final boolean filterButtonTakesFocus) {
    return createEastButtonPanel(entityComboBox, entityComboBox.createForeignKeyFilterAction(foreignKeyPropertyID),
            filterButtonTakesFocus);
  }

  public static void showEntityMenu(final Entity entity, final JComponent component, final Point location,
                                    final EntityConnectionProvider connectionProvider) {
    if (entity != null) {
      final JPopupMenu popupMenu = new JPopupMenu();
      populateEntityMenu(popupMenu, (Entity) entity.getCopy(), connectionProvider);
      popupMenu.show(component, location.x, (int) location.getY());
    }
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
      if (property.getMin() != null && property.getMax() != null) {
        ((IntField) field).setRange(property.getMin(), property.getMax());
      }
    }
    else if (property.isDouble()) {
      field = new DoubleField();
      if (property.getMaximumFractionDigits() > 0) {
        ((DoubleField) field).setMaximumFractionDigits(property.getMaximumFractionDigits());
      }
      if (property.getMin() != null && property.getMax() != null) {
        ((DoubleField) field).setRange(Math.min(property.getMin(), 0), property.getMax());
      }
    }
    else if (property.isDateOrTime()) {
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
    field.setToolTipText(property.getDescription());
    if (property.getMaxLength() > 0 && field.getDocument() instanceof SizedDocument) {
      ((SizedDocument) field.getDocument()).setMaxLength(property.getMaxLength());
    }
    if (property instanceof Property.ColumnProperty) {//todo property.allowLookup()?
      UiUtil.addLookupDialog(field, editModel.getValueProvider(property));
    }

    return field;
  }

  private static void checkProperty(final Property property, final EntityEditModel editModel) {
    if (!property.getEntityID().equals(editModel.getEntityID())) {
      throw new IllegalArgumentException("Entity type mismatch: " + property.getEntityID() + ", should be: " + editModel.getEntityID());
    }
  }

  /**
   * Populates the given root menu with the property values of the given entity
   * @param rootMenu the menu to populate
   * @param entity the entity
   * @param connectionProvider if provided then lazy loaded entity references are loaded so that the full object graph can be shown
   */
  private static void populateEntityMenu(final JComponent rootMenu, final Entity entity,
                                         final EntityConnectionProvider connectionProvider) {
    populatePrimaryKeyMenu(rootMenu, entity, new ArrayList<>(Entities.getPrimaryKeyProperties(entity.getEntityID())));
    populateForeignKeyMenu(rootMenu, entity, connectionProvider, new ArrayList<>(Entities.getForeignKeyProperties(entity.getEntityID())));
    populateValueMenu(rootMenu, entity, new ArrayList<>(Entities.getProperties(entity.getEntityID(), true)));
  }

  private static void populatePrimaryKeyMenu(final JComponent rootMenu, final Entity entity, final List<Property.PrimaryKeyProperty> primaryKeyProperties) {
    Util.collate(primaryKeyProperties);
    for (final Property.PrimaryKeyProperty property : primaryKeyProperties) {
      String value = "[PK] " + property.getColumnName() + ": " + entity.getValueAsString(property.getPropertyID());
      if (entity.isModified(property.getPropertyID())) {
        value += getOriginalValue(entity, property);
      }
      final JMenuItem menuItem = new JMenuItem(value);
      menuItem.setToolTipText(property.getColumnName());
      rootMenu.add(menuItem);
    }
  }

  private static void populateForeignKeyMenu(final JComponent rootMenu, final Entity entity,
                                             final EntityConnectionProvider connectionProvider,
                                             final List<Property.ForeignKeyProperty> fkProperties) {
    try {
      Util.collate(fkProperties);
      final Entity.Validator validator = Entities.getValidator(entity.getEntityID());
      for (final Property.ForeignKeyProperty property : fkProperties) {
        final boolean fkValueNull = entity.isForeignKeyNull(property);
        final boolean isLoaded = entity.isLoaded(property.getPropertyID());
        final boolean valid = isValid(validator, entity, property);
        final String toolTipText = getReferenceColumnNames(property);
        if (!fkValueNull) {
          final Entity referencedEntity;
          if (isLoaded) {
            referencedEntity = entity.getForeignKeyValue(property.getPropertyID());
          }
          else {
            referencedEntity = connectionProvider.getConnection().selectSingle(entity.getReferencedPrimaryKey(property));
            entity.removeValue(property.getPropertyID());
            entity.setValue(property, referencedEntity);
          }
          final String text = "[FK" + (isLoaded ? "] " : "+] ") + property.getCaption() + ": " + referencedEntity.toString();
          final JMenu foreignKeyMenu = new JMenu(text);
          if (!valid) {
            setInvalid(foreignKeyMenu);
          }
          foreignKeyMenu.setToolTipText(toolTipText);
          populateEntityMenu(foreignKeyMenu, entity.getForeignKeyValue(property.getPropertyID()), connectionProvider);
          rootMenu.add(foreignKeyMenu);
        }
        else {
          final String text = "[FK] " + property.getCaption() + ": <null>";
          final JMenuItem menuItem = new JMenuItem(text);
          if (!valid) {
            setInvalid(menuItem);
          }
          menuItem.setToolTipText(toolTipText);
          rootMenu.add(menuItem);
        }
      }
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private static String getReferenceColumnNames(final Property.ForeignKeyProperty property) {
    final List<String> columnNames = new ArrayList<>(property.getReferenceProperties().size());
    for (final Property.ColumnProperty referenceProperty : property.getReferenceProperties()) {
      columnNames.add(referenceProperty.getColumnName());
    }

    return Util.getArrayContentsAsString(columnNames.toArray(), false);
  }

  private static void populateValueMenu(final JComponent rootMenu, final Entity entity, final List<Property> properties) {
    Util.collate(properties);
    final int maxValueLength = 20;
    final Entity.Validator validator = Entities.getValidator(entity.getEntityID());
    for (final Property property : properties) {
      final boolean valid = isValid(validator, entity, property);
      final boolean isForeignKeyProperty = property instanceof Property.ColumnProperty
              && ((Property.ColumnProperty) property).isForeignKeyProperty();
      if (!isForeignKeyProperty && !(property instanceof Property.ForeignKeyProperty)) {
        final String prefix = "[" + property.getTypeClass().getSimpleName().substring(0, 1)
                + (property instanceof Property.DenormalizedViewProperty ? "*" : "")
                + (property instanceof Property.DenormalizedProperty ? "+" : "") + "] ";
        final String value = entity.isValueNull(property.getPropertyID()) ? "<null>" : entity.getValueAsString(property.getPropertyID());
        final boolean longValue = value != null && value.length() > maxValueLength;
        final JMenuItem menuItem = new JMenuItem(prefix + property + ": " + (longValue ? value.substring(0, maxValueLength) + "..." : value));
        if (!valid) {
          setInvalid(menuItem);
        }
        String toolTipText = "";
        if (property instanceof Property.ColumnProperty) {
          toolTipText = ((Property.ColumnProperty) property).getColumnName();
        }
        if (longValue) {
          toolTipText += (value.length() > 1000 ? value.substring(0, 1000) : value);
        }
        menuItem.setToolTipText(toolTipText);
        rootMenu.add(menuItem);
      }
    }
  }

  private static void setInvalid(final JMenuItem menuItem) {
    final Font currentFont = menuItem.getFont();
    menuItem.setBackground(INVALID_COLOR);
    menuItem.setFont(new Font(currentFont.getName(), Font.BOLD, currentFont.getSize()));
  }

  private static String getOriginalValue(final Entity entity, final Property property) {
    final Object originalValue = entity.getOriginalValue(property.getPropertyID());

    return " | " + (originalValue == null ? "<null>" : originalValue.toString());
  }

  private static boolean isValid(final Entity.Validator validator, final Entity entity, final Property property) {
    try {
      validator.validate(entity, property.getPropertyID());
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  private  static final class LookupUIValue implements Value<Entity> {
    private final Event changeEvent = Events.event();
    private final EntityLookupModel lookupModel;

    private LookupUIValue(final EntityLookupModel lookupModel) {
      this.lookupModel = lookupModel;
      this.lookupModel.addSelectedEntitiesListener(changeEvent);
    }

    /** {@inheritDoc} */
    @Override
    public void set(final Entity value) {
      lookupModel.setSelectedEntity(value);
    }

    /** {@inheritDoc} */
    @Override
    public Entity get() {
      final Collection<Entity> selectedEntities = lookupModel.getSelectedEntities();
      return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
    }

    /** {@inheritDoc} */
    @Override
    public EventObserver getChangeObserver() {
      return changeEvent.getObserver();
    }
  }

  private static final class CreateEntityAction extends AbstractAction {

    private final JComponent component;
    private final EntityDataProvider dataProvider;
    private final EntityPanelProvider panelProvider;
    private final List<Entity> lastInsertedEntities = new ArrayList<>();

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

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(final ActionEvent e) {
      final EntityEditPanel editPanel = panelProvider.createEditPanel(dataProvider.getConnectionProvider());
      editPanel.initializePanel();
      editPanel.getEditModel().addAfterInsertListener(new EventInfoListener<EntityEditModel.InsertEvent>() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred(final EntityEditModel.InsertEvent info) {
          lastInsertedEntities.clear();
          lastInsertedEntities.addAll(info.getInsertedEntities());
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

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(final ActionEvent e) {
      editPanel.setInitialFocus();
    }
  }
}
