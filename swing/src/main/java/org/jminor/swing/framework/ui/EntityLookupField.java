/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Event;
import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.State;
import org.jminor.common.States;
import org.jminor.common.Values;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Util;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityLookupModel;
import org.jminor.swing.common.model.combobox.SwingFilteredComboBoxModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.UiValues;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.textfield.SizedDocument;
import org.jminor.swing.common.ui.textfield.TextFieldHint;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A UI component based on the EntityLookupModel.
 *
 * The lookup is triggered by the ENTER key and behaves in the following way:
 * If the lookup result is empty a message is shown, if a single entity fits the
 * criteria then that entity is selected, otherwise a list containing the entities
 * fitting the criteria is shown in a dialog allowing either a single or multiple
 * selection based on the lookup model settings.
 *
 * @see EntityLookupModel
 */
public final class EntityLookupField extends JTextField {

  private static final int BORDER_SIZE = 15;
  private static final int ENABLE_LOOKUP_DELAY = 250;

  private final EntityLookupModel model;
  private final TextFieldHint searchHint;
  private final SettingsPanel settingsPanel;
  private final Action transferFocusAction = new UiUtil.TransferFocusAction(this);
  private final Action transferFocusBackwardAction = new UiUtil.TransferFocusAction(this, true);
  /**
   * A hack used to prevent the lookup being triggered by the closing of
   * the "empty result" message, which happens on windows
   */
  private final State lookupEnabledState = States.state(true);

  private Color validBackgroundColor;
  private Color invalidBackgroundColor;
  private boolean performingLookup = false;

  /**
   * Initializes a new EntityLookupField.
   * @param lookupModel the lookup model on which to base this lookup field
   */
  public EntityLookupField(final EntityLookupModel lookupModel) {
    this(lookupModel, true);
  }

  /**
   * Initializes a new EntityLookupField
   * @param lookupModel the lookup model on which to base this lookup field
   * @param lookupOnKeyRelease if true then lookup is performed on key release, otherwise it is performed on keyPressed.
   */
  public EntityLookupField(final EntityLookupModel lookupModel, final boolean lookupOnKeyRelease) {
    Util.rejectNullValue(lookupModel, "lookupModel");
    this.model = lookupModel;
    this.settingsPanel = new SettingsPanel(lookupModel);
    linkToModel();
    setValidBackgroundColor(getBackground());
    setInvalidBackgroundColor(Color.LIGHT_GRAY);
    setToolTipText(lookupModel.getDescription());
    setComponentPopupMenu(initializePopupMenu());
    addFocusListener(initializeFocusListener());
    addEscapeListener();
    this.searchHint = TextFieldHint.enable(this, Messages.get(Messages.SEARCH_FIELD_HINT));
    updateColors();
    UiUtil.addKeyEvent(this, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, lookupOnKeyRelease, initializeLookupAction());
    UiUtil.linkToEnabledState(lookupModel.getSearchStringRepresentsSelectedObserver(), transferFocusAction);
    UiUtil.linkToEnabledState(lookupModel.getSearchStringRepresentsSelectedObserver(), transferFocusBackwardAction);
  }

  /**
   * @return the lookup model this lookup field is based on
   */
  public EntityLookupModel getModel() {
    return model;
  }

  /**
   * @param validBackgroundColor the background color to use when the text represents the selected items
   * @return this lookup field
   */
  public EntityLookupField setValidBackgroundColor(final Color validBackgroundColor) {
    this.validBackgroundColor = validBackgroundColor;
    return this;
  }

  /**
   * @param invalidBackgroundColor the background color to use when the text does not represent the selected items
   * @return this lookup field
   */
  public EntityLookupField setInvalidBackgroundColor(final Color invalidBackgroundColor) {
    this.invalidBackgroundColor = invalidBackgroundColor;
    return this;
  }

  /**
   * Activates the transferal of focus on ENTER
   * @return this lookup field
   */
  public EntityLookupField setTransferFocusOnEnter() {
    UiUtil.addKeyEvent(this, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, false, transferFocusAction);
    UiUtil.addKeyEvent(this, KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, JComponent.WHEN_FOCUSED, false, transferFocusBackwardAction);
    return this;
  }

  private void selectEntities(final List<Entity> entities) {
    final JList<Entity> list = new JList<>(entities.toArray(new Entity[entities.size()]));
    final Window owner = UiUtil.getParentWindow(this);
    final JDialog dialog = new JDialog(owner, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY));
    final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getModel().setSelectedEntities(list.getSelectedValuesList());
        dialog.dispose();
      }
    };
    final Action cancelAction = new UiUtil.DisposeWindowAction(dialog);
    list.setSelectionMode(model.getMultipleSelectionAllowedValue().get() ?
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
    UiUtil.prepareScrollPanelDialog(dialog, this, list, okAction, cancelAction);
  }

  private void linkToModel() {
    Values.link(model.getSearchStringValue(), UiValues.textValue(this, null, true));
    model.getSearchStringValue().getObserver().addInfoListener(new EventInfoListener<String>() {
      @Override
      public void eventOccurred(final String info) {
        updateColors();
      }
    });
    model.addSelectedEntitiesListener(new EventInfoListener<Collection<Entity>>() {
      @Override
      public void eventOccurred(final Collection<Entity> info) {
        setCaretPosition(0);
      }
    });
  }

  private void addEscapeListener() {
    final AbstractAction escapeAction = new AbstractAction("EntityLookupField.escape") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getModel().refreshSearchText();
        selectAll();
      }
    };
    UiUtil.linkToEnabledState(getModel().getSearchStringRepresentsSelectedObserver().getReversedObserver(), escapeAction);
    UiUtil.addKeyEvent(this, KeyEvent.VK_ESCAPE, escapeAction);
  }

  private FocusListener initializeFocusListener() {
    return new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {
        updateColors();
      }
      @Override
      public void focusLost(final FocusEvent e) {
        if (getText().length() == 0) {
          getModel().setSelectedEntity(null);
        }
        else if (!searchHint.isHintTextVisible() && !performingLookup && !model.searchStringRepresentsSelected()) {
          performLookup(false);
        }
        updateColors();
      }
    };
  }

  private void updateColors() {
    final boolean validBackground = model.searchStringRepresentsSelected() || (searchHint != null && searchHint.isHintTextVisible());
    setBackground(validBackground ? validBackgroundColor : invalidBackgroundColor);
  }

  private AbstractAction initializeLookupAction() {
    final AbstractAction lookupAction = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        performLookup(true);
      }
    };
    UiUtil.linkToEnabledState(States.aggregateState(Conjunction.AND,
            getModel().getSearchStringRepresentsSelectedObserver().getReversedObserver(),
            lookupEnabledState), lookupAction);

    return lookupAction;
  }

  private void performLookup(final boolean promptUser) {
    try {
      performingLookup = true;
      if (Util.nullOrEmpty(model.getSearchString())) {
        model.setSelectedEntities(null);
      }
      else {
        if (!model.searchStringRepresentsSelected()) {
          List<Entity> queryResult;
          try {
            UiUtil.setWaitCursor(true, this);
            queryResult = model.performQuery();
          }
          finally {
            UiUtil.setWaitCursor(false, this);
          }
          if (queryResult.size() == 1) {
            model.setSelectedEntities(queryResult);
          }
          else if (promptUser) {
            if (queryResult.isEmpty()) {
              showEmptyResultMessage();
            }
            else {
              selectEntities(queryResult);
            }
          }
        }
      }
      updateColors();
    }
    finally {
      performingLookup = false;
    }
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new SettingsAction(settingsPanel));

    return popupMenu;
  }

  /**
   * Necessary due to a bug on windows, where pressing Enter to dismiss this message
   * triggers another lookup, resulting in a loop
   */
  private void showEmptyResultMessage() {
    final Event closeEvent = Events.event();
    final JButton okButton = new JButton(new AbstractAction(Messages.get(Messages.OK)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        closeEvent.fire();
      }
    });
    UiUtil.addKeyEvent(okButton, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, true, new OKAction(okButton, closeEvent));
    final JPanel btnBase = new JPanel(UiUtil.createFlowLayout(FlowLayout.CENTER));
    btnBase.add(okButton);
    final JLabel messageLabel = new JLabel(FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CRITERIA));
    messageLabel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, 0, BORDER_SIZE));
    final JPanel messagePanel = new JPanel(UiUtil.createBorderLayout());
    messagePanel.add(messageLabel, BorderLayout.CENTER);
    messagePanel.add(btnBase, BorderLayout.SOUTH);
    disableLookup();
    UiUtil.displayInDialog(this, messagePanel, Messages.get("OptionPane.messageDialogTitle"), closeEvent);
    enableLookup();
  }

  private void disableLookup() {
    lookupEnabledState.setActive(false);
  }

  /**
   * @see #lookupEnabledState
   */
  private void enableLookup() {
    final Timer timer = new Timer(ENABLE_LOOKUP_DELAY, new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        lookupEnabledState.setActive(true);
      }
    });
    timer.setRepeats(false);
    timer.start();
  }

  private final class SettingsAction extends AbstractAction {

    private final SettingsPanel settingsPanel;

    private SettingsAction(final SettingsPanel settingsPanel) {
      super(Messages.get(Messages.SETTINGS));
      this.settingsPanel = settingsPanel;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      UiUtil.displayInDialog(EntityLookupField.this, settingsPanel, Messages.get(Messages.SETTINGS));
    }
  }

  private static final class OKAction extends AbstractAction {

    private final JButton okButton;
    private final Event closeEvent;

    private OKAction(final JButton okButton, final Event closeEvent) {
      super("EntityLookupField.emptyResultOK");
      this.closeEvent = closeEvent;
      this.okButton = okButton;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      okButton.doClick();
      closeEvent.fire();
    }
  }

  private static final class SettingsPanel extends JPanel {

    private SettingsPanel(final EntityLookupModel lookupModel) {
      initializeUI(lookupModel);
    }

    private void initializeUI(final EntityLookupModel lookupModel) {
      final JPanel propertyBasePanel = new JPanel(new CardLayout(5, 5));
      final SwingFilteredComboBoxModel<Property.ColumnProperty> propertyComboBoxModel = new SwingFilteredComboBoxModel<>();
      for (final Map.Entry<Property.ColumnProperty, EntityLookupModel.LookupSettings> entry :
              lookupModel.getPropertyLookupSettings().entrySet()) {
        propertyComboBoxModel.addItem(entry.getKey());
        propertyBasePanel.add(initializePropertyPanel(entry.getValue()), entry.getKey().getPropertyID());
      }
      if (propertyComboBoxModel.getSize() > 0) {
        propertyComboBoxModel.addSelectionListener(new EventListener() {
          @Override
          public void eventOccurred() {
            ((CardLayout) propertyBasePanel.getLayout()).show(propertyBasePanel, propertyComboBoxModel.getSelectedValue().getPropertyID());
          }
        });
        propertyComboBoxModel.setSelectedItem(propertyComboBoxModel.getElementAt(0));
      }

      final JCheckBox boxAllowMultipleValues = new JCheckBox(FrameworkMessages.get(FrameworkMessages.ENABLE_MULTIPLE_SEARCH_VALUES));
      ValueLinks.toggleValueLink(boxAllowMultipleValues.getModel(), lookupModel.getMultipleSelectionAllowedValue(), false);
      final SizedDocument document = new SizedDocument();
      document.setMaxLength(1);
      final JTextField txtMultipleValueSeparator = new JTextField(document, "", 1);
      ValueLinks.textValueLink(txtMultipleValueSeparator, lookupModel.getMultipleItemSeparatorValue(), null, true, false);

      final JPanel generalSettingsPanel = new JPanel(UiUtil.createGridLayout(2, 1));
      generalSettingsPanel.setBorder(BorderFactory.createTitledBorder(""));

      generalSettingsPanel.add(boxAllowMultipleValues);

      final JPanel pnlValueSeparator = new JPanel(UiUtil.createBorderLayout());
      pnlValueSeparator.add(txtMultipleValueSeparator, BorderLayout.WEST);
      pnlValueSeparator.add(new JLabel(FrameworkMessages.get(FrameworkMessages.MULTIPLE_SEARCH_VALUE_SEPARATOR)), BorderLayout.CENTER);

      generalSettingsPanel.add(pnlValueSeparator);

      setLayout(UiUtil.createBorderLayout());
      add(new JComboBox<>(propertyComboBoxModel), BorderLayout.NORTH);
      add(propertyBasePanel, BorderLayout.CENTER);
      add(generalSettingsPanel, BorderLayout.SOUTH);
    }

    private JPanel initializePropertyPanel(final EntityLookupModel.LookupSettings settings) {
      final JPanel panel = new JPanel(UiUtil.createGridLayout(3, 1));
      final JCheckBox boxCaseSensitive = new JCheckBox(FrameworkMessages.get(FrameworkMessages.CASE_SENSITIVE));
      ValueLinks.toggleValueLink(boxCaseSensitive.getModel(), settings.getCaseSensitiveValue(), false);
      final JCheckBox boxPrefixWildcard = new JCheckBox(FrameworkMessages.get(FrameworkMessages.PREFIX_WILDCARD));
      ValueLinks.toggleValueLink(boxPrefixWildcard.getModel(), settings.getWildcardPrefixValue(), false);
      final JCheckBox boxPostfixWildcard = new JCheckBox(FrameworkMessages.get(FrameworkMessages.POSTFIX_WILDCARD));
      ValueLinks.toggleValueLink(boxPostfixWildcard.getModel(), settings.getWildcardPostfixValue(), false);

      panel.add(boxCaseSensitive);
      panel.add(boxPrefixWildcard);
      panel.add(boxPostfixWildcard);

      return panel;
    }
  }
}
