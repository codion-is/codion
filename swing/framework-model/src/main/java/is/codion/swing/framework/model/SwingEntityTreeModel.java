/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.table.TableSelectionModel;
import is.codion.common.state.State;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A basic tree model for viewing self-referential entities, using a {@link SwingEntityTableModel} as a data source.
 * For instances use the {@link #swingEntityTreeModel(SwingEntityTableModel, ForeignKey)} or
 * {@link #swingEntityTreeModel(SwingEntityTableModel, ForeignKey, Function)} factory methods.
 * @see #swingEntityTreeModel(SwingEntityTableModel, ForeignKey)
 * @see #swingEntityTreeModel(SwingEntityTableModel, ForeignKey, Function)
 */
public final class SwingEntityTreeModel extends DefaultTreeModel {

  private final TreeSelectionModel treeSelectionModel;
  private final ForeignKey parentForeignKey;

  private SwingEntityTreeModel(SwingEntityTableModel tableModel, ForeignKey parentForeignKey,
                               Function<Entity, String> stringFunction) {
    super(new EntityTreeNode(requireNonNull(tableModel, "tableModel"), null,
            requireNonNull(stringFunction, "stringFunction"), requireNonNull(parentForeignKey, "parentForeignKey"),
            new EntityTreeNodeComparator(tableModel.entityDefinition().comparator())));
    if (!tableModel.entityType().equals(parentForeignKey.entityType())) {
      throw new IllegalArgumentException("parentForeignKey entity type (" + parentForeignKey.entityType() +
              ") must be the same as the table model entity type (" + tableModel.entityType() + ")");
    }
    this.parentForeignKey = parentForeignKey;
    this.treeSelectionModel = new DefaultTreeSelectionModel();
    this.treeSelectionModel.addTreeSelectionListener(new EntityTreeSelectionListener(treeSelectionModel,
            tableModel.selectionModel(), getRoot()));
    bindEvents(tableModel);
  }

  /**
   * @return the root node
   */
  @Override
  public EntityTreeNode getRoot() {
    return (EntityTreeNode) super.getRoot();
  }

  /**
   * @return the selection model
   */
  public TreeSelectionModel treeSelectionModel() {
    return treeSelectionModel;
  }

  /**
   * Refreshes the root node, that is, reloads its children.
   */
  public void refreshRoot() {
    refreshNode(getRoot());
  }

  /**
   * Selects the given entities in this tree model.
   * @param entities the entities to select, an empty collection to clear the selection
   */
  public void select(Collection<Entity> entities) {
    requireNonNull(entities);
    treeSelectionModel.clearSelection();
    entities.forEach(entity -> treeSelectionModel.addSelectionPath(find(getRoot(), entity)));
  }

  /**
   * Refreshes this tree and selects the given entities
   * @param entities the modified entities
   */
  public void refreshSelect(Collection<Entity> entities) {
    requireNonNull(entities);
    refreshRoot();
    select(entities);
  }

  /**
   * Removes the given entities from this tree model
   * @param entities the entities to remove
   */
  public void remove(Collection<Entity> entities) {
    for (Entity entity : requireNonNull(entities).stream()
            .filter(entity -> entity.isNotNull(parentForeignKey))
            .collect(toList())) {
      TreePath treePath = find(getRoot(), entity);
      if (treePath != null) {
        removeNodeFromParent((EntityTreeNode) treePath.getLastPathComponent());
      }
    }
  }

  /**
   * Instantiates a new {@link SwingEntityTreeModel} instance.
   * @param tableModel the table model data source
   * @param parentForeignKey the foreign key referencing parent nodes
   * @return a new {@link SwingEntityTreeModel} instance.
   */
  public static SwingEntityTreeModel swingEntityTreeModel(SwingEntityTableModel tableModel,
                                                          ForeignKey parentForeignKey) {
    return swingEntityTreeModel(tableModel, parentForeignKey, Entity::toString);
  }

  /**
   * Instantiates a new {@link SwingEntityTreeModel} instance.
   * @param tableModel the table model data source
   * @param parentForeignKey the foreign key referencing parent nodes
   * @param stringFunction the function for creating String representations of the entities
   * @return a new {@link SwingEntityTreeModel} instance.
   */
  public static SwingEntityTreeModel swingEntityTreeModel(SwingEntityTableModel tableModel,
                                                          ForeignKey parentForeignKey,
                                                          Function<Entity, String> stringFunction) {
    return new SwingEntityTreeModel(tableModel, parentForeignKey, stringFunction);
  }

  private void refreshNode(EntityTreeNode treeNode) {
    treeNode.refresh();
    nodeStructureChanged(treeNode);
  }

  private void bindEvents(SwingEntityTableModel tableModel) {
    tableModel.refresher().addRefreshListener(this::refreshRoot);
    tableModel.editModel().addAfterUpdateListener(updatedEntities -> refreshSelect(updatedEntities.values()));
    tableModel.editModel().addAfterInsertListener(this::refreshSelect);
    tableModel.editModel().addAfterDeleteListener(this::remove);
  }

  private static TreePath find(EntityTreeNode root, Entity entity) {
    Enumeration<TreeNode> enumeration = root.depthFirstEnumeration();
    while (enumeration.hasMoreElements()) {
      EntityTreeNode node = (EntityTreeNode) enumeration.nextElement();
      if (Objects.equals(node.nodeEntity, entity)) {
        return new TreePath(node.getPath());
      }
    }

    return null;
  }

  /**
   * A tree node based on an {@link Entity} instance.
   */
  public static final class EntityTreeNode extends DefaultMutableTreeNode {

    private final SwingEntityTableModel tableModel;
    private final Entity nodeEntity;
    private final Comparator<EntityTreeNode> nodeComparator;
    private final Function<Entity, String> stringFunction;
    private final ForeignKey parentForeignKey;

    private EntityTreeNode(SwingEntityTableModel tableModel, Entity nodeEntity,
                           Function<Entity, String> stringFunction, ForeignKey parentForeignKey,
                           Comparator<EntityTreeNode> nodeComparator) {
      super(nodeEntity);
      if (nodeEntity != null && !nodeEntity.entityType().equals(tableModel.entityType())) {
        throw new IllegalArgumentException("Entity of type " +
                tableModel.entityType() + " expected, got: " + nodeEntity.entityType());
      }
      this.tableModel = tableModel;
      this.nodeEntity = nodeEntity;
      this.stringFunction = stringFunction;
      this.parentForeignKey = parentForeignKey;
      this.nodeComparator = nodeComparator;
    }

    /**
     * Refreshes this node, that is, reloads all child nodes.
     * @return this tree node
     */
    public EntityTreeNode refresh() {
      removeAllChildren();
      loadChildren().forEach(this::add);

      return this;
    }

    /**
     * Returns the entity, note that this entity is null for the root node.
     * @return the entity, null in case of the root node
     */
    public Entity entity() {
      return nodeEntity;
    }

    @Override
    public String toString() {
      return nodeEntity == null ? "" : stringFunction.apply(nodeEntity);
    }

    private List<EntityTreeNode> loadChildren() {
      return tableModel.items().stream()
              .filter(entity -> Objects.equals(this.nodeEntity, entity.referencedEntity(parentForeignKey)))
              .map(entity -> new EntityTreeNode(tableModel, entity, stringFunction, parentForeignKey, nodeComparator))
              .map(EntityTreeNode::refresh)
              .sorted(nodeComparator)
              .collect(toList());
    }
  }

  private static final class EntityTreeNodeComparator implements Comparator<EntityTreeNode> {

    private final Comparator<Entity> entityComparator;

    private EntityTreeNodeComparator(Comparator<Entity> entityComparator) {
      this.entityComparator = entityComparator;
    }

    @Override
    public int compare(EntityTreeNode node1, EntityTreeNode node2) {
      if (node1.getChildCount() > 0 && node2.getChildCount() == 0) {
        return -1;
      }
      if (node2.getChildCount() > 0 && node1.getChildCount() == 0) {
        return 1;
      }

      return entityComparator.compare(node1.nodeEntity, node2.nodeEntity);
    }
  }

  private static final class EntityTreeSelectionListener implements TreeSelectionListener {

    private final TableSelectionModel<Entity> tableSelectionModel;
    private final TreeSelectionModel treeSelectionModel;
    private final State treeSelectionChangingState = State.state();
    private final State tableSelectionChangingState = State.state();

    private EntityTreeSelectionListener(TreeSelectionModel treeSelectionModel, TableSelectionModel<Entity> tableSelectionModel,
                                        EntityTreeNode rootNode) {
      this.treeSelectionModel = treeSelectionModel;
      this.tableSelectionModel = tableSelectionModel;
      this.tableSelectionModel.addSelectedItemsListener(selected -> {
        try {
          tableSelectionChangingState.set(true);
          if (!treeSelectionChangingState.get()) {
            treeSelectionModel.clearSelection();
            selected.forEach(entity -> treeSelectionModel.addSelectionPath(find(rootNode, entity)));
          }
        }
        finally {
          tableSelectionChangingState.set(false);
        }
      });
    }

    @Override
    public void valueChanged(TreeSelectionEvent event) {
      try {
        treeSelectionChangingState.set(true);
        if (!tableSelectionChangingState.get()) {
          List<Entity> selectedEntities = new ArrayList<>(treeSelectionModel.getSelectionCount());
          for (TreePath selectedPath : treeSelectionModel.getSelectionPaths()) {
            selectedEntities.add(((EntityTreeNode) selectedPath.getLastPathComponent()).entity());
          }
          tableSelectionModel.setSelectedItems(selectedEntities);
        }
      }
      finally {
        treeSelectionChangingState.set(false);
      }
    }
  }
}
