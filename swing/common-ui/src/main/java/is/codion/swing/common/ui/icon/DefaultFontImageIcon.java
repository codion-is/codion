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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.icon;

import is.codion.swing.common.ui.icon.FontImageIcon.Builder.ColorStep;
import is.codion.swing.common.ui.icon.FontImageIcon.Builder.SizeStep;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import java.awt.Color;

import static java.util.Objects.requireNonNull;

/**
 * A default FontImageIcon implementation.
 */
final class DefaultFontImageIcon implements FontImageIcon {

	private final FontIcon fontIcon;
	private final ImageIcon imageIcon;
	private final IconPainter iconPainter;

	private DefaultFontImageIcon(DefaultBuilder builder) {
		this.fontIcon = FontIcon.of(builder.ikon, builder.size, builder.color);
		this.imageIcon = builder.imageIconFactory.createImageIcon(fontIcon);
		this.iconPainter = builder.iconPainter;
		this.iconPainter.paintIcon(fontIcon, imageIcon);
	}

	@Override
	public ImageIcon imageIcon() {
		return imageIcon;
	}

	@Override
	public void color(Color color) {
		fontIcon.setIconColor(color);
		iconPainter.paintIcon(fontIcon, imageIcon);
	}

	private static final class DefaultIkonStep implements Builder.IkonStep {

		@Override
		public SizeStep ikon(Ikon ikon) {
			return new DefaultSizeStep(requireNonNull(ikon));
		}
	}

	private static final class DefaultSizeStep implements SizeStep {

		private final Ikon ikon;

		private DefaultSizeStep(Ikon ikon) {
			this.ikon = ikon;
		}

		@Override
		public ColorStep size(int size) {
			return new DefaultColorStep(ikon, size);
		}
	}

	private static final class DefaultColorStep implements ColorStep {

		private final Ikon ikon;
		private final int size;

		private DefaultColorStep(Ikon ikon, int size) {
			this.ikon = ikon;
			this.size = size;
		}

		@Override
		public Builder color(Color color) {
			return new DefaultBuilder(ikon, size, requireNonNull(color));
		}
	}

	static final class DefaultBuilder implements Builder {

		static final Builder.IkonStep IKON = new DefaultIkonStep();

		private static final IconPainter DEFAULT_ICON_PAINTER = new DefaultIconPainter();
		private static final ImageIconFactory DEFAULT_ICON_FACTORY = new DefaultImageIconFactory();

		private final Ikon ikon;
		private final int size;
		private final Color color;

		private IconPainter iconPainter = DEFAULT_ICON_PAINTER;
		private ImageIconFactory imageIconFactory = DEFAULT_ICON_FACTORY;

		private DefaultBuilder(Ikon ikon, int size, Color color) {
			this.ikon = ikon;
			this.size = size;
			this.color = color;
		}

		@Override
		public Builder iconPainter(IconPainter iconPainter) {
			this.iconPainter = requireNonNull(iconPainter);
			return this;
		}

		@Override
		public Builder imageIconFactory(ImageIconFactory imageIconFactory) {
			this.imageIconFactory = requireNonNull(imageIconFactory);
			return this;
		}

		@Override
		public FontImageIcon build() {
			return new DefaultFontImageIcon(this);
		}
	}

	private static final class DefaultIconPainter implements IconPainter {}

	private static final class DefaultImageIconFactory implements ImageIconFactory {}
}
