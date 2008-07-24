package org.jminor.framework.client.ui;

import org.jminor.common.db.CriteriaSet;
import org.jminor.common.db.ICriteria;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.SearchType;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.textfield.TextFieldPlus;
import org.jminor.framework.FrameworkConstants;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityCriteria;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.PropertyCriteria;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class EntitySearchField extends TextFieldPlus {

  public final Event evtSelectedEntityChanged = new Event("EntitySearchField.selectedEntityChanged");

  private final String entityID;
  private final List<Property> searchProperties;
  private final ICriteria additionalSearchCriteria;
  private final boolean caseSensitive;
  private final boolean wildcardPrefix;
  private final boolean wildcardPostfix;

  private final Action searchAction;

  private Entity selectedEntity;

  public EntitySearchField(final IEntityDbProvider dbProvider, final String entityID, final String... searchPropertyIDs) {
    this(dbProvider, entityID, null, searchPropertyIDs);
  }

  public EntitySearchField(final IEntityDbProvider dbProvider, final String entityID, final ICriteria additionalSearchCriteria,
                           final String... searchPropertyIDs) {
    this(dbProvider, entityID, additionalSearchCriteria, false, searchPropertyIDs);
  }

  public EntitySearchField(final IEntityDbProvider dbProvider, final String entityID, final ICriteria additionalSearchCriteria,
                             final boolean caseSensitive, final String... searchPropertyIDs) {
    this(dbProvider, entityID, additionalSearchCriteria, caseSensitive, true, true, searchPropertyIDs);
  }

  public EntitySearchField(final IEntityDbProvider dbProvider, final String entityID, final ICriteria additionalSearchCriteria,
                           final boolean caseSensitive, final boolean wildcardPrefix, final boolean wildcardPostfix,
                           final String... searchPropertyIDs) {
    this.entityID = entityID;
    this.searchProperties = EntityRepository.get().getProperties(entityID, searchPropertyIDs);
    this.additionalSearchCriteria = additionalSearchCriteria;
    this.caseSensitive = caseSensitive;
    this.wildcardPrefix = wildcardPrefix;
    this.wildcardPostfix = wildcardPostfix;
    this.searchAction = new AbstractAction(FrameworkMessages.get(FrameworkMessages.SEARCH)) {
      public void actionPerformed(final ActionEvent e) {
        try {
          performSearch(dbProvider);
        }
        catch (Exception ex) {
          UiUtil.handleException(ex, EntitySearchField.this);
        }
      }
    };
    addActionListener(searchAction);
    getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(final DocumentEvent e) {
        handleChange();
      }
      public void insertUpdate(final DocumentEvent e) {
        handleChange();
      }
      public void removeUpdate(final DocumentEvent e) {
        handleChange();
      }
    });
  }

  public void setSelectedEntity(final Entity entity) {
    this.selectedEntity = entity;
    setText(entity == null ? "" : entity.toString());
    evtSelectedEntityChanged.fire();
  }

  public Entity getSelectedEntity() {
    return selectedEntity;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public boolean isWildcardPostfix() {
    return wildcardPostfix;
  }

  public boolean isWildcardPrefix() {
    return wildcardPrefix;
  }

  public Action getSearchAction() {
    return searchAction;
  }

  public EntityCriteria getEntityCriteria() {
    final CriteriaSet baseCriteria = new CriteriaSet(CriteriaSet.Conjunction.OR);
    final String searchText = (isWildcardPrefix() ? FrameworkConstants.WILDCARD : "") + getText()
            + (isWildcardPostfix() ? FrameworkConstants.WILDCARD : "");
    for (final Property searchProperty : searchProperties)
      baseCriteria.addCriteria(new PropertyCriteria(searchProperty, SearchType.LIKE, searchText).setCaseSensitive(isCaseSensitive()));

    return new EntityCriteria(entityID, additionalSearchCriteria == null ? baseCriteria :
            new CriteriaSet(CriteriaSet.Conjunction.AND, additionalSearchCriteria, baseCriteria));
  }

  private void performSearch(final IEntityDbProvider dbProvider) throws Exception {
    final List<Entity> searchResult = getSearchResult(dbProvider);
    if (searchResult.size() == 0) {
      JOptionPane.showMessageDialog(this, FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CRITERIA));
    }
    else if (searchResult.size() == 1) {
      setSelectedEntity(searchResult.get(0));
    }
    else {
      selectEntity(searchResult);
    }
  }

  private List<Entity> getSearchResult(final IEntityDbProvider dbProvider) throws Exception {
    return dbProvider.getEntityDb().selectMany(getEntityCriteria());
  }

  private void selectEntity(final List<Entity> entities) {
    Collections.sort(entities, new Comparator<Entity>() {
      public int compare(final Entity e1, final Entity e2) {
        return e1.toString().compareTo(e2.toString());
      }
    });
    final JList list = new JList(new Vector<Entity>(entities));
    final Window owner = UiUtil.getParentWindow(EntitySearchField.this);
    final JDialog dialog = new JDialog(owner, FrameworkMessages.get(FrameworkMessages.SELECT_ENTITY));
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
      public void actionPerformed(ActionEvent e) {
        if (!list.isSelectionEmpty())
          setSelectedEntity((Entity) list.getSelectedValue());
        dialog.dispose();
      }
    };
    final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
      public void actionPerformed(ActionEvent e) {
        dialog.dispose();
      }
    };
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final JButton btnClose  = new JButton(okAction);
    final JButton btnCancel = new JButton(cancelAction);
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
    btnClose.setMnemonic('L');
    btnCancel.setMnemonic('H');
    dialog.setLayout(new BorderLayout());
    final JScrollPane scroller = new JScrollPane(list);
    dialog.add(scroller, BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
    buttonPanel.add(btnClose);
    buttonPanel.add(btnCancel);
    dialog.getRootPane().setDefaultButton(btnClose);
    dialog.add(buttonPanel, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setModal(true);
    dialog.setResizable(true);
    dialog.setVisible(true);
  }

  private void handleChange() {
    final String selectedAsString = selectedEntity == null ? null : selectedEntity.toString();
    if (selectedAsString != null && !selectedAsString.equals(getText()))
      setBackground(Color.LIGHT_GRAY);
    else
      setBackground(Color.WHITE);
  }
}
