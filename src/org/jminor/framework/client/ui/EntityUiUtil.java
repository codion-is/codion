/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.Criteria;
import org.jminor.common.db.DbException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.common.ui.textfield.TextFieldPlus;
import org.jminor.framework.Configuration;
import org.jminor.framework.DateUtil;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.combobox.BooleanComboBoxModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.client.ui.property.BooleanPropertyLink;
import org.jminor.framework.client.ui.property.ComboBoxPropertyLink;
import org.jminor.framework.client.ui.property.DateTextPropertyLink;
import org.jminor.framework.client.ui.property.DoubleTextPropertyLink;
import org.jminor.framework.client.ui.property.IntTextPropertyLink;
import org.jminor.framework.client.ui.property.LookupModelPropertyLink;
import org.jminor.framework.client.ui.property.TextPropertyLink;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.PropertyEvent;
import org.jminor.framework.domain.PropertyListener;
import org.jminor.framework.domain.Type;
import org.jminor.framework.i18n.FrameworkMessages;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;
import org.apache.log4j.Level;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class EntityUiUtil {

  private static EntityExceptionHandler exceptionHandler = new DefaultEntityExceptionHandler();

  private EntityUiUtil() {}

  public static void setExceptionHandler(final EntityExceptionHandler handler) {
    exceptionHandler = handler;
  }

  public static EntityExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  /**
   * Shows a JRViewer for report printing
   * @param jasperPrint the JasperPrint object to view
   * @param frameTitle the title to display on the frame
   */
  public static void viewReport(final JasperPrint jasperPrint, final String frameTitle) {
    final JFrame frame = new JFrame(frameTitle == null ? FrameworkMessages.get(FrameworkMessages.REPORT_PRINTER) : frameTitle);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.getContentPane().add(new JRViewer(jasperPrint));
    UiUtil.resizeWindow(frame, 0.8, new Dimension(800, 600));
    UiUtil.centerWindow(frame);
    frame.setVisible(true);
  }

  public static void setLoggingLevel(final JComponent dialogParent) {
    final DefaultComboBoxModel model = new DefaultComboBoxModel(
            new Object[] {Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG});
    model.setSelectedItem(Util.getLoggingLevel());
    JOptionPane.showMessageDialog(dialogParent, new JComboBox(model),
            FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL), JOptionPane.QUESTION_MESSAGE);
    Util.setLoggingLevel((Level) model.getSelectedItem());
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
      @Override
      protected void bindEvents() {
        super.bindEvents();
        evtTableDoubleClick.addListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (!getTableModel().getSelectionModel().isSelectionEmpty())
              okAction.actionPerformed(e);
          }
        });
      }
      @Override
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
        catch (UserException ex) {
          throw ex.getRuntimeException();
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
    dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    dialog.getRootPane().getActionMap().put("cancel", cancelAction);
    entityPanel.getJTable().getInputMap(
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    dialog.setLayout(new BorderLayout());
    if (preferredSize != null)
      entityPanel.setPreferredSize(preferredSize);
    dialog.add(entityPanel, BorderLayout.CENTER);
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

    if (selected.size() == 1 && selected.contains(null))
      throw new UserCancelException();
    else
      return selected;
  }

  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel) {
    return createCheckBox(property, editModel, null);
  }

  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel,
                                         final State enabledState) {
    return createCheckBox(property, editModel, enabledState, true);
  }

  public static JCheckBox createCheckBox(final Property property, final EntityEditModel editModel,
                                         final State enabledState, final boolean includeCaption) {
    final JCheckBox ret = includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox();
    if (!includeCaption)
      ret.setToolTipText(property.getCaption());
    ret.setModel(new BooleanPropertyLink(editModel, property).getButtonModel());
    UiUtil.linkToEnabledState(enabledState, ret);
    setPropertyToolTip(editModel.getEntityID(), property, ret);
    if ((Boolean) Configuration.getValue(Configuration.TRANSFER_FOCUS_ON_ENTER))
      UiUtil.transferFocusOnEnter(ret);

    return ret;
  }

  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityEditModel editModel) {
    return createBooleanComboBox(property, editModel, null);
  }

  public static SteppedComboBox createBooleanComboBox(final Property property, final EntityEditModel editModel,
                                                      final State enabledState) {
    final SteppedComboBox box = createComboBox(property, editModel, new BooleanComboBoxModel(), enabledState);
    box.setPopupWidth(40);

    return box;
  }

  public static EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                    final EntityEditModel editModel,
                                                    final EntityPanelProvider newRecordPanelProvider,
                                                    final boolean newButtonFocusable) {
    return createEntityComboBox(foreignKeyProperty, editModel, newRecordPanelProvider, newButtonFocusable, null);
  }

  public static EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                    final EntityEditModel editModel,
                                                    final EntityPanelProvider newRecordPanelProvider,
                                                    final boolean newButtonFocusable, final State enabledState) {
    try {
      final EntityComboBoxModel boxModel = editModel.initializeEntityComboBoxModel(foreignKeyProperty);
      if (!boxModel.isDataInitialized())
        boxModel.refresh();
      final EntityComboBox ret = new EntityComboBox(boxModel, newRecordPanelProvider, newButtonFocusable);
      new ComboBoxPropertyLink(ret, editModel, foreignKeyProperty);
      UiUtil.linkToEnabledState(enabledState, ret);
      MaximumMatch.enable(ret);
      setPropertyToolTip(editModel.getEntityID(), foreignKeyProperty, ret);
      if ((Boolean) Configuration.getValue(Configuration.TRANSFER_FOCUS_ON_ENTER))
        UiUtil.transferFocusOnEnter((JComponent) ret.getEditor().getEditorComponent());

      return ret;
    }
    catch (UserException e) {
      throw e.getRuntimeException();
    }
  }

  public static JPanel createEntityFieldPanel(final Property.ForeignKeyProperty foreignKeyProperty,
                                              final EntityEditModel editModel, final EntityTableModel lookupModel) {
    final JPanel ret = new JPanel(new BorderLayout(5,5));
    final JTextField txt = createEntityField(foreignKeyProperty, editModel);
    final JButton btn = new JButton(new AbstractAction("...") {
      public void actionPerformed(ActionEvent e) {
        try {
          final List<Entity> selected = EntityUiUtil.selectEntities(lookupModel, UiUtil.getParentWindow(ret),
                  true, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY), null, false);
          editModel.setValue(foreignKeyProperty, selected.size() > 0 ? selected.get(0) : null);
        }
        catch (UserCancelException ex) {/**/}
      }
    });
    btn.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);

    ret.add(txt, BorderLayout.CENTER);
    ret.add(btn, BorderLayout.EAST);

    return ret;
  }

  public static JTextField createEntityField(final Property.ForeignKeyProperty foreignKeyProperty,
                                             final EntityEditModel editModel) {
    final JTextField txt = new JTextField();
    txt.setEditable(false);
    setPropertyToolTip(editModel.getEntityID(), foreignKeyProperty, txt);
    editModel.getPropertyChangeEvent(foreignKeyProperty).addListener(new PropertyListener() {
      @Override
      public void propertyChanged(final PropertyEvent e) {
        txt.setText(e.getNewValue() == null ? "" : e.getNewValue().toString());
      }
    });

    return txt;
  }

  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel) {
    final String[] searchPropertyIDs = EntityRepository.getEntitySearchPropertyIDs(foreignKeyProperty.referenceEntityID);
    if (searchPropertyIDs == null)
      throw new RuntimeException("No default search properties specified for entity: " + foreignKeyProperty.referenceEntityID
              + ", unable to create EntityLookupField, you must specify the searchPropertyIDs");

    return createEntityLookupField(foreignKeyProperty, editModel, searchPropertyIDs);
  }

  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel, final String... searchPropertyIDs) {
    return createEntityLookupField(foreignKeyProperty, editModel, null, searchPropertyIDs);
  }

  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel,
                                                          final Criteria additionalSearchCriteria,
                                                          final String... searchPropertyIDs) {
    if (searchPropertyIDs.length == 0)
      throw new RuntimeException("No search properties specified for entity lookup field: " + foreignKeyProperty.referenceEntityID);
    final List<Property> searchProperties = EntityRepository.getProperties(foreignKeyProperty.referenceEntityID, searchPropertyIDs);
    for (final Property searchProperty : searchProperties)
      if (searchProperty.getPropertyType() != Type.STRING)
        throw new IllegalArgumentException("Can only create EntityLookupField with a search property of STRING type");

    final EntityLookupField lookupField =
            new EntityLookupField(editModel.createEntityLookupModel(foreignKeyProperty.referenceEntityID,
                    additionalSearchCriteria, searchProperties),
                    (Boolean) Configuration.getValue(Configuration.TRANSFER_FOCUS_ON_ENTER));
    lookupField.setBorder(BorderFactory.createEtchedBorder());
    new LookupModelPropertyLink(lookupField.getModel(), editModel, foreignKeyProperty);
    setPropertyToolTip(editModel.getEntityID(), foreignKeyProperty, lookupField);

    return lookupField;
  }

  public static SteppedComboBox createComboBox(final Property property, final EntityEditModel editModel,
                                               final ComboBoxModel model, final State enabledState) {
    return createComboBox(property, editModel, model, enabledState, false);
  }

  public static SteppedComboBox createComboBox(final Property property, final EntityEditModel editModel,
                                               final ComboBoxModel model, final State enabledState,
                                               final boolean editable) {
    final SteppedComboBox ret = new SteppedComboBox(model);
    ret.setEditable(editable);
    new ComboBoxPropertyLink(ret, editModel, property);
    UiUtil.linkToEnabledState(enabledState, ret);
    setPropertyToolTip(editModel.getEntityID(), property, ret);
    if ((Boolean) Configuration.getValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter((JComponent) ret.getEditor().getEditorComponent());
      UiUtil.transferFocusOnEnter(ret);
    }

    return ret;
  }

  public static DateInputPanel createDateInputPanel(final Date initialValue, final SimpleDateFormat maskFormat) {
    final JFormattedTextField txtField = UiUtil.createFormattedField(DateUtil.getDateMask(maskFormat));
    if (initialValue != null)
      txtField.setText(maskFormat.format(initialValue));

    return new DateInputPanel(txtField, maskFormat, true, null);
  }

  public static DateInputPanel createDateInputPanel(final Property property, final EntityEditModel editModel,
                                                    final SimpleDateFormat dateFormat, final LinkType linkType,
                                                    final boolean includeButton) {
    return createDateInputPanel(property, editModel, dateFormat, linkType, includeButton, null);
  }

  public static DateInputPanel createDateInputPanel(final Property property, final EntityEditModel editModel,
                                                    final SimpleDateFormat dateFormat, final LinkType linkType,
                                                    final boolean includeButton, final State enabledState) {
    if (property.getPropertyType() != Type.DATE && property.getPropertyType() != Type.TIMESTAMP)
      throw new IllegalArgumentException("Property " + property + " is not a date property");

    final JFormattedTextField field = (JFormattedTextField) createTextField(property, editModel, linkType,
            DateUtil.getDateMask(dateFormat), true, dateFormat, enabledState);

    return new DateInputPanel(field, dateFormat, includeButton, enabledState);
  }

  public static JTextArea createTextArea(final Property property, final EntityEditModel editModel) {
    return createTextArea(property, editModel, -1, -1);
  }

  public static JTextArea createTextArea(final Property property, final EntityEditModel editModel,
                                         final int rows, final int columns) {
    if (property.getPropertyType() != Type.STRING)
      throw new RuntimeException("Cannot create a text area for a non-string property");

    final JTextArea ret = rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea();
    ret.setLineWrap(true);
    ret.setWrapStyleWord(true);

    new TextPropertyLink(ret, editModel, property, true, LinkType.READ_WRITE);
    setPropertyToolTip(editModel.getEntityID(), property, ret);

    return ret;
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
                                           final boolean immediateUpdate, final State enabledState) {
    return createTextField(property, editModel, linkType, formatMaskString, immediateUpdate, null, enabledState);
  }

  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final SimpleDateFormat dateFormat,
                                           final State enabledState) {
    return createTextField(property, editModel, linkType, formatMaskString, immediateUpdate, dateFormat,
            enabledState, false);
  }

  public static JTextField createTextField(final Property property, final EntityEditModel editModel,
                                           final LinkType linkType, final String formatMaskString,
                                           final boolean immediateUpdate, final SimpleDateFormat dateFormat,
                                           final State enabledState, final boolean valueContainsLiteralCharacters) {
    final JTextField ret;
    switch (property.getPropertyType()) {
      case STRING:
        new TextPropertyLink(ret = formatMaskString == null ? new TextFieldPlus() :
                UiUtil.createFormattedField(formatMaskString, valueContainsLiteralCharacters, false),
                editModel, property, immediateUpdate, linkType);
        break;
      case INT:
        new IntTextPropertyLink((IntField) (ret = new IntField(0)), editModel, property, immediateUpdate, linkType);
        break;
      case DOUBLE:
        new DoubleTextPropertyLink((DoubleField) (ret = new DoubleField(0)), editModel, property, immediateUpdate, linkType);
        break;
      case DATE:
      case TIMESTAMP:
        new DateTextPropertyLink((JFormattedTextField) (ret = UiUtil.createFormattedField(formatMaskString, true)),
                editModel, property, linkType, dateFormat, formatMaskString);
        break;
      default:
        throw new IllegalArgumentException("Not a text based property: " + property);
    }
    UiUtil.linkToEnabledState(enabledState, ret);
    if ((Boolean) Configuration.getValue(Configuration.TRANSFER_FOCUS_ON_ENTER))
      UiUtil.transferFocusOnEnter(ret);
    setPropertyToolTip(editModel.getEntityID(), property, ret);
    if (ret instanceof TextFieldPlus && property.getMaxLength() > 0)
      ((TextFieldPlus) ret).setMaxLength(property.getMaxLength());
    if (property.isDatabaseProperty())
      addLookupDialog(ret, property, editModel);

    return ret;
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityEditModel editModel) {
    return createPropertyComboBox(propertyID, editModel, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityEditModel editModel,
                                                       final Event refreshEvent) {
    return createPropertyComboBox(propertyID, editModel, refreshEvent, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityEditModel editModel,
                                                       final Event refreshEvent, final State state) {
    return createPropertyComboBox(propertyID, editModel, refreshEvent, state, null);
  }

  public static SteppedComboBox createPropertyComboBox(final String propertyID, final EntityEditModel editModel,
                                                       final Event refreshEvent, final State state,
                                                       final String nullValue) {
    return createPropertyComboBox(EntityRepository.getProperty(editModel.getEntityID(), propertyID),
            editModel, refreshEvent, state, nullValue);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityEditModel editModel) {
    return createPropertyComboBox(property, editModel, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityEditModel editModel,
                                                       final Event refreshEvent) {
    return createPropertyComboBox(property, editModel, refreshEvent, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityEditModel editModel,
                                                       final Event refreshEvent, final State state) {
    return createPropertyComboBox(property, editModel, refreshEvent, state, null);
  }

  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityEditModel editModel,
                                                       final Event refreshEvent, final State state,
                                                       final String nullValue) {
    return createPropertyComboBox(property, editModel, refreshEvent, state, nullValue, false);
  }


  public static SteppedComboBox createPropertyComboBox(final Property property, final EntityEditModel editModel,
                                                       final Event refreshEvent, final State state,
                                                       final String nullValue, final boolean editable) {
    final SteppedComboBox ret = createComboBox(property, editModel,
            editModel.initializePropertyComboBoxModel(property, refreshEvent, nullValue), state, editable);
    if (!editable)
      MaximumMatch.enable(ret);

    return ret;
  }

  public static void setPropertyToolTip(final String entityID, final Property property, final JComponent component) {
    final String propertyDescription = EntityRepository.getPropertyDescription(entityID, property);
    if (propertyDescription != null)
      component.setToolTipText(propertyDescription);
  }

  public static Object lookupPropertyValue(final JComponent dialogOwner, final String entityID,
                                           final Property property, final EntityDbProvider dbProvider) {
    try {
      final List<?> values = dbProvider.getEntityDb().selectPropertyValues(entityID, property.getPropertyID(), true);
      final DefaultListModel listModel = new DefaultListModel();
      for (final Object value : values)
        listModel.addElement(value);

      final JList list = new JList(new Vector<Object>(values));
      final Window owner = UiUtil.getParentWindow(dialogOwner);
      final JDialog dialog = new JDialog(owner, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY));
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
        public void actionPerformed(ActionEvent e) {
          dialog.dispose();
        }
      };
      final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
        public void actionPerformed(ActionEvent e) {
          list.clearSelection();
          dialog.dispose();
        }
      };
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      final JButton btnOk  = new JButton(okAction);
      final JButton btnCancel = new JButton(cancelAction);
      final String cancelMnemonic = Messages.get(Messages.CANCEL_MNEMONIC);
      final String okMnemonic = Messages.get(Messages.OK_MNEMONIC);
      btnOk.setMnemonic(okMnemonic.charAt(0));
      btnCancel.setMnemonic(cancelMnemonic.charAt(0));
      dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
      dialog.getRootPane().getActionMap().put("cancel", cancelAction);
      list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
              KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
      list.addMouseListener(new MouseAdapter() {
        @Override
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

      return list.getSelectedValue();
    }
    catch (UserException ue) {
      throw ue.getRuntimeException();
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void addLookupDialog(final JTextField txtField, final Property property, final EntityEditModel editModel) {
    addLookupDialog(txtField, editModel.getEntityID(), property, editModel.getDbProvider());
  }

  public static void addLookupDialog(final JTextField txtField, final String entityID, final Property property,
                                     final EntityDbProvider dbProvider) {
    txtField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown()) {
          final Object value = lookupPropertyValue(txtField, entityID, property, dbProvider);
          if (value != null)
            txtField.setText(value.toString());
        }
      }
    });
  }

  public static interface EntityExceptionHandler {
    public void handleException(final Throwable exception, final JComponent dialogParent);
  }

  public static class DefaultEntityExceptionHandler implements EntityExceptionHandler {

    public void handleException(final Throwable exception, final JComponent dialogParent) {
      if (exception instanceof UserCancelException)
        return;
      if (exception instanceof DbException)
        handleDbException((DbException) exception, dialogParent);
      else if (exception instanceof JRException && exception.getCause() != null)
        handleException(exception.getCause(), dialogParent);
      else if (exception instanceof UserException && exception.getCause() instanceof DbException)
        handleDbException((DbException) exception.getCause(), dialogParent);
      else {
        ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent), getMessageTitle(exception), exception.getMessage(), exception);
      }
    }

    public void handleDbException(final DbException dbException, final JComponent dialogParent) {
      String errMsg = dbException.getMessage();
      if (errMsg == null || errMsg.length() == 0) {
        if (dbException.getCause() == null)
          errMsg = trimMessage(dbException);
        else
          errMsg = trimMessage(dbException.getCause());
      }
      ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent),
              Messages.get(Messages.EXCEPTION), errMsg, dbException);
    }

    private String getMessageTitle(final Throwable e) {
      if (e instanceof FileNotFoundException)
        return FrameworkMessages.get(FrameworkMessages.UNABLE_TO_OPEN_FILE);

      return Messages.get(Messages.EXCEPTION);
    }

    private String trimMessage(final Throwable e) {
      final String msg = e.getMessage();
      if (msg.length() > 50)
        return msg.substring(0, 50) + "...";

      return msg;
    }
  }
}
