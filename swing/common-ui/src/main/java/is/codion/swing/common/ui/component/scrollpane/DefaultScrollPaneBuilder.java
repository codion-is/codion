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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.scrollpane;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.LayoutManager;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultScrollPaneBuilder extends AbstractComponentBuilder<JScrollPane, ScrollPaneBuilder> implements ScrollPaneBuilder {

	private @Nullable Supplier<? extends JComponent> view;
	private int vsbPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
	private int hsbPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
	private boolean wheelScrollingEnabled = true;
	private int verticalUnitIncrement;
	private int horizontalUnitIncrement;
	private int verticalBlockIncrement;
	private int horizontalBlockIncrement;
	private @Nullable LayoutManager layout;
	private @Nullable JScrollPane followHorizontal;

	DefaultScrollPaneBuilder() {}

	@Override
	public ScrollPaneBuilder view(JComponent view) {
		return view(() -> requireNonNull(view));
	}

	@Override
	public ScrollPaneBuilder view(Supplier<? extends JComponent> view) {
		this.view = requireNonNull(view);
		return this;
	}

	@Override
	public ScrollPaneBuilder verticalScrollBarPolicy(int verticalScrollBarPolicy) {
		this.vsbPolicy = verticalScrollBarPolicy;
		return this;
	}

	@Override
	public ScrollPaneBuilder horizontalScrollBarPolicy(int horizontalScrollBarPolicy) {
		this.hsbPolicy = horizontalScrollBarPolicy;
		return this;
	}

	@Override
	public ScrollPaneBuilder verticalUnitIncrement(int verticalUnitIncrement) {
		this.verticalUnitIncrement = verticalUnitIncrement;
		return this;
	}

	@Override
	public ScrollPaneBuilder horizontalUnitIncrement(int horizontalUnitIncrement) {
		this.horizontalUnitIncrement = horizontalUnitIncrement;
		return this;
	}

	@Override
	public ScrollPaneBuilder verticalBlockIncrement(int verticalBlockIncrement) {
		this.verticalBlockIncrement = verticalBlockIncrement;
		return this;
	}

	@Override
	public ScrollPaneBuilder horizontalBlockIncrement(int horizontalBlockIncrement) {
		this.horizontalBlockIncrement = horizontalBlockIncrement;
		return this;
	}

	@Override
	public ScrollPaneBuilder wheelScrollingEnable(boolean wheelScrollingEnabled) {
		this.wheelScrollingEnabled = wheelScrollingEnabled;
		return this;
	}

	@Override
	public ScrollPaneBuilder layout(@Nullable LayoutManager layout) {
		this.layout = layout;
		return this;
	}

	@Override
	public ScrollPaneBuilder followHorizontal(@Nullable JScrollPane scrollPane) {
		this.followHorizontal = scrollPane;
		return this;
	}

	@Override
	protected JScrollPane createComponent() {
		JScrollPane scrollPane = new JScrollPane(view != null ? view.get() : null, vsbPolicy, hsbPolicy);
		scrollPane.setWheelScrollingEnabled(wheelScrollingEnabled);
		if (verticalUnitIncrement > 0) {
			scrollPane.getVerticalScrollBar().setUnitIncrement(verticalUnitIncrement);
		}
		if (horizontalUnitIncrement > 0) {
			scrollPane.getHorizontalScrollBar().setUnitIncrement(horizontalUnitIncrement);
		}
		if (verticalBlockIncrement > 0) {
			scrollPane.getVerticalScrollBar().setBlockIncrement(verticalBlockIncrement);
		}
		if (horizontalBlockIncrement > 0) {
			scrollPane.getHorizontalScrollBar().setBlockIncrement(horizontalBlockIncrement);
		}
		if (layout != null) {
			scrollPane.setLayout(layout);
		}
		if (followHorizontal != null) {
			new HorizontalFollower(followHorizontal, scrollPane);
		}

		return scrollPane;
	}

	// Copies the range properties of the leader's horizontal scroll bar model to the follower's, one way.
	// Sharing the model is not an option, each scroll pane keeps writing the extent and maximum
	// of its own viewport to the model of its scroll bar, the two fighting over a shared one.
	private static final class HorizontalFollower {

		private final BoundedRangeModel leaderModel;
		private final BoundedRangeModel followerModel;

		private HorizontalFollower(JScrollPane leader, JScrollPane follower) {
			leaderModel = leader.getHorizontalScrollBar().getModel();
			followerModel = follower.getHorizontalScrollBar().getModel();
			leaderModel.addChangeListener(e -> follow());
			// catches up when the follower is added or shown, the leader possibly scrolled in the meantime
			follower.addHierarchyListener(e -> follow());
		}

		private void follow() {
			followerModel.setRangeProperties(leaderModel.getValue(), leaderModel.getExtent(),
							leaderModel.getMinimum(), leaderModel.getMaximum(), leaderModel.getValueIsAdjusting());
		}
	}
}
