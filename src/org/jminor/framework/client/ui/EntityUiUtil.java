/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.Criteria;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.Util;
import org.jminor.common.model.combobox.BooleanComboBoxModel;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.images.Images;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.common.ui.textfield.TextFieldPlus;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.event.InsertEvent;
import org.jminor.framework.client.model.util.DateUtil;
import org.jminor.framework.client.ui.property.BooleanPropertyLink;
import org.jminor.framework.client.ui.property.ComboBoxPropertyLink;
import org.jminor.framework.client.ui.property.DatePropertyLink;
import org.jminor.framework.client.ui.property.DoublePropertyLink;
import org.jminor.framework.client.ui.property.FormattedPropertyLink;
import org.jminor.framework.client.ui.property.IntPropertyLink;
import org.jminor.framework.client.ui.property.LookupPropertyLink;
import org.jminor.framework.client.ui.property.TextPropertyLink;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;
import org.jminor.framework.i18n.FrameworkMessages;

import org.apache.log4j.Level;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class EntityUiUtil {

  private EntityUiUtil() {}

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
    final List<Entity> selected = new ArrayList<Entity>();
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

    final EntityTablePanel entityTablePanel = new EntityTablePanel(lookupModel, null, false) {
      @Override
      protected void bindEvents() {
        evtTableDoubleClicked.addListener(new ActionListener() {
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
    entityTablePanel.setSearchPanelVisible(true);
    if (singleSelection)
      entityTablePanel.getJTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    final Action searchAction = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(ActionEvent e) {
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
    dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    dialog.getRootPane().getActionMap().put("cancel", cancelAction);
    entityTablePanel.getJTable().getInputMap(
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    dialog.setLayout(new BorderLayout());
    if (preferredSize != null)
      entityTablePanel.setPreferredSize(preferredSize);
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

    if (selected.size() == 1 && selected.contains(null))
      throw new UserCancelException();
    else
      return selected;
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
    final String text = property.getCaption();
    if (text == null || text.length() == 0)
      throw new IllegalArgumentException("Cannot create a label for property: " + property + ", no caption");

    final JLabel label = new JLabel(text, horizontalAlignment);
    if (property.getMnemonic() != null)
      label.setDisplayedMnemonic(property.getMnemonic());

    return label;
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
    final JCheckBox checkBox = includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox();
    if (!includeCaption)
      checkBox.setToolTipText(property.getCaption());
    new BooleanPropertyLink(checkBox.getModel(), editModel, property);
    UiUtil.linkToEnabledState(enabledState, checkBox);
    checkBox.setToolTipText(property.getDescription());
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER))
      UiUtil.transferFocusOnEnter(checkBox);

    return checkBox;
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
                                                    final EntityEditModel editModel) {
    return createEntityComboBox(foreignKeyProperty, editModel, null);
  }

  public static EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                    final EntityEditModel editModel, final State enabledState) {
    final EntityComboBoxModel boxModel = editModel.initializeEntityComboBoxModel(foreignKeyProperty);
    if (!boxModel.isDataInitialized())
      boxModel.refresh();
    final EntityComboBox comboBox = new EntityComboBox(boxModel);
    new ComboBoxPropertyLink(comboBox, editModel, foreignKeyProperty);
    UiUtil.linkToEnabledState(enabledState, comboBox);
    MaximumMatch.enable(comboBox);
    comboBox.setToolTipText(foreignKeyProperty.getDescription());
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER))
      UiUtil.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());

    return comboBox;
  }

  public static JPanel createEntityFieldPanel(final Property.ForeignKeyProperty foreignKeyProperty,
                                              final EntityEditModel editModel, final EntityTableModel lookupModel) {
    final JPanel panel = new JPanel(new BorderLayout(5,5));
    final JTextField txt = createEntityField(foreignKeyProperty, editModel);
    final JButton btn = new JButton(new AbstractAction("...") {
      public void actionPerformed(ActionEvent e) {
        try {
          final List<Entity> selected = EntityUiUtil.selectEntities(lookupModel, UiUtil.getParentWindow(panel),
                  true, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY), null, false);
          editModel.setValue(foreignKeyProperty, selected.size() > 0 ? selected.get(0) : null);
        }
        catch (UserCancelException ex) {/**/}
      }
    });
    btn.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);

    panel.add(txt, BorderLayout.CENTER);
    panel.add(btn, BorderLayout.EAST);

    return panel;
  }

  public static JTextField createEntityField(final Property.ForeignKeyProperty foreignKeyProperty,
                                             final EntityEditModel editModel) {
    final JTextField textField = new JTextField();
    textField.setEditable(false);
    textField.setToolTipText(foreignKeyProperty.getDescription());
    editModel.getPropertyChangeEvent(foreignKeyProperty).addListener(new Property.Listener() {
      @Override
      public void propertyChanged(final Property.Event e) {
        textField.setText(e.getNewValue() == null ? "" : e.getNewValue().toString());
      }
    });

    return textField;
  }

  public static EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                          final EntityEditModel editModel) {
    final String[] searchPropertyIDs = EntityRepository.getEntitySearchPropertyIDs(foreignKeyProperty.getReferencedEntityID());
    if (searchPropertyIDs == null)
      throw new RuntimeException("No default search properties specified for entity: " + foreignKeyProperty.getReferencedEntityID()
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
    if (searchPropertyIDs == null || searchPropertyIDs.length == 0)
      throw new RuntimeException("No search properties specified for entity lookup field: " + foreignKeyProperty.getReferencedEntityID());
    final List<Property> searchProperties = EntityRepository.getProperties(foreignKeyProperty.getReferencedEntityID(), searchPropertyIDs);
    for (final Property searchProperty : searchProperties)
      if (searchProperty.getPropertyType() != Type.STRING)
        throw new IllegalArgumentException("Can only create EntityLookupField with a search property of STRING type");

    final EntityLookupField lookupField =
            new EntityLookupField(editModel.createEntityLookupModel(foreignKeyProperty.getReferencedEntityID(),
                    additionalSearchCriteria, searchProperties),
                    Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER));
    lookupField.setBorder(BorderFactory.createEtchedBorder());
    new LookupPropertyLink(lookupField.getModel(), editModel, foreignKeyProperty);
    lookupField.setToolTipText(foreignKeyProperty.getDescription());
    UiUtil.selectAllOnFocusGained(lookupField);

    return lookupField;
  }

  public static SteppedComboBox createComboBox(final Property property, final EntityEditModel editModel,
                                               final ComboBoxModel model, final State enabledState) {
    return createComboBox(property, editModel, model, enabledState, false);
  }

  public static SteppedComboBox createComboBox(final Property property, final EntityEditModel editModel,
                                               final ComboBoxModel model, final State enabledState,
                                               final boolean editable) {
    final SteppedComboBox comboBox = new SteppedComboBox(model);
    comboBox.setEditable(editable);
    new ComboBoxPropertyLink(comboBox, editModel, property);
    UiUtil.linkToEnabledState(enabledState, comboBox);
    comboBox.setToolTipText(property.getDescription());
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER)) {
      UiUtil.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
      UiUtil.transferFocusOnEnter(comboBox);
    }

    return comboBox;
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

    final JTextArea textArea = rows > 0 && columns > 0 ? new JTextArea(rows, columns) : new JTextArea();
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);

    new TextPropertyLink(textArea, editModel, property, true, LinkType.READ_WRITE);
    textArea.setToolTipText(property.getDescription());

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
    final JTextField textField = initTextField(property, editModel, enabledState, formatMaskString, valueContainsLiteralCharacters);
    switch (property.getPropertyType()) {
      case STRING:
        if (formatMaskString != null)
          new FormattedPropertyLink((JFormattedTextField) textField, editModel, property, null, immediateUpdate, linkType);
        else
          new TextPropertyLink(textField, editModel, property, immediateUpdate, linkType);
        break;
      case INT:
        new IntPropertyLink((IntField) textField, editModel, property, immediateUpdate, linkType);
        break;
      case DOUBLE:
        new DoublePropertyLink((DoubleField) textField, editModel, property, immediateUpdate, linkType);
        break;
      case DATE:
      case TIMESTAMP:
        new DatePropertyLink((JFormattedTextField) textField, editModel, property, linkType, dateFormat);
        break;
      default:
        throw new IllegalArgumentException("Not a text based property: " + property);
    }

    return textField;
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
    final SteppedComboBox comboBox = createComboBox(property, editModel,
            editModel.initializePropertyComboBoxModel(property, refreshEvent, nullValue), state, editable);
    if (!editable)
      MaximumMatch.enable(comboBox);

    return comboBox;
  }

  public static void addLookupDialog(final JTextField txtField, final Property property, final EntityEditModel editModel) {
    addLookupDialog(txtField, editModel.getEntityID(), property, editModel.getDbProvider());
  }

  public static void addLookupDialog(final JTextField txtField, final String entityID, final Property property,
                                     final EntityDbProvider dbProvider) {
    txtField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK), "valueLookup");
    txtField.getActionMap().put("valueLookup", new AbstractAction() {
      public void actionPerformed(final ActionEvent e) {
        final Object value = lookupPropertyValue(txtField, entityID, property, dbProvider);
        if (value != null)
          txtField.setText(value.toString());
      }
    });
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
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static JPanel createLookupFieldPanel(final EntityLookupField lookupField, final EntityTableModel tableModel) {
    final JButton btn = new JButton(new AbstractAction("...") {
      public void actionPerformed(ActionEvent e) {
        try {
          lookupField.getModel().setSelectedEntities(selectEntities(tableModel, UiUtil.getParentWindow(lookupField),
                  true, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY), null, false));
        }
        catch (UserCancelException ex) {/**/}
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
    return createEastButtonPanel(entityComboBox, initializeNewRecordAction(entityComboBox, panelProvider),
            newRecordButtonTakesFocus);
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
                                          final State enabledState, final String formatMaskString,
                                          final boolean valueContainsLiteralCharacters) {
    final JTextField field;
    switch (property.getPropertyType()) {
      case INT:
        field = new IntField(0);
        break;
      case DOUBLE:
        field = new DoubleField(0);
        break;
      case DATE:
      case TIMESTAMP:
        field = UiUtil.createFormattedField(formatMaskString, true);
        break;
      case STRING:
        field = formatMaskString == null ? new TextFieldPlus() : UiUtil.createFormattedField(formatMaskString, valueContainsLiteralCharacters);
        break;
      default:
        throw new RuntimeException("Unable to create text field for property type: " + property.getPropertyType());
    }
    UiUtil.linkToEnabledState(enabledState, field);
    if (Configuration.getBooleanValue(Configuration.TRANSFER_FOCUS_ON_ENTER))
      UiUtil.transferFocusOnEnter(field);
    field.setToolTipText(property.getDescription());
    if (field instanceof TextFieldPlus && property.getMaxLength() > 0)
      ((TextFieldPlus) field).setMaxLength(property.getMaxLength());
    if (property.isDatabaseProperty())
      addLookupDialog(field, property, editModel);

    return field;
  }

  private static AbstractAction initializeNewRecordAction(final EntityComboBox comboBox, final EntityPanelProvider panelProvider) {
    return new AbstractAction("", Images.loadImage(Images.IMG_ADD_16)) {
      public void actionPerformed(ActionEvent e) {
        final EntityPanel entityPanel = EntityPanel.createInstance(panelProvider, comboBox.getModel().getDbProvider());
        entityPanel.initializePanel();
        final List<Entity.Key> lastInsertedPrimaryKeys = new ArrayList<Entity.Key>();
        entityPanel.getModel().getEditModel().evtAfterInsert.addListener(new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            lastInsertedPrimaryKeys.clear();
            lastInsertedPrimaryKeys.addAll(((InsertEvent) e).getInsertedKeys());
          }
        });
        final Window parentWindow = UiUtil.getParentWindow(comboBox);
        final String caption = panelProvider.getCaption() == null || panelProvider.getCaption().equals("") ?
                entityPanel.getCaption() : panelProvider.getCaption();
        final JDialog dialog = new JDialog(parentWindow, caption);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.add(entityPanel, BorderLayout.CENTER);
        final JButton btnClose = initializeOkButton(comboBox.getModel(), entityPanel.getModel().getTableModel(),
                dialog, lastInsertedPrimaryKeys);
        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(btnClose);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(parentWindow);
        dialog.setModal(true);
        dialog.setResizable(true);
        dialog.setVisible(true);
      }
    };
  }

  private static JButton initializeOkButton(final EntityComboBoxModel comboBoxModel, final EntityTableModel tableModel,
                                            final JDialog dialog, final List<Entity.Key> lastInsertedPrimaryKeys) {
    final JButton button = new JButton(new AbstractAction(Messages.get(Messages.OK)) {
      public void actionPerformed(ActionEvent e) {
        comboBoxModel.refresh();
        if (lastInsertedPrimaryKeys != null && lastInsertedPrimaryKeys.size() > 0) {
          comboBoxModel.setSelectedEntityByPrimaryKey(lastInsertedPrimaryKeys.get(0));
        }
        else {
          final Entity selectedEntity = tableModel.getSelectedEntity();
          if (selectedEntity != null)
            comboBoxModel.setSelectedItem(selectedEntity);
        }
        dialog.dispose();
      }
    });
    button.setMnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0));

    return button;
  }
}
