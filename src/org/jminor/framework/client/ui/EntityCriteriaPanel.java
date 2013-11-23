/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.EventListener;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.table.ColumnSearchPanel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.EntityTableSearchModel;
import org.jminor.framework.client.model.ForeignKeySearchModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A panel for configuring the PropertySearchModels for a given EntityTableModel.
 */
public final class EntityCriteriaPanel extends JPanel {

  private static final int DEFAULT_WIDTH = 200;
  private static final int DEFAULT_HEIGHT = 40;

  private final Map<PropertySearchModel, ColumnSearchPanel> panels = new HashMap<>();

  /**
   * Instantiates a new EntityCriteriaPanel for the given table model
   * @param tableModel the table model
   */
  public EntityCriteriaPanel(final EntityTableModel tableModel) {
    setLayout(UiUtil.createBorderLayout());

    final JPanel editPanel = new JPanel(new BorderLayout());
    editPanel.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

    final JList propertyList = initializePropertyList(tableModel.getSearchModel(), editPanel);
    final JScrollPane scroller = new JScrollPane(propertyList);

    final JPanel propertyBase = new JPanel(new BorderLayout());
    propertyBase.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.LIMIT_QUERY)));

    propertyBase.add(scroller, BorderLayout.NORTH);
    propertyBase.add(editPanel, BorderLayout.CENTER);
    add(propertyBase, BorderLayout.CENTER);

    tableModel.getSearchModel().refresh();

    add(initializeConfigurationPanel(tableModel), BorderLayout.SOUTH);
  }

  private JPanel initializeConfigurationPanel(final EntityTableModel tableModel) {
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panel.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.FILTER_SETTINGS)));
    panel.add(ControlProvider.createCheckBox(Controls.toggleControl(tableModel,
            "queryCriteriaRequired", FrameworkMessages.get(FrameworkMessages.REQUIRE_QUERY_CRITERIA))));

    return panel;
  }

  private JList initializePropertyList(final EntityTableSearchModel searchModel, final JPanel editorPanel) {
    final List<PropertySearchModel> searchCriteria = getSortedCriteria(searchModel);
    final JList<PropertySearchModel> propertyList = new JList<>(new DefaultListModel<PropertySearchModel>());
    for (final PropertySearchModel model : searchCriteria) {
      ((DefaultListModel<PropertySearchModel>) propertyList.getModel()).addElement(model);
      model.addSearchStateListener(new RepaintListener(propertyList));
    }
    propertyList.setCellRenderer(new CriteriaListCellRenderer());
    propertyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    propertyList.addListSelectionListener(new SearchModelSelectionListener(editorPanel, propertyList));

    return propertyList;
  }

  private List<PropertySearchModel> getSortedCriteria(final EntityTableSearchModel searchModel) {
    final List<PropertySearchModel> searchCriteria =
            new ArrayList<PropertySearchModel>(searchModel.getPropertySearchModels());
    Collections.sort(searchCriteria, new SearchModelComparator());

    return searchCriteria;
  }

  private static final class RepaintListener implements EventListener {
    private final JList propertyList;

    private RepaintListener(final JList propertyList) {
      this.propertyList = propertyList;
    }

    @Override
    public void eventOccurred() {
      propertyList.repaint();
    }
  }

  private static final class CriteriaListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                  final boolean isSelected, final boolean cellHasFocus) {
      final Component cellRenderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      final PropertySearchModel selected = (PropertySearchModel) value;
      ((JLabel)cellRenderer).setText(selected.getColumnIdentifier().toString());
      cellRenderer.setForeground(selected.isEnabled() ? Color.red : Color.black);

      return cellRenderer;
    }
  }

  private static final class SearchModelComparator implements Comparator<PropertySearchModel>, Serializable {
    private static final long serialVersionUID = 1;
    @Override
    public int compare(final PropertySearchModel o1, final PropertySearchModel o2) {
      final Property propertyOne = (Property) o1.getColumnIdentifier();
      final Property propertyTwo = (Property) o2.getColumnIdentifier();
      if (propertyOne.getCaption() != null && propertyTwo.getCaption() != null) {
        return propertyOne.getCaption().compareTo(propertyTwo.getCaption());
      }
      else {
        return propertyOne.getPropertyID().compareTo(propertyTwo.getPropertyID());
      }
    }
  }

  private final class SearchModelSelectionListener implements ListSelectionListener {

    private final JPanel editorPanel;
    private final JList propertyList;

    private SearchModelSelectionListener(final JPanel editorPanel, final JList propertyList) {
      this.editorPanel = editorPanel;
      this.propertyList = propertyList;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void valueChanged(final ListSelectionEvent e) {
      editorPanel.removeAll();
      final PropertySearchModel selected = (PropertySearchModel) propertyList.getSelectedValue();
      if (selected != null) {
        ColumnSearchPanel panel = panels.get(selected);
        if (panel == null) {
          if (selected instanceof ForeignKeySearchModel) {
            panel = new ForeignKeySearchPanel((ForeignKeySearchModel) selected, true, true);
          }
          else {
            panel = new PropertySearchPanel(selected, true, true);
          }
          panels.put(selected, panel);
        }

        editorPanel.add(panel, BorderLayout.NORTH);
        revalidate();
        repaint();
      }
    }
  }
}
