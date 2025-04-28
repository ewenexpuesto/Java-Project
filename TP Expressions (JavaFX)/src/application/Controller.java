package application;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import expressions.Expression;
import expressions.binary.BinaryOperatorRules;
import expressions.models.ExpressionDisplay;
import expressions.models.ExpressionsModel;
import expressions.models.VariableDisplay;
import expressions.terminal.TerminalExpression;
import expressions.terminal.TerminalType;
import expressions.terminal.VariableExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import logger.LoggerFactory;
import parser.exceptions.ParserException;
import utils.IconFactory;

/**
 * Controller associated with ExpressionsFrame.fxml
 * Contains:
 * <ul>
 * 	<li>FXML UI elements that need to be referenced in business logic</li>
 * 	<li>onXXXX() callback methods handling UI requests</li>
 * </ul>
 * @author davidroussel
 * @see Initializable Initializable so it can initialize FXML related attributes.
 */
public class Controller implements Initializable
{
	// -------------------------------------------------------------------------
	// Internal attributes (non FXML)
	// -------------------------------------------------------------------------
	/**
	 * Logger to show debug message or only log them in a file
	 */
	private Logger logger = null;

	/**
	 * Logger level used by {@link #logger}
	 * @see #Controller()
	 * @see #loadPreferences()
	 * @see #onSetLoggerLevelAction(ActionEvent)
	 * @see #savePreferences(Stage)
	 */
	private Level loggerLevel = Level.INFO;

	/**
	 * The data model containing expressions
	 */
	private ExpressionsModel<Number> expressionsModel;

	/**
	 * The specimen property holding the number used to store number types in
	 * expressions.
	 * Either an {@link Integer}, a {@link Float} or a {@link Double}
	 * @implNote This property can be bound (evt bidirectionnaly) to the
	 * model's corresponding property.
	 * @see #Controller()
	 * @see #loadPreferences()
	 * @see #onSelectNumberType(ActionEvent)
	 * @see #savePreferences(Stage)
	 */
	private ObjectProperty<Number> specimen;

	/**
	 * Reference to parent stage so it can be quickly closed on quit
	 * Initialized through {@link #setParentStage(Stage)} in
	 * {@link MainFX#start(Stage)}
	 * @see #chooseAndLoadFile(boolean)
	 * @see #onSaveAction(ActionEvent)
	 * @see #quitActionImpl(Event)
	 * @see #setParentStage(Stage)
	 */
	private Stage parentStage = null;

	/**
	 * Set of buttons with display style that can change.
	 * These buttons are {@link #addButton}, {@link #openButton},
	 * {@link #saveButton}, {@link #selectAllButton}, {@link #deleteButton},
	 * {@link #quitButton}.
	 * @see #initialize(URL, ResourceBundle)
	 * @see #onDisplayButtonsWithStyle(ActionEvent)
	 */
	private Set<Labeled> styleableButtons = null;

	/**
	 * Display style for {@link #styleableButtons}.
	 * Either
	 * <ul>
	 * 	<li>ContentDisplay#GRAPHIC_ONLY</li>
	 * 	<li>ContentDisplay#TEXT_ONLY</li>
	 * 	<li>ContentDisplay#LEFT</li>
	 * </ul>
	 * @see #onDisplayButtonsWithStyle(ActionEvent)
	 * @see #loadPreferences()
	 * @see #savePreferences(Stage)
	 */
	private ContentDisplay contentDisplay;

	/**
	 * List of elements showing {@link Expression}s within a {@link TableView}
	 * such as {@link #expressionsTableView} using the {@link ExpressionDisplay}
	 * class
	 * @implNote this list shall be bound to
	 * {@link ExpressionsModel#getExpressions()}
	 * list changes to be automatically updated when needed.
	 * @see #Controller()
	 * @see #initialize(URL, ResourceBundle)
	 */
	private ObservableList<ExpressionDisplay<Number>> expressionsDisplayList;

	/**
	 * List of elemensts showing {@link VariableExpression}s within a
	 * {@link TableView} such as {@link #variablesTableView} using the
	 * {@link VariableDisplay} class.
	 * @implNote this list shall be bound to
	 * {@link ExpressionsModel#getVariables()} list changes
	 * to be automatically updated when needed
	 * @see #Controller()
	 * @see #initialize(URL, ResourceBundle)
	 */
	private ObservableList<VariableDisplay<Number>> variablesDisplayList;

	// -------------------------------------------------------------------------
	// FXML attributes
	// Every attribute featuring an fx:id in ExpressionsFrame.fxml
	// shall be found here as a @FXML annotated attribute.
	// And every @FXML annotated attribute found here shall also be identified
	// with an fx:id in ExpressionsFrame.fxml
	// -------------------------------------------------------------------------

	/**
	 * Message Label at the bottom of UI
	 * (to be cleared at startup and used for info messages)
	 */
	@FXML
	private Label messageLabel;

	/**
	 * Toggle group for buttons display
	 * {@link javafx.scene.control.RadioMenuItem}s:
	 * {@link #graphicsOnlyRadiomenuItem}, {@link #textAndGraphicsRadiomenuItem}
	 * and {@link #textOnlyRadiomenuItem}
	 */
	@FXML
	private ToggleGroup buttonsDisplayGroup;

	/**
	 * Toggle group for debug level {@link javafx.scene.control.RadioMenuItem}s:
	 * {@link #infoWarningSevereRadiomenuItem},
	 * {@link #warningSevereRadiomenuItem}, {@link #severeRadiomenuItem} &
	 * {@link #offRadiomenuItem}
	 */
	@FXML
	private ToggleGroup debugLevelGroup;

	/**
	 * Toggle group for number type {@link javafx.scene.control.RadioMenuItem}s:
	 * {@link #integersRadioMenuItem}, {@link #floatsRadiomenuItem} &
	 * {@link #doublesRadiomenuItem}
	 */
	@FXML
	private ToggleGroup numberTypeGroup;

	/**
	 * The {@link TextField} where expressions are typed
	 * @see #onAddAction(ActionEvent)
	 */
	@FXML
	private TextField inputField;

	/**
	 * The {@link TreeView} to display {@link Expression}s tree
	 * @see #initialize(URL, ResourceBundle)
	 * @see #onDeleteSelectedAction(ActionEvent)
	 * @see #onEditCommitAction(CellEditEvent)
	 */
	@FXML
	private TreeView<Expression<Number>> expressionsTreeView;

	/**
	 * The {@link TableView} to display Expressions (and also values).
	 * {@link ExpressionDisplay} class is used to pack data to display for each
	 * expression : The expression string
	 * (through {@link ExpressionDisplay#contentProperty()} holding
	 * {@link String}) and the expression value (through
	 * {@link ExpressionDisplay#valueProperty()} holding a {@link Number})
	 * @see #initialize(URL, ResourceBundle)
	 * @see #onDeleteSelectedAction(ActionEvent)
	 * @see #onDeselectAllAction(ActionEvent)
	 * @see #onEditCommitAction(CellEditEvent)
	 * @see #onSelectAllAction(ActionEvent)
	 */
	@FXML
	private TableView<ExpressionDisplay<Number>> expressionsTableView;

	/**
	 * The first column of {@link #expressionsTableView} containng
	 * {@link Expression}'s toString
	 * @see #initialize(URL, ResourceBundle)
	 * @see #onSelectAllAction(ActionEvent)
	 */
	@FXML
	private TableColumn<ExpressionDisplay<Number>, String> expressionsContentColumn;

	/**
	 * The second column of {@link #expressionsTableView} containng
	 * {@link Expression}'s value (if any)
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private TableColumn<ExpressionDisplay<Number>, Number> expressionsValueColumn;

	/**
	 * The {@link TableView} to display variables names and values (if any).
	 * {@link VariableDisplay} class is used to pack data to display for each
	 * variable : The variable name
	 * (through {@link VariableDisplay#contentProperty()} holding
	 * {@link String}) and the expression value (through
	 * {@link VariableDisplay#valueProperty()} holding a {@link Number})
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private TableView<VariableDisplay<Number>> variablesTableView;

	/**
	 * The first column of {@link #variablesTableView} containng
	 * {@link VariableExpression}'s toString (or name)
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private TableColumn<VariableDisplay<Number>, String> variablesContentColumn;

	/**
	 * The second column of {@link #variablesTableView} containng
	 * {@link VariableExpression}'s value (if any)
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private TableColumn<VariableDisplay<Number>, Number> variablesValueColumn;

	/**
	 * The {@link TextField} where searched elements are typed
	 * @see #initialize(URL, ResourceBundle)
	 * @see #onClearFilterAction(ActionEvent)
	 * @see #onFilterAction(ActionEvent)
	 */
	@FXML
	private TextField searchField;

	/**
	 * The {@link ComboBox} to chose {@link TerminalExpression} type to filter
	 * @see #initialize(URL, ResourceBundle)
	 * @see #onClearFilterAction(ActionEvent)
	 * @see #onFilterAction(ActionEvent)
	 */
	@FXML
	private ComboBox<TerminalType> terminalTypeCombobox;

	/**
	 * The {@link ComboBox} to chose {@link BinaryOperatorRules} type to filter
	 * @see #initialize(URL, ResourceBundle)
	 * @see #onClearFilterAction(ActionEvent)
	 * @see #onFilterAction(ActionEvent)
	 */
	@FXML
	private ComboBox<BinaryOperatorRules> binaryTypeCombobox;

	/**
	 * The add action {@link Button}
	 * @implNote part of {@link #styleableButtons}
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private Button addButton;

	/**
	 * The open action {@link Button}
	 * @implNote part of {@link #styleableButtons}
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private Button openButton;

	/**
	 * The save action {@link Button}
	 * @implNote part of {@link #styleableButtons}
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private Button saveButton;

	/**
	 * The save action {@link MenuItem}
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private MenuItem saveMenuItem;

	/**
	 * The revert action {@link MenuItem}
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private MenuItem revertMenuItem;

	/**
	 * The selectAll action {@link Button}
	 * @implNote part of {@link #styleableButtons}
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private Button selectAllButton;

	/**
	 * The selectNone action {@link Button}
	 * @implNote part of {@link #styleableButtons}
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private Button selectNoneButton;

	/**
	 * The delete action {@link Button}
	 * @implNote part of {@link #styleableButtons}
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private Button deleteButton;

	/**
	 * The quit action {@link Button}
	 * @implNote part of {@link #styleableButtons}
	 * @see #initialize(URL, ResourceBundle)
	 */
	@FXML
	private Button quitButton;

	/**
	 * {@link RadioMenuItem} used to set {@link Integer}s as number types
	 * in expressions
	 * @see #initialize(URL, ResourceBundle)
	 * @see #onSelectNumberType(ActionEvent)
	 */
	@FXML
	private RadioMenuItem integersRadioMenuItem;

	/**
	 * {@link RadioMenuItem} used to set {@link Float}s as number types
	 * in expressions
	 * @see #initialize(URL, ResourceBundle)
	 * @see #onSelectNumberType(ActionEvent)
	 */
	@FXML
	private RadioMenuItem floatsRadiomenuItem;

	/**
	 * {@link RadioMenuItem} used to set {@link Double}s as number types
	 * in expressions
	 * @see #initialize(URL, ResourceBundle)
	 * @see #onSelectNumberType(ActionEvent)
	 */
	@FXML
	private RadioMenuItem doublesRadiomenuItem;

	/**
	 * {@link RadioMenuItem} used to set {@link #styleableButtons}
	 * with graphics only: {@link ContentDisplay#GRAPHIC_ONLY}
	 * @see #onDisplayButtonsWithStyle(ActionEvent)
	 */
	@FXML
	private RadioMenuItem graphicsOnlyRadiomenuItem;

	/**
	 * {@link RadioMenuItem} used to set {@link #styleableButtons}
	 * with text and graphics: {@link ContentDisplay#LEFT}
	 * @see #onDisplayButtonsWithStyle(ActionEvent)
	 */
	@FXML
	private RadioMenuItem textAndGraphicsRadiomenuItem;

	/**
	 * {@link RadioMenuItem} used to set {@link #styleableButtons}
	 * with text only: {@link ContentDisplay#TEXT_ONLY}
	 * @see #onDisplayButtonsWithStyle(ActionEvent)
	 */
	@FXML
	private RadioMenuItem textOnlyRadiomenuItem;

	/**
	 * {@link RadioMenuItem} used to set {@link #loggerLevel} to
	 * {@link Level#INFO}
	 * @see #onSetLoggerLevelAction(ActionEvent)
	 */
	@FXML
	private RadioMenuItem infoWarningSevereRadiomenuItem;

	/**
	 * {@link RadioMenuItem} used to set {@link #loggerLevel} to
	 * {@link Level#WARNING}
	 * @see #onSetLoggerLevelAction(ActionEvent)
	 */
	@FXML
	private RadioMenuItem warningSevereRadiomenuItem;

	/**
	 * {@link RadioMenuItem} used to set {@link #loggerLevel} to
	 * {@link Level#SEVERE}
	 * @see #onSetLoggerLevelAction(ActionEvent)
	 */
	@FXML
	private RadioMenuItem severeRadiomenuItem;

	/**
	 * {@link RadioMenuItem} used to set {@link #loggerLevel} to
	 * {@link Level#OFF}
	 * @see #onSetLoggerLevelAction(ActionEvent)
	 */
	@FXML
	private RadioMenuItem offRadiomenuItem;


	// -------------------------------------------------------------------------
	// Class constants
	// -------------------------------------------------------------------------
	/**
	 * {@link Preferences} key for log level {@link #loggerLevel}
	 * @see #loadPreferences()
	 * @see #savePreferences(Stage)
	 */
	private final static String PREF_LOG_LEVEL = "log_level";
	/**
	 * {@link Preferences} key for {@link #styleableButtons}
	 * {@link #contentDisplay} style
	 * @see #loadPreferences()
	 * @see #savePreferences(Stage)
	 */
	private final static String PREF_BUTTONS_STYLE = "buttons_style";
	/**
	 * {@link Preferences} key for stage width
	 * @see #savePreferences(Stage)
	 */
	private final static String PREF_WIDTH = MainFX.PREF_WIDTH;
	/**
	 * {@link Preferences} key for stage height
	 * @see #savePreferences(Stage)
	 */
	private final static String PREF_HEIGHT = MainFX.PREF_HEIGHT;

	/**
	 * {@link Preferences} key for number types in expressions
	 * @see #loadPreferences()
	 * @see #savePreferences(Stage)
	 */
	private final static String PREF_NUMBER_TYPE = "number_type";

	/**
	 * Default constructor.
	 * Initialize all non FXML attributes
	 */
	public Controller()
	{
		/*
		 * Can't get parent logger now, so standalone logger.
		 * Parent logger will be set in Main.
		 */
		logger = LoggerFactory.getParentLogger(getClass(), null, loggerLevel);

		/*
		 * load preferences initializes
		 * 	- loggerLevel
		 * 	- contentDisplay
		 * 	- specimen (used to create #expressionsModel)
		 */
		loadPreferences();

		expressionsModel = new ExpressionsModel<>(specimen.get(), logger);

		expressionsDisplayList = FXCollections.<ExpressionDisplay<Number>>observableArrayList();

		variablesDisplayList = FXCollections.<VariableDisplay<Number>>observableArrayList();

		styleableButtons = new HashSet<Labeled>();
	}

	/**
	 * Called to initialize a controller after its root element has been
	 * completely processed.
	 * @param location The location used to resolve relative paths for the root
	 * object, or
	 * {@code null} if the location is not known.
	 * @param resources The resources used to localize the root object, or
	 * {@code null} if the root object was not localized.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		// --------------------------------------------------------------------
		// Initialize FXML related attributes
		// --------------------------------------------------------------------
		/*
		 * DONE Based on #specimen value set during #loadPreferences
		 * set the corresponding RadioMenuItem to selected:
		 * 	- Integer --> integersRadioMenuItem
		 * 	- Float --> floatsRadiomenuItem
		 * 	- Double --> doublesRadiomenuItem
		 */
		Number value = specimen.get();
		if (value instanceof Integer)
		{
			integersRadioMenuItem.setSelected(true);
		}
		if (value instanceof Float)
		{
			floatsRadiomenuItem.setSelected(true);
		}
		if (value instanceof Double)
		{
			doublesRadiomenuItem.setSelected(true);
		}
		/*
		 * DONE Adds a change listener to specimen property in order to
		 * select the right RadioMenuItem whenever this property changes.
		 */
		specimen.addListener((observable, oldValue, newValue) -> {
			if (newValue instanceof Integer)
			{
				integersRadioMenuItem.setSelected(true);
			}
			if (newValue instanceof Float)
			{
				floatsRadiomenuItem.setSelected(true);
			}
			if (newValue instanceof Double)
			{
				doublesRadiomenuItem.setSelected(true);
			}
		});
		/*
		 * Now that controller's specimen have been used to initiate Expressions
		 * model specimen, we'll follow model changes in the controller:
		 * TODO Bind Controller's specimen property to expressions model's property
		 * so that changes in the model can be reflected in this controller.
		 */

		/*
		 * Based on #contentDisplay value set during #loadPreferences
		 * TODO Set the corresponding RadioMenuItem to selected among :
		 * 	- graphicsOnlyRadiomenuItem
		 * 	- textAndGraphicsRadiomenuItem
		 * 	- textOnlyRadiomenuItem
		 */

		/*
		 * TODO Add all buttons to be stylised to #styleableButtons
		 * 	- addButton
		 * 	- openButton
		 * 	- saveButton
		 * 	- selectAllButton
		 * 	- selectNoneButton
		 * 	- deleteButton
		 * 	- quitButton
		 */

		/*
		 * Based on #contentDisplay value set during #loadPreferences
		 * TODO apply it to #styleableButtons using onDisplayButtonsWithStyle
		 * callback
		 */

		/*
		 * TODO Setup terminalTypeCombobox
		 * 	- Set terminalTypeCombobox items by creating an ObservableList
		 * 	from TerminalType.all() using FXCollections factory methods
		 * 	- Set terminalTypeCombobox's selection model's selected item as
		 * 	TerminalType.ALL
		 */

		/*
		 * TODO Setup binaryTypeCombobox
		 * 	- Set binaryTypeCombobox items by creating an ObservableList
		 * 	from BinaryOperatorRules.all() using FXCollections factory methods
		 * 	- Set binaryTypeCombobox's selection model's selected item as
		 * 	BinaryOperatorRules.ANY
		 */

		/*
		 * Setup #expressionsTableView ...
		 */
		/*
		 * DONE Register a change listener on #expressionsModel expressions list to
		 * update #expressionsDisplayList
		 */
		expressionsModel.getExpressions().addListener(
			new ListChangeListener<Expression<Number>>()
		    {
			    @Override
			    public void onChanged(Change<? extends Expression<Number>> change)
			    {
			    	/*
			    	 * Ignore change's details, just rebuild the whole list
			    	 */
			    	expressionsDisplayList.clear();
			    	for (Expression<Number> expression : change.getList())
			    	{
			    		expressionsDisplayList.add(new ExpressionDisplay<Number>(expression));
			    	}
			    }
		    });

		/*
		 * Setup #expressionsContentColumn CellValueFactory as ExpressionDisplay.contentProperty()
		 */
		expressionsContentColumn.setCellValueFactory(cellData ->
			cellData.getValue().contentProperty());
		/*
		 * Setup #expressionsValueColumn CellValueFactory as ExpressionDisplay.valueProperty()
		 */
		expressionsValueColumn.setCellValueFactory(cellData ->
			cellData.getValue().valueProperty());
		/*
		 * Set the items of #expressionsTableView as #expressionsDisplayList
		 */
		expressionsTableView.setItems(expressionsDisplayList);
		/*
		 * TODO Enable multiple selections on #expressionsTableView
		 */

		/*
		 * Set a TextFieldTableCell as a CellFactory on expressionsContentColumn
		 * in order to be able to edit the cells in this column
		 */
		expressionsContentColumn.setCellFactory(TextFieldTableCell
		    .<ExpressionDisplay<Number>> forTableColumn());

		/*
		 * Setup #variablesTableView ...
		 */
		/*
		 * Register a change listener on #expressionsModel variables to update
		 * #variablesDisplayList
		 */
		expressionsModel.getVariables().addListener(
			new MapChangeListener<String, Optional<? extends Number>>()
		    {
				@Override
				public void onChanged(Change<? extends String, ? extends Optional<? extends Number>> change)
				{
			    	/*
			    	 * Ignore change's details, just rebuild the whole list
			    	 */
					variablesDisplayList.clear();
					ObservableMap<? extends String, ? extends Optional<? extends Number>> map = change.getMap();
					Set<? extends String> keys = map.keySet();
					for (String key : keys)
					{
						variablesDisplayList.add(new VariableDisplay<>(key));
					}
				}
		    });
		/*
		 * Setup #variablesContentColumn CellValueFactory as VariableDisplay.contentProperty()
		 */
		variablesContentColumn.setCellValueFactory(cellData->
			cellData.getValue().contentProperty());
		/*
		 * Setup #variablesValueColumn CellValueFactory as VariableDisplay.valueProperty()
		 */
		variablesValueColumn.setCellValueFactory(cellData ->
			cellData.getValue().valueProperty());
		/*
		 * Set the items of #variablesTableView as #variablesDisplayList
		 */
		variablesTableView.setItems(variablesDisplayList);

		/*
		 * TODO Binds #expressionsTreeView's root property to model's root tree item
		 */

		/*
		 * TODO Bind Model's filtering properties to UI filtering properties
		 * 	- model's operatorFiltering property --> binaryTypeCombobox's value property
		 * 	- model's operandFiltering property --> terminalTypeCombobox's value property
		 * 	- models's nameFilteringProperty --> searchField's text property
		 */

		/*
		 * expressionsModel features a hasFileProperty() which is true if a file
		 * has been loaded.
		 * #saveButton, #saveMenuItem and #revertMenuItem should be disabled
		 * if no file has been loaded yet.
		 * All 3 feature a feature a disable property
		 * TODO Bind the disable property of these 3 elements to the __inverse__
		 * of expressionsModel hasFileProperty using BooleanExpression#not()
		 */
	}

	// ------------------------------------------------------------------------
	// FXML Callbacks
	// ------------------------------------------------------------------------

	/**
	 * Action to quit the application
	 * @param event Event associated with this action
	 */
	@FXML
	public void onQuitAction(ActionEvent event)
	{
		logger.info("Quit Action triggered ...");
		messageLabel.setText("Bye");
		quitActionImpl(event);
	}

	/**
	 * Action to parse expressions in {@link #inputField} and add them to
	 * the data model
	 * @param event Event associated with this action
	 * @see #onClearFilterAction(ActionEvent)
	 */
	@FXML
	public void onAddAction(ActionEvent event)
	{
		String title = "Add Action";
		logger.info(title + " triggered ...");

		/*
		 * TODO Clear filters
		 */

		/*
		 * TODO Add expressions in #inputField to #expressionsModel
		 * Get #inputField's content
		 * Parse this new context in #expressionsModel
		 * Set a message in #messageLabel indicating expression have been added
		 * (or not)
		 * Clear #inputField
		 * If a ParserException occurs show an Alert ERROR box indicating a
		 * parser error occurred.
		 * If context is empty set a message in #messageLabel
		 */
	}

	/**
	 * Action triggered when edition is completed on a cell of
	 * the first column {@link #expressionsContentColumn}
	 * of {@link #expressionsTableView}
	 * @param event Cell edit event associated with this action
	 */
	@FXML
	public void onEditCommitAction(CellEditEvent<ExpressionDisplay<? extends Number>, String> event)
	{
		String title = "Edit action";
		logger.info(title + " commited on " + event);
		ExpressionDisplay<? extends Number> row = event.getRowValue();
		Expression<? extends Number> expression = row.getExpression();
		String oldContext = event.getOldValue();
		String newContext = event.getNewValue();

		/**
		 * TODO If context haven't changed set message in to #messageLabel
		 * and return
		 */

		/*
		 * If context has changed then
		 * TODO try to reparse the expression with newContext
		 * If a ParserException occurs show an Alert ERROR box indicating a
		 * parser error occurred.
		 * On all other possible exceptions just show a logger.severe(...)
		 * message
		 */
	}

	/**
	 * Action to delete selected expressions.
	 * These expressions can be selected either in {@link #expressionsTreeView}
	 * or {@link #expressionsTableView} : Both should be checked for selections.
	 * @param event Event associated with this action
	 */
	@FXML
	public void onDeleteSelectedAction(ActionEvent event)
	{
		logger.info("Delete Selected Action triggered ...");
		/*
		 * TODO Build list of expressions to remove from #expressionsModel
		 * based on #expressionsTableView's selection model's selected items
		 */

		/*
		 * TODO Clear #expressionsTableView's selection model before removing
		 * expressions from model (in order to avoid inconsistencies)
		 */

		/**
		 * TODO Actually remove expressions from #expressionsModel
		 */

		/*
		 * TODO If expressions have been removed then display "Expressions deleted"
		 * in messageLabel
		 */
	}

	/**
	 * Action to select all expressions in {@link #expressionsTableView}
	 * @param event Event associated with this action
	 */
	@FXML
	public void onSelectAllAction(ActionEvent event)
	{
		logger.info("Select All Action triggered ...");
		/*
		 * TODO Select all lines of column #expressionsContentColumn
		 * And none of #expressionsValueColumn
		 * Show message in #messageLabel all expressions selected
		 */
	}

	/**
	 * Action to deselect all expressions in {@link #expressionsTableView}
	 * @param event Event associated with this action
	 */
	@FXML
	public void onDeselectAllAction(ActionEvent event)
	{
		logger.info("Deselect All Action triggered ...");
		messageLabel.setText("All expressions de-selected");
		/*
		 * TODO Clear selection in #expressionsTableView
		 */
	}

	/**
	 * Action to clear all expressions and start over
	 * @param event Event associated with this action
	 * @see #onClearFilterAction(ActionEvent)
	 */
	@FXML
	public void onNewAction(ActionEvent event)
	{
		logger.info("New Action triggered ...");
		/*
		 * TODO Restart from blank slate
		 * Clear filters
		 * Reset file on #expressionsModel
		 * Clear all expressions in #expressionsModel
		 * Set a message in #messageLabel indicating new action performed
		 */
	}


	/**
	 * Action to open a text file and parse expressions from it, then replace
	 * expressions in {@link #expressionsModel} with parsed expressions
	 * @param event Event associated with this action
	 * @see #onClearFilterAction(ActionEvent)
	 * @see #chooseAndLoadFile(boolean)
	 */
	@FXML
	public void onOpenAction(ActionEvent event)
	{
		logger.info("Open Action triggered ...");
		/*
		 * TODO Open a file
		 * Clear filters
		 * Choose and Load a File (without appending expressions)
		 */
	}

	/**
	 * Action to open a text file and parse expressions from it, then append
	 * the parsed expressions to {@link #expressionsModel}
	 * @param event Event associated with this action
	 * @see #onClearFilterAction(ActionEvent)
	 * @see #chooseAndLoadFile(boolean)
	 */
	@FXML
	public void onAppendAction(ActionEvent event)
	{
		logger.info("Append Action triggered ...");
		/*
		 * TODO Append a file
		 * Clear filters
		 * Choose and Load a File (with appending expressions)
		 */
	}

	/**
	 * Action to save all expressions to opened file (if any).
	 * If there is no opened file then revert to save as action requiring a
	 * file name.
	 * @param event Event associated with this action
	 * @see #saveFile(File)
	 * @see #onSaveAsAction(ActionEvent)
	 */
	@FXML
	public void onSaveAction(ActionEvent event)
	{
		logger.info("Undo Action triggered ...");
		/*
		 * TODO If there is no file opened then trigger save as action
		 * (with same event) and return
		 */

		/*
		 * TODO Save expressions to file provided by expressionsModel#getFile()
		 */
	}

	/**
	 * Action to save all expressions in a chosen file.
	 * Opens a {@link FileChooser} dialog to select a file and then triggers
	 * the {@link #onSaveAction(ActionEvent)} (with a null event).
	 * @param event Event associated with this action
	 * @see #saveFile(File)
	 */
	@FXML
	public void onSaveAsAction(ActionEvent event)
	{
		logger.info("Save as action triggered ...");
		File currentFile = expressionsModel.getFile();
		/*
		 * TODO Choose a file and save to
		 * Choose a file to save to with a FileChooser
		 * If a file is selected then save expressions to this file
		 * If no file is selected then just return
		 */
		File selectedFile = null;

		saveFile(selectedFile);
	}

	/**
	 * Action to revert to last opened file state :
	 * Clears actual data model and reload last opened file (if any)
	 * @implNote UI elements triggering this action should be disabled until a
	 * file is actually loaded
	 * @param event Event associated with this action
	 * @see #loadFile(File, boolean)
	 */
	@FXML
	public void onRevertAction(ActionEvent event)
	{
		String title = "Revert Action";
		logger.info(title + " triggered ...");
		/*
		 * TODO Revert to file initial state
		 * Clear filters
		 * Clear expressions model
		 * (re)Load file from expressions model
		 * If #expressionsModel has no file then show an Alert WARNING box
		 * indicating no file to revert to
		 */
	}

	/**
	 * Action to set number type on Data model depending on event source.
	 * {@link #integersRadioMenuItem} should set expressions numbers type to
	 * {@link Integer}s.
	 * {@link #floatsRadiomenuItem} should set expressions numbers type to
	 * {@link Float}s.
	 * {@link #doublesRadiomenuItem} should set expressions numbers type to
	 * {@link Double}s.
	 * Should trigger a re-parsing of all expressions
	 * @param event Event associated with this action
	 */
	public void onSelectNumberType(ActionEvent event)
	{
		logger.info("Select Number type action triggered");
		/*
		 * TODO Get all expressions model's content into a text buffer
		 * (to be able to reparse it after changing number type)
		 */
		String buffer = expressionsModel.toString();
		// Record current speciment number (to compare with new one later)
		Number oldSpecimen = specimen.get();
		// Note : specimen is bound to model's specimen and can't be changed
		Number newSpecimen = null;
		/*
		 * TODO Depending on event source set newSpecimen to the right number type
		 * and log the action
		 */
		Object source = event.getSource();

		/*
		 * TODO If new specimen is different from old one then
		 * Clear current model
		 * Set new number type to expressions model
		 */

		/*
		 * TODO Parse all expressions in text buffer
		 * (with possible ParserException)
		 * Set message in #messageLabel
		 */
	}

	/**
	 * Action to update expression filtering by taking into account
	 * {@link #searchField}, {@link #terminalTypeCombobox} and
	 * {@link #binaryTypeCombobox}
	 * @param event Event associated with this action
	 */
	public void onFilterAction(ActionEvent event)
	{
		logger.info("Filtering action triggered");
		/*
		 * All properties related to filtering have already been bound
		 * in #initialize method
		 * So we just need to log the action and
		 * TODO set a message in #messageLabel indicating what type of filtering
		 * has just been applied
		 */
		Object source = event.getSource();
		String message = null;



		logger.info(message);
		messageLabel.setText(message);
	}

	/**
	 * Action to clear all expression filtering by clearing
	 * {@link #searchField}, and resetting {@link #terminalTypeCombobox} and
	 * {@link #binaryTypeCombobox} to default values
	 * @param event Event associated with this action
	 */
	public void onClearFilterAction(ActionEvent event)
	{
		logger.info("Clear filtering action triggered");
		/*
		 * TODO Clear or reset all filtering properties
		 * 	- searchField cleared
		 * 	- terminalTypeCombobox to default value (first one)
		 * 	- binaryTypeCombobox to default value (first one)
		 * 	- messageLabel cleared
		 */
	}

	/**
	 * Action to show {@link #styleableButtons} buttons with specific
	 * {@link ContentDisplay} on
	 * {@link #contentDisplay} depending on event source.
	 * If source is {@link #graphicsOnlyRadiomenuItem} then set
	 * {@link ContentDisplay#GRAPHIC_ONLY}
	 * If source is {@link #textAndGraphicsRadiomenuItem} then set
	 * {@link ContentDisplay#LEFT} or {@link ContentDisplay#RIGHT} or
	 * {@link ContentDisplay#CENTER} as you prefer.
	 * If source is {@link #textOnlyRadiomenuItem} then set
	 * {@link ContentDisplay#TEXT_ONLY}
	 * Then apply {@link #contentDisplay} to all {@link #styleableButtons}
	 * @param event Event associated with this action
	 */
	@FXML
	public void onDisplayButtonsWithStyle(ActionEvent event)
	{
		logger.info("Display Buttons with Graphics only action triggered");
		/*
		 * TODO Set contentDisplay to the right value depending on event source
		 * - graphicsOnlyRadiomenuItem --> ContentDisplay.GRAPHIC_ONLY
		 * - textAndGraphicsRadiomenuItem --> ContentDisplay.LEFT
		 * - textOnlyRadiomenuItem --> ContentDisplay.TEXT_ONLY
		 * - null event --> should not crash since we might use it during #initialize
		 */
		if (event != null)
		{
			Object source = event.getSource();
		}

		/*
		 * TODO Set selected content display to all styleable buttons
		 */

		/*
		 * TODO Set message in #messageLabel indicating content display
		 */
	}

	/**
	 * Action to set {@link #logger} level depending on event source.
	 * @param event Event associated with this action
	 */
	@FXML
	public void onSetLoggerLevelAction(ActionEvent event)
	{
		logger.info("Set Logger level action");
		Object source = event.getSource();
		/*
		 * TODO Set logger level depending on event source
		 * - infoWarningSevereRadiomenuItem --> Level.INFO
		 * - warningSevereRadiomenuItem --> Level.WARNING
		 * - severeRadiomenuItem --> Level.SEVERE
		 * - offRadiomenuItem --> Level.OFF
		 */
	}

	/**
	 * Action to show simple dialog presenting the application
	 * @param event Event associated with this action
	 */
	@FXML
	public void onAboutAction(ActionEvent event)
	{
		logger.info("About action triggered");
		ImageView icon = new ImageView(IconFactory.getLargeIcon("math"));
		/*
		 * TODO Show an Alert INFORMATION Box with the following content:
		 * 	- Title: "About ..."
		 * 	- Header: "Expressions Manager v2.0"
		 * 	- Content: "A simple application to manage math expressions"
		 * 	- Icon: icon
		 */
		String header = "Expressions Manager";
	}


	// -------------------------------------------------------------------------
	// Protected utility methods: Used exclusively from Main
	// -------------------------------------------------------------------------

	/**
	 * Set parent stage (so it can be closed on quit)
	 * @param stage The new parent stage to set
	 * @apiNote Protected method so it can be called from Main
	 */
	protected void setParentStage(Stage stage)
	{
		parentStage = stage;
	}

	/**
	 * Sets parent logger
	 * @param logger The new parent logger
	 * @apiNote Protected method so it can be called from Main
	 */
	protected void setParentLogger(Logger logger)
	{
		this.logger.setParent(logger);
	}

	// -------------------------------------------------------------------------
	// Private utility methods: to be used exclusively within Controller
	// -------------------------------------------------------------------------
	/**
	 * Implementation of the quit logic.
	 * <ul>
	 * 	<li>Saves preferences.</li>
	 * 	<li>Closes the stage.</li>
	 * </ul>
	 * @param event The event passed to this callback (either {@link ActionEvent}
	 * or {@link WindowEvent} depending on what triggered this action).
	 * @apiNote Protected method so it can be called from Main
	 */
	protected void quitActionImpl(Event event)
	{
		/*
		 * 	- closes the stage by
		 * 		- getting the stage from source if event is a WindowEvent
		 * 		- getting the stage from #parentStage or otherwise if event is
		 * 		an ActionEvent
		 */
		logger.info("Quit action triggered");

		Object source = event.getSource();
		Stage stage = null;

		if (event instanceof WindowEvent)
		{
			// Stage is the source
			stage = (Stage) source;
		}
		else if (event instanceof ActionEvent)
		{
			if (parentStage != null)
			{
				// We already have a registered stage
				stage = parentStage;
			}
			else
			{
				// Search for the stage
				if (source instanceof Button)
				{
					Button sourceButton = (Button) source;
					stage = (Stage) sourceButton.getScene().getWindow();
				}
				else
				{
					logger.warning("Unable to get Stage to close from: "
					    + source.getClass().getSimpleName());
				}
			}
		}
		else
		{
			logger.warning("Unknown event source: " + event.getSource());
		}

		if (stage != null)
		{
			/*
			 * Save preferences
			 */
			savePreferences(stage);
			/*
			 * Closes stage
			 */
			stage.close();
		}
		else
		{
			logger.warning("Window not closed");
		}
		clearMessage();
	}

	/**
	 * Clear message at the bottom of UI
	 * To be used in every any callback which doesn't show info message
	 */
	private void clearMessage()
	{
		messageLabel.setText(null);
	}
	/**
	 * Opens a file dialog to chose a file to open or append
	 * @param append If true current expressions in {@link #expressionsModel} are
	 * not cleared and expressions from loaded file are added (if possible) to
	 * current expressions.
	 * @see #loadFile(File, boolean)
	 */
	private void chooseAndLoadFile(boolean append)
	{
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(append ? "Append File" : "Load File");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Text Files","*.txt"),
		                                         new ExtensionFilter("All Files","*.*"));
		File selectedFile = fileChooser.showOpenDialog(parentStage);
		if (selectedFile == null)
		{
			return;
		}

		loadFile(selectedFile, append);
	}

	/**
	 * Loads provided file with {@link #expressionsModel}
	 * @param file The file to load
	 * @param append If true current expressions in {@link #expressionsModel}
	 * are not cleared and expressions from loaded file are added (if possible)
	 * to current contacts.
	 * @return true if provided file has been loaded, false otherwise
	 * @apiNote Protected method so it can be called from Main
	 */
	private boolean loadFile(File file, boolean append)
	{
		boolean loaded = false;
		try
		{
			loaded = expressionsModel.load(file, append);
			if (!loaded)
			{
				logger.warning("Unable to load " + file);
				return false;
			}
			String message = String.format("File %s %s%s",
			                               file.getName(),
			                               loaded ? "" : "not ",
			                               append ? "appended" : "loaded");
			logger.info(message);
			messageLabel.setText(message);
		}
		catch (NullPointerException e)
		{
			logger.warning("Null file: " + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			logger.severe("I/O error: " + e.getLocalizedMessage());
		}
		catch (ParserException e)
		{
			logger.severe("Parser error: " + e.getLocalizedMessage());
		}

		return loaded;
	}

	/**
	 * Saves {@link #expressionsModel} expressions to provided file
	 * @param file the file to write to
	 * @return true if the file has been written, false otherwise
	 */
	private boolean saveFile(File file)
	{
		boolean saved = false;

		try
		{
			saved = expressionsModel.save(file);
			if (!saved)
			{
				logger.warning("Unable to save " + file);
				return false;
			}
			String message = String.format("File %s %ssaved",
			                               file.getName(),
			                               saved ? "" : "not ");
			logger.info(message);
			messageLabel.setText(message);
		}
		catch (IOException e)
		{
			logger.severe("I/O error: " + e.getLocalizedMessage());
		}

		return saved;
	}

	/**
	 * Load registered preferences or set default values if no preferences
	 * are found
	 * @throws NullPointerException If {@link #logger} is null
	 * or when manager is null whenever a file can be loaded
	 * @see #PREF_BUTTONS_STYLE
	 * @see #PREF_LOG_LEVEL
	 * @see #PREF_WIDTH
	 * @see #PREF_HEIGHT
	 */
	private void loadPreferences() throws NullPointerException
	{
		Preferences preferences = Preferences.userNodeForPackage(getClass());
		List<String> prefKeys = null;
		try
		{
			prefKeys = Arrays.asList(preferences.keys());
		}
		catch (BackingStoreException e)
		{
			logger
			    .severe("Backing Store exception retrieving preferences keys, "
			        + e.getLocalizedMessage());
		}

		/*
		 * If preferences can provide a logger level set #logger's #loggerLevel
		 */
		if (prefKeys.contains(PREF_LOG_LEVEL))
		{
			String levelName = preferences.get(PREF_LOG_LEVEL, "INFO");
			try
			{
				loggerLevel = Level.parse(levelName);
				logger.info("Preferences Logger level = " + loggerLevel.toString());
			}
			catch (IllegalArgumentException e)
			{
				loggerLevel = Level.INFO;
			}
			Objects.requireNonNull(logger);
			logger.setLevel(loggerLevel);
		}
		else
		{
			// Default value
			loggerLevel = Level.INFO;
		}

		/*
		 * If preferences can provide a button display then set it
		 */
		if (prefKeys.contains(PREF_BUTTONS_STYLE))
		{
			int prefStyleIndex = preferences.getInt(PREF_BUTTONS_STYLE, 0);
			switch (prefStyleIndex)
			{
				case 0:
					contentDisplay = ContentDisplay.TEXT_ONLY;
					logger.info("Preferences buttons style = Text only");
					break;
				case 1:
					contentDisplay = ContentDisplay.GRAPHIC_ONLY;
					logger.info("Preferences buttons style = Graphics only");
					break;
				case 2:
					contentDisplay = ContentDisplay.LEFT;
					logger.info("Preferences buttons style = Text + Graphics");
					break;
			}
		}
		else
		{
			// Default value
			contentDisplay = ContentDisplay.GRAPHIC_ONLY;
			logger.info("Preferences default buttons style = Text + Graphics");
		}

		if (prefKeys.contains(PREF_NUMBER_TYPE))
		{
			int prefNumberIndex = preferences.getInt(PREF_NUMBER_TYPE, 0);
			switch(prefNumberIndex)
			{
				case 0:
					specimen = new SimpleObjectProperty<Number>(Integer.valueOf(0));
					logger.info("Preferences number type = Integer");
					break;
				case 1:
					specimen = new SimpleObjectProperty<Number>(Float.valueOf(0.0f));
					logger.info("Preferences number type = Float");
					break;
				case 2:
					specimen = new SimpleObjectProperty<Number>(Double.valueOf(0.0));
					logger.info("Preferences number type = Double");
					break;
			}
		}
		else
		{
			// Default value
			specimen =  new SimpleObjectProperty<Number>(Double.valueOf(0.0));
			logger.info("Preferences default number type = Double");
		}

		/*
		 * Stage width and height should have been set in Main class
		 */
	}

	/**
	 * Save registered preferences
	 * @param stage The stage of the app in order to save width and height to
	 * preferences
	 * @throws NullPointerException If manager is null
	 * @see #PREF_BUTTONS_STYLE
	 * @see #PREF_LOG_LEVEL
	 * @see #PREF_WIDTH
	 * @see #PREF_HEIGHT
	 */
	private void savePreferences(Stage stage) throws NullPointerException
	{
		Preferences preferences = Preferences.userNodeForPackage(getClass());

		logger.info("Save log level preference = " + loggerLevel);
		preferences.put(PREF_LOG_LEVEL, loggerLevel.toString());

		double width = stage.getWidth();
		logger.info("Save width preference = " + width);
		preferences.putDouble(PREF_WIDTH, width);

		double height = stage.getHeight();
		logger.info("Save height preference = " + height);
		preferences.putDouble(PREF_HEIGHT, height);

		int contentDisplayIndex;
		switch (contentDisplay)
		{
			case GRAPHIC_ONLY:
				logger.info("Save Buttons display style preference = Graphics Only");
				contentDisplayIndex = 1;
				break;
			case LEFT:
				logger.info("Save Buttons display style preference = Text + Graphics");
				contentDisplayIndex = 2;
				break;
			case TEXT_ONLY:
			default:
				logger.info("Save Buttons display style preference = Text Only");
				contentDisplayIndex = 0;
				break;
		}
		preferences.putInt(PREF_BUTTONS_STYLE, contentDisplayIndex);

		Number value = specimen.get();
		if (value instanceof Integer)
		{
			logger.info("Save number type preference = Integers");
			preferences.putInt(PREF_NUMBER_TYPE, 0);
		}
		if (value instanceof Float)
		{
			logger.info("Save number type preference = Floats");
			preferences.putInt(PREF_NUMBER_TYPE, 1);
		}
		if (value instanceof Double)
		{
			logger.info("Save number type preference = Doubles");
			preferences.putInt(PREF_NUMBER_TYPE, 2);
		}
	}
}
