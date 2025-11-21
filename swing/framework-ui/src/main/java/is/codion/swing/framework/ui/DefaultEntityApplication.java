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
package is.codion.swing.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.Text;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.DomainType;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.dialog.ExceptionDialogBuilder;
import is.codion.swing.common.ui.dialog.LoginDialogBuilder.LoginValidator;
import is.codion.swing.common.ui.scaler.Scaler;
import is.codion.swing.common.ui.window.Windows;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.systemLookAndFeelClassName;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.lookAndFeelEnabler;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.findLookAndFeel;
import static is.codion.swing.common.ui.window.Windows.screenSizeRatio;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

final class DefaultEntityApplication<M extends SwingEntityApplicationModel, P extends EntityApplicationPanel<M>>
				implements EntityApplication<M, P> {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityApplication.class);

	private static final MessageBundle MESSAGES =
					messageBundle(DefaultEntityApplication.class, getBundle(DefaultEntityApplication.class.getName()));

	/**
	 * Map between FlatLaf IntelliJ theme look and feels and the codion plugin ones,
	 * in order for look and feel application preferences to work correctly when
	 * switching to the plugin based ones.
	 */
	static final Map<String, String> INTELLIJ_THEMES = new LinkedHashMap<>();

	private static final String CODION_CLIENT_VERSION = "codion.client.version";
	private static final String CODION_VERSION = "codion.version";
	private static final String DASH = " - ";
	private static final String EMPTY_JSON_OBJECT = "{}";

	private final Class<M> applicationModelClass;
	private final Class<P> applicationPanelClass;
	private final @Nullable String preferencesLookAndFeel;

	private @Nullable Function<User, EntityConnectionProvider> connectionProviderFunction;
	private @Nullable EntityConnectionProvider connectionProvider;
	private @Nullable String name;
	private Function<EntityConnectionProvider, M> model = new DefaultApplicationModelFactory();
	private Function<M, P> panel = new DefaultApplicationPanelFactory();
	private @Nullable Observable<String> frameTitle;
	private boolean connectionInfoUpperCase = CONNECTION_INFO_UPPER_CASE.getOrThrow();

	private @Nullable DomainType domain = EntityConnectionProvider.CLIENT_DOMAIN_TYPE.get();
	private Supplier<User> userSupplier = new DefaultUserSupplier();
	private Supplier<JFrame> frameSupplier = new DefaultFrameSupplier();
	private boolean startupDialog = STARTUP_DIALOG.getOrThrow();
	private @Nullable ImageIcon icon;
	private @Nullable Version version;
	private boolean saveDefaultUsername = SAVE_DEFAULT_USERNAME.getOrThrow();
	private Supplier<JComponent> loginPanelSouthComponentSupplier = new DefaultSouthComponentSupplier();
	private @Nullable Runnable onStarting;
	private @Nullable Consumer<P> onStarted;

	private String defaultLookAndFeelClassName = systemLookAndFeelClassName();
	private @Nullable String lookAndFeelClassName;
	private boolean uncaughtExceptionHandler = true;
	private boolean maximizeFrame = false;
	private boolean displayFrame = true;
	private boolean mainMenu = true;
	private @Nullable Dimension frameSize;
	private @Nullable Dimension defaultFrameSize;
	private @Nullable User defaultUser;
	private @Nullable User user = USER.optional()
					.map(User::parse)
					.orElse(null);

	DefaultEntityApplication(Class<M> applicationModelClass, Class<P> applicationPanelClass) {
		this.applicationModelClass = requireNonNull(applicationModelClass);
		this.applicationPanelClass = requireNonNull(applicationPanelClass);
		ApplicationPreferences preferences = EntityApplicationModel.USER_PREFERENCES.getOrThrow() ?
						ApplicationPreferences.load(applicationModelClass, applicationPanelClass) :
						ApplicationPreferences.fromString(EMPTY_JSON_OBJECT);
		this.defaultUser = preferences.defaultLoginUser();
		this.frameSize = preferences.frameSize();
		this.maximizeFrame = preferences.frameMaximized();
		this.preferencesLookAndFeel = preferences.lookAndFeel();
		Scaler.SCALING.set(preferences.scaling());
	}

	@Override
	public EntityApplication<M, P> domain(DomainType domain) {
		this.domain = requireNonNull(domain);
		return this;
	}

	@Override
	public EntityApplication<M, P> icon(ImageIcon icon) {
		this.icon = requireNonNull(icon);
		return this;
	}

	@Override
	public EntityApplication<M, P> defaultLookAndFeel(Class<? extends LookAndFeel> defaultLookAndFeelClass) {
		return defaultLookAndFeel(requireNonNull(defaultLookAndFeelClass).getName());
	}

	@Override
	public EntityApplication<M, P> defaultLookAndFeel(String defaultLookAndFeelClassName) {
		this.defaultLookAndFeelClassName = requireNonNull(defaultLookAndFeelClassName);
		return this;
	}

	@Override
	public EntityApplication<M, P> lookAndFeel(Class<? extends LookAndFeel> lookAndFeelClass) {
		return lookAndFeel(requireNonNull(lookAndFeelClass).getName());
	}

	@Override
	public EntityApplication<M, P> lookAndFeel(String lookAndFeelClassName) {
		this.lookAndFeelClassName = requireNonNull(lookAndFeelClassName);
		return this;
	}

	@Override
	public EntityApplication<M, P> name(String name) {
		if (Text.nullOrEmpty(name)) {
			throw new IllegalArgumentException("Application name cannot be null or empty");
		}
		this.name = name;
		return this;
	}

	@Override
	public EntityApplication<M, P> version(Version version) {
		this.version = requireNonNull(version);
		return this;
	}

	@Override
	public EntityApplication<M, P> model(Function<EntityConnectionProvider, M> model) {
		this.model = requireNonNull(model);
		return this;
	}

	@Override
	public EntityApplication<M, P> panel(Function<M, P> panel) {
		this.panel = requireNonNull(panel);
		return this;
	}

	@Override
	public EntityApplication<M, P> defaultUser(@Nullable User defaultUser) {
		this.defaultUser = defaultUser;
		return this;
	}

	@Override
	public EntityApplication<M, P> user(@Nullable User user) {
		this.user = user;
		return this;
	}

	@Override
	public EntityApplication<M, P> user(Supplier<User> userSupplier) {
		this.userSupplier = requireNonNull(userSupplier);
		return this;
	}

	@Override
	public EntityApplication<M, P> saveDefaultUsername(boolean saveDefaultUsername) {
		this.saveDefaultUsername = saveDefaultUsername;
		return this;
	}

	@Override
	public EntityApplication<M, P> frame(Supplier<JFrame> frame) {
		this.frameSupplier = requireNonNull(frame);
		return this;
	}

	@Override
	public EntityApplication<M, P> frameTitle(String frameTitle) {
		return frameTitle(Value.nullable(requireNonNull(frameTitle)));
	}

	@Override
	public EntityApplication<M, P> frameTitle(Observable<String> frameTitle) {
		this.frameTitle = requireNonNull(frameTitle);
		return this;
	}

	@Override
	public EntityApplication<M, P> connectionInfoUpperCase(boolean connectionInfoUpperCase) {
		this.connectionInfoUpperCase = connectionInfoUpperCase;
		return this;
	}

	@Override
	public EntityApplication<M, P> mainMenu(boolean mainMenu) {
		this.mainMenu = mainMenu;
		return this;
	}

	@Override
	public EntityApplication<M, P> maximizeFrame(boolean maximizeFrame) {
		this.maximizeFrame = maximizeFrame;
		return this;
	}

	@Override
	public EntityApplication<M, P> displayFrame(boolean displayFrame) {
		this.displayFrame = displayFrame;
		return this;
	}

	@Override
	public EntityApplication<M, P> uncaughtExceptionHandler(boolean uncaughtExceptionHandler) {
		this.uncaughtExceptionHandler = uncaughtExceptionHandler;
		return this;
	}

	@Override
	public EntityApplication<M, P> startupDialog(boolean startupDialog) {
		this.startupDialog = startupDialog;
		return this;
	}

	@Override
	public EntityApplication<M, P> frameSize(@Nullable Dimension frameSize) {
		this.frameSize = frameSize;
		return this;
	}

	@Override
	public EntityApplication<M, P> defaultFrameSize(@Nullable Dimension defaultFrameSize) {
		this.defaultFrameSize = defaultFrameSize;
		return this;
	}

	@Override
	public EntityApplication<M, P> loginPanelSouthComponent(Supplier<JComponent> loginPanelSouthComponentSupplier) {
		this.loginPanelSouthComponentSupplier = requireNonNull(loginPanelSouthComponentSupplier);
		return this;
	}

	@Override
	public EntityApplication<M, P> onStarting(@Nullable Runnable onStarting) {
		this.onStarting = onStarting;
		return this;
	}

	@Override
	public EntityApplication<M, P> onStarted(@Nullable Consumer<P> onStarted) {
		this.onStarted = onStarted;
		return this;
	}

	@Override
	public EntityApplication<M, P> connectionProvider(EntityConnectionProvider connectionProvider) {
		this.connectionProvider = requireNonNull(connectionProvider);
		return this;
	}

	@Override
	public EntityApplication<M, P> connectionProvider(Function<User, EntityConnectionProvider> connectionProvider) {
		this.connectionProviderFunction = requireNonNull(connectionProvider);
		return this;
	}

	@Override
	public void start() {
		start(true);
	}

	@Override
	public void start(boolean onEventDispatchThread) {
		if (onEventDispatchThread) {
			SwingUtilities.invokeLater(new ApplicationStarter());
		}
		else {
			startApplication();
		}
	}

	private void startApplication() {
		LOG.debug("{} application starting", applicationName());
		if (uncaughtExceptionHandler) {
			setDefaultUncaughtExceptionHandler(new DisplayUncaughtExceptionAndExit());
		}
		setVersionProperty();
		enableLookAndFeel();
		configureIcons();
		if (onStarting != null) {
			onStarting.run();
		}
		EntityConnectionProvider connectionProvider = initializeConnectionProvider();
		long initializationStarted = currentTimeMillis();
		if (startupDialog) {
			Dialogs.progressWorker()
							.task(new InitializeApplicationModel(connectionProvider))
							.title(applicationName())
							.icon(icon)
							.border(emptyBorder())
							.westComponent(createStartupIconLabel())
							.onResult(new StartApplication(initializationStarted))
							.onException(new DisplayExceptionAndExit())
							.execute();
		}
		else {
			startApplication(initializeApplicationModel(connectionProvider), initializationStarted);
		}
	}

	private String applicationName() {
		if (name != null) {
			return name;
		}
		if (domain != null) {
			return domain.name();
		}

		return applicationPanelClass.getSimpleName();
	}

	private void enableLookAndFeel() {
		try {
			Class<LookAndFeel> lookAndFeelClass = (Class<LookAndFeel>) Class.forName(lookAndFeelClassName());
			findLookAndFeel(lookAndFeelClass)
							.orElse(lookAndFeelEnabler(new LookAndFeelInfo(lookAndFeelClass.getSimpleName(), lookAndFeelClass.getName())))
							.enable();
		}
		catch (Exception e) {
			LOG.error("Exception while enabling Look and Feel", e);
		}
	}

	private String lookAndFeelClassName() {
		if (lookAndFeelClassName != null) {
			return lookAndFeelClassName;
		}

		String userPreference = fromFlatLaf(preferencesLookAndFeel);

		return userPreference == null ? defaultLookAndFeelClassName : userPreference;
	}

	private static @Nullable String fromFlatLaf(@Nullable String lookAndFeelClassName) {
		if (lookAndFeelClassName == null) {
			return null;
		}
		if (lookAndFeelClassName.startsWith("com.formdev.flatlaf.intellijthemes")) {
			return INTELLIJ_THEMES.get(lookAndFeelClassName);
		}

		return lookAndFeelClassName;
	}

	private void configureIcons() {
		if (icon == null) {
			icon = FrameworkIcons.instance().logo();
		}
	}

	/**
	 * Sets the application and framework versions as a system properties, so that they appear automatically in exception dialogs.
	 */
	private void setVersionProperty() {
		if (version != null) {
			System.setProperty(CODION_CLIENT_VERSION, version.toString());
		}
		System.setProperty(CODION_VERSION, Version.versionAndMetadataString());
	}

	private void startApplication(M applicationModel, long initializationStarted) {
		JFrame applicationFrame = createFrame();
		if (uncaughtExceptionHandler) {
			setDefaultUncaughtExceptionHandler(new DisplayUncaughtExceptionHandler(applicationFrame));
		}
		if (displayFrame) {
			applicationFrame.setTitle(MESSAGES.getString("initializing") + " " + applicationName());
			applicationFrame.setVisible(true);
		}
		try {
			P panel = initializeApplicationPanel(applicationModel);
			panel.setSaveDefaultUsername(saveDefaultUsername);
			configureFrame(applicationFrame, panel);
			LOG.info("{}, application started successfully: {} ms", applicationFrame.getTitle(), currentTimeMillis() - initializationStarted);
			if (displayFrame) {
				applicationFrame.setVisible(true);
			}
			panel.requestInitialFocus();
			if (onStarted != null) {
				onStarted.accept(panel);
			}
		}
		catch (Exception e) {
			displayExceptionAndExit(e, applicationFrame);
		}
	}

	private JFrame createFrame() {
		JFrame frame = frameSupplier.get();
		frame.getContentPane().setLayout(new BorderLayout());
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		if (icon != null) {
			frame.setIconImage(icon.getImage());
		}
		if (frameSize != null) {
			frame.setSize(frameSize);
		}
		else if (defaultFrameSize != null) {
			frame.setSize(defaultFrameSize);
		}
		else {
			frame.setSize(screenSizeRatio(0.5));
		}
		frame.setLocationRelativeTo(null);
		if (maximizeFrame) {
			frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		}

		return frame;
	}

	private M initializeApplicationModel(EntityConnectionProvider connectionProvider) {
		return model.apply(connectionProvider);
	}

	private P initializeApplicationPanel(M applicationModel) {
		return (P) panel.apply(applicationModel).initialize();
	}

	private void configureFrame(JFrame frame, P applicationPanel) {
		frame.addWindowListener(new ExitOnClose(applicationPanel));
		if (frameTitle != null) {
			frame.setTitle(frameTitle.get());
			frameTitle.addConsumer(new FrameTitleConsumer(frame));
		}
		else {
			frame.setTitle(createDefaultFrameTitle(applicationPanel.applicationModel()));
		}
		if (mainMenu) {
			applicationPanel.createMenuBar().ifPresent(frame::setJMenuBar);
		}
		frame.setAlwaysOnTop(applicationPanel.alwaysOnTop().is());
		frame.getContentPane().add(applicationPanel, BorderLayout.CENTER);
		if (frameSize == null && defaultFrameSize == null) {
			frame.pack();
			Windows.resizeToFitScreen(frame);
			frame.setLocationRelativeTo(null);
		}
	}

	private String createDefaultFrameTitle(M applicationModel) {
		StringBuilder builder = new StringBuilder(applicationName());
		if (version != null) {
			if (builder.length() > 0) {
				builder.append(DASH);
			}
			builder.append(version);
		}
		if (builder.length() > 0) {
			builder.append(DASH);
		}
		builder.append(connectionInfo(applicationModel.connectionProvider()));

		return builder.toString();
	}

	private @Nullable JLabel createStartupIconLabel() {
		if (icon != null) {
			return new JLabel(icon);
		}

		return null;
	}

	private EntityConnectionProvider initializeConnectionProvider() {
		if (connectionProvider != null) {
			return connectionProvider;
		}
		User connectionUser = user == null ? userSupplier.get() : user;
		if (connectionProviderFunction != null) {
			return connectionProviderFunction.apply(connectionUser);
		}
		if (userSupplier instanceof DefaultEntityApplication.DefaultUserSupplier &&
						((DefaultUserSupplier) userSupplier).loginValidator.connectionProvider != null) {
			return ((DefaultUserSupplier) userSupplier).loginValidator.connectionProvider;
		}

		return createConnectionProvider(connectionUser);
	}

	private EntityConnectionProvider createConnectionProvider(User user) {
		if (domain == null) {
			throw new IllegalArgumentException("domain must be specified before creating a EntityConnectionProvider");
		}

		return EntityConnectionProvider.builder()
						.user(user)
						.domain(domain)
						.clientType(applicationName())
						.clientVersion(version)
						.build();
	}

	private String connectionInfo(EntityConnectionProvider connectionProvider) {
		String username = connectionProvider.user().username();
		String connectionInfo = connectionProvider.description()
						.map(connectionDescription -> username + (connectionDescription.isEmpty() ? "" : "@" + connectionDescription))
						.orElse(username);
		if (connectionInfoUpperCase) {
			connectionInfo = connectionInfo.toUpperCase();
		}

		return connectionInfo;
	}

	private static void displayExceptionAndExit(Throwable exception, @Nullable JFrame applicationFrame) {
		Throwable unwrapped = ExceptionDialogBuilder.unwrap(exception);
		if (unwrapped instanceof CancelException) {
			System.exit(0);
		}
		else {
			displayException(exception, applicationFrame);
			System.exit(1);
		}
	}

	private static void displayException(Throwable exception, @Nullable JFrame applicationFrame) {
		if (!(exception instanceof CancelException)) {
			Window focusOwnerParentWindow = Ancestor.window().of(getCurrentKeyboardFocusManager().getFocusOwner()).get();
			Dialogs.displayException(exception, focusOwnerParentWindow == null ? applicationFrame : focusOwnerParentWindow);
		}
	}

	private final class DefaultUserSupplier implements Supplier<User> {

		private final DefaultLoginValidator loginValidator = new DefaultLoginValidator();

		@Override
		public User get() {
			return Dialogs.login()
							.defaultUser(defaultUser)
							.validator(loginValidator)
							.title(loginDialogTitle())
							.icon(icon)
							.southComponent(loginPanelSouthComponentSupplier.get())
							.show();
		}

		private String loginDialogTitle() {
			StringBuilder builder = new StringBuilder(applicationName());
			if (builder.length() > 0 && version != null) {
				builder.append(DASH).append(version);
			}
			if (builder.length() > 0) {
				builder.append(DASH);
			}

			return builder.append(Messages.login()).toString();
		}
	}

	private final class DefaultLoginValidator implements LoginValidator {

		private @Nullable EntityConnectionProvider connectionProvider;

		@Override
		public void validate(User user) {
			connectionProvider = createConnectionProvider(user);
			try {
				connectionProvider.connection();//throws exception if the server is not reachable
			}
			catch (Exception e) {
				connectionProvider.close();
				connectionProvider = null;
				throw e;
			}
		}
	}

	private final class DefaultApplicationModelFactory implements Function<EntityConnectionProvider, M> {

		@Override
		public M apply(EntityConnectionProvider connectionProvider) {
			try {
				return applicationModelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private final class DefaultApplicationPanelFactory implements Function<M, P> {

		@Override
		public P apply(M model) {
			try {
				return applicationPanelClass.getConstructor(model.getClass()).newInstance(model);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private final class ApplicationStarter implements Runnable {

		@Override
		public void run() {
			startApplication();
		}
	}

	private final class StartApplication implements Consumer<M> {

		private final long initializationStarted;

		private StartApplication(long initializationStarted) {
			this.initializationStarted = initializationStarted;
		}

		@Override
		public void accept(M applicationModel) {
			startApplication(applicationModel, initializationStarted);
		}
	}

	private final class InitializeApplicationModel implements ProgressWorker.ResultTask<M> {

		private final EntityConnectionProvider connectionProvider;

		private InitializeApplicationModel(EntityConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public M execute() throws Exception {
			return initializeApplicationModel(connectionProvider);
		}
	}

	private static class DisplayExceptionAndExit implements Consumer<Exception> {

		@Override
		public void accept(Exception e) {
			displayExceptionAndExit(e, null);
		}
	}

	private static final class DefaultSouthComponentSupplier implements Supplier<JComponent> {

		@Override
		public @Nullable JComponent get() {
			return null;
		}
	}

	private static final class DefaultFrameSupplier implements Supplier<JFrame> {

		@Override
		public JFrame get() {
			return new JFrame();
		}
	}

	private static class DisplayUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

		private final JFrame applicationFrame;

		private DisplayUncaughtExceptionHandler(JFrame applicationFrame) {
			this.applicationFrame = applicationFrame;
		}

		@Override
		public void uncaughtException(Thread thread, Throwable exception) {
			displayException(exception, applicationFrame);
		}
	}

	private static class DisplayUncaughtExceptionAndExit implements Thread.UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread thread, Throwable e) {
			displayExceptionAndExit(e, null);
		}
	}

	private static final class ExitOnClose extends WindowAdapter {

		private final EntityApplicationPanel<?> applicationPanel;

		private ExitOnClose(EntityApplicationPanel<?> applicationPanel) {
			this.applicationPanel = applicationPanel;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			try {
				applicationPanel.exit();
			}
			catch (CancelException ignored) {/*ignored*/}
		}
	}

	private static final class FrameTitleConsumer implements Consumer<String> {

		private final JFrame frame;

		public FrameTitleConsumer(JFrame frame) {
			this.frame = frame;
		}

		@Override
		public void accept(String title) {
			SwingUtilities.invokeLater(new SetFrameTitle(frame, title));
		}
	}

	private static class SetFrameTitle implements Runnable {

		private final JFrame frame;
		private final String title;

		private SetFrameTitle(JFrame frame, String title) {
			this.frame = frame;
			this.title = title;
		}

		@Override
		public void run() {
			frame.setTitle(title);
		}
	}

	static {
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatArcDarkIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.ArcDarkMaterial");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.AtomOneDark");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneLightIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.AtomOneLight");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatDraculaIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.DraculaMaterial");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.GitHubMaterial");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubDarkIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.GitHubDarkMaterial");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatLightOwlIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.LightOwl");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.MaterialDarker");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDeepOceanIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.MaterialDeepOcean");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.MaterialLighter");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.MaterialOceanic");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialPalenightIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.MaterialPalenight");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMonokaiProIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.MonokaiProMaterial");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMoonlightIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.Moonlight");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatNightOwlIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.NightOwl");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatSolarizedDarkIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.SolarizedDark");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatSolarizedLightIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.material.SolarizedLight");

		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.arc.ArcOrange");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.arc.ArcDarkOrange");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.arc.ArcDark");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatArcIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.arc.Arc");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.carbon.Carbon");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.cobalt2.Cobalt2");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.cyan.Cyan");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.darkflat.DarkFlat");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.darkpurple.DarkPurple");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.dracula.Dracula");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatGradiantoDarkFuchsiaIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoDarkFuchsia");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatGradiantoDeepOceanIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoDeepOcean");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoMidnightBlue");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatGradiantoNatureGreenIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoNatureGreen");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatGrayIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.gray.Gray");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxDarkHard");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkMediumIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxDarkMedium");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkSoftIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxDarkSoft");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.hiberbee.HiberbeeDark");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.highcontrast.HighContrast");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.lightflat.LightFlat");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.materialtheme.MaterialTheme");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatMonocaiIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.monocai.Monocai");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.monokaipro.MonokaiPro");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatNordIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.nord.Nord");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.onedark.OneDark");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.solarized.SolarizedDark");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.solarized.SolarizedLight");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.spacegray.Spacegray");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatVuesionIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.vuesion.Vuesion");
		INTELLIJ_THEMES.put(
						"com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme",
						"is.codion.plugin.flatlaf.intellij.themes.xcodedark.XcodeDark");
	}
}
