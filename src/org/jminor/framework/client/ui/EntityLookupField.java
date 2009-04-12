/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.CriteriaSet;
import org.jminor.common.db.ICriteria;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.UserException;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.textfield.TextFieldPlus;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.Property;

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
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class EntityLookupField extends TextFieldPlus {

  public final Event evtSelectedEntitiesChanged = new Event("EntityLookupField.selectedEntitiesChanged");

  /**
   * The ID of the entity this lookup field is based on
   */
  private final String entityID;

  /**
   * The properties to use when doing the lookup
   */
  private final List<Property> lookupProperties;

  /**
   * The selected entitites
   */
  private final List<Entity> selectedEntities = new ArrayList<Entity>();

  private final Action lookupAction;

  private IEntityDbProvider dbProvider;
  private ICriteria additionalLookupCriteria;

  private State stTextRepresentsSelected = new State();

  private boolean allowMultipleSelection;
  private boolean caseSensitive;
  private boolean wildcardPrefix;
  private boolean wildcardPostfix;
  private String wildcard = (String) FrameworkSettings.get().getProperty(FrameworkSettings.WILDCARD_CHARACTER);
  private String multiValueSeperator = ",";
  private boolean transferFocusOnEnter = false;

  public EntityLookupField(final IEntityDbProvider dbProvider, final String entityID, final List<Property> lookupProperties) {
    this(dbProvider, entityID, null, lookupProperties);
  }

  public EntityLookupField(final IEntityDbProvider dbProvider, final String entityID, final ICriteria additionalLookupCriteria,
                           final List<Property> lookupProperties) {
    this(dbProvider, entityID, additionalLookupCriteria, false, lookupProperties);
  }

  public EntityLookupField(final IEntityDbProvider dbProvider, final String entityID, final ICriteria additionalLookupCriteria,
                           final boolean caseSensitive, final List<Property> lookupProperties) {
    this(dbProvider, entityID, additionalLookupCriteria, caseSensitive, true, true, lookupProperties);
  }

  public EntityLookupField(final IEntityDbProvider dbProvider, final String entityID, final ICriteria additionalLookupCriteria,
                           final boolean caseSensitive, final boolean wildcardPrefix, final boolean wildcardPostfix,
                           final List<Property> lookupProperties) {
    setDbProvider(dbProvider);
    this.entityID = entityID;
    this.lookupProperties = lookupProperties;
    this.additionalLookupCriteria = additionalLookupCriteria;
    this.caseSensitive = caseSensitive;
    this.wildcardPrefix = wildcardPrefix;
    this.wildcardPostfix = wildcardPostfix;
    this.lookupAction = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(final ActionEvent e) {
        try {
          if (getText().length() == 0) {
            setSelectedEntities(null);
          }
          else {
            if (stTextRepresentsSelected.isActive() && transferFocusOnEnter)
              transferFocus();
            else
              performLookup();
          }
        }
        catch (UserException ex) {
          ExceptionDialog.handleException(ex, EntityLookupField.this);
        }
      }
    };
    addActionListener(lookupAction);
    getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(final DocumentEvent e) {
        updateLookupState();
      }
      public void insertUpdate(final DocumentEvent e) {
        updateLookupState();
      }
      public void removeUpdate(final DocumentEvent e) {
        updateLookupState();
      }
    });
    setComponentPopupMenu(initializePopupMenu());
    updateLookupState();
  }

  public void setDbProvider(final IEntityDbProvider dbProvider) {
    this.dbProvider = dbProvider;
  }

  public boolean isTransferFocusOnEnter() {
    return transferFocusOnEnter;
  }

  public void setTransferFocusOnEnter(final boolean transferFocusOnEnter) {
    this.transferFocusOnEnter = transferFocusOnEnter;
  }

  public boolean isAllowMultipleSelection() {
    return allowMultipleSelection;
  }

  public void setAllowMultipleSelection(final boolean allowMultipleSelection) {
    this.allowMultipleSelection = allowMultipleSelection;
  }

  public void setSelectedEntity(final Entity entity) {
    setSelectedEntities(Arrays.asList(entity));
  }

  public void setSelectedEntities(final List<Entity> entities) {
    this.selectedEntities.clear();
    if (entities != null)
      this.selectedEntities.addAll(entities);
    refreshText();
    evtSelectedEntitiesChanged.fire();
  }

  public List<Entity> getSelectedEntities() {
    return selectedEntities;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public void setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public boolean isWildcardPostfix() {
    return wildcardPostfix;
  }

  public void setWildcardPostfix(final boolean wildcardPostfix) {
    this.wildcardPostfix = wildcardPostfix;
  }

  public boolean isWildcardPrefix() {
    return wildcardPrefix;
  }

  public void setWildcardPrefix(final boolean wildcardPrefix) {
    this.wildcardPrefix = wildcardPrefix;
  }

  /**
   * @return the wildcard
   */
  public String getWildcard() {
    return wildcard;
  }

  /**
   * Sets the wildcard to use
   * @param wildcard the wildcard
   */
  public void setWildcard(final String wildcard) {
    this.wildcard = wildcard;
  }

  public String getMultiValueSeperator() {
    return multiValueSeperator;
  }

  public void setMultiValueSeperator(final String multiValueSeperator) {
    this.multiValueSeperator = multiValueSeperator;
    refreshText();
  }

  public void setAdditionalLookupCriteria(final ICriteria additionalLookupCriteria) {
    this.additionalLookupCriteria = additionalLookupCriteria;
    setSelectedEntities(null);
  }

  public Action getLookupAction() {
    return lookupAction;
  }

  public void refreshText() {
    setText(getSelectedEntities().size() == 0 ? "" : toString(getSelectedEntities()));
  }

  public EntityCriteria getEntityCriteria() {
    final CriteriaSet baseCriteria = new CriteriaSet(CriteriaSet.Conjunction.OR);
    final String[] lookupTexts = isAllowMultipleSelection() ? getText().split(getMultiValueSeperator()) : new String[] {getText()};
    for (final Property lookupProperty : lookupProperties) {
      for (final String lookupText : lookupTexts) {
        final String modifiedLookupText = (isWildcardPrefix() ? getWildcard() : "") + lookupText
                + (isWildcardPostfix() ? getWildcard() : "");
        baseCriteria.addCriteria(new PropertyCriteria(lookupProperty, SearchType.LIKE, modifiedLookupText).setCaseSensitive(isCaseSensitive()));
      }
    }

    return new EntityCriteria(entityID, additionalLookupCriteria == null ? baseCriteria :
            new CriteriaSet(CriteriaSet.Conjunction.AND, additionalLookupCriteria, baseCriteria));
  }

  private void performLookup() throws UserException {
    final List<Entity> lookupResult = doLookup();
    if (lookupResult.size() == 0)
      JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CRITERIA));
    else if (lookupResult.size() == 1)
      setSelectedEntities(lookupResult);
    else
      selectEntities(lookupResult);
  }

  private List<Entity> doLookup() throws UserException {
    try {
      return dbProvider.getEntityDb().selectMany(getEntityCriteria());
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  private void selectEntities(final List<Entity> entities) {
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
        final List<Entity> entities = new ArrayList<Entity>(selectedValues.length);
        for (final Object obj : selectedValues)
          entities.add((Entity) obj);
        setSelectedEntities(entities);
        dialog.dispose();
      }
    };
    final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
      public void actionPerformed(ActionEvent e) {
        dialog.dispose();
      }
    };
    list.setSelectionMode(isAllowMultipleSelection() ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
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

  private void updateLookupState() {
    final String selectedAsString = toString(getSelectedEntities());
    stTextRepresentsSelected.setActive(getSelectedEntities().size() > 0 && selectedAsString.equals(getText()));
    setBackground(stTextRepresentsSelected.isActive() ? Color.WHITE : Color.LIGHT_GRAY);
  }

  private String toString(final List<Entity> entityList) {
    final StringBuffer ret = new StringBuffer();
    for (int i = 0; i < entityList.size(); i++) {
      ret.append(entityList.get(i).toString());
      if (i < entityList.size()-1)
        ret.append(getMultiValueSeperator());
    }

    return ret.toString();
  }

  private JPopupMenu initializePopupMenu() {
    final JPopupMenu ret = new JPopupMenu();
    ret.add(new AbstractAction(FrameworkMessages.get(FrameworkMessages.SETTINGS)) {
      public void actionPerformed(ActionEvent e) {
        final JPanel panel = new JPanel(new GridLayout(3,1,5,5));
        final JCheckBox boxCaseSensitive = new JCheckBox(FrameworkMessages.get(FrameworkMessages.CASE_SENSITIVE), isCaseSensitive());
        final JCheckBox boxPrefixWildcard = new JCheckBox(FrameworkMessages.get(FrameworkMessages.PREFIX_WILDCARD), isWildcardPrefix());
        final JCheckBox boxPostfixWildcard = new JCheckBox(FrameworkMessages.get(FrameworkMessages.POSTFIX_WILDCARD), isWildcardPostfix());
        panel.add(boxCaseSensitive);
        panel.add(boxPrefixWildcard);
        panel.add(boxPostfixWildcard);
        final AbstractAction action = new AbstractAction(Messages.get(Messages.OK)) {
          public void actionPerformed(final ActionEvent e) {
            setCaseSensitive(boxCaseSensitive.isSelected());
            setWildcardPrefix(boxPrefixWildcard.isSelected());
            setWildcardPostfix(boxPostfixWildcard.isSelected());
          }
        };
        action.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));
        UiUtil.showInDialog(UiUtil.getParentWindow(EntityLookupField.this), panel, true,
                FrameworkMessages.get(FrameworkMessages.SETTINGS), true, true, action);
      }
    });

    return ret;
  }
}
