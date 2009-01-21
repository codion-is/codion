/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.DbException;
import org.jminor.common.db.ICriteria;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.model.formats.AbstractDateMaskFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.common.ui.textfield.TextFieldPlus;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.combobox.BooleanComboBoxModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.client.ui.property.CheckBoxPropertyLink;
import org.jminor.framework.client.ui.property.ComboBoxPropertyLink;
import org.jminor.framework.client.ui.property.DateTextPropertyLink;
import org.jminor.framework.client.ui.property.DoubleTextPropertyLink;
import org.jminor.framework.client.ui.property.IntTextPropertyLink;
import org.jminor.framework.client.ui.property.SearchFieldPropertyLink;
import org.jminor.framework.client.ui.property.TextPropertyLink;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.PropertyChangeEvent;
import org.jminor.framework.model.PropertyListener;
import org.jminor.framework.model.Type;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;
import org.apache.log4j.Level;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class FrameworkUiUtil {

  public static final Dimension DIMENSION18x18 = new Dimension(18,18);

  private FrameworkUiUtil() {}

  public static void previewReport(final JasperPrint jp, final Container dialogParent) {
    JRViewer viewer = new JRViewer(jp);
    JDialog dlg = new JDialog(UiUtil.getParentWindow(dialogParent), Dialog.ModalityType.APPLICATION_MODAL);
    dlg.getContentPane().add(viewer);
    dlg.pack();
    dlg.setLocationRelativeTo(dialogParent);
    UiUtil.centerWindow(dlg);
    dlg.setVisible(true);
  }

  public static void setLoggingLevel(final JComponent dialogParent) {
    final DefaultComboBoxModel model = new DefaultComboBoxModel(
            new Object[] {Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG});
    model.setSelectedItem(Util.getLoggingLevel());
    JOptionPane.showMessageDialog(dialogParent, new JComboBox(model),
            FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL), JOptionPane.QUESTION_MESSAGE);
    Util.setLoggingLevel((Level) model.getSelectedItem());
  }

  public static void handleException(final Throwable exception, final String entityID, final JComponent dialogParent) {
    if (exception instanceof UserCancelException)
      return;
    if (exception instanceof DbException)
      handleDbException((DbException) exception, entityID, dialogParent);
    else if (exception instanceof JRException && exception.getCause() != null)
      handleException(exception.getCause(), entityID, dialogParent);
    else if (exception instanceof UserException && exception.getCause() instanceof DbException)
      handleDbException((DbException) exception.getCause(), entityID, dialogParent);
    else {
      ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent), getMessageTitle(exception), exception.getMessage(), exception);
    }
  }

  private static String getMessageTitle(final Throwable e) {
    if (e instanceof FileNotFoundException)
      return FrameworkMessages.get(FrameworkMessages.UNABLE_TO_OPEN_FILE);

    return Messages.get(Messages.EXCEPTION);
  }

  public static void handleDbException(final DbException dbException, final String entityID,
                                       final JComponent dialogParent) {
    if (dbException.isInsertNullValueException()) {
      String columnName = dbException.getNullErrorColumnName().toLowerCase();
      if (entityID != null) {
        if (EntityRepository.get().hasProperty(entityID, columnName)) {
          final Property property = EntityRepository.get().getProperty(entityID, columnName);
          if (property.getCaption() != null)
            columnName = property.getCaption();
        }
      }

      ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent),
              Messages.get(Messages.EXCEPTION),
              FrameworkMessages.get(FrameworkMessages.VALUE_MISSING) + ": " + columnName, dbException);
    }
    else {
      String errMsg = dbException.getORAErrorMessage();
      if (errMsg == null || errMsg.length() == 0) {
        if (dbException.getCause() == null)
          errMsg = trimMessage(dbException);
        else
          errMsg = trimMessage(dbException.getCause());
      }
      ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent),
              Messages.get(Messages.EXCEPTION), errMsg, dbException);
    }
  }

  private static String trimMessage(final Throwable e) {
    final String msg = e.getMessage();
    if (msg.length() > 50)
      return msg.substring(0, 50) + "...";

    return msg;
  }

  public static List<Entity> selectEntities(final EntityTableModel lookupModel, final Window owner,
                                            final boolean singleSelection, final String dialogTitle) throws UserCancelException {
    return selectEntities(lookupModel, owner, singleSelection, dialogTitle, null, false);
  }

  public static List<Entity> selectEntities(final EntityTableModel lookupModel, final Window owner,
                                            final boolean singleSelection, final String dialogTitle,
                                            final Dimension preferredSize, final boolean simpleSearchPanel) throws UserCancelException {
    final ArrayList<Entity> selected = new ArrayList<Entity>();
    final JDialog dialog = new JDialog(owner, dialogTitle);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
      public void actionPerformed(ActionEvent e) {
        final List<Entity> entities = lookupModel.getSelectedEntities();
        for (final Entity entity : entities)
          selected.add(entity);
        dialog.dispose();
      }
    };
    final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
      public void actionPerformed(ActionEvent e) {
        selected.add(null);//hack to indicate cancel
        dialog.dispose();
      }
    };

    final EntityTablePanel entityPanel = new EntityTablePanel(lookupModel, null, false) {
      protected void bindEvents() {
        super.bindEvents();
        evtTableDoubleClick.addListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (!getTableModel().getSelectionModel().isSelectionEmpty())
              okAction.actionPerformed(e);
          }
        });
      }
      protected JPanel initializeSearchPanel() {
        return simpleSearchPanel ? initializeSimpleSearchPanel() : initializeAdvancedSearchPanel();
      }
    };
    entityPanel.setSearchPanelVisible(true);
    if (singleSelection)
      entityPanel.getJTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    final Action searchAction = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(ActionEvent e) {
        try {
          lookupModel.refresh();
          if (lookupModel.getRowCount() > 0) {
            lookupModel.setSelectedItemIndexes(new int[] {0});
            entityPanel.getJTable().requestFocusInWindow();
          }
          else {
            JOptionPane.showMessageDialog(UiUtil.getParentWindow(entityPanel),
                    FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CRITERIA));
          }
        }
        catch (UserException e1) {
          throw e1.getRuntimeException();
        }
      }
    };

    final JButton btnClose  = new JButton(okAction);
    final JButton btnCancel = new JButton(cancelAction);
    final JButton btnSearch = new JButton(searchAction);
    dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    dialog.getRootPane().getActionMap().put("cancel", cancelAction);
    entityPanel.getJTable().getInputMap(
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    btnClose.setMnemonic('L');
    btnCancel.setMnemonic('H');
    dialog.setLayout(new BorderLayout());
    if (preferredSize != null)
      entityPanel.setPreferredSize(preferredSize);
    dialog.add(entityPanel, BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
    buttonPanel.add(btnSearch);
    buttonPanel.add(btnClose);
    buttonPanel.add(btnCancel);
    dialog.getRootPane().setDefaultButton(btnClose);
    dialog.add(buttonPanel, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setModal(true);
    dialog.setResizable(true);
    dialog.setVisible(true);

    if (selected.size() == 1 && selected.contains(null))
      throw new UserCancelException();
    else
      return selected;
  }

  public static UiUtil.DateInputPanel createDateChooserPanel(final Date initialValue, final AbstractDateMaskFormat maskFormat) {
    final JFormattedTextField txtField = UiUtil.createFormattedField(maskFormat.getDateMask());
    txtField.setText(maskFormat.format(initialValue == null ? new Date() : initialValue));

    return new UiUtil.DateInputPanel(txtField, maskFormat, new AbstractAction("...") {
      public void actionPerformed(ActionEvent e) {
        try {
          final Date d = UiUtil.getDateFromUser(initialValue, FrameworkMessages.get(FrameworkMessages.SELECT_DATE), txtField);
          final String dString = maskFormat.format(d);
          txtField.setText(dString);
        }
        catch (UserCancelException e1) {/**/}
      }
    }, null);
  }

  public static JTextField createDateChooserField(final Date initialValue, final JComponent parent) {
    final JTextField txtField =
            new JTextField(ShortDashDateFormat.get().format(initialValue == null ? new Date() : initialValue));
    txtField.setEditable(false);
    txtField.addMouseListener(new MouseAdapter() {
      public void mouseClicked(final MouseEvent e) {
        try {
          final Date d = UiUtil.getDateFromUser(initialValue, FrameworkMessages.get(FrameworkMessages.SELECT_DATE), parent);
          txtField.setText(ShortDashDateFormat.get().format(d));
        }
        catch (UserCancelException e1) {/**/}
      }
    });

    return txtField;
  }

  public static JCheckBox createCheckBox(final Property property, final EntityModel entityModel) {
    return createCheckBox(property, entityModel, null);
  }

  public static JCheckBox createCheckBox(final Property property, final EntityModel entityModel,
                                         final State enabledState) {
    return createCheckBox(property, entityModel, enabledState, true);
  }

  public static JCheckBox createCheckBox(final Property property, final EntityModel entityModel,
                                         final State enabledState, final boolean includeCaption) {
    final JCheckBox ret = includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox();
    if (!includeCaption)
      ret.setToolTipText(property.getCaption());
    UiUtil.linkToEnabledState(enabledState, ret);
    new CheckBoxPropertyLink(entityModel, property, ret.getModel());
    setPropertyToolTip(entityModel.getEntityID(), property, ret);
    final boolean transferFocusOnEnter =
            (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.TRANSFER_FOCUS_ON_ENTER);
    if (transferFocusOnEnter)
      UiUtil.transferFocusOnEnter(ret);

    return ret;
  }

  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityModel entityModel) {
    return createBooleanComboBox(property, entityModel, null);
  }

  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityModel entityModel,
                                                      final State enabledState) {
    final SteppedComboBox box = createComboBox(property, entityModel, new BooleanComboBoxModel(), enabledState);
    box.setPopupWidth(40);

    return box;
  }

  public static EntityComboBox createEntityComboBox(final Property.EntityProperty property, final EntityModel entityModel,
                                                    final EntityPanel.EntityPanelInfo appInfo,
                                                    final boolean newButtonFocusable) {
    return createEntityComboBox(property, entityModel, appInfo, newButtonFocusable, null);
  }

  public static EntityComboBox createEntityComboBox(final Property.EntityProperty property, final EntityModel entityModel,
                                                    final EntityPanel.EntityPanelInfo appInfo,
                                                    final boolean newButtonFocusable, final State enabledState) {
    try {
      final EntityComboBoxModel boxModel = entityModel.getEntityComboBoxModel(property);
      if (!boxModel.isDataInitialized())
        boxModel.refresh();
      final EntityComboBox ret = new EntityComboBox(boxModel, appInfo, newButtonFocusable);
      UiUtil.linkToEnabledState(enabledState, ret);
      new ComboBoxPropertyLink(entityModel, property, ret);
      MaximumMatch.enable(ret);
      setPropertyToolTip(entityModel.getEntityID(), property, ret);
      final boolean transferFocusOnEnter =
            (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.TRANSFER_FOCUS_ON_ENTER);
      if (transferFocusOnEnter)
        UiUtil.transferFocusOnEnter((JComponent) ret.getEditor().getEditorComponent());

      return ret;
    }
    catch (UserException e) {
      throw e.getRuntimeException();
    }
  }

  public static JPanel createEntityFieldPanel(final Property.EntityProperty property, final EntityModel entityModel,
                                              final EntityTableModel lookupModel) {
    final JPanel ret = new JPanel(new BorderLayout(5,5));
    final JTextField txt = createEntityField(property, entityModel);
    final JButton btn = new JButton(new AbstractAction("...") {
      public void actionPerformed(ActionEvent e) {
        try {
          final List<Entity> selected = FrameworkUiUtil.selectEntities(lookupModel, UiUtil.getParentWindow(ret),
                  true, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY), null, false);
          entityModel.uiSetValue(property, selected.size() > 0 ? selected.get(0) : null);
        }
        catch (UserCancelException e1) {/**/}
      }
    });
    btn.setPreferredSize(FrameworkUiUtil.DIMENSION18x18);

    ret.add(txt, BorderLayout.CENTER);
    ret.add(btn, BorderLayout.EAST);

    return ret;
  }

  public static JTextField createEntityField(final Property.EntityProperty property, final EntityModel entityModel) {
    final JTextField txt = new JTextField();
    txt.setEditable(false);
    setPropertyToolTip(entityModel.getEntityID(), property, txt);
    entityModel.getPropertyChangeEvent(property).addListener(new PropertyListener() {
      protected void propertyChanged(final PropertyChangeEvent e) {
        txt.setText(e.getNewValue() == null ? "" : e.getNewValue().toString());
      }
    });

    return txt;
  }

  public static EntitySearchField createEntitySearchField(final Property.EntityProperty property, final EntityModel entityModel                                                          ) {
    final String[] searchPropertyIDs = EntityRepository.get().getEntitySearchPropertyIDs(property.referenceEntityID);
    if (searchPropertyIDs == null)
      throw new RuntimeException("No default search properties specified for entity: " + property.referenceEntityID
              + ", unable to create EntitySearchField, you must specify the searchPropertyIDs");

    return createEntitySearchField(property, entityModel, searchPropertyIDs);
  }

  public static EntitySearchField createEntitySearchField(final Property.EntityProperty property, final EntityModel entityModel,
                                                          final String... searchPropertyIDs) {
    return createEntitySearchField(property, entityModel, null, searchPropertyIDs);
  }

  public static EntitySearchField createEntitySearchField(final Property.EntityProperty property, final EntityModel entityModel,
                                                          final ICriteria additionalSearchCriteria,
                                                          final String... searchPropertyIDs) {
    final List<Property> searchProperties = EntityRepository.get().getProperties(property.referenceEntityID, searchPropertyIDs);
    for (final Property searchProperty : searchProperties)
      if (searchProperty.getPropertyType() != Type.STRING)
        throw new IllegalArgumentException("Can only create EntitySearchField with a search property of STRING type");

    final EntitySearchField searchField = new EntitySearchField(entityModel.getDbConnectionProvider(), property.referenceEntityID,
            additionalSearchCriteria, searchPropertyIDs);
    searchField.setBorder(BorderFactory.createEtchedBorder());
    new SearchFieldPropertyLink(entityModel, property.propertyID, searchField);
    setPropertyToolTip(entityModel.getEntityID(), property, searchField);

    return searchField;
  }

  public static JPanel createEntitySearchFieldPanel(final Property.EntityProperty property, final EntityModel entityModel,
                                                    final String searchPropertyID,
                                                    final EntityTableModel lookupModel) {
    return createEntitySearchFieldPanel(property, entityModel, searchPropertyID, null, lookupModel);
  }

  public static JPanel createEntitySearchFieldPanel(final Property.EntityProperty property, final EntityModel entityModel,
                                                    final String searchPropertyID,
                                                    final ICriteria additionalSearchCriteria,
                                                    final EntityTableModel lookupModel) {
    final Property searchProperty = EntityRepository.get().getProperty(property.referenceEntityID, searchPropertyID);
    if (searchProperty.getPropertyType() != Type.STRING)
      throw new IllegalArgumentException("Can only create EntitySearchField with a search property of STRING type");

    final EntitySearchField searchField = createEntitySearchField(property, entityModel,
            additionalSearchCriteria, searchPropertyID);
    final JButton btn = new JButton(new AbstractAction("...") {
      public void actionPerformed(ActionEvent e) {
        try {
          final List<Entity> selected = FrameworkUiUtil.selectEntities(lookupModel, UiUtil.getParentWindow(searchField),
                  true, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY), null, false);
          entityModel.uiSetValue(property, selected.size() > 0 ? selected.get(0) : null);
        }
        catch (UserCancelException e1) {/**/}
      }
    });
    btn.setPreferredSize(FrameworkUiUtil.DIMENSION18x18);

    final JPanel ret = new JPanel(new BorderLayout(5,0));
    ret.add(searchField, BorderLayout.CENTER);
    ret.add(btn, BorderLayout.EAST);

    return ret;
  }

  public static SteppedComboBox createComboBox(final Property property, final EntityModel entityModel,
                                               final ComboBoxModel model, final State enabledState) {
    return createComboBox(property, entityModel, model, enabledState, false);
  }

  public static SteppedComboBox createComboBox(final Property property, final EntityModel entityModel,
                                               final ComboBoxModel model, final State enabledState,
                                               final boolean editable) {
    final SteppedComboBox ret = new SteppedComboBox(model);
    ret.setEditable(editable);
    UiUtil.linkToEnabledState(enabledState, ret);
    new ComboBoxPropertyLink(entityModel, property, ret);
    setPropertyToolTip(entityModel.getEntityID(), property, ret);
    final boolean transferFocusOnEnter =
            (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.TRANSFER_FOCUS_ON_ENTER);
    if (transferFocusOnEnter)
      UiUtil.transferFocusOnEnter((JComponent) ret.getEditor().getEditorComponent());

    return ret;
  }

  public static UiUtil.DateInputPanel createDateFieldPanel(final Property property, final EntityModel entityModel,
                                                           final AbstractDateMaskFormat dateMaskFormat,
                                                           final LinkType linkType,
                                                           final boolean includeButton) {
    return createDateFieldPanel(property, entityModel, dateMaskFormat, linkType, includeButton, null);
  }

  public static UiUtil.DateInputPanel createDateFieldPanel(final Property property, final EntityModel entityModel,
                                                           final AbstractDateMaskFormat dateMaskFormat,
                                                           final LinkType linkType,
                                                           final boolean includeButton, final State enabledState) {
    if (property.getPropertyType() != Type.SHORT_DATE && property.getPropertyType() != Type.LONG_DATE)
      throw new IllegalArgumentException("Property " + property + " is not a date property");

    final JFormattedTextField field = (JFormattedTextField) createTextField(property, entityModel, linkType,
            dateMaskFormat.getDateMask(), true, dateMaskFormat, enabledState);

    return new UiUtil.DateInputPanel(field, dateMaskFormat, includeButton ? new AbstractAction("...") {
      public void actionPerformed(ActionEvent e) {
        try {
          final Date currentValue = (Date) entityModel.getValue(property);
          entityModel.uiSetValue(property, UiUtil.getDateFromUser(
                  Entity.isValueNull(property.getPropertyType(), currentValue) ? null : currentValue,
                  FrameworkMessages.get(FrameworkMessages.SELECT_DATE), field));
        }
        catch (UserCancelException e1) {/**/}
      }
    } : null, enabledState);
  }

  public static JTextArea createTextArea(final Property property, final EntityModel entityModel) {
    return createTextArea(property, entityModel, -1, -1);
  }

  public static JTextArea createTextArea(final Property property, final EntityModel entityModel,
                                         final int rows, final int columns) {
    if (property.getPropertyType() != Type.STRING)
      throw new RuntimeException("Cannot create a text area for a non-string property");

    final JTextArea ret = rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea();
    ret.setLineWrap(true);
    ret.setWrapStyleWord(true);

    new TextPropertyLink(entityModel, property, ret, true, LinkType.READ_WRITE);
    setPropertyToolTip(entityModel.getEntityID(), property, ret);

    return ret;
  }

  public static JTextField createTextField(final Property property, final EntityModel entityModel) {
    return createTextField(property, entityModel, LinkType.READ_WRITE, null, true);
  }

  public static JTextField createTextField(final Property property, final EntityModel entityModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate) {
    return createTextField(property, entityModel, linkType, formatMaskString, immediateUpdate, null);
  }

  public static JTextField createTextField(final Property property, final EntityModel entityModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final State enabledState) {
    return createTextField(property, entityModel, linkType, formatMaskString, immediateUpdate, null, enabledState);
  }

  public static JTextField createTextField(final Property property, final EntityModel entityModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final AbstractDateMaskFormat dateFormat,
                                           final State enabledState) {
    return createTextField(property, entityModel, linkType, formatMaskString, immediateUpdate, dateFormat,
            enabledState, false);
  }

  public static JTextField createTextField(final Property property, final EntityModel entityModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final AbstractDateMaskFormat dateFormat,
                                           final State enabledState, final boolean valueContainsLiteralCharacters) {
    final boolean transferFocusOnEnter =
            (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.TRANSFER_FOCUS_ON_ENTER);
    final JTextField ret;
    switch (property.getPropertyType()) {
      case STRING:
        new TextPropertyLink(entityModel, property, ret = formatMaskString == null
                ? new TextFieldPlus() :
                UiUtil.createFormattedField(formatMaskString, valueContainsLiteralCharacters, false),
                immediateUpdate, linkType);
        break;
      case INT:
        new IntTextPropertyLink(entityModel, property,
                (IntField) (ret = new IntField(0)), immediateUpdate, linkType, null);
        break;
      case DOUBLE:
        new DoubleTextPropertyLink(entityModel, property,
                (DoubleField) (ret = new DoubleField(0)), immediateUpdate, linkType, null);
        break;
      case SHORT_DATE:
      case LONG_DATE:
        new DateTextPropertyLink(entityModel, property,
                (JFormattedTextField) (ret = UiUtil.createFormattedField(formatMaskString, true)),
                linkType, null, dateFormat, formatMaskString);
        break;
      default:
        throw new IllegalArgumentException("Not a text based property: " + property);
    }
    UiUtil.linkToEnabledState(enabledState, ret);
    if (transferFocusOnEnter)
      UiUtil.transferFocusOnEnter(ret);
    setPropertyToolTip(entityModel.getEntityID(), property, ret);
    if (property.isDatabaseProperty())
      addLookupDialog(ret, property, entityModel);

    return ret;
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityModel entityModel) {
    return createPropertyComboBox(propertyID, entityModel, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityModel entityModel,
                                                       final Event refreshEvent) {
    return createPropertyComboBox(propertyID, entityModel, refreshEvent, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityModel entityModel,
                                                       final Event refreshEvent, final State state) {
    return createPropertyComboBox(propertyID, entityModel, refreshEvent, state, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityModel entityModel,
                                                       final Event refreshEvent, final State state,
                                                       final Object nullValue) {
    return createPropertyComboBox(EntityRepository.get().getProperty(entityModel.getEntityID(), propertyID),
            entityModel, refreshEvent, state, nullValue);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityModel entityModel) {
    return createPropertyComboBox(property, entityModel, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityModel entityModel,
                                                       final Event refreshEvent) {
    return createPropertyComboBox(property, entityModel, refreshEvent, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityModel entityModel,
                                                       final Event refreshEvent, final State state) {
    return createPropertyComboBox(property, entityModel, refreshEvent, state, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityModel entityModel,
                                                       final Event refreshEvent, final State state,
                                                       final Object nullValue) {
    return createPropertyComboBox(property, entityModel, refreshEvent, state, nullValue, false);
  }


  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityModel entityModel,
                                                       final Event refreshEvent, final State state,
                                                       final Object nullValue, final boolean editable) {
    final SteppedComboBox ret = createComboBox(property, entityModel,
            entityModel.getPropertyComboBoxModel(property, refreshEvent, nullValue), state, editable);
    if (!editable)
      MaximumMatch.enable(ret);

    return ret;
  }

  public static void setPropertyToolTip(final String entityID, final Property property, final JComponent component) {
    final String propertyDescription = EntityRepository.get().getPropertyDescription(entityID, property);
    if (propertyDescription != null)
      component.setToolTipText(propertyDescription);
  }

  public static void addLookupDialog(final JTextField txtField, final Property property, final EntityModel model) {
    txtField.addKeyListener(new KeyAdapter() {
      public void keyReleased(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown()) {
          try {
            final List<?> values = model.getDbConnectionProvider().getEntityDb().selectPropertyValues(model.getEntityID(), property.propertyID, true, true);
            final DefaultListModel listModel = new DefaultListModel();
            for (final Object value : values)
              listModel.addElement(value);

            final JList list = new JList(new Vector<Object>(values));
            final Window owner = UiUtil.getParentWindow(txtField);
            final JDialog dialog = new JDialog(owner, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY));
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
              public void actionPerformed(ActionEvent e) {
                final Object selectedValue = list.getSelectedValue();
                if (selectedValue != null)
                  model.uiSetValue(property.propertyID, selectedValue);
                dialog.dispose();
              }
            };
            final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
              public void actionPerformed(ActionEvent e) {
                dialog.dispose();
              }
            };
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            final JButton btnOk  = new JButton(okAction);
            final JButton btnCancel = new JButton(cancelAction);
            final String cancelMnemonic = Messages.get(Messages.CANCEL_MNEMONIC);
            final String okMnemonic = Messages.get(Messages.OK_MNEMONIC);
            btnOk.setMnemonic(cancelMnemonic.charAt(0));
            btnCancel.setMnemonic(okMnemonic.charAt(0));
            dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
            dialog.getRootPane().getActionMap().put("cancel", cancelAction);
            list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
            list.addMouseListener(new MouseAdapter() {
              public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2)
                  okAction.actionPerformed(null);
              }
            });
            dialog.setLayout(new BorderLayout());
            final JScrollPane scroller = new JScrollPane(list);
            dialog.add(scroller, BorderLayout.CENTER);
            final JPanel buttonPanel = new JPanel(new GridLayout(1,2,5,5));
            buttonPanel.add(btnOk);
            buttonPanel.add(btnCancel);
            final JPanel buttonBasePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonBasePanel.add(buttonPanel);
            dialog.getRootPane().setDefaultButton(btnOk);
            dialog.add(buttonBasePanel, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setLocationRelativeTo(owner);
            dialog.setModal(true);
            dialog.setResizable(true);
            dialog.setVisible(true);
          }
          catch (UserException ue) {
            throw ue.getRuntimeException();
          }
          catch (Exception e1) {
            throw new RuntimeException(e1);
          }
        }
      }
    });
  }
}
