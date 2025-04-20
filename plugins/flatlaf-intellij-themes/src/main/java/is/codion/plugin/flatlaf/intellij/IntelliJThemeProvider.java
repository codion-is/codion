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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.plugin.flatlaf.intellij;

import is.codion.plugin.flatlaf.intellij.themes.akusan.AkusanDark;
import is.codion.plugin.flatlaf.intellij.themes.akusan.AkusanGray;
import is.codion.plugin.flatlaf.intellij.themes.akusan.AkusanLight;
import is.codion.plugin.flatlaf.intellij.themes.arc.Arc;
import is.codion.plugin.flatlaf.intellij.themes.arc.ArcDark;
import is.codion.plugin.flatlaf.intellij.themes.arc.ArcDarkOrange;
import is.codion.plugin.flatlaf.intellij.themes.arc.ArcOrange;
import is.codion.plugin.flatlaf.intellij.themes.carbon.Carbon;
import is.codion.plugin.flatlaf.intellij.themes.cobalt2.Cobalt2;
import is.codion.plugin.flatlaf.intellij.themes.codely.Codely;
import is.codion.plugin.flatlaf.intellij.themes.codely.CodelyBlue;
import is.codion.plugin.flatlaf.intellij.themes.codely.CodelyDark;
import is.codion.plugin.flatlaf.intellij.themes.codely.CodelyLight;
import is.codion.plugin.flatlaf.intellij.themes.cutepink.CutePinkDark;
import is.codion.plugin.flatlaf.intellij.themes.cutepink.CutePinkLight;
import is.codion.plugin.flatlaf.intellij.themes.cyberpunk.Cyberpunk;
import is.codion.plugin.flatlaf.intellij.themes.darculasolid.DarculaSolid;
import is.codion.plugin.flatlaf.intellij.themes.darkflat.DarkFlat;
import is.codion.plugin.flatlaf.intellij.themes.darkhorizon.DarkHorizon;
import is.codion.plugin.flatlaf.intellij.themes.darkpurple.DarkPurple;
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
import is.codion.plugin.flatlaf.intellij.themes.hiberbee.HiberbeeDark;
import is.codion.plugin.flatlaf.intellij.themes.highcontrast.HighContrast;
import is.codion.plugin.flatlaf.intellij.themes.lightflat.LightFlat;
import is.codion.plugin.flatlaf.intellij.themes.lotus.LotusDark;
import is.codion.plugin.flatlaf.intellij.themes.lotus.LotusLight;
import is.codion.plugin.flatlaf.intellij.themes.material.ArcDarkMaterial;
import is.codion.plugin.flatlaf.intellij.themes.material.AtomOneDark;
import is.codion.plugin.flatlaf.intellij.themes.material.AtomOneLight;
import is.codion.plugin.flatlaf.intellij.themes.material.Dracula;
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
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderDark;
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderDay;
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderLight;
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderNight;
import is.codion.plugin.flatlaf.intellij.themes.srcery.Srcery;
import is.codion.plugin.flatlaf.intellij.themes.tayviscon.TeyvisconDark;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPanda;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaBlacklight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaDawnlight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaDaylight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaMoonlight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaStarlight;
import is.codion.plugin.flatlaf.intellij.themes.vuesion.Vuesion;
import is.codion.plugin.flatlaf.intellij.themes.xcodedark.XcodeDark;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.LoggingFacade;

import javax.swing.UIManager.LookAndFeelInfo;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.lookAndFeelEnabler;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * <p>Provides Look And Feels based on IntelliJ themes.
 * <p>Uses Flat Look And Feel under the hood.
 */
public final class IntelliJThemeProvider implements LookAndFeelProvider {

	private final Collection<LookAndFeelEnabler> providers;

	static {
		// Turn off FlatLaf logging to get around
		// https://github.com/JFormDesigner/FlatLaf/issues/990
		LoggingFacade facade = LoggingFacade.INSTANCE;
		Logger.getLogger(FlatLaf.class.getName()).setLevel(Level.OFF);
	}

	public IntelliJThemeProvider() {
		providers = unmodifiableList(asList(
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Aurora Borealis", AuroraBorealis.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Autumn", Autumn.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Everest", Everest.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Roraima", Roraima.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Sakura", Sakura.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Sequoia", Sequoia.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Srcery", Srcery.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Darcula", GerryDarcula.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Dark", GerryDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Oceanic", GerryOceanic.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Storm", GerryStorm.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Violet", GerryViolet.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Light", GerryLight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Lotus Dark", LotusDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Lotus Light", LotusLight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Teyviscon Dark", TeyvisconDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Dark Hard", EverforestDarkHard.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Dark Medium", EverforestDarkMedium.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Dark Soft", EverforestDarkSoft.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Light Hard", EverforestLightHard.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Light Medium", EverforestLightMedium.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Light Soft", EverforestLightSoft.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Dark Horizon", DarkHorizon.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Akusan Dark", AkusanDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Akusan Gray", AkusanGray.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Akusan Light", AkusanLight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("One Pauintxi All Blue", OnePauintxiAllBlue.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("One Pauintxi Blue", OnePauintxiBlue.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("One Pauintxi All Orange", OnePauintxiAllOrange.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("One Pauintxi Orange", OnePauintxiOrange.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Light Hard", GruvboxMaterialLightHard.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Light Medium", GruvboxMaterialLightMedium.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Light Soft", GruvboxMaterialLightSoft.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Obsidian Sunset Blue", ObsidianSunsetBlue.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Obsidian Sunset Orange", ObsidianSunsetOrange.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Github Dark", GithubDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Github Dark Default", GithubDarkDefault.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Github Light", GithubLight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Rider Dark", RiderDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Rider Day", RiderDay.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Rider Light", RiderLight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Rider Night", RiderNight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Rider Dark", RiderDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Darcula Solid", DarculaSolid.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Nightfall", Nightfall.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda", TrashPanda.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda Blacklight", TrashPandaBlacklight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda Dawnlight", TrashPandaDawnlight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda Daylight", TrashPandaDaylight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda Moonlight", TrashPandaMoonlight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda Starlight", TrashPandaStarlight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Cute Pink Light", CutePinkLight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Cute Pink Dark", CutePinkDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Codely", Codely.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Codely Blue", CodelyBlue.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Codely Dark", CodelyDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Codely Light", CodelyLight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Cyberpunk", Cyberpunk.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Elegant Dark", ElegantDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Elegant Light", ElegantLight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Espresso Light", EspressoLight.class.getName())),

						// From FlatLaf
						lookAndFeelEnabler(new LookAndFeelInfo("Arc Dark (Material)", ArcDarkMaterial.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Atom One Dark (Material)", AtomOneDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Atom One Light (Material)", AtomOneLight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Dracula (Material)", Dracula.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Github Dark (Material)", GitHubDarkMaterial.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Github (Material)", GitHubMaterial.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Light Owl (Material)", LightOwl.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Darker (Material)", MaterialDarker.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Deep Ocean (Material)", MaterialDeepOcean.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Lighter (Material)", MaterialLighter.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Oceanic (Material)", MaterialOceanic.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Palenight (Material)", MaterialPalenight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Monokai Pro (Material)", MonokaiProMaterial.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Moonlight (Material)", Moonlight.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Night Owl (Material)", NightOwl.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Solarized Dark (Material)", SolarizedDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Solarized Light (Material)", SolarizedLight.class.getName())),
						// End material

						lookAndFeelEnabler(new LookAndFeelInfo("Arc", Arc.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Arc Dark", ArcDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Arc Orance", ArcOrange.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Arc Dark Orange", ArcDarkOrange.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Carbon", Carbon.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Cobalt 2", Cobalt2.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Dark Flat", DarkFlat.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Dark Purple", DarkPurple.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Dracula", Dracula.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gradianto Dark Fuchsia", GradiantoDarkFuchsia.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gradianto Deep Ocean", GradiantoDeepOcean.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gradianto Midnight Blue", GradiantoMidnightBlue.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gradianto Nature Green", GradiantoNatureGreen.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gray", Gray.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Dark Hard", GruvboxDarkHard.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Dark Medium", GruvboxDarkMedium.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Dark Soft", GruvboxDarkSoft.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Hiberbee Dark", HiberbeeDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("High Contrast", HighContrast.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Light Flat", LightFlat.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Theme", MaterialTheme.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Monocai", Monocai.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Monokai Pro", MonokaiPro.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("One Dark", OneDark.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("Vuesion", Vuesion.class.getName())),
						lookAndFeelEnabler(new LookAndFeelInfo("XCode Dark", XcodeDark.class.getName()))
		));
	}

	/**
	 * @return all available IntelliJ Theme Look and Feels
	 */
	public Collection<LookAndFeelEnabler> get() {
		return providers;
	}
}
