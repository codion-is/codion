/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.AbstractSearchModel;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.EntityTableSearchModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * A panel for configuring the PropertySearchModels for a given EntityTableModel.
 */
public class EntityCriteriaPanel extends JPanel {

  private final Map<PropertySearchModel, PropertySearchPanel> panels = new HashMap<PropertySearchModel, PropertySearchPanel>();

  public EntityCriteriaPanel(final EntityTableModel tableModel) {
    setLayout(new BorderLayout(5,5));

    final JPanel editPanel = new JPanel(new BorderLayout());
    editPanel.setPreferredSize(new Dimension(200,40));

    final JList propertyList = initializePropertyList(tableModel.getSearchModel(), editPanel);
    final JScrollPane scroller = new JScrollPane(propertyList);

    final JPanel propertyBase = new JPanel(new BorderLayout());
    propertyBase.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.LIMIT_QUERY)));

    propertyBase.add(scroller, BorderLayout.NORTH);
    propertyBase.add(editPanel, BorderLayout.CENTER);
    add(propertyBase, BorderLayout.CENTER);

    tableModel.getSearchModel().refreshSearchComboBoxModels();

    if (tableModel.isQueryFilteredByMaster())
      add(initializeShowAllPanel(tableModel), BorderLayout.SOUTH);
  }

  private JPanel initializeShowAllPanel(final EntityTableModel tableModel) {
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    panel.setBorder(BorderFactory.createTitledBorder(FrameworkMessages.get(FrameworkMessages.FILTER_SETTINGS)));
    panel.add(ControlProvider.createCheckBox(ControlFactory.toggleControl(tableModel,
            "showAllWhenNotFiltered", FrameworkMessages.get(FrameworkMessages.SHOW_ALL_WHEN_NO_FILTER), null)));

    return panel;
  }

  private JList initializePropertyList(final EntityTableSearchModel entityModel, final JPanel editorPanel) {
    final List<PropertySearchModel> searchCriteria = getSortedCriteria(entityModel);
    final JList propertyList = new JList(new Vector<AbstractSearchModel>(searchCriteria));
    for (final AbstractSearchModel model : searchCriteria) {
      model.eventSearchStateChanged().addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          propertyList.repaint();
        }
      });
    }
    propertyList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        final Component cellRenderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        final PropertySearchModel selected = (PropertySearchModel) value;
        ((JLabel)cellRenderer).setText(selected.getSearchProperty().toString());
        cellRenderer.setForeground(selected.isSearchEnabled() ? Color.red : Color.black);

        return cellRenderer;
      }
    });
    propertyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    propertyList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        editorPanel.removeAll();
        final PropertySearchModel selected = (PropertySearchModel) propertyList.getSelectedValue();
        if (selected != null) {
          PropertySearchPanel panel = panels.get(selected);
          if (panel == null)
            panels.put(selected, panel = new PropertySearchPanel(selected, true, true));

          editorPanel.add(panel, BorderLayout.NORTH);
          revalidate();
          repaint();
        }
      }
    });

    return propertyList;
  }

  private List<PropertySearchModel> getSortedCriteria(final EntityTableSearchModel entityModel) {
    final List<PropertySearchModel> searchCriteria = entityModel.getPropertySearchModels();
    Collections.sort(searchCriteria, new Comparator<AbstractSearchModel<Property>>() {
      public int compare(final AbstractSearchModel<Property> searchModelOne, final AbstractSearchModel<Property> searchModelTwo) {
        final Property propertyOne = searchModelOne.getSearchProperty();
        final Property propertyTwo = searchModelTwo.getSearchProperty();
        if (propertyOne.getCaption() != null && propertyTwo.getCaption() != null)
          return propertyOne.getCaption().compareTo(propertyTwo.getCaption());
        else
          return propertyOne.getPropertyID().compareTo(propertyTwo.getPropertyID());
      }
    });

    return searchCriteria;
  }
}
