/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.ValueLinks;
import org.jminor.common.ui.textfield.SizedDocument;
import org.jminor.common.ui.textfield.TextFieldHint;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A UI component based on the EntityLookupModel.
 *
 * The lookup is triggered by the ENTER key and behaves in the following way:
 * If the lookup result is empty a message is shown, if a single entity fits the
 * criteria then that entity is selected, otherwise a list containing the entities
 * fitting the criteria is shown in a dialog allowing either a single or multiple
 * selection based on {@link org.jminor.framework.client.model.EntityLookupModel#isMultipleSelectionAllowed()}}.
 *
 * @see EntityLookupModel
 */
public final class EntityLookupField extends JTextField {

  private static final int BORDER_SIZE = 15;

  private final EntityLookupModel model;
  private final TextFieldHint searchHint;
  private final Action transferFocusAction = new UiUtil.TransferFocusAction(this);
  private final Action transferFocusBackwardAction = new UiUtil.TransferFocusAction(this, true);

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
   * @param lookupOnKeyRelease if true then lookup is performed on key release, otherwise it
   * is performed on keyPressed.
   */
  public EntityLookupField(final EntityLookupModel lookupModel, final boolean lookupOnKeyRelease) {
    Util.rejectNullValue(lookupModel, "lookupModel");
    this.model = lookupModel;
    setValidBackgroundColor(getBackground());
    setInvalidBackgroundColor(Color.LIGHT_GRAY);
    setToolTipText(lookupModel.getDescription());
    setComponentPopupMenu(initializePopupMenu());
    addFocusListener(initializeFocusListener());
    addEscapeListener();
    linkToModel();
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

  private boolean selectEntities(final List<Entity> entities) {
    Collections.sort(entities, new EntityComparator());
    final JList list = new JList(entities.toArray());
    final Window owner = UiUtil.getParentWindow(this);
    final JDialog dialog = new JDialog(owner, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY));
    final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        getModel().setSelectedEntities(toEntityList(list.getSelectedValues()));
        dialog.dispose();
      }
    };
    final Action cancelAction = new UiUtil.DisposeWindowAction(dialog);
    list.setSelectionMode(model.isMultipleSelectionAllowed() ?
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
    UiUtil.prepareScrollPanelDialog(dialog, this, list, okAction, cancelAction);

    return model.searchStringRepresentsSelected();
  }

  private void linkToModel() {
    ValueLinks.textValueLink(this, getModel(), "searchString", String.class, getModel().getSearchStringObserver());
    model.addSearchStringListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        updateColors();
      }
    });
    getModel().addSelectedEntitiesListener(new EventAdapter() {
      @Override
      public void eventOccurred() {
        setCaretPosition(0);
      }
    });
  }

  private void addEscapeListener() {
    final AbstractAction escapeAction = new AbstractAction("EntityLookupField.escape") {
      /** {@inheritDoc} */
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
      /** {@inheritDoc} */
      @Override
      public void focusGained(final FocusEvent e) {
        updateColors();
      }
      /** {@inheritDoc} */
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
    final boolean validBackground = model.searchStringRepresentsSelected() || (searchHint !=null && searchHint.isHintTextVisible());
    setBackground(validBackground ? validBackgroundColor : invalidBackgroundColor);
  }

  private AbstractAction initializeLookupAction() {
    final AbstractAction lookupAction = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        performLookup(true);
      }
    };
    UiUtil.linkToEnabledState(getModel().getSearchStringRepresentsSelectedObserver().getReversedObserver(), lookupAction);

    return lookupAction;
  }

  private void performLookup(final boolean promptUser) {
    try {
      performingLookup = true;
      if (model.getSearchString().length() == 0) {
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
    }
    finally {
      performingLookup = false;
    }
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new SettingsAction(this));

    return popupMenu;
  }

  private List<Entity> toEntityList(final Object[] selectedValues) {
    final List<Entity> entityList = new ArrayList<Entity>(selectedValues.length);
    for (final Object object : selectedValues) {
      entityList.add((Entity) object);
    }

    return entityList;
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
    UiUtil.displayInDialog(this, messagePanel, Messages.get("OptionPane.messageDialogTitle"), closeEvent);
  }

  private static final class SettingsAction extends AbstractAction {

    private final EntityLookupField lookupPanel;

    private SettingsAction(final EntityLookupField lookupPanel) {
      super(Messages.get(Messages.SETTINGS));
      this.lookupPanel = lookupPanel;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(final ActionEvent e) {
      final JPanel panel = new JPanel(UiUtil.createGridLayout(5, 1));
      final JCheckBox boxCaseSensitive =
              new JCheckBox(FrameworkMessages.get(FrameworkMessages.CASE_SENSITIVE), lookupPanel.getModel().isCaseSensitive());
      final JCheckBox boxPrefixWildcard =
              new JCheckBox(FrameworkMessages.get(FrameworkMessages.PREFIX_WILDCARD), lookupPanel.getModel().isWildcardPrefix());
      final JCheckBox boxPostfixWildcard =
              new JCheckBox(FrameworkMessages.get(FrameworkMessages.POSTFIX_WILDCARD), lookupPanel.getModel().isWildcardPostfix());
      final JCheckBox boxAllowMultipleValues =
              new JCheckBox(FrameworkMessages.get(FrameworkMessages.ENABLE_MULTIPLE_SEARCH_VALUES), lookupPanel.getModel().isMultipleSelectionAllowed());
      final SizedDocument document = new SizedDocument();
      document.setMaxLength(1);
      final JTextField txtMultipleValueSeparator = new JTextField(document, "", 1);
      txtMultipleValueSeparator.setText(lookupPanel.getModel().getMultipleValueSeparator());

      panel.add(boxCaseSensitive);
      panel.add(boxPrefixWildcard);
      panel.add(boxPostfixWildcard);
      panel.add(boxAllowMultipleValues);

      final JPanel pnlValueSeparator = new JPanel(UiUtil.createBorderLayout());
      pnlValueSeparator.add(txtMultipleValueSeparator, BorderLayout.WEST);
      pnlValueSeparator.add(new JLabel(FrameworkMessages.get(FrameworkMessages.MULTIPLE_SEARCH_VALUE_SEPARATOR)), BorderLayout.CENTER);

      panel.add(pnlValueSeparator);
      final AbstractAction action = new AbstractAction(Messages.get(Messages.OK)) {
        /** {@inheritDoc} */
        @Override
        public void actionPerformed(final ActionEvent e) {
          lookupPanel.getModel().setCaseSensitive(boxCaseSensitive.isSelected());
          lookupPanel.getModel().setWildcardPrefix(boxPrefixWildcard.isSelected());
          lookupPanel.getModel().setWildcardPostfix(boxPostfixWildcard.isSelected());
          lookupPanel.getModel().setMultipleSelectionAllowed(boxAllowMultipleValues.isSelected());
          if (txtMultipleValueSeparator.getText().length() != 0) {
            lookupPanel.getModel().setMultipleValueSeparator(txtMultipleValueSeparator.getText());
          }
        }
      };
      action.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));
      UiUtil.displayInDialog(lookupPanel, panel, Messages.get(Messages.SETTINGS), action);
    }
  }

  private static final class EntityComparator implements Comparator<Entity>, Serializable {
    private static final long serialVersionUID = 1;
    /** {@inheritDoc} */
    @Override
    public int compare(final Entity o1, final Entity o2) {
      return o1.compareTo(o2);
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

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(final ActionEvent e) {
      okButton.doClick();
      closeEvent.fire();
    }
  }
}
