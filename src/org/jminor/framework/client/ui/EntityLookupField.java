/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.TextBeanPropertyLink;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class EntityLookupField extends JTextField {

  private final EntityLookupModel model;

  private Action enterAction;

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
    this.model = model;
    this.enterAction = enterAction;
    setComponentPopupMenu(initializePopupMenu());
    addActionListener(new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(final ActionEvent e) {
        try {
          if (model.getSearchString().length() == 0) {
            model.setSelectedEntities(null);
          }
          else {
            if (model.textRepresentsSelected() && getEnterAction() != null)
              getEnterAction().actionPerformed(new ActionEvent(EntityLookupField.this, 0, "actionPerformed"));
            else if (!model.textRepresentsSelected())
              selectEntities(model.performQuery());
          }
        }
        catch (UserException ex) {
          ExceptionDialog.handleException(ex, EntityLookupField.this);
        }
      }
    });
    new TextBeanPropertyLink(this, model, "searchString", String.class, model.evtSearchStringChanged, null);
    model.evtSearchStringChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updateBackgroundColor();
      }
    });
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

  public JPanel createPanel(final EntityTableModel lookupModel) {
    final JButton btn = new JButton(new AbstractAction("...") {
      public void actionPerformed(ActionEvent e) {
        try {
          model.setSelectedEntities(FrameworkUiUtil.selectEntities(lookupModel, UiUtil.getParentWindow(EntityLookupField.this),
                  true, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY), null, false));
        }
        catch (UserCancelException ex) {/**/}
      }
    });
    btn.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);

    final JPanel ret = new JPanel(new BorderLayout(5,0));
    ret.add(this, BorderLayout.CENTER);
    ret.add(btn, BorderLayout.EAST);

    return ret;
  }

  private void selectEntities(final List<Entity> entities) {
    if (entities.size() == 0)
      JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CRITERIA));
    else if (entities.size() == 1)
      model.setSelectedEntities(entities);
    else {
      Collections.sort(entities, new Comparator<Entity>() {
        public int compare(final Entity e1, final Entity e2) {
          return e1.toString().compareTo(e2.toString());
        }
      });
      final JList list = new JList(new Vector<Entity>(entities));
      final Window owner = UiUtil.getParentWindow(EntityLookupField.this);
      final JDialog dialog = new JDialog(owner, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY));
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
        public void actionPerformed(ActionEvent e) {
          final Object[] selectedValues = list.getSelectedValues();
          final List<Entity> entityList = new ArrayList<Entity>(selectedValues.length);
          for (final Object object : selectedValues)
            entityList.add((Entity) object);
          model.setSelectedEntities(entityList);
          dialog.dispose();
        }
      };
      final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
        public void actionPerformed(ActionEvent e) {
          dialog.dispose();
        }
      };
      list.setSelectionMode(model.isMultipleSelectionAllowed() ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
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
    }
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu ret = new JPopupMenu();
    ret.add(new AbstractAction(FrameworkMessages.get(FrameworkMessages.SETTINGS)) {
      public void actionPerformed(ActionEvent e) {
        final JPanel panel = new JPanel(new GridLayout(3,1,5,5));
        final JCheckBox boxCaseSensitive = new JCheckBox(FrameworkMessages.get(FrameworkMessages.CASE_SENSITIVE), model.isCaseSensitive());
        final JCheckBox boxPrefixWildcard = new JCheckBox(FrameworkMessages.get(FrameworkMessages.PREFIX_WILDCARD), model.isWildcardPrefix());
        final JCheckBox boxPostfixWildcard = new JCheckBox(FrameworkMessages.get(FrameworkMessages.POSTFIX_WILDCARD), model.isWildcardPostfix());
        panel.add(boxCaseSensitive);
        panel.add(boxPrefixWildcard);
        panel.add(boxPostfixWildcard);
        final AbstractAction action = new AbstractAction(Messages.get(Messages.OK)) {
          public void actionPerformed(final ActionEvent ae) {
            model.setCaseSensitive(boxCaseSensitive.isSelected());
            model.setWildcardPrefix(boxPrefixWildcard.isSelected());
            model.setWildcardPostfix(boxPostfixWildcard.isSelected());
          }
        };
        action.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));
        UiUtil.showInDialog(UiUtil.getParentWindow(EntityLookupField.this), panel, true,
                FrameworkMessages.get(FrameworkMessages.SETTINGS), true, true, action);
      }
    });

    return ret;
  }

  private void updateBackgroundColor() {
    setBackground(model.textRepresentsSelected() ? Color.WHITE : Color.LIGHT_GRAY);
  }
}
