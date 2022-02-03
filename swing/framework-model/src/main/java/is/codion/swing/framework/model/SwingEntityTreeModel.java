/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.table.SelectionModel;
import is.codion.common.state.State;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;

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
 * A basic tree model for viewing self-referential entities,
 * using a {@link SwingEntityTableModel} as a data source.
 */
public final class SwingEntityTreeModel extends DefaultTreeModel {

  private final TreeSelectionModel treeSelectionModel;
  private final ForeignKey parentForeignKey;

  public SwingEntityTreeModel(final SwingEntityTableModel tableModel, final ForeignKey parentForeignKey) {
    this(tableModel, parentForeignKey, Entity::toString);
    bindEvents(tableModel);
  }

  public SwingEntityTreeModel(final SwingEntityTableModel tableModel, final ForeignKey parentForeignKey,
                              final Function<Entity, String> stringFunction) {
    super(new EntityTreeNode(requireNonNull(tableModel, "tableModel"), null,
            requireNonNull(stringFunction, "stringFunction"), requireNonNull(parentForeignKey, "parentForeignKey"),
            new EntityTreeNodeComparator(tableModel.getEntityDefinition().getComparator())));
    this.parentForeignKey = parentForeignKey;
    this.treeSelectionModel = new DefaultTreeSelectionModel();
    this.treeSelectionModel.addTreeSelectionListener(new EntityTreeSelectionListener(treeSelectionModel,
            tableModel.getSelectionModel(), getRoot()));
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
  public TreeSelectionModel getTreeSelectionModel() {
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
  public void setSelectedEntities(final Collection<Entity> entities) {
    requireNonNull(entities, "entities");
    treeSelectionModel.clearSelection();
    entities.forEach(entity -> treeSelectionModel.addSelectionPath(find(getRoot(), entity)));
  }

  /**
   * Refreshes this tree and selects the given entities
   * @param entities the modified entities
   */
  public void nodesUpdated(final Collection<Entity> entities) {
    refreshRoot();
    setSelectedEntities(entities.stream()
            .filter(entity -> entity.isNotNull(parentForeignKey))
            .collect(toList()));
  }

  /**
   * Removes the given entities from this tree model
   * @param entities the entities to remove
   */
  public void nodesDeleted(final Collection<Entity> entities) {
    for (final Entity deleted : entities.stream()
            .filter(entity -> entity.isNotNull(parentForeignKey))
            .collect(toList())) {
      final TreePath treePath = find(getRoot(), deleted);
      if (treePath != null) {
        removeNodeFromParent((EntityTreeNode) treePath.getLastPathComponent());
      }
    }
  }

  private void refreshNode(final EntityTreeNode treeNode) {
    treeNode.refresh();
    nodeStructureChanged(treeNode);
  }

  private void bindEvents(final SwingEntityTableModel tableModel) {
    tableModel.addRefreshListener(this::refreshRoot);
    tableModel.getEditModel().addAfterUpdateListener(updatedEntities -> this.nodesUpdated(updatedEntities.values()));
    tableModel.getEditModel().addAfterInsertListener(this::nodesUpdated);
    tableModel.getEditModel().addAfterDeleteListener(this::nodesDeleted);
  }

  private static TreePath find(final EntityTreeNode root, final Entity entity) {
    final Enumeration<TreeNode> enumeration = root.depthFirstEnumeration();
    while (enumeration.hasMoreElements()) {
      final EntityTreeNode node = (EntityTreeNode) enumeration.nextElement();
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

    private EntityTreeNode(final SwingEntityTableModel tableModel, final Entity nodeEntity,
                           final Function<Entity, String> stringFunction, final ForeignKey parentForeignKey,
                           final Comparator<EntityTreeNode> nodeComparator) {
      super(nodeEntity);
      if (nodeEntity != null && !nodeEntity.getEntityType().equals(tableModel.getEntityType())) {
        throw new IllegalArgumentException("Entity of type " +
                tableModel.getEntityType() + " expected, got: " + nodeEntity.getEntityType());
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
    public Entity getEntity() {
      return nodeEntity;
    }

    @Override
    public String toString() {
      return nodeEntity == null ? "" : stringFunction.apply(nodeEntity);
    }

    private List<EntityTreeNode> loadChildren() {
      return tableModel.getItems().stream()
              .filter(entity -> Objects.equals(this.nodeEntity, entity.getForeignKey(parentForeignKey)))
              .map(entity -> new EntityTreeNode(tableModel, entity, stringFunction, parentForeignKey, nodeComparator))
              .map(EntityTreeNode::refresh)
              .sorted(nodeComparator)
              .collect(toList());
    }
  }

  private static final class EntityTreeNodeComparator implements Comparator<EntityTreeNode> {

    private final Comparator<Entity> entityComparator;

    private EntityTreeNodeComparator(final Comparator<Entity> entityComparator) {
      this.entityComparator = entityComparator;
    }

    @Override
    public int compare(final EntityTreeNode node1, final EntityTreeNode node2) {
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

    private final SelectionModel<Entity> tableSelectionModel;
    private final TreeSelectionModel treeSelectionModel;
    private final State treeSelectionChangingState = State.state();
    private final State tableSelectionChangingState = State.state();

    private EntityTreeSelectionListener(final TreeSelectionModel treeSelectionModel, final SelectionModel<Entity> tableSelectionModel,
                                        final EntityTreeNode rootNode) {
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
    public void valueChanged(final TreeSelectionEvent event) {
      try {
        treeSelectionChangingState.set(true);
        if (!tableSelectionChangingState.get()) {
          final List<Entity> selectedEntities = new ArrayList<>(treeSelectionModel.getSelectionCount());
          for (final TreePath selectedPath : treeSelectionModel.getSelectionPaths()) {
            selectedEntities.add(((EntityTreeNode) selectedPath.getLastPathComponent()).getEntity());
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
