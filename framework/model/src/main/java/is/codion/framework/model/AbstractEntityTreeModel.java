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
package is.codion.framework.model;

import is.codion.common.model.component.tree.FilterTreeModel;
import is.codion.common.model.component.tree.FilterTreeSort;
import is.codion.common.model.component.tree.NodePath;
import is.codion.common.model.component.tree.TreeSelection;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * <p>A base class for {@link EntityTreeModel} implementations, based on a tree model built by a toolkit, a
 * {@code javax.swing.tree.TreeModel} based one for example, the entity tree logic, the queries, living here.
 * <p>The {@link FilterTreeModel} methods delegate to the tree model, see {@link #treeModel()}.
 * @param <M> the type of the underlying tree model
 * @see AbstractEntityTreeModelBuilder
 */
public abstract class AbstractEntityTreeModel<M extends FilterTreeModel<Entity>> implements EntityTreeModel {

	private final EntityConnection connection;
	private final EntityTreeStructure structure;
	private final M treeModel;

	/**
	 * @param builder the builder
	 * @param treeModel builds the underlying tree model
	 */
	protected AbstractEntityTreeModel(AbstractEntityTreeModelBuilder<?> builder, TreeModelBuilder<M> treeModel) {
		this.connection = requireNonNull(builder).connection;
		this.structure = new EntityTreeStructure(builder);
		List<Consumer<FilterTreeModel.Builder<Entity, ?>>> options = new ArrayList<>(builder.options);
		this.treeModel = requireNonNull(requireNonNull(treeModel).build(structure::roots, structure::children, treeModelBuilder -> {
			treeModelBuilder.comparators(builder.comparators == null ? structure::comparator : builder.comparators)
							.leaves(structure::leaves);
			options.forEach(option -> option.accept(treeModelBuilder));
		}));
	}

	@Override
	public final EntityConnection connection() {
		return connection;
	}

	@Override
	public final Nodes<Entity> nodes() {
		return treeModel.nodes();
	}

	@Override
	public final Expansion<Entity> expansion() {
		return treeModel.expansion();
	}

	@Override
	public final VisibleNodes<Entity> visible() {
		return treeModel.visible();
	}

	@Override
	public TreeSelection<Entity> selection() {
		return treeModel.selection();
	}

	@Override
	public final FilterTreeSort<Entity> sort() {
		return treeModel.sort();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [roots: " + structure.types().get(0) + "]";
	}

	/**
	 * @return the underlying tree model
	 */
	protected final M treeModel() {
		return treeModel;
	}

	/**
	 * Builds the underlying tree model of an {@link EntityTreeModel}.
	 * @param <M> the tree model type
	 */
	protected interface TreeModelBuilder<M extends FilterTreeModel<Entity>> {

		/**
		 * Builds the tree model, given the roots supplier and children function, the tree model builder being
		 * configured via the given options before it builds the model.
		 * @param roots supplies the roots
		 * @param children provides the children of a node
		 * @param options configures the tree model builder with the entity tree defaults and the builder options
		 * @return a new tree model
		 */
		M build(Supplier<Collection<Entity>> roots, Function<NodePath<Entity>, Collection<Entity>> children,
						Consumer<FilterTreeModel.Builder<Entity, ?>> options);
	}
}
