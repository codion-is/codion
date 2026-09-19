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
package is.codion.swing.common.ui.component.scrollpane;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ScrollPaneBuilderTest {

	@Test
	void followHorizontal() throws Exception {
		JScrollPane leader = ScrollPaneBuilder.builder()
						.view(view(1000, 100))
						.build();
		JScrollPane follower = follower(leader);
		layout(leader, 300, 120);
		layout(follower, 300, 20);

		SwingUtilities.invokeAndWait(() -> {
			leader.getHorizontalScrollBar().setValue(200);
			assertEquals(200, follower.getViewport().getViewPosition().x);
			leader.getHorizontalScrollBar().setValue(0);
			assertEquals(0, follower.getViewport().getViewPosition().x);

			// one way, the leader does not follow the follower
			follower.getHorizontalScrollBar().setValue(100);
			assertEquals(0, leader.getViewport().getViewPosition().x);
		});
	}

	@Test
	void followerCatchesUpWhenAdded() throws Exception {
		JScrollPane leader = ScrollPaneBuilder.builder()
						.view(view(1000, 100))
						.build();
		layout(leader, 300, 120);
		leader.getHorizontalScrollBar().setValue(200);
		// a follower created after the leader was scrolled catches up when added to a parent
		JScrollPane follower = follower(leader);
		layout(follower, 300, 20);
		assertEquals(0, follower.getViewport().getViewPosition().x);
		SwingUtilities.invokeAndWait(() -> {
			new JPanel().add(follower);
			assertEquals(200, follower.getViewport().getViewPosition().x);
		});
	}

	private static JScrollPane follower(JScrollPane leader) {
		return ScrollPaneBuilder.builder()
						.view(view(1000, 20))
						.horizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
						.verticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
						.followHorizontal(leader)
						.build();
	}

	// validate() does not lay out a scroll pane which has never been displayed, laying out the scroll pane
	// and its viewport does, the scroll bar models getting their range from the viewport
	private static void layout(JScrollPane scrollPane, int width, int height) {
		scrollPane.setSize(width, height);
		scrollPane.doLayout();
		scrollPane.getViewport().doLayout();
	}

	private static JPanel view(int width, int height) {
		JPanel view = new JPanel();
		view.setPreferredSize(new Dimension(width, height));

		return view;
	}
}
