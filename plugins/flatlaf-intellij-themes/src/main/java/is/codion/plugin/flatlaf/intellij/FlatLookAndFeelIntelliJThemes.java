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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.plugin.flatlaf.intellij;

import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.plugin.flatlaf.intellij.themes.akusan.AkusanDark;
import is.codion.plugin.flatlaf.intellij.themes.akusan.AkusanGray;
import is.codion.plugin.flatlaf.intellij.themes.akusan.AkusanLight;
import is.codion.plugin.flatlaf.intellij.themes.arc.Arc;
import is.codion.plugin.flatlaf.intellij.themes.arc.ArcDark;
import is.codion.plugin.flatlaf.intellij.themes.arc.ArcDarkOrange;
import is.codion.plugin.flatlaf.intellij.themes.arc.ArcOrange;
import is.codion.plugin.flatlaf.intellij.themes.carbon.Carbon;
import is.codion.plugin.flatlaf.intellij.themes.catppuccin.CatppuccinFrappe;
import is.codion.plugin.flatlaf.intellij.themes.catppuccin.CatppuccinLatte;
import is.codion.plugin.flatlaf.intellij.themes.catppuccin.CatppuccinMacchiato;
import is.codion.plugin.flatlaf.intellij.themes.catppuccin.CatppuccinMocha;
import is.codion.plugin.flatlaf.intellij.themes.cobalt2.Cobalt2;
import is.codion.plugin.flatlaf.intellij.themes.codely.Codely;
import is.codion.plugin.flatlaf.intellij.themes.codely.CodelyBlue;
import is.codion.plugin.flatlaf.intellij.themes.codely.CodelyDark;
import is.codion.plugin.flatlaf.intellij.themes.codely.CodelyLight;
import is.codion.plugin.flatlaf.intellij.themes.copilot.Copilot;
import is.codion.plugin.flatlaf.intellij.themes.cutepink.CutePinkDark;
import is.codion.plugin.flatlaf.intellij.themes.cutepink.CutePinkLight;
import is.codion.plugin.flatlaf.intellij.themes.cyberpunk.Cyberpunk;
import is.codion.plugin.flatlaf.intellij.themes.darculasolid.DarculaSolid;
import is.codion.plugin.flatlaf.intellij.themes.darkflat.DarkFlat;
import is.codion.plugin.flatlaf.intellij.themes.darkhorizon.DarkHorizon;
import is.codion.plugin.flatlaf.intellij.themes.darkpurple.DarkPurple;
import is.codion.plugin.flatlaf.intellij.themes.darkubuntu.DarkUbuntu;
import is.codion.plugin.flatlaf.intellij.themes.dracula.Dracula;
import is.codion.plugin.flatlaf.intellij.themes.elegant.ElegantDark;
import is.codion.plugin.flatlaf.intellij.themes.elegant.ElegantLight;
import is.codion.plugin.flatlaf.intellij.themes.espresso.EspressoLight;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestDarkHard;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestDarkMedium;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestDarkSoft;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestLightHard;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestLightMedium;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestLightSoft;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryDarcula;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryDark;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryLight;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryOceanic;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryStorm;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryViolet;
import is.codion.plugin.flatlaf.intellij.themes.github.GithubDark;
import is.codion.plugin.flatlaf.intellij.themes.github.GithubDarkDefault;
import is.codion.plugin.flatlaf.intellij.themes.github.GithubLight;
import is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoDarkFuchsia;
import is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoDeepOcean;
import is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoMidnightBlue;
import is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoNatureGreen;
import is.codion.plugin.flatlaf.intellij.themes.gray.Gray;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxDarkHard;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxDarkMedium;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxDarkSoft;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxMaterialLightHard;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxMaterialLightMedium;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxMaterialLightSoft;
import is.codion.plugin.flatlaf.intellij.themes.halcyon.Halcyon;
import is.codion.plugin.flatlaf.intellij.themes.hardhacker.HardHackerDarker;
import is.codion.plugin.flatlaf.intellij.themes.hardhacker.HardHackerNormal;
import is.codion.plugin.flatlaf.intellij.themes.hiberbee.HiberbeeDark;
import is.codion.plugin.flatlaf.intellij.themes.highcontrast.HighContrast;
import is.codion.plugin.flatlaf.intellij.themes.iceberg.Iceberg;
import is.codion.plugin.flatlaf.intellij.themes.lightflat.LightFlat;
import is.codion.plugin.flatlaf.intellij.themes.lotus.LotusDark;
import is.codion.plugin.flatlaf.intellij.themes.lotus.LotusLight;
import is.codion.plugin.flatlaf.intellij.themes.material.ArcDarkMaterial;
import is.codion.plugin.flatlaf.intellij.themes.material.AtomOneDark;
import is.codion.plugin.flatlaf.intellij.themes.material.AtomOneLight;
import is.codion.plugin.flatlaf.intellij.themes.material.DraculaMaterial;
import is.codion.plugin.flatlaf.intellij.themes.material.GitHubDarkMaterial;
import is.codion.plugin.flatlaf.intellij.themes.material.GitHubMaterial;
import is.codion.plugin.flatlaf.intellij.themes.material.LightOwl;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialDarker;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialDeepOcean;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialLighter;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialOceanic;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialPalenight;
import is.codion.plugin.flatlaf.intellij.themes.material.MonokaiProMaterial;
import is.codion.plugin.flatlaf.intellij.themes.material.Moonlight;
import is.codion.plugin.flatlaf.intellij.themes.material.NightOwl;
import is.codion.plugin.flatlaf.intellij.themes.material.SolarizedDark;
import is.codion.plugin.flatlaf.intellij.themes.material.SolarizedLight;
import is.codion.plugin.flatlaf.intellij.themes.materialtheme.MaterialTheme;
import is.codion.plugin.flatlaf.intellij.themes.monocai.Monocai;
import is.codion.plugin.flatlaf.intellij.themes.monokaipro.MonokaiPro;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.AuroraBorealis;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.Autumn;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.Everest;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.Roraima;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.Sakura;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.Sequoia;
import is.codion.plugin.flatlaf.intellij.themes.nightfall.Nightfall;
import is.codion.plugin.flatlaf.intellij.themes.obsidiansunset.ObsidianSunsetBlue;
import is.codion.plugin.flatlaf.intellij.themes.obsidiansunset.ObsidianSunsetOrange;
import is.codion.plugin.flatlaf.intellij.themes.onedark.OneDark;
import is.codion.plugin.flatlaf.intellij.themes.onepauintxi.OnePauintxiAllBlue;
import is.codion.plugin.flatlaf.intellij.themes.onepauintxi.OnePauintxiAllOrange;
import is.codion.plugin.flatlaf.intellij.themes.onepauintxi.OnePauintxiBlue;
import is.codion.plugin.flatlaf.intellij.themes.onepauintxi.OnePauintxiOrange;
import is.codion.plugin.flatlaf.intellij.themes.pinkasheck.PinkAsHeck;
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderDark;
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderDay;
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderLight;
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderNight;
import is.codion.plugin.flatlaf.intellij.themes.solidcherry.SolidCherry;
import is.codion.plugin.flatlaf.intellij.themes.srcery.Srcery;
import is.codion.plugin.flatlaf.intellij.themes.tayviscon.TeyvisconDark;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPanda;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaBlacklight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaDawnlight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaDaylight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaMoonlight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaStarlight;
import is.codion.plugin.flatlaf.intellij.themes.vscode.VSCodeDark;
import is.codion.plugin.flatlaf.intellij.themes.vscode.VSCodeDarkModern;
import is.codion.plugin.flatlaf.intellij.themes.vscode.VSCodeLight;
import is.codion.plugin.flatlaf.intellij.themes.vuesion.Vuesion;
import is.codion.plugin.flatlaf.intellij.themes.xcodedark.XcodeDark;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.scaler.Scaler;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.IntelliJTheme;
import com.formdev.flatlaf.util.LoggingFacade;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.lookAndFeelEnabler;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * <p>Provides Look And Feels based on IntelliJ themes.
 * <p>Uses Flat Look And Feel under the hood.
 */
public final class FlatLookAndFeelIntelliJThemes {

	static {
		// Turn off FlatLaf logging to get around
		// https://github.com/JFormDesigner/FlatLaf/issues/990
		LoggingFacade facade = LoggingFacade.INSTANCE;
		Logger.getLogger(FlatLaf.class.getName()).setLevel(Level.OFF);
	}

	private static final Consumer<LookAndFeelInfo> ENABLER = new DefaultEnabler();

	private static final Collection<LookAndFeelEnabler> LOOK_AND_FEELS = unmodifiableList(asList(
					enabler(AuroraBorealis.class, "Nature Aurora Borealis"),
					enabler(Autumn.class, "Nature Autumn"),
					enabler(Everest.class, "Nature Everest"),
					enabler(Roraima.class, "Nature Roraima"),
					enabler(Sakura.class, "Nature Sakura"),
					enabler(Sequoia.class, "Nature Sequoia"),
					enabler(Srcery.class, "Srcery"),
					enabler(GerryDarcula.class, "Gerry Darcula"),
					enabler(GerryDark.class, "Gerry Dark"),
					enabler(GerryOceanic.class, "Gerry Oceanic"),
					enabler(GerryStorm.class, "Gerry Storm"),
					enabler(GerryViolet.class, "Gerry Violet"),
					enabler(GerryLight.class, "Gerry Light"),
					enabler(LotusDark.class, "Lotus Dark"),
					enabler(LotusLight.class, "Lotus Light"),
					enabler(TeyvisconDark.class, "Teyviscon Dark"),
					enabler(EverforestDarkHard.class, "Everforest Dark Hard"),
					enabler(EverforestDarkMedium.class, "Everforest Dark Medium"),
					enabler(EverforestDarkSoft.class, "Everforest Dark Soft"),
					enabler(EverforestLightHard.class, "Everforest Light Hard"),
					enabler(EverforestLightMedium.class, "Everforest Light Medium"),
					enabler(EverforestLightSoft.class, "Everforest Light Soft"),
					enabler(DarkHorizon.class, "Dark Horizon"),
					enabler(AkusanDark.class, "Akusan Dark"),
					enabler(AkusanGray.class, "Akusan Gray"),
					enabler(AkusanLight.class, "Akusan Light"),
					enabler(OnePauintxiAllBlue.class, "One Pauintxi All Blue"),
					enabler(OnePauintxiBlue.class, "One Pauintxi Blue"),
					enabler(OnePauintxiAllOrange.class, "One Pauintxi All Orange"),
					enabler(OnePauintxiOrange.class, "One Pauintxi Orange"),
					enabler(GruvboxMaterialLightHard.class, "Gruvbox Light Hard"),
					enabler(GruvboxMaterialLightMedium.class, "Gruvbox Light Medium"),
					enabler(GruvboxMaterialLightSoft.class, "Gruvbox Light Soft"),
					enabler(ObsidianSunsetBlue.class, "Obsidian Sunset Blue"),
					enabler(ObsidianSunsetOrange.class, "Obsidian Sunset Orange"),
					enabler(GithubDark.class, "Github Dark"),
					enabler(GithubDarkDefault.class, "Github Dark Default"),
					enabler(GithubLight.class, "Github Light"),
					enabler(RiderDark.class, "Rider Dark"),
					enabler(RiderDay.class, "Rider Day"),
					enabler(RiderLight.class, "Rider Light"),
					enabler(RiderNight.class, "Rider Night"),
					enabler(RiderDark.class, "Rider Dark"),
					enabler(DarculaSolid.class, "Darcula Solid"),
					enabler(Nightfall.class, "Nightfall"),
					enabler(TrashPanda.class, "Trash Panda"),
					enabler(TrashPandaBlacklight.class, "Trash Panda Blacklight"),
					enabler(TrashPandaDawnlight.class, "Trash Panda Dawnlight"),
					enabler(TrashPandaDaylight.class, "Trash Panda Daylight"),
					enabler(TrashPandaMoonlight.class, "Trash Panda Moonlight"),
					enabler(TrashPandaStarlight.class, "Trash Panda Starlight"),
					enabler(CutePinkLight.class, "Cute Pink Light"),
					enabler(CutePinkDark.class, "Cute Pink Dark"),
					enabler(Codely.class, "Codely"),
					enabler(CodelyBlue.class, "Codely Blue"),
					enabler(CodelyDark.class, "Codely Dark"),
					enabler(CodelyLight.class, "Codely Light"),
					enabler(Cyberpunk.class, "Cyberpunk"),
					enabler(ElegantDark.class, "Elegant Dark"),
					enabler(ElegantLight.class, "Elegant Light"),
					enabler(EspressoLight.class, "Espresso Light"),
					enabler(Dracula.class, "Dracula"),
					enabler(CatppuccinFrappe.class, "Catppuccin Frappe"),
					enabler(CatppuccinLatte.class, "Catppuccin Latte"),
					enabler(CatppuccinMacchiato.class, "Catppuccin Macchiato"),
					enabler(CatppuccinMocha.class, "Catppuccin Mocha"),
					enabler(VSCodeDark.class, "VSCode Dark"),
					enabler(VSCodeDarkModern.class, "VSCode Dark Modern"),
					enabler(VSCodeLight.class, "VSCode Light"),
					enabler(Halcyon.class, "Halcyon"),
					enabler(SolidCherry.class, "Solid Cherry"),
					enabler(HardHackerDarker.class, "Hard Hacker Darker"),
					enabler(HardHackerNormal.class, "Hard Hacker Normal"),
					enabler(DarkUbuntu.class, "Dark Ubuntu"),
					enabler(Autumn.class, "Autumn"),
					enabler(Iceberg.class, "Iceberg"),
					enabler(Copilot.class, "Copilot"),
					enabler(PinkAsHeck.class, "Pink As Heck"),

					// From FlatLaf
					enabler(ArcDarkMaterial.class, "Arc Dark (Material)"),
					enabler(AtomOneDark.class, "Atom One Dark (Material)"),
					enabler(AtomOneLight.class, "Atom One Light (Material)"),
					enabler(DraculaMaterial.class, "Dracula (Material)"),
					enabler(GitHubDarkMaterial.class, "Github Dark (Material)"),
					enabler(GitHubMaterial.class, "Github (Material)"),
					enabler(LightOwl.class, "Light Owl (Material)"),
					enabler(MaterialDarker.class, "Material Darker (Material)"),
					enabler(MaterialDeepOcean.class, "Material Deep Ocean (Material)"),
					enabler(MaterialLighter.class, "Material Lighter (Material)"),
					enabler(MaterialOceanic.class, "Material Oceanic (Material)"),
					enabler(MaterialPalenight.class, "Material Palenight (Material)"),
					enabler(MonokaiProMaterial.class, "Monokai Pro (Material)"),
					enabler(Moonlight.class, "Moonlight (Material)"),
					enabler(NightOwl.class, "Night Owl (Material)"),
					enabler(SolarizedDark.class, "Solarized Dark (Material)"),
					enabler(SolarizedLight.class, "Solarized Light (Material)"),
					// End material

					enabler(Arc.class, "Arc"),
					enabler(ArcDark.class, "Arc Dark"),
					enabler(ArcOrange.class, "Arc Orance"),
					enabler(ArcDarkOrange.class, "Arc Dark Orange"),
					enabler(Carbon.class, "Carbon"),
					enabler(Cobalt2.class, "Cobalt 2"),
					enabler(DarkFlat.class, "Dark Flat"),
					enabler(DarkPurple.class, "Dark Purple"),
					enabler(GradiantoDarkFuchsia.class, "Gradianto Dark Fuchsia"),
					enabler(GradiantoDeepOcean.class, "Gradianto Deep Ocean"),
					enabler(GradiantoMidnightBlue.class, "Gradianto Midnight Blue"),
					enabler(GradiantoNatureGreen.class, "Gradianto Nature Green"),
					enabler(Gray.class, "Gray"),
					enabler(GruvboxDarkHard.class, "Gruvbox Dark Hard"),
					enabler(GruvboxDarkMedium.class, "Gruvbox Dark Medium"),
					enabler(GruvboxDarkSoft.class, "Gruvbox Dark Soft"),
					enabler(HiberbeeDark.class, "Hiberbee Dark"),
					enabler(HighContrast.class, "High Contrast"),
					enabler(LightFlat.class, "Light Flat"),
					enabler(MaterialTheme.class, "Material Theme"),
					enabler(Monocai.class, "Monocai"),
					enabler(MonokaiPro.class, "Monokai Pro"),
					enabler(OneDark.class, "One Dark"),
					enabler(Vuesion.class, "Vuesion"),
					enabler(XcodeDark.class, "XCode Dark")
	));

	private FlatLookAndFeelIntelliJThemes() {}

	/**
	 * Registers the IntelliJ themes, making them available via {@link LookAndFeelProvider}, for example
	 * to a {@link is.codion.swing.common.ui.laf.LookAndFeelComboBox}. Call once during application startup.
	 */
	public static void addAll() {
		add(info -> true);
	}

	/**
	 * Registers the IntelliJ themes, making them available via {@link LookAndFeelProvider}, for example
	 * to a {@link is.codion.swing.common.ui.laf.LookAndFeelComboBox}. Call once during application startup.
	 * @param include controls which look and feels to include
	 */
	public static void add(Predicate<LookAndFeelEnabler> include) {
		requireNonNull(include);
		LOOK_AND_FEELS.stream()
						.filter(include)
						.forEach(LookAndFeelProvider::addLookAndFeel);
	}

	static Collection<LookAndFeelEnabler> lookAndFeels() {
		return LOOK_AND_FEELS;
	}

	private static LookAndFeelEnabler enabler(Class<? extends IntelliJTheme.ThemeLaf> lookAndFeelClass, String name) {
		try {
			IntelliJTheme.ThemeLaf theme = lookAndFeelClass.getDeclaredConstructor().newInstance();

			return lookAndFeelEnabler(new LookAndFeelInfo(name, lookAndFeelClass.getName()), theme.isDark(), ENABLER);
		}
		catch (Exception e) {
			throw Exceptions.runtime(e);
		}
	}

	/**
	 * Requrired since scaling must happen before the look and feel is applied.
	 */
	private static final class DefaultEnabler implements Consumer<LookAndFeelInfo> {

		@Override
		public void accept(LookAndFeelInfo lookAndFeelInfo) {
			try {
				Scaler.instance(requireNonNull(lookAndFeelInfo).getClassName()).ifPresent(Scaler::apply);
				UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
				Utilities.updateComponentTreeForAllWindows();
			}
			catch (Exception e) {
				throw Exceptions.runtime(e);
			}
		}
	}
}
