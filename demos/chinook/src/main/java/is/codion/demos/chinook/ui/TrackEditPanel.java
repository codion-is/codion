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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.model.TrackEditModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.component.list.FilterListSelection;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;

import static is.codion.demos.chinook.domain.api.Chinook.*;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_UP;

public final class TrackEditPanel extends EntityEditPanel {

	private final FilterListSelection<Entity> trackSelection;
	private final UpdateCommand updateAndDecrementSelectedIndexes;
	private final UpdateCommand updateAndIncrementSelectedIndexes;

	public TrackEditPanel(TrackEditModel editModel, FilterListSelection<Entity> trackSelection) {
		super(editModel);
		this.trackSelection = trackSelection;
		this.updateAndDecrementSelectedIndexes = updateCommand()
						.onUpdate(trackSelection.indexes()::decrement)
						.build();
		this.updateAndIncrementSelectedIndexes = updateCommand()
						.onUpdate(trackSelection.indexes()::increment)
						.build();
		addKeyEvents();
	}

	@Override
	protected void initializeUI() {
		createSearchField(Track.ALBUM_FK);
		createTextField(Track.NAME)
						.columns(12);
		createComboBoxPanel(Track.MEDIATYPE_FK, this::createMediaTypeEditPanel)
						.preferredWidth(160)
						.includeAddButton(true)
						.includeEditButton(true);
		createComboBoxPanel(Track.GENRE_FK, this::createGenreEditPanel)
						.preferredWidth(160)
						.includeAddButton(true)
						.includeEditButton(true);
		createTextFieldPanel(Track.COMPOSER)
						.columns(12);

		component(Track.MILLISECONDS).set(new DurationPanelBuilder());

		createIntegerField(Track.BYTES)
						.columns(6);
		createIntegerSpinner(Track.RATING)
						.columns(2);
		createTextField(Track.UNITPRICE)
						.columns(4);
		createIntegerField(Track.PLAY_COUNT)
						.columns(4);

		JPanel genreMediaTypePanel = gridLayoutPanel(1, 2)
						.add(createInputPanel(Track.GENRE_FK))
						.add(createInputPanel(Track.MEDIATYPE_FK))
						.build();

		JPanel durationInputPanel = gridLayoutPanel(1, 2)
						.add(createInputPanel(Track.BYTES))
						.add(component(Track.MILLISECONDS).get())
						.build();

		JPanel unitPricePanel = flexibleGridLayoutPanel(1, 3)
						.add(createInputPanel(Track.RATING))
						.add(createInputPanel(Track.UNITPRICE))
						.add(createInputPanel(Track.PLAY_COUNT))
						.build();

		setLayout(flexibleGridLayout(4, 2));
		addInputPanel(Track.ALBUM_FK);
		addInputPanel(Track.NAME);
		add(genreMediaTypePanel);
		addInputPanel(Track.COMPOSER);
		add(durationInputPanel);
		add(unitPricePanel);
	}

	private EntityEditPanel createMediaTypeEditPanel() {
		return new MediaTypeEditPanel(new SwingEntityEditModel(MediaType.TYPE, editModel().connectionProvider()));
	}

	private GenreEditPanel createGenreEditPanel() {
		return new GenreEditPanel(new SwingEntityEditModel(Genre.TYPE, editModel().connectionProvider()));
	}

	private void addKeyEvents() {
		// We add key events for CTRL-DOWN and CTRL-UP
		// for incrementing and decrementing the selected
		// index, respectively, after updating the selected
		// item in case it is modified.
		KeyEvents.builder()
						// Set the condition
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						// and modifiers
						.modifiers(MENU_SHORTCUT_MASK)
						// set a keycode
						.keyCode(VK_UP)
						// and an action
						.action(command(this::decrementSelection))
						// and enable
						.enable(this)
						// set a new keycode
						.keyCode(VK_DOWN)
						// and a new action
						.action(command(this::incrementSelection))
						// and enable
						.enable(this);
	}

	private void decrementSelection() {
		if (editModel().editor().modified().is()) {
			updateAndDecrementSelectedIndexes.execute();
		}
		else {
			trackSelection.indexes().decrement();
		}
	}

	private void incrementSelection() {
		if (editModel().editor().modified().is()) {
			updateAndIncrementSelectedIndexes.execute();
		}
		else {
			trackSelection.indexes().increment();
		}
	}
}