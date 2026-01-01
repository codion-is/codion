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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.value.Value;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.key.KeyEvents;

import org.jspecify.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A base class for Dialog builders.
 * @param <B> the builder type
 */
public abstract class AbstractDialogBuilder<B extends DialogBuilder<B>> implements DialogBuilder<B> {

	protected final List<KeyEvents.Builder> keyEventBuilders = new ArrayList<>(1);
	protected final List<Consumer<JDialog>> onBuildConsumers = new ArrayList<>(1);

	protected @Nullable Window owner;
	protected @Nullable Component locationRelativeTo;
	protected @Nullable Point location;
	protected @Nullable Observable<String> title;
	protected @Nullable ImageIcon icon;

	@Override
	public final B owner(@Nullable Window owner) {
		this.owner = owner;
		if (locationRelativeTo == null) {
			locationRelativeTo = owner;
		}
		if (icon == null && owner instanceof JFrame) {
			icon = new ImageIcon(((JFrame) owner).getIconImage());
		}

		return self();
	}

	@Override
	public final B owner(@Nullable Component owner) {
		this.owner = owner == null ? null : Ancestor.window().of(owner).get();
		if (locationRelativeTo == null) {
			locationRelativeTo = owner;
		}
		return self();
	}

	@Override
	public final B locationRelativeTo(@Nullable Component locationRelativeTo) {
		this.locationRelativeTo = locationRelativeTo;
		return self();
	}

	@Override
	public final B location(@Nullable Point location) {
		this.location = location;
		return self();
	}

	@Override
	public final B title(@Nullable String title) {
		return title(Value.nullable(title));
	}

	@Override
	public final B title(@Nullable Observable<String> title) {
		this.title = title;
		return self();
	}

	@Override
	public final B icon(@Nullable ImageIcon icon) {
		this.icon = icon;
		return self();
	}

	@Override
	public final B keyEvent(KeyEvents.Builder keyEventBuilder) {
		this.keyEventBuilders.add(requireNonNull(keyEventBuilder));
		return self();
	}

	@Override
	public final B onBuild(Consumer<JDialog> onBuild) {
		this.onBuildConsumers.add(requireNonNull(onBuild));
		return self();
	}

	protected final B self() {
		return (B) this;
	}
}
