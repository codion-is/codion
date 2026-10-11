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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.component;

import is.codion.common.model.component.tree.NodePath;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.AbstractEntityTableTreeModel;
import is.codion.framework.model.AbstractEntityTableTreeModelBuilder;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.tree.FilterTreeSelection;
import is.codion.swing.common.model.component.tree.SwingFilterTreeModel;

import org.jspecify.annotations.Nullable;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * An {@link AbstractEntityTableTreeModel} based on a {@link SwingFilterTreeModel}, which the {@code TreeModel} methods
 * delegate to.
 */
final class DefaultSwingEntityTableTreeModel extends AbstractEntityTableTreeModel<SwingFilterTreeModel<Entity>>
				implements SwingEntityTableTreeModel {

	private DefaultSwingEntityTableTreeModel(DefaultBuilder builder) {
		super(builder, (roots, children, options) -> {
			SwingFilterTreeModel.Builder<Entity> treeModelBuilder = SwingFilterTreeModel.builder()
							.roots(roots)
							.children(children);
			options.accept(treeModelBuilder);

			return treeModelBuilder.build();
		});
	}

	@Override
	public FilterTreeSelection<Entity> selection() {
		return treeModel().selection();
	}

	@Override
	public NodePath<Entity> getRoot() {
		return treeModel().getRoot();
	}

	@Override
	public Object getChild(Object parent, int index) {
		return treeModel().getChild(parent, index);
	}

	@Override
	public int getChildCount(Object parent) {
		return treeModel().getChildCount(parent);
	}

	@Override
	public boolean isLeaf(Object node) {
		return treeModel().isLeaf(node);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		treeModel().valueForPathChanged(path, newValue);
	}

	@Override
	public int getIndexOfChild(@Nullable Object parent, @Nullable Object child) {
		return treeModel().getIndexOfChild(parent, child);
	}

	@Override
	public void addTreeModelListener(TreeModelListener listener) {
		treeModel().addTreeModelListener(listener);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener listener) {
		treeModel().removeTreeModelListener(listener);
	}

	@Override
	public void fireNodesChanged(Collection<NodePath<Entity>> paths) {
		treeModel().fireNodesChanged(paths);
	}

	@Override
	public TreePath treePath(NodePath<Entity> path) {
		return treeModel().treePath(path);
	}

	static final class DefaultBuilder extends AbstractEntityTableTreeModelBuilder<SwingEntityTableTreeModel.Builder>
					implements SwingEntityTableTreeModel.Builder {

		static final SwingEntityTableTreeModel.Builder.TableModelStep TABLE_MODEL = new DefaultTableModelStep();

		private DefaultBuilder(EntityTableModel<?, ?> tableModel, ForeignKey parent) {
			super(tableModel, parent);
		}

		@Override
		public SwingEntityTableTreeModel build() {
			return new DefaultSwingEntityTableTreeModel(this);
		}
	}

	private static final class DefaultTableModelStep implements SwingEntityTableTreeModel.Builder.TableModelStep {

		@Override
		public SwingEntityTableTreeModel.Builder.ParentStep tableModel(EntityTableModel<?, ?> tableModel) {
			return new DefaultParentStep(requireNonNull(tableModel));
		}
	}

	private static final class DefaultParentStep implements SwingEntityTableTreeModel.Builder.ParentStep {

		private final EntityTableModel<?, ?> tableModel;

		private DefaultParentStep(EntityTableModel<?, ?> tableModel) {
			this.tableModel = tableModel;
		}

		@Override
		public SwingEntityTableTreeModel.Builder parent(ForeignKey parent) {
			return new DefaultBuilder(tableModel, parent);
		}
	}
}
