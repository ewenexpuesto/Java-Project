package application;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import logger.LoggerFactory;
import utils.IconFactory;

/**
 * JavaFX Expressions Manaher Application main program
 * @author davidroussel
 */
public class MainFX extends Application
{
	/**
	 * Verbose status indicating if debug messages should be displayed
	 * on the console or only sent to a log file
	 */
	private boolean verbose = true;

	/**
	 * Logger used to display debug or info messages
	 * @implNote Needs to be initialized {@link #init()}
	 */
	private Logger logger = null;

	/**
	 * Preferences to load
	 */
	private Preferences preferences;

	/**
	 * {@link Preferences} key for stage width
	 */
	protected final static String PREF_WIDTH = "width";

	/**
	 * {@link Preferences} key for stage height
	 */
	protected final static String PREF_HEIGHT = "height";

	/**
	 * {@link Preferences} key for numbers type
	 */
	protected final static String PREF_NUMBER_TYPE = "type";

	/**
	 * Application initialization method.
	 * Called after construction and before actual starting
	 */
	@Override
	public void init() throws Exception
	{
		super.init();

		Application.Parameters appParameters = getParameters();
		verbose = false;

		/*
		 * logger instantiation
		 */
		logger = null;
		Class<?> runningClass = getClass();
		String logFilename =
		    (verbose ? null : runningClass.getSimpleName() + ".log");
		Logger parent = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		Level level = (verbose ? Level.ALL : Level.INFO);
		try
		{
			logger = LoggerFactory.getLogger(runningClass,
			                                 verbose,
			                                 logFilename,
			                                 false,
			                                 parent,
			                                 level);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.exit(ex.hashCode());
		}

		preferences = Preferences.userNodeForPackage(getClass());

		setAttributes(appParameters);
	}


	/**
	 * The main entry point for all JavaFX applications.
	 * The start method is called after the init method has returned,
	 * and after the system is ready for the application to begin running.
	 * NOTE: This method is called on the JavaFX Application Thread.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception
	{
		// --------------------------------------------------------------------
		// Loads Scene from FXML
		// --------------------------------------------------------------------
		logger.info("Loading FXML file ...");
		FXMLLoader loader = new FXMLLoader(getClass().getResource("ExpressionsFrame.fxml"));
		VBox root = null;
		try
		{
			root = loader.<VBox>load();
		}
		catch (IOException e)
		{
			logger.severe("Can't load FXML file " + e.getMessage());
			System.exit(e.hashCode());
		}

		// --------------------------------------------------------------------
		// Get controller's instance and get/set some values such as
		//	- set parent logger to issue messages
		//	- set parent stage on controller so it can be closed later
		// --------------------------------------------------------------------
		logger.info("Setting up controller");
		Controller controller = (Controller) loader.getController();
		controller.setParentLogger(logger);
		controller.setParentStage(primaryStage);

		// --------------------------------------------------------------------
		// Finally launch GUI
		// --------------------------------------------------------------------
		logger.info("Setting up GUI...");
		double width = preferences.getDouble(PREF_WIDTH, 600);
		double height = preferences.getDouble(PREF_HEIGHT, 420);
		Scene scene = new Scene(root, -1, -1, true, SceneAntialiasing.BALANCED);
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		primaryStage.setScene(scene);

		/*
		 * Setup App title and icon
		 */
		StringBuilder sb = new StringBuilder();
		sb.append("Expressions Manager");
		sb.append(" v ");
		sb.append("2.0");
		primaryStage.setTitle(sb.toString());
		primaryStage.setWidth(width);
		primaryStage.setHeight(height);
		Image image = IconFactory.getLargeIcon("math");
		if (!image.isError())
		{
			primaryStage.getIcons().add(image);
		}
		else
		{
			logger.severe("Couldn't load icon");
		}
		/*
		 * Set up quit logic to stage close request by using
		 * Controller#quitAction_Impl as the EventHandler of the
		 * setOnCloseRequest of the primaryStage.
		 * This ensure proper quit logic is performed even if we directly closes
		 * the window instead of properly quitting
		 */
		primaryStage.setOnCloseRequest(controller::quitActionImpl);
		primaryStage.show();
	}

	/**
	 * Main program entry
	 * @param args program arguments
	 */
	public static void main(String[] args)
	{
		launch(args);
	}

	/**
	 * Sets attributes values based on argument parsing.
	 * Valid arguments are:
	 * <ul>
	 * 	<li>{@code --verbose}: to set verbose on</li>
	 * 	<li>{@code --load <filepath>}: to load a file containing expressions</li>
	 * 	<li>{@code --type {int|float|double}}: to set number type</li>
	 * 	<li>{@code --country <2 Letters country code>}: to set country</li>
	 * </ul>
	 * @param parameters The parameters to use for setting attributes
	 * @throws IllegalArgumentException If a command line argument is invalid
	 */
	protected void setAttributes(Application.Parameters parameters)
		throws IllegalArgumentException
	{
		/*
		 * Unnamed arguments processing
		 */
		List<String> unNamedParameters = parameters.getUnnamed();

		verbose = unNamedParameters.contains("--verbose") ||
			unNamedParameters.contains("-v");
		if (verbose)
		{
			logger.info("Setting verbose on");
		}
		if (unNamedParameters.contains("--load"))
		{
			throw new IllegalArgumentException("Missing filePath after --load");
		}

		/*
		 * Named arguments processing
		 */
		Map<String, String> namedParameters = parameters.getNamed();

		/*
		 * Get --type {int|float|double} argument and set preferences
		 * accordingly.
		 */
		String numberType = namedParameters.get(PREF_NUMBER_TYPE);
		if (numberType != null)
		{
			numberType = numberType.toLowerCase();
			if (numberType.equals("int"))
			{
				preferences.putInt(PREF_NUMBER_TYPE, 0);

			}
			else if (numberType.equals("float"))
			{
				preferences.putInt(PREF_NUMBER_TYPE, 1);
			}
			else if (numberType.equals("double"))
			{
				preferences.putInt(PREF_NUMBER_TYPE, 2);
			}
			else
			{
				throw new IllegalArgumentException(
					"Invalid type: " + numberType);
			}
		}
	}
}
