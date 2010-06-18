/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Util;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.TextBeanValueLink;
import org.jminor.common.ui.textfield.SearchFieldHint;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A UI component based on the EntityLookupModel
 * @see EntityLookupModel
 */
public class EntityLookupField extends JTextField {

  private final EntityLookupModel model;
  private final SearchFieldHint searchHint;

  private Action enterAction;
  private Color defaultBackgroundColor = getBackground();

  /**
   * Initializes a new EntityLookupField
   * @param model the model
   */
  public EntityLookupField(final EntityLookupModel model) {
    this(model, false);
  }

  /**
   * Initializes a new EntityLookupField
   * @param model the model
   * @param transferFocusOnEnter if true then the field transfers focus on enter if the field text
   * represents the selected entities
   */
  public EntityLookupField(final EntityLookupModel model, final boolean transferFocusOnEnter) {
    this(model, null);
    if (transferFocusOnEnter) {
      setEnterAction(new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
          transferFocus();
        }
      });
    }
  }

  /**
   * Initializes a new EntityLookupField
   * @param model the model
   * @param enterAction the action that is performed if enter is pressed while the field text represents
   * the selected entities
   */
  public EntityLookupField(final EntityLookupModel model, final Action enterAction) {
    Util.rejectNullValue(model);
    this.model = model;
    this.searchHint = SearchFieldHint.enable(this);
    setEnterAction(enterAction);
    setToolTipText(model.getDescription());
    setComponentPopupMenu(initializePopupMenu());
    addActionListener(initializeLookupAction());
    addFocusListener(initializeFocusListener());
    addEscapeListener();
    bindProperty();
  }

  public EntityLookupModel getModel() {
    return model;
  }

  public Action getEnterAction() {
    return enterAction;
  }

  public void setEnterAction(final Action enterAction) {
    this.enterAction = enterAction;
  }

  public void setDefaultBackgroundColor(final Color defaultBackgroundColor) {
    this.defaultBackgroundColor = defaultBackgroundColor;
  }

  private void selectEntities(final List<Entity> entities) {
    if (entities.size() == 0) {
      JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CRITERIA));
    }
    else if (entities.size() == 1) {
      getModel().setSelectedEntities(entities);
    }
    else {
      Collections.sort(entities, new Comparator<Entity>() {
        public int compare(final Entity e1, final Entity e2) {
          return e1.compareTo(e2);
        }
      });
      final JList list = new JList(entities.toArray());
      final Window owner = UiUtil.getParentWindow(EntityLookupField.this);
      final JDialog dialog = new JDialog(owner, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY));
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
        public void actionPerformed(ActionEvent e) {
          getModel().setSelectedEntities(toEntityList(list.getSelectedValues()));
          dialog.dispose();
        }
      };
      final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
        public void actionPerformed(ActionEvent e) {
          dialog.dispose();
        }
      };
      list.setSelectionMode(getModel().isMultipleSelectionAllowed() ?
              ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
      final JButton btnOk  = new JButton(okAction);
      final JButton btnCancel = new JButton(cancelAction);
      final String cancelMnemonic = Messages.get(Messages.CANCEL_MNEMONIC);
      final String okMnemonic = Messages.get(Messages.OK_MNEMONIC);
      btnOk.setMnemonic(okMnemonic.charAt(0));
      btnCancel.setMnemonic(cancelMnemonic.charAt(0));
      UiUtil.addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, cancelAction);
      list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
              KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
      list.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
          if (e.getClickCount() == 2) {
            okAction.actionPerformed(null);
          }
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
  }

  private void bindProperty() {
    new TextBeanValueLink(this, getModel(), "searchString", String.class, getModel().eventSearchStringChanged()) {
      @Override
      protected void setUIValue(final Object propertyValue) {
        super.setUIValue(propertyValue);
        updateColors();
        searchHint.updateState();
      }
    };
    getModel().eventSearchStringChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateColors();
      }
    });
  }

  private void addEscapeListener() {
    UiUtil.addKeyEvent(this, KeyEvent.VK_ESCAPE, new AbstractAction("cancel") {
      public void actionPerformed(ActionEvent e) {
        getModel().refreshSearchText();
        selectAll();
      }
    });
  }

  private FocusListener initializeFocusListener() {
    return new FocusListener() {
      public void focusGained(final FocusEvent e) {
        updateColors();
      }
      public void focusLost(final FocusEvent e) {
        if (getText().length() == 0) {
          getModel().setSelectedEntity(null);
        }
//        else //todo?
//          performLookup();
        updateColors();
      }
    };
  }

  private void updateColors() {
    final boolean defaultBackground = getModel().searchStringRepresentsSelected() || searchHint.isHintVisible();
    setBackground(defaultBackground ? defaultBackgroundColor : Color.LIGHT_GRAY);
  }

  private AbstractAction initializeLookupAction() {
    return new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(final ActionEvent e) {
        performLookup();
      }
    };
  }

  private void performLookup() {
    if (getModel().getSearchString().length() == 0) {
      getModel().setSelectedEntities(null);
      if (getEnterAction() != null) {
        getEnterAction().actionPerformed(new ActionEvent(this, 0, "actionPerformed"));
      }
    }
    else {
      if (getModel().searchStringRepresentsSelected() && getEnterAction() != null) {
        getEnterAction().actionPerformed(new ActionEvent(this, 0, "actionPerformed"));
      }
      else if (!getModel().searchStringRepresentsSelected()) {
        List<Entity> queryResult;
        try {
          UiUtil.setWaitCursor(true, this);
          queryResult = getModel().performQuery();
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }
        selectEntities(queryResult);
      }
    }
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new AbstractAction(FrameworkMessages.get(FrameworkMessages.SETTINGS)) {
      public void actionPerformed(ActionEvent e) {
        final JPanel panel = new JPanel(new GridLayout(3,1,5,5));
        final JCheckBox boxCaseSensitive =
                new JCheckBox(FrameworkMessages.get(FrameworkMessages.CASE_SENSITIVE), getModel().isCaseSensitive());
        final JCheckBox boxPrefixWildcard =
                new JCheckBox(FrameworkMessages.get(FrameworkMessages.PREFIX_WILDCARD), getModel().isWildcardPrefix());
        final JCheckBox boxPostfixWildcard =
                new JCheckBox(FrameworkMessages.get(FrameworkMessages.POSTFIX_WILDCARD), getModel().isWildcardPostfix());
        panel.add(boxCaseSensitive);
        panel.add(boxPrefixWildcard);
        panel.add(boxPostfixWildcard);
        final AbstractAction action = new AbstractAction(Messages.get(Messages.OK)) {
          public void actionPerformed(final ActionEvent ae) {
            getModel().setCaseSensitive(boxCaseSensitive.isSelected());
            getModel().setWildcardPrefix(boxPrefixWildcard.isSelected());
            getModel().setWildcardPostfix(boxPostfixWildcard.isSelected());
          }
        };
        action.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));
        UiUtil.showInDialog(UiUtil.getParentWindow(EntityLookupField.this), panel, true,
                FrameworkMessages.get(FrameworkMessages.SETTINGS), true, true, action);
      }
    });

    return popupMenu;
  }

  private List<Entity> toEntityList(final Object[] selectedValues) {
    final List<Entity> entityList = new ArrayList<Entity>(selectedValues.length);
    for (final Object object : selectedValues) {
      entityList.add((Entity) object);
    }

    return entityList;
  }
}
