/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.AggregateState;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.DefaultExceptionHandler;
import org.jminor.common.ui.ExceptionHandler;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.checkbox.TristateCheckBox;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.valuemap.ValueChangeMapEditPanel;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A UI component based on the EntityEditModel.
 * @see org.jminor.framework.client.model.EntityEditModel
 */
public abstract class EntityEditPanel extends ValueChangeMapEditPanel<String, Object> implements ExceptionHandler {

  private static final Logger LOG = Util.getLogger(EntityEditPanel.class);

  public static final int CONFIRM_TYPE_INSERT = 1;
  public static final int CONFIRM_TYPE_UPDATE = 2;
  public static final int CONFIRM_TYPE_DELETE = 3;

  public static final int ACTION_NONE = 0;
  public static final int ACTION_INSERT = 1;
  public static final int ACTION_UPDATE = 2;
  public static final int ACTION_DELETE = 3;

  //Control codes
  public static final String INSERT = "insert";
  public static final String UPDATE = "update";
  public static final String DELETE = "delete";
  public static final String REFRESH = "refresh";
  public static final String CLEAR = "clear";

  private final Map<String, Control> controlMap = new HashMap<String, Control>();

  /**
   * Instantiates a new EntityEditPanel based on the provided EntityEditModel
   * @param editModel the EntityEditModel instance to base this EntityEditPanel on
   */
  public EntityEditPanel(final EntityEditModel editModel) {
    super(editModel);
    setupControls();
    initializeUI();
    bindEventsInternal();
    bindEvents();
  }

  @Override
  public EntityEditModel getEditModel() {
    return (EntityEditModel) super.getEditModel();
  }

  /**
   * @return the ControlSet on which to base the control panel
   */
  public ControlSet getControlPanelControlSet() {
    final ControlSet controlSet = new ControlSet("Actions");
    if (controlMap.containsKey(INSERT)) {
      controlSet.add(controlMap.get(INSERT));
    }
    if (controlMap.containsKey(UPDATE)) {
      controlSet.add(controlMap.get(UPDATE));
    }
    if (controlMap.containsKey(DELETE)) {
      controlSet.add(controlMap.get(DELETE));
    }
    if (controlMap.containsKey(CLEAR)) {
      controlSet.add(controlMap.get(CLEAR));
    }
    if (controlMap.containsKey(REFRESH)) {
      controlSet.add(controlMap.get(REFRESH));
    }

    return controlSet;
  }

  /**
   * @param controlCode the control code
   * @return the control associated with <code>controlCode</code>
   * @throws RuntimeException in case no control is associated with the given control code
   */
  public final Control getControl(final String controlCode) {
    if (!controlMap.containsKey(controlCode)) {
      throw new RuntimeException(controlCode + " control not available in panel: " + this);
    }

    return controlMap.get(controlCode);
  }

  /**
   * @return a control for refreshing the model data
   */
  public Control getRefreshControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.REFRESH_MNEMONIC);
    return ControlFactory.methodControl(getEditModel(), "refresh", FrameworkMessages.get(FrameworkMessages.REFRESH),
            getEditModel().stateActive(), FrameworkMessages.get(FrameworkMessages.REFRESH_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_REFRESH_16));
  }

  /**
   * @return a control for deleting the active entity
   */
  public Control getDeleteControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.DELETE_MNEMONIC);
    return ControlFactory.methodControl(this, "delete", FrameworkMessages.get(FrameworkMessages.DELETE),
            new AggregateState(AggregateState.Type.AND,
                    getEditModel().stateActive(),
                    getEditModel().stateAllowDelete(),
                    getEditModel().stateEntityNull().getReversedState()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP) + " (ALT-" + mnemonic + ")", mnemonic.charAt(0), null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for clearing the UI controls
   */
  public Control getClearControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.CLEAR_MNEMONIC);
    return ControlFactory.methodControl(getEditModel(), "clearValues", FrameworkMessages.get(FrameworkMessages.CLEAR),
            getEditModel().stateActive(), FrameworkMessages.get(FrameworkMessages.CLEAR_ALL_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_NEW_16));
  }

  /**
   * @return a control for performing an update on the active entity
   */
  public Control getUpdateControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.UPDATE_MNEMONIC);
    return ControlFactory.methodControl(this, "update", FrameworkMessages.get(FrameworkMessages.UPDATE),
            new AggregateState(AggregateState.Type.AND,
                    getEditModel().stateActive(),
                    getEditModel().stateAllowUpdate(),
                    getEditModel().stateEntityNull().getReversedState(),
                    getEditModel().stateModified()),
            FrameworkMessages.get(FrameworkMessages.UPDATE_TIP) + " (ALT-" + mnemonic + ")", mnemonic.charAt(0),
            null, Images.loadImage(Images.IMG_SAVE_16));
  }

  /**
   * @return a control for performing an insert on the active entity
   */
  public Control getInsertControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.INSERT_MNEMONIC);
    return ControlFactory.methodControl(this, "save", FrameworkMessages.get(FrameworkMessages.INSERT),
            new AggregateState(AggregateState.Type.AND, getEditModel().stateActive(), getEditModel().stateAllowInsert()),
            FrameworkMessages.get(FrameworkMessages.INSERT_TIP) + " (ALT-" + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage("Add16.gif"));
  }

  /**
   * @return a control for performing a save on the active entity
   */
  public Control getSaveControl() {
    final String insertCaption = FrameworkMessages.get(FrameworkMessages.INSERT_UPDATE);
    final State stInsertUpdate = new AggregateState(AggregateState.Type.OR, getEditModel().stateAllowInsert(),
            new AggregateState(AggregateState.Type.AND, getEditModel().stateAllowUpdate(),
                    getEditModel().stateModified()));
    return ControlFactory.methodControl(this, "save", insertCaption,
            new AggregateState(AggregateState.Type.AND, getEditModel().stateActive(), stInsertUpdate),
            FrameworkMessages.get(FrameworkMessages.INSERT_UPDATE_TIP),
            insertCaption.charAt(0), null, Images.loadImage(Images.IMG_PROPERTIES_16));
  }

  public void handleException(final Throwable throwable) {
    if (throwable instanceof ValidationException) {
      JOptionPane.showMessageDialog(this, throwable.getMessage(), Messages.get(Messages.EXCEPTION),
              JOptionPane.ERROR_MESSAGE);
      selectComponent((String) ((ValidationException) throwable).getKey());
    }
    else {
      handleException(throwable, this);
    }
  }

  /**
   * Handles the given exception
   * @param exception the exception to handle
   * @param dialogParent the component to use as exception dialog parent
   */
  public void handleException(final Throwable exception, final JComponent dialogParent) {
    LOG.error(this, exception);
    DefaultExceptionHandler.getInstance().handleException(exception, dialogParent);
  }

  /**
   * Initializes the control panel, that is, the panel containing buttons for editing entities (Insert, Update...)
   * @param horizontal true if the buttons should be layed out horizontally, false otherwise
   * @return the control panel
   */
  public JPanel createControlPanel(final boolean horizontal) {
    if (horizontal) {
      final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER,5,5));
      panel.add(ControlProvider.createHorizontalButtonPanel(getControlPanelControlSet()));
      return panel;
    }
    else {
      final JPanel panel = new JPanel(new BorderLayout(5,5));
      panel.add(ControlProvider.createVerticalButtonPanel(getControlPanelControlSet()), BorderLayout.NORTH);
      return panel;
    }
  }

  /**
   * Initializes the control toolbar, that is, the toolbar containing buttons for editing entities (Insert, Update...)
   * @return the control toolbar
   */
  public JToolBar getControlToolBar() {
    return ControlProvider.createToolbar(getControlPanelControlSet(), JToolBar.VERTICAL);
  }

  //#############################################################################################
  // Begin - control methods, see setupControls
  //#############################################################################################

  /**
   * Saves the active entity, that is, if no entity is selected it performs a insert otherwise the user
   * is asked whether to update the selected entity or insert a new one
   */
  public final void save() {
    if (getEditModel().isEntityNew() || !getEditModel().isEntityModified() || !getEditModel().isUpdateAllowed()) {
      //no entity selected, selected entity is unmodified or update is not allowed, can only insert
      insert();
    }
    else {//possibly update
      final int choiceIdx = JOptionPane.showOptionDialog(this, FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT),
              FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT_TITLE), -1, JOptionPane.QUESTION_MESSAGE, null,
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_RECORD),
                      FrameworkMessages.get(FrameworkMessages.INSERT_NEW), Messages.get(Messages.CANCEL)},
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE)});
      if (choiceIdx == 0) //update
      {
        update(false);
      }
      else if (choiceIdx == 1) //insert
      {
        insert(false);
      }
    }
  }

  /**
   * Performs a insert on the active entity
   * @return true in case of successful insert, false otherwise
   */
  public final boolean insert() {
    return insert(true);
  }

  /**
   * Performs a insert on the active entity
   * @param confirm if true then confirmInsert() is called
   * @return true in case of successful insert, false otherwise
   */
  public final boolean insert(final boolean confirm) {
    try {
      if (!confirm || confirmInsert()) {
        validateData();
        try {
          UiUtil.setWaitCursor(true, this);
          getEditModel().insert();
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }
        prepareUI(true, true);
        return true;
      }
    }
    catch (Exception ex) {
      handleException(ex);
    }

    return false;
  }

  /**
   * Performs a delete on the active entity
   * @return true if the delete operation was successful
   */
  public final boolean delete() {
    return delete(true);
  }

  /**
   * Performs a delete on the active entity
   * @param confirm if true then confirmDelete() is called
   * @return true if the delete operation was successful
   */
  public final boolean delete(final boolean confirm) {
    try {
      if (!confirm || confirmDelete()) {
        try {
          UiUtil.setWaitCursor(true, this);
          getEditModel().delete();
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }

        return true;
      }
    }
    catch (Exception e) {
      handleException(e);
    }

    return false;
  }

  /**
   * Performs an update on the active entity
   * @return true if the update operation was successful
   */
  public final boolean update() {
    return update(true);
  }

  /**
   * Performs an update on the active entity
   * @param confirm if true then confirmUpdate() is called
   * @return true if the update operation was successful
   */
  public final boolean update(final boolean confirm) {
    try {
      if (!confirm || confirmUpdate()) {
        validateData();
        try {
          UiUtil.setWaitCursor(true, this);
          getEditModel().update();
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }
        prepareUI(true, false);

        return true;
      }
    }
    catch (Exception e) {
      handleException(e);
    }

    return false;
  }

  //#############################################################################################
  // End - control methods
  //#############################################################################################

  /**
   * for overriding, called before insert/update
   * @throws org.jminor.common.model.valuemap.exception.ValidationException in case of a validation failure
   * @throws org.jminor.common.model.CancelException in case the user cancels the action during validation
   */
  protected void validateData() throws ValidationException, CancelException {}

  /**
   * Called before a insert is performed, the default implementation simply returns true
   * @return true if a insert should be performed, false if it should be vetoed
   */
  protected boolean confirmInsert() {
    return true;
  }

  /**
   * Called before a delete is performed, if true is returned the delete action is performed otherwise it is canceled
   * @return true if the delete action should be performed
   */
  protected boolean confirmDelete() {
    final String[] messages = getConfirmationMessages(CONFIRM_TYPE_DELETE);
    return confirm(messages[0], messages[1]);
  }

  /**
   * Called before an update is performed, if true is returned the update action is performed otherwise it is cancelled
   * @return true if the update action should be performed
   */
  protected boolean confirmUpdate() {
    final String[] messages = getConfirmationMessages(CONFIRM_TYPE_UPDATE);
    return confirm(messages[0], messages[1]);
  }

  /**
   * Presents a OK/Cancel confirm dialog with the given message and title,
   * returns true if OK was selected.
   * @param message the message
   * @param title the dialog title
   * @return true if OK was selected
   */
  protected boolean confirm(final String message, final String title) {
    final int res = JOptionPane.showConfirmDialog(this, message, title, JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  /**
   * @param type the confirmation message type, one of the following:
   * EntityPanel.CONFIRM_TYPE_INSERT, EntityPanel.CONFIRM_TYPE_DELETE or EntityPanel.CONFIRM_TYPE_UPDATE
   * @return a string array containing two elements, the element at index 0 is used
   * as the message displayed in the dialog and the element at index 1 is used as the dialog title,
   * i.e. ["Are you sure you want to delete the selected records?", "About to delete selected records"]
   */
  protected String[] getConfirmationMessages(final int type) {
    switch (type) {
      case CONFIRM_TYPE_DELETE:
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_ENTITY),
            FrameworkMessages.get(FrameworkMessages.DELETE)};
      case CONFIRM_TYPE_INSERT:
        return FrameworkMessages.getDefaultConfirmInsertMessages();
      case CONFIRM_TYPE_UPDATE:
        return FrameworkMessages.getDefaultConfirmUpdateMessages();
    }

    throw new IllegalArgumentException("Unknown confirmation type constant: " + type);
  }

  /**
   * Initializes the controls available to this EntityEditPanel by mapping them to their respective
   * control codes (EntityEditPanel.INSERT, UPDATE etc) via the <code>setControl(String, Control) method,
   * these can then be retrieved via the <code>getControl(String)</code> method.
   * @see org.jminor.common.ui.control.Control
   * @see #setControl(String, org.jminor.common.ui.control.Control)
   * @see #getControl(String)
   */
  protected void setupControls() {
    if (!getEditModel().isReadOnly()) {
      if (getEditModel().isInsertAllowed()) {
        setControl(INSERT, getInsertControl());
      }
      if (getEditModel().isUpdateAllowed()) {
        setControl(UPDATE, getUpdateControl());
      }
      if (getEditModel().isDeleteAllowed()) {
        setControl(DELETE, getDeleteControl());
      }
    }
    setControl(CLEAR, getClearControl());
    setControl(REFRESH, getRefreshControl());
  }

  /**
   * Associates <code>control</code> with <code>controlCode</code>
   * @param controlCode the control code
   * @param control the control to associate with <code>controlCode</code>
   */
  protected void setControl(final String controlCode, final Control control) {
    if (control == null) {
      controlMap.remove(controlCode);
    }
    else {
      controlMap.put(controlCode, control);
    }
  }

  /**
   * Initializes this EntityEditPanel UI
   */
  protected abstract void initializeUI();

  /**
   * Performs event binding, for overriding
   */
  protected void bindEvents() {}

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * The default layout of the resulting panel is with the label on top and <code>inputComponent</code> below.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyID, final JComponent inputComponent) {
    return createPropertyPanel(propertyID, inputComponent, true);
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
  protected final JPanel createPropertyPanel(final String propertyID, final JComponent inputComponent,
                                             final boolean labelOnTop) {
    return createPropertyPanel(propertyID, inputComponent, labelOnTop, 5, 5);
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
  protected final JPanel createPropertyPanel(final String propertyID, final JComponent inputComponent,
                                             final boolean labelOnTop, final int hgap, final int vgap) {
    return createPropertyPanel(propertyID, inputComponent, labelOnTop, hgap, vgap, JLabel.LEADING);
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
  protected final JPanel createPropertyPanel(final String propertyID, final JComponent inputComponent,
                                             final boolean labelOnTop, final int hgap, final int vgap,
                                             final int labelAlignment) {
    return createPropertyPanel(EntityUiUtil.createLabel(EntityRepository.getProperty(getEditModel().getEntityID(),
            propertyID), labelAlignment), inputComponent, labelOnTop, hgap, vgap);
  }

  /**
   * Creates a panel containing a label component and the <code>inputComponent</code>.
   * @param labelComponent the label component
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final JComponent labelComponent, final JComponent inputComponent,
                                             final boolean labelOnTop) {
    return createPropertyPanel(labelComponent, inputComponent, labelOnTop, 5, 5);
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
  protected final JPanel createPropertyPanel(final JComponent labelComponent, final JComponent inputComponent,
                                             final boolean labelOnTop, final int hgap, final int vgap) {
    final JPanel panel = new JPanel(labelOnTop ?
            new BorderLayout(hgap, vgap) : new FlowLayout(FlowLayout.LEADING, hgap, vgap));
    if (labelComponent instanceof JLabel) {
      ((JLabel) labelComponent).setLabelFor(inputComponent);
    }
    if (labelOnTop) {
      panel.add(labelComponent, BorderLayout.NORTH);
      panel.add(inputComponent, BorderLayout.CENTER);
    }
    else {
      panel.add(labelComponent);
      panel.add(inputComponent);
    }

    return panel;
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
    final JTextArea textArea = EntityUiUtil.createTextArea(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
            getEditModel(), rows, columns);
    setComponent(propertyID, textArea);

    return textArea;
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
    return createTextInputPanel(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID), linkType,
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
    final TextInputPanel ret = EntityUiUtil.createTextInputPanel(property, getEditModel(), linkType, immediateUpdate, buttonFocusable);
    setComponent(property.getPropertyID(), ret.getTextComponent());

    return ret;
  }

  /**
   * Creates a new DateInputPanel using the default short date format, bound to the property
   * identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @return a DateInputPanel using the default short date format
   * @see org.jminor.framework.Configuration#DEFAULT_DATE_FORMAT
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID) {
    return createDateInputPanel(propertyID, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final SimpleDateFormat dateFormat) {
    return createDateInputPanel(propertyID, dateFormat, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel using the default short date format
   * @see org.jminor.framework.Configuration#DEFAULT_DATE_FORMAT
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final boolean includeButton) {
    return createDateInputPanel(propertyID, Configuration.getDefaultDateFormat(), includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton) {
    return createDateInputPanel(propertyID, dateFormat, includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton, final State enabledState) {
    return createDateInputPanel(propertyID, dateFormat, includeButton, enabledState, LinkType.READ_WRITE);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param linkType the property link type
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton, final State enabledState,
                                                      final LinkType linkType) {
    return createDateInputPanel(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
            dateFormat, includeButton, enabledState, linkType);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property) {
    return createDateInputPanel(property, (SimpleDateFormat) property.getFormat());
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final SimpleDateFormat dateFormat) {
    return createDateInputPanel(property, dateFormat, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton) {
    return createDateInputPanel(property, dateFormat, includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton, final State enabledState) {
    return createDateInputPanel(property, dateFormat, includeButton, enabledState, LinkType.READ_WRITE);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @param dateFormat the format to use for masking the input field
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param linkType the property link type
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final SimpleDateFormat dateFormat,
                                                      final boolean includeButton, final State enabledState,
                                                      final LinkType linkType) {
    final DateInputPanel panel = EntityUiUtil.createDateInputPanel(property, getEditModel(), dateFormat, linkType, includeButton, enabledState);
    setComponent(property.getPropertyID(), panel.getInputField());

    return panel;
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
    return createTextField(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
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
    final JTextField txt = EntityUiUtil.createTextField(property, getEditModel(), linkType, maskString, immediateUpdate,
            null, enabledState, valueIncludesLiteralCharacters);
    setComponent(property.getPropertyID(), txt);

    return txt;
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
    return createCheckBox(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID), enabledState, includeCaption);
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
    final JCheckBox box = EntityUiUtil.createCheckBox(property, getEditModel(), enabledState, includeCaption);
    setComponent(property.getPropertyID(), box);

    return box;
  }

  /**
   * Creates a TristateCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final String propertyID) {
    return createTristateCheckBox(propertyID, null);
  }

  /**
   * Creates a TristateCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final String propertyID, final State enabledState) {
    return createTristateCheckBox(propertyID, enabledState, true);
  }

  /**
   * Creates a TristateCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final String propertyID, final State enabledState,
                                                          final boolean includeCaption) {
    return createTristateCheckBox(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID), enabledState, includeCaption);
  }

  /**
   * Creates a TristateCheckBox bound to the given property
   * @param property the property to bind
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final Property property) {
    return createTristateCheckBox(property, null);
  }

  /**
   * Creates a TristateCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final Property property, final State enabledState) {
    return createTristateCheckBox(property, enabledState, true);
  }

  /**
   * Creates a TristateCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final Property property, final State enabledState,
                                                          final boolean includeCaption) {
    final TristateCheckBox box = EntityUiUtil.createTristateCheckBox(property, getEditModel(), enabledState, includeCaption);
    setComponent(property.getPropertyID(), box);

    return box;
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
    return createBooleanComboBox(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),enabledState);
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
    final JComboBox ret = EntityUiUtil.createBooleanComboBox(property, getEditModel(), enabledState);
    setComponent(property.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(propertyID, comboBoxModel, maximumMatch, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final State enabledState) {
    return createComboBox(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
            comboBoxModel, maximumMatch, enabledState);
  }

  /**
   * Creates a SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.common.ui.combobox.MaximumMatch
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
   * @see org.jminor.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final State enabledState) {
    final SteppedComboBox comboBox = EntityUiUtil.createComboBox(property, getEditModel(), comboBoxModel, enabledState);
    if (maximumMatch) {
      MaximumMatch.enable(comboBox);
    }
    setComponent(property.getPropertyID(), comboBox);

    return comboBox;
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyID the propertyID
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyID) {
    return createValueListComboBox(propertyID, null);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyID the propertyID
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyID, final State enabledState) {
    final Property property = EntityRepository.getProperty(getEditModel().getEntityID(), propertyID);
    if (!(property instanceof Property.ValueListProperty)) {
      throw new IllegalArgumentException("Property identified by '" + propertyID + "' is not a ValueListProperty");
    }

    return createValueListComboBox((Property.ValueListProperty) property, enabledState);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final Property.ValueListProperty property) {
    return createValueListComboBox(property, null);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final State enabledState) {
    final SteppedComboBox box = EntityUiUtil.createValueListComboBox(property, getEditModel(), enabledState);
    setComponent(property.getPropertyID(), box);

    return box;
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
    return createEditableComboBox(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
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
    final SteppedComboBox ret = EntityUiUtil.createComboBox(property, getEditModel(), comboBoxModel, enabledState, true);
    setComponent(property.getPropertyID(), ret);

    return ret;
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
   * @param nullValueString the value used to represent a null value, shown at the top of the combo box value list
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final State enabledState,
                                                         final String nullValueString) {
    return createPropertyComboBox(propertyID, enabledState, nullValueString, false);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param nullValueString the value used to represent a null value, shown at the top of the combo box value list
   * @param editable true if the combo box should be editable, only works with combo boxes based on String.class properties
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final State enabledState,
                                                         final String nullValueString, final boolean editable) {
    return createPropertyComboBox(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID),
            enabledState, nullValueString, editable);
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
   * @param nullValueString the value used to represent a null value, shown at the top of the combo box value list
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property property, final State enabledState,
                                                         final String nullValueString) {
    return createPropertyComboBox(property, enabledState, nullValueString, false);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param nullValueString the value used to represent a null value, shown at the top of the combo box value list
   * @param editable true if the combo box should be editable, only works with combo boxes based on String.class properties
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property property, final State enabledState,
                                                         final String nullValueString, final boolean editable) {
    final SteppedComboBox ret = EntityUiUtil.createPropertyComboBox(property, getEditModel(), null, enabledState, nullValueString, editable);
    setComponent(property.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates a EntityComboBox bound to the property identified by <code>propertyID</code>
   * @param foreignKeyPropertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final String foreignKeyPropertyID, final State enabledState) {
    return createEntityComboBox((Property.ForeignKeyProperty)
            EntityRepository.getProperty(getEditModel().getEntityID(), foreignKeyPropertyID), enabledState);
  }

  /**
   * Creates an EntityComboBox bound to the property identified by <code>propertyID</code>
   * @param foreignKeyPropertyID the ID of the foreign key property to bind
   * combination used to create new instances of the entity this EntityComboBox is based on
   * EntityComboBox is focusable
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final String foreignKeyPropertyID) {
    return createEntityComboBox(EntityRepository.getForeignKeyProperty(getEditModel().getEntityID(),
            foreignKeyPropertyID), null);
  }

  /**
   * Creates an EntityComboBox bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty) {
    return createEntityComboBox(foreignKeyProperty, null);
  }

  /**
   * Creates an EntityComboBox bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * combination used to create new instances of the entity this EntityComboBox is based on
   * EntityComboBox is focusable
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                      final State enabledState) {
    final EntityComboBox ret = EntityUiUtil.createEntityComboBox(foreignKeyProperty, getEditModel(), enabledState);
    setComponent(foreignKeyProperty.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates an EntityLookupField bound to the property identified by <code>propertyID</code>, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyID the ID of the foreign key property to bind
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final String foreignKeyPropertyID) {
    final Property.ForeignKeyProperty fkProperty = EntityRepository.getForeignKeyProperty(getEditModel().getEntityID(),
            foreignKeyPropertyID);
    final Collection<String> searchPropertyIDs = EntityRepository.getEntitySearchPropertyIDs(fkProperty.getReferencedEntityID());
    return createEntityLookupField(fkProperty, searchPropertyIDs.toArray(new String[searchPropertyIDs.size()]));
  }

  /**
   * Creates an EntityLookupField bound to the property identified by <code>propertyID</code>, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyID the ID of the foreign key property to bind
   * @param searchPropertyIDs the IDs of the properties to use in the lookup
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final String foreignKeyPropertyID,
                                                            final String... searchPropertyIDs) {
    final Property.ForeignKeyProperty fkProperty = EntityRepository.getForeignKeyProperty(getEditModel().getEntityID(),
            foreignKeyPropertyID);
    if (searchPropertyIDs == null || searchPropertyIDs.length == 0) {
      final Collection<String> propertyIDs = EntityRepository.getEntitySearchPropertyIDs(fkProperty.getReferencedEntityID());
      return createEntityLookupField(fkProperty, propertyIDs.toArray(new String[propertyIDs.size()]));
    }

    return createEntityLookupField(fkProperty, searchPropertyIDs);
  }

  /**
   * Creates an EntityLookupField bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @param searchPropertyIDs the IDs of the properties to use in the lookup
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                            final String... searchPropertyIDs) {
    final EntityLookupField ret = EntityUiUtil.createEntityLookupField(foreignKeyProperty, getEditModel(), searchPropertyIDs);
    setComponent(foreignKeyProperty.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates an uneditable JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createEntityField(final String propertyID) {
    return createEntityField(EntityRepository.getForeignKeyProperty(getEditModel().getEntityID(), propertyID));
  }

  /**
   * Creates an uneditable JTextField bound to the given property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createEntityField(final Property.ForeignKeyProperty foreignKeyProperty) {
    final JTextField ret = EntityUiUtil.createEntityField(foreignKeyProperty, getEditModel());
    setComponent(foreignKeyProperty.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates a JPanel containing an uneditable JTextField bound to the property identified by <code>propertyID</code>
   * and a button for selecting an Entity to set as the property value
   * @param propertyID the ID of the property to bind
   * @param lookupModel an EntityTableModel to use when looking up entities
   * @return an uneditable JTextField bound to the property
   */
  protected final JPanel createEntityFieldPanel(final String propertyID, final EntityTableModel lookupModel) {
    return createEntityFieldPanel((Property.ForeignKeyProperty)
            EntityRepository.getProperty(getEditModel().getEntityID(), propertyID), lookupModel);
  }

  /**
   * Creates a JPanel containing an uneditable JTextField bound to the given property identified
   * and a button for selecting an Entity to set as the property value
   * @param foreignKeyProperty the foreign key property to bind
   * @param lookupModel an EntityTableModel to use when looking up entities
   * @return an uneditable JTextField bound to the property
   */
  protected final EntityUiUtil.EntityFieldPanel createEntityFieldPanel(final Property.ForeignKeyProperty foreignKeyProperty,
                                                                       final EntityTableModel lookupModel) {
    final EntityUiUtil.EntityFieldPanel ret = EntityUiUtil.createEntityFieldPanel(foreignKeyProperty, getEditModel(), lookupModel);
    setComponent(foreignKeyProperty.getPropertyID(), ret.getTextField());

    return ret;
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
    return EntityUiUtil.createLabel(EntityRepository.getProperty(getEditModel().getEntityID(), propertyID), horizontalAlignment);
  }

  private void bindEventsInternal() {
    getEditModel().eventRefreshStarted().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        UiUtil.setWaitCursor(true, EntityEditPanel.this);
      }
    });
    getEditModel().eventRefreshDone().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        UiUtil.setWaitCursor(false, EntityEditPanel.this);
      }
    });
  }
}
