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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.observer.Observable;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A base class for Dialog builders.
 * @param <B> the builder type
 */
public class AbstractDialogBuilder<B extends DialogBuilder<B>> implements DialogBuilder<B> {

	protected final List<KeyEvents.Builder> keyEventBuilders = new ArrayList<>(1);

	protected Window owner;
	protected Component locationRelativeTo;
	protected Point location;
	protected Observable<String> title;
	protected ImageIcon icon;

	@Override
	public final B owner(Window owner) {
		this.owner = owner;
		if (locationRelativeTo == null) {
			locationRelativeTo = owner;
		}
		return self();
	}

	@Override
	public final B owner(Component owner) {
		this.owner = owner == null ? null : Utilities.parentWindow(owner);
		if (locationRelativeTo == null) {
			locationRelativeTo = owner;
		}
		return self();
	}

	@Override
	public final B locationRelativeTo(Component locationRelativeTo) {
		this.locationRelativeTo = locationRelativeTo;
		return self();
	}

	@Override
	public final B location(Point location) {
		this.location = location;
		return self();
	}

	@Override
	public final B title(String title) {
		return title(Value.value(title));
	}

	@Override
	public final B title(Observable<String> title) {
		this.title = title;
		return self();
	}

	@Override
	public final B icon(ImageIcon icon) {
		this.icon = icon;
		return self();
	}

	@Override
	public final B keyEvent(KeyEvents.Builder keyEventBuilder) {
		this.keyEventBuilders.add(requireNonNull(keyEventBuilder));
		return self();
	}

	protected final B self() {
		return (B) this;
	}
}
