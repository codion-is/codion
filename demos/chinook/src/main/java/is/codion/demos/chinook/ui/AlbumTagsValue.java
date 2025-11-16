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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.component.list.FilterList;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import static is.codion.common.reactive.state.State.present;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.*;

final class AlbumTagsValue extends AbstractComponentValue<AlbumTagsValue.AlbumTagsPanel, List<String>> {

	AlbumTagsValue() {
		super(new AlbumTagsPanel());
		component().value.addListener(this::notifyObserver);
	}

	@Override
	protected List<String> getComponentValue() {
		return component().value.get();
	}

	@Override
	protected void setComponentValue(List<String> value) {
		component().value.set(value);
	}

	static final class AlbumTagsPanel extends JPanel {

		private static final FrameworkIcons ICONS = FrameworkIcons.instance();

		private final FilterListModel<String> model = FilterListModel.builder()
						.<String>items()
						.build();
		private final ComponentValue<FilterList<String>, List<String>> value = FilterList.builder()
						.model(model)
						.items()
						.nullable(true)
						.transferFocusOnEnter(true)
						.buildValue();
		private final FilterList<String> list = value.component();
		private final Control addTagControl = Control.builder()
						.command(this::addTag)
						.smallIcon(ICONS.get(Foundation.PLUS).small())
						.build();
		private final Control removeTagControl = Control.builder()
						.command(this::removeTags)
						.smallIcon(ICONS.get(Foundation.MINUS).small())
						.enabled(model.selection().empty().not())
						.build();
		private final Control moveSelectionUpControl = Control.builder()
						.command(this::moveSelectedTagsUp)
						.smallIcon(ICONS.up().small())
						.enabled(model.selection().empty().not())
						.build();
		private final Control moveSelectionDownControl = Control.builder()
						.command(this::moveSelectedTagsDown)
						.smallIcon(ICONS.down().small())
						.enabled(model.selection().empty().not())
						.build();

		AlbumTagsPanel() {
			super(borderLayout());
			add(createCenterPanel(), BorderLayout.CENTER);
			setupKeyEvents();
		}

		private JPanel createCenterPanel() {
			return borderLayoutPanel()
							.center(scrollPane()
											.view(list)
											.preferredWidth(120))
							.south(borderLayoutPanel()
											.west(createButtonPanel(moveSelectionDownControl, moveSelectionUpControl))
											.east(createButtonPanel(addTagControl, removeTagControl)))
							.build();
		}

		private JPanel createButtonPanel(Control leftControl, Control rightControl) {
			return buttonPanel()
							.controls(Controls.builder()
											.control(leftControl)
											.control(rightControl))
							.transferFocusOnEnter(true)
							.buttonGap(0)
							.build();
		}

		private void setupKeyEvents() {
			KeyEvents.builder()
							.keyCode(VK_INSERT)
							.action(addTagControl)
							.enable(list);
			KeyEvents.builder()
							.keyCode(VK_DELETE)
							.action(removeTagControl)
							.enable(list);
			KeyEvents.builder()
							.keyCode(VK_UP)
							.modifiers(MENU_SHORTCUT_MASK)
							.action(moveSelectionUpControl)
							.enable(list);
			KeyEvents.builder()
							.keyCode(VK_DOWN)
							.modifiers(MENU_SHORTCUT_MASK)
							.action(moveSelectionDownControl)
							.enable(list);
		}

		private void addTag() {
			ComponentValue<JTextField, String> tagValue = stringField().buildValue();
			model.items().add(Dialogs.input()
							.component(tagValue)
							.owner(this)
							.title(FrameworkMessages.add())
							.valid(present(tagValue))
							.show());
		}

		private void removeTags() {
			model.items().remove(model.selection().items().get());
		}

		private void moveSelectedTagsUp() {
			List<String> tags = new ArrayList<>(model.items().get());
			int[] selected = list.getSelectedIndices();
			if (selected.length > 0 && selected[0] != 0) {
				moveUp(selected, tags);
			}
			model.items().set(tags);
			list.ensureIndexIsVisible(selected[0] - 1);
		}

		private void moveSelectedTagsDown() {
			List<String> tags = new ArrayList<>(model.items().get());
			int[] selected = list.getSelectedIndices();
			if (selected.length > 0 && selected[selected.length - 1] != model.items().included().size() - 1) {
				moveDown(selected, tags);
			}
			model.items().set(tags);
			list.ensureIndexIsVisible(selected[selected.length - 1] + 1);
		}

		private static void moveUp(int[] selected, List<String> tags) {
			for (int i = 0; i < selected.length; i++) {
				tags.add(selected[i] - 1, tags.remove(selected[i]));
			}
		}

		private static void moveDown(int[] selected, List<String> tags) {
			for (int i = selected.length - 1; i >= 0; i--) {
				tags.add(selected[i] + 1, tags.remove(selected[i]));
			}
		}
	}
}
