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
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.AbstractEntityTreeModel;
import is.codion.framework.model.AbstractEntityTreeModelBuilder;
import is.codion.framework.model.EntityTreeModel.Builder.Roots;
import is.codion.swing.common.model.component.tree.FilterTreeSelection;
import is.codion.swing.common.model.component.tree.SwingFilterTreeModel;

import org.jspecify.annotations.Nullable;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.util.Collection;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * An {@link AbstractEntityTreeModel} based on a {@link SwingFilterTreeModel}, which the {@code TreeModel} methods
 * delegate to.
 */
final class DefaultSwingEntityTreeModel extends AbstractEntityTreeModel<SwingFilterTreeModel<Entity>>
				implements SwingEntityTreeModel {

	private DefaultSwingEntityTreeModel(AbstractEntityTreeModelBuilder<?> builder) {
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

	static final class DefaultBuilder extends AbstractEntityTreeModelBuilder<SwingEntityTreeModel.Builder>
					implements SwingEntityTreeModel.Builder {

		static final SwingEntityTreeModel.Builder.RootsStep ROOTS = new DefaultRootsStep();

		private DefaultBuilder(EntityType rootType, Consumer<Roots> roots, EntityConnection connection) {
			super(rootType, roots, connection);
		}

		@Override
		public SwingEntityTreeModel build() {
			return new DefaultSwingEntityTreeModel(this);
		}
	}

	private static final class DefaultRootsStep implements SwingEntityTreeModel.Builder.RootsStep {

		@Override
		public SwingEntityTreeModel.Builder.ConnectionStep roots(EntityType entityType) {
			return roots(entityType, configuration -> {});
		}

		@Override
		public SwingEntityTreeModel.Builder.ConnectionStep roots(EntityType entityType, Consumer<Roots> roots) {
			return new DefaultConnectionStep(requireNonNull(entityType), requireNonNull(roots));
		}
	}

	private static final class DefaultConnectionStep implements SwingEntityTreeModel.Builder.ConnectionStep {

		private final EntityType rootType;
		private final Consumer<Roots> roots;

		private DefaultConnectionStep(EntityType rootType, Consumer<Roots> roots) {
			this.rootType = rootType;
			this.roots = roots;
		}

		@Override
		public SwingEntityTreeModel.Builder connection(EntityConnection connection) {
			return new DefaultBuilder(rootType, roots, connection);
		}
	}
}
