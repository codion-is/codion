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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.state.State;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;

import static is.codion.framework.demos.chinook.domain.Chinook.Album;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.dialog.Dialogs.inputDialog;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static javax.swing.BorderFactory.createEmptyBorder;

public final class AlbumEditPanel extends EntityEditPanel {

	private final DefaultListModel<String> tagsListModel = new DefaultListModel<>();
	private final State tagSelectionEmpty = State.state(true);
	private final Control addTagControl = Control.builder(this::addTag)
					.smallIcon(FrameworkIcons.instance().icon(Foundation.PLUS))
					.build();
	private final Control removeTagControl = Control.builder(this::removeTag)
					.smallIcon(FrameworkIcons.instance().icon(Foundation.MINUS))
					.enabled(tagSelectionEmpty.not())
					.build();

	public AlbumEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}

	@Override
	protected void initializeUI() {
		initialFocusAttribute().set(Album.ARTIST_FK);

		createForeignKeySearchFieldPanel(Album.ARTIST_FK, this::createArtistEditPanel)
						.columns(15)
						.add(true)
						.edit(true);
		createTextField(Album.TITLE)
						.columns(15);
		createList(tagsListModel)
						.items(Album.TAGS)
						.listSelectionListener(new TagListSelectionListener())
						.keyEvent(KeyEvents.builder(KeyEvent.VK_INSERT)
										.action(addTagControl))
						.keyEvent(KeyEvents.builder(KeyEvent.VK_DELETE)
										.action(removeTagControl));
		component(Album.COVER).set(new CoverArtPanel(editModel().value(Album.COVER)));

		JPanel centerPanel = borderLayoutPanel()
						.westComponent(borderLayoutPanel()
										.northComponent(gridLayoutPanel(2, 1)
														.add(createInputPanel(Album.ARTIST_FK))
														.add(createInputPanel(Album.TITLE))
														.build())
										.centerComponent(createTagPanel())
										.build())
						.centerComponent(createInputPanel(Album.COVER))
						.build();

		setLayout(borderLayout());
		add(centerPanel, BorderLayout.CENTER);
	}

	private EntityEditPanel createArtistEditPanel() {
		return new ArtistEditPanel(new SwingEntityEditModel(Artist.TYPE, editModel().connectionProvider()));
	}

	private JPanel createTagPanel() {
		return createInputPanel(Album.TAGS, borderLayoutPanel()
						.centerComponent(scrollPane(component(Album.TAGS).get())
										.preferredSize(new Dimension(120, 60))
										.build())
						.southComponent(borderLayoutPanel()
										.eastComponent(buttonPanel(Controls.builder()
														.control(addTagControl)
														.control(removeTagControl))
														.buttonBuilder(buttonBuilder ->
																		buttonBuilder.transferFocusOnEnter(true))
														.buttonGap(0)
														.border(createEmptyBorder(0, 0, Layouts.GAP.get(), 0))
														.build())
										.build())
						.build());
	}

	private void addTag() {
		tagsListModel.addElement(inputDialog(stringField().buildValue())
						.owner(this)
						.title(FrameworkMessages.add())
						.show());
	}

	private void removeTag() {
		((JList<String>) component(Album.TAGS).get()).getSelectedValuesList().forEach(tagsListModel::removeElement);
	}

	private final class TagListSelectionListener implements ListSelectionListener {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			tagSelectionEmpty.set(((JList<?>) e.getSource()).isSelectionEmpty());
		}
	}
}
