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
package is.codion.swing.framework.ui;

import is.codion.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityMerge;
import is.codion.framework.model.EntityMerge.AttributeMerge;
import is.codion.framework.model.EntityMerge.Side;
import is.codion.swing.common.model.component.table.SwingFilterTableModel;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.key.KeyEvents;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.Components.scrollPane;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS;

/**
 * <p>Displays an {@link EntityMerge}, a row for each attribute whose values differ between the two versions, with the
 * value of each, and with a base, which version changed it and its original value. The value taken is shown in bold.
 * <p>Left takes yours for the selected attributes and Right theirs, as does clicking a value. The panel has no
 * actions of its own, whoever shows it merging once {@link EntityMerge#complete()}.
 * @see #builder(EntityMerge)
 */
public final class EntityMergePanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityMergePanel.class, getBundle(EntityMergePanel.class.getName()));

	/**
	 * The merge panel controls.
	 */
	public static final class ControlKeys {

		/**
		 * Takes yours for the selected attributes.<br>
		 * Default key stroke: LEFT ARROW
		 */
		public static final ControlKey<CommandControl> MINE = CommandControl.key("mine", keyStroke(VK_LEFT));
		/**
		 * Takes theirs for the selected attributes.<br>
		 * Default key stroke: RIGHT ARROW
		 */
		public static final ControlKey<CommandControl> THEIRS = CommandControl.key("theirs", keyStroke(VK_RIGHT));

		private ControlKeys() {}
	}

	private final EntityMerge merge;
	private final FilterTable<AttributeMerge<?>, MergeColumn> table;

	private EntityMergePanel(DefaultBuilder builder) {
		super(new BorderLayout());
		this.merge = builder.merge;
		SwingFilterTableModel<AttributeMerge<?>, MergeColumn> model = SwingFilterTableModel.builder()
						.columns(new MergeColumns(builder))
						.items(merge::attributes)
						.build();
		// the attributes at hand, so in place before the first one is selected below
		model.items().refresher().async().set(false);
		model.items().refresh();
		this.table = FilterTable.builder()
						.model(model)
						.cellRenderer(MergeColumn.MINE, String.class, renderer -> renderer
										.toolTip(Objects::toString)
										.customizer(new Taken(Side.MINE)))
						.cellRenderer(MergeColumn.THEIRS, String.class, renderer -> renderer
										.toolTip(Objects::toString)
										.customizer(new Taken(Side.THEIRS)))
						.cellRenderer(MergeColumn.BASE, String.class, renderer -> renderer
										.toolTip(Objects::toString))
						.visibleRows(Math.max(1, Math.min(merge.attributes().size(), 12)))
						.autoResizeMode(AUTO_RESIZE_ALL_COLUMNS)
						.build();
		merge.attributes().forEach(attribute -> attribute.side().addListener(table::repaint));
		table.addMouseListener(new TakeOnClick());
		bindKey(ControlKeys.MINE, Side.MINE);
		bindKey(ControlKeys.THEIRS, Side.THEIRS);
		if (!merge.attributes().isEmpty()) {
			table.model().selection().index().set(0);
		}
		add(scrollPane()
						.view(table)
						.build(), BorderLayout.CENTER);
	}

	/**
	 * @return the merge
	 */
	public EntityMerge merge() {
		return merge;
	}

	@Override
	public boolean requestFocusInWindow() {
		return table.requestFocusInWindow();
	}

	FilterTable<AttributeMerge<?>, MergeColumn> table() {
		return table;
	}

	/**
	 * @param merge the merge to display
	 * @return a new {@link Builder}
	 */
	public static Builder builder(EntityMerge merge) {
		return new DefaultBuilder(requireNonNull(merge));
	}

	/**
	 * Builds a {@link EntityMergePanel}
	 */
	public interface Builder {

		/**
		 * @param baseCaption the caption of the base, "Original" by default
		 * @return this builder instance
		 */
		Builder baseCaption(String baseCaption);

		/**
		 * @param mineCaption the caption of mine, "Yours" by default
		 * @return this builder instance
		 */
		Builder mineCaption(String mineCaption);

		/**
		 * @param theirsCaption the caption of theirs, "Theirs" by default
		 * @return this builder instance
		 */
		Builder theirsCaption(String theirsCaption);

		/**
		 * @return a new {@link EntityMergePanel}
		 */
		EntityMergePanel build();
	}

	private void bindKey(ControlKey<CommandControl> controlKey, Side side) {
		controlKey.defaultKeystroke().optional().ifPresent(keyStroke ->
						KeyEvents.builder()
										.keyStroke(keyStroke)
										.condition(JComponent.WHEN_FOCUSED)
										.action(command(() -> table.model().selection().items().get()
														.forEach(attribute -> attribute.side().set(side))))
										.enable(table));
	}

	enum MergeColumn {
		ATTRIBUTE, CHANGE, BASE, MINE, THEIRS
	}

	private static final class Taken implements FilterTableCellRenderer.Customizer<AttributeMerge<?>, MergeColumn> {

		private final Side side;

		private Taken(Side side) {
			this.side = side;
		}

		@Override
		public void customize(FilterTable<AttributeMerge<?>, MergeColumn> table, @Nullable AttributeMerge<?> row,
													MergeColumn identifier, JComponent component) {
			Font font = table.getFont();
			component.setFont(row != null && row.side().is(side) ? font.deriveFont(Font.BOLD) : font);
		}
	}

	private final class TakeOnClick extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent event) {
			int row = table.rowAtPoint(event.getPoint());
			int column = table.columnAtPoint(event.getPoint());
			if (row >= 0 && column >= 0) {
				MergeColumn identifier = table.columns().getColumn(column).identifier();
				AttributeMerge<?> attribute = table.model().items().included().get().get(row);
				if (identifier == MergeColumn.MINE) {
					attribute.side().set(Side.MINE);
				}
				else if (identifier == MergeColumn.THEIRS) {
					attribute.side().set(Side.THEIRS);
				}
			}
		}
	}

	private static final class MergeColumns implements TableColumns<AttributeMerge<?>, MergeColumn> {

		private final EntityMerge merge;
		private final List<MergeColumn> identifiers;
		private final String baseCaption;
		private final String mineCaption;
		private final String theirsCaption;

		private MergeColumns(DefaultBuilder builder) {
			this.merge = builder.merge;
			this.baseCaption = builder.baseCaption;
			this.mineCaption = builder.mineCaption;
			this.theirsCaption = builder.theirsCaption;
			List<MergeColumn> columns = new ArrayList<>();
			columns.add(MergeColumn.ATTRIBUTE);
			// without a base, neither who changed a value nor its original known
			if (merge.base().isPresent()) {
				columns.add(MergeColumn.CHANGE);
				columns.add(MergeColumn.BASE);
			}
			columns.add(MergeColumn.MINE);
			columns.add(MergeColumn.THEIRS);
			this.identifiers = unmodifiableList(columns);
		}

		@Override
		public List<MergeColumn> identifiers() {
			return identifiers;
		}

		@Override
		public Class<?> type(MergeColumn identifier) {
			return String.class;
		}

		@Override
		public String caption(MergeColumn identifier) {
			switch (identifier) {
				case ATTRIBUTE:
					return MESSAGES.getString("attribute");
				case CHANGE:
					return MESSAGES.getString("changed");
				case BASE:
					return baseCaption;
				case MINE:
					return mineCaption;
				case THEIRS:
					return theirsCaption;
				default:
					throw new IllegalArgumentException();
			}
		}

		@Override
		public String value(AttributeMerge<?> row, MergeColumn identifier) {
			switch (identifier) {
				case ATTRIBUTE:
					return merge.mine().definition().attributes().definition(row.attribute()).caption();
				case CHANGE:
					return change(row);
				case BASE:
					return row.change() == EntityMerge.Change.UNKNOWN ? "" : formatted(merge.base().orElseThrow(), row);
				case MINE:
					return formatted(merge.mine(), row);
				case THEIRS:
					return formatted(merge.theirs(), row);
				default:
					throw new IllegalArgumentException();
			}
		}

		private String change(AttributeMerge<?> row) {
			switch (row.change()) {
				case MINE:
					return mineCaption;
				case THEIRS:
					return theirsCaption;
				case BOTH:
					return MESSAGES.getString("both");
				default:
					return "";
			}
		}

		private static String formatted(Entity entity, AttributeMerge<?> row) {
			return entity.formatted(row.attribute());
		}
	}

	private static final class DefaultBuilder implements Builder {

		private final EntityMerge merge;

		private String baseCaption = MESSAGES.getString("original");
		private String mineCaption = MESSAGES.getString("yours");
		private String theirsCaption = MESSAGES.getString("theirs");

		private DefaultBuilder(EntityMerge merge) {
			this.merge = merge;
		}

		@Override
		public Builder baseCaption(String baseCaption) {
			this.baseCaption = requireNonNull(baseCaption);
			return this;
		}

		@Override
		public Builder mineCaption(String mineCaption) {
			this.mineCaption = requireNonNull(mineCaption);
			return this;
		}

		@Override
		public Builder theirsCaption(String theirsCaption) {
			this.theirsCaption = requireNonNull(theirsCaption);
			return this;
		}

		@Override
		public EntityMergePanel build() {
			return new EntityMergePanel(this);
		}
	}
}
