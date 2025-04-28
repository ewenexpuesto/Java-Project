package expressions.models;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import expressions.Expression;
import expressions.binary.AssignmentExpression;
import expressions.binary.BinaryExpression;
import expressions.binary.BinaryOperatorRules;
import expressions.special.GroupExpression;
import expressions.terminal.TerminalExpression;
import expressions.terminal.TerminalType;
import expressions.terminal.VariableExpression;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import logger.LoggerFactory;
import parser.ExpressionParser;
import parser.exceptions.ParserException;
import parser.exceptions.UnsupportedNumberClassException;

/**
 * Data Model containing all expressions and used as the Model part
 * in an MVC Framework
 * @param <E> the type of numbers used in expressions
 */
public class ExpressionsModel<E extends Number>
{
	/**
	 * Logger to log messages
	 */
	private Logger logger;

	/**
	 * Number specimen property
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #getSpecimen()
	 * @see #load(File, boolean)
	 * @see #save(File)
	 * @see #setNumberType(Number)
	 * @see #specimenProperty()
	 */
	private ObjectProperty<E> specimen;

	/**
	 * The root expression of all expressions in {@link #expressions}
	 * @impleNote This expression can be used as a root of all expressions
	 * within a tree.
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #refreshRoot()
	 */
	private GroupExpression<E> rootExpression;

	/**
	 * The root item of the tree to display expressions.
	 * Contains the {@link #rootExpression} containing all {@link #expressions}.
	 * @implNote this item can be bound to {@link TreeView#rootProperty()}
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #getRootItem()
	 * @see #refreshRoot()
	 * @see #rootItemProperty()
	 * @see #setRootItem(ExpressionTreeItem)
	 */
	private ObjectProperty<TreeItem<Expression<E>>> rootItem;

	/**
	 * The observable list of all expressions.
	 * @see #cleanupVariablesMap()
	 * @see #clear()
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #load(File, boolean)
	 * @see #merge(List)
	 * @see #refreshRoot()
	 * @see #remove(Expression)
	 * @see #reparse(Expression, String)
	 * @see #save(File)
	 * @see #toString()
	 */
	private ObservableList<Expression<E>> expressions;

	/**
	 * Map relating variables names to {@link Optional} values.
	 * @implNote should be build upon {@link VariableExpression#getValues()}
	 * @see #cleanupVariablesMap()
	 * @see #clear()
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #getVariables()
	 * @see #load(File, boolean)
	 * @see #save(File)
	 * @see #toString()
	 */
	private ObservableMap<String, Optional<? extends Number>> variablesMap;

	/**
	 * List of {@link #expressions} filtered with {@link #predicate}
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #getExpressions()
	 * @see #setPredicate(BinaryOperatorRules, TerminalType, String)
	 */
	private FilteredList<Expression<E>> filteredExpressions;

	/**
	 * Predicate to apply on {@link #expressions} to obtain
	 * {@link #filteredExpressions}
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #setPredicate(BinaryOperatorRules, TerminalType, String)
	 */
	private Predicate<Expression<E>> predicate;

	/**
	 * Property used to filter {@link BinaryExpression}s based on
	 * {@link BinaryOperatorRules}
	 * @implNote this property can typically be bound to a
	 * {@link javafx.scene.control.ComboBox#valueProperty()}
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #getOperatorFiltering()
	 * @see #operatorFilteringProperty()
	 * @see #setOperatorFiltering(BinaryOperatorRules)
	 */
	private ObjectProperty<BinaryOperatorRules> operatorFiltering;

	/**
	 * Property used to filter {@link TerminalExpression}s based on
	 * {@link TerminalType}
	 * @implNote this property can typically be bound to a
	 * {@link javafx.scene.control.ComboBox#valueProperty()}
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #getOperandFiltering()
	 * @see #operandFilteringProperty()
	 * @see #setOperandFiltering(TerminalType)
	 */
	private ObjectProperty<TerminalType> operandFiltering;

	/**
	 * Property used to filter expressions containing specific names or values
	 * @implNote this property can typically be bound to a
	 * {@link javafx.scene.control.TextField#textProperty()}
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #getNameFiltering()
	 * @see #nameFilteringProperty()
	 * @see #setNameFiltering(String)
	 */
	private StringProperty nameFiltering;

	/**
	 * The parser used to parse expressions
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #parse(String)
	 * @see #reparse(Expression, String)
	 * @see #setNumberType(Number)
	 */
	private ExpressionParser<E> parser;

	/**
	 * File used to load and/or save expressions from/to
	 * @implSpec might be null if no file is used
	 * @see #getFile()
	 * @see #load(File, boolean)
	 * @see #resetFile()
	 * @see #save(File)
	 */
	private File file;

	/**
	 * Property indicating this model has a non null file to save to or read
	 * from.
	 * @implSpec This property is initialized to false and become true when
	 * {@link #load(File, boolean)} or {@link #save(File)} are successfully
	 * completed.
	 * @implNote the inverse of this property ({@link BooleanExpression#not()})
	 * can be bound to a
	 * {@link javafx.scene.control.Button#disabledProperty()} for instance to
	 * indicate that saving is not allowed until a file is chosen with "save as
	 * ..." for instance.
	 * @implNote {@link ReadOnlyBooleanWrapper} allows to be set internally
	 * but can provide a {@link ReadOnlyBooleanProperty} for external usage
	 * where set operation shall be forbidden.
	 * @see #ExpressionsModel(Number, Logger)
	 * @see #hasFile()
	 * @see #hasFileProperty()
	 * @see #load(File, boolean)
	 * @see #resetFile()
	 * @see #save(File)
	 */
	private ReadOnlyBooleanWrapper hasFile;

	/**
	 * Constructor from number specimen and parent logger
	 * @param specimen a number specimen
	 * @param parentLogger The parent logger
	 * @see application.Controller#Controller()
	 */
	public ExpressionsModel(E specimen, Logger parentLogger)
	{
		logger = LoggerFactory
		    .getParentLogger(getClass(),
		                     parentLogger,
		                     parentLogger == null ? Level.INFO : parentLogger
		                         .getLevel());
		this.specimen = new SimpleObjectProperty<E>(specimen);
		rootExpression = new GroupExpression<>();
		TreeItem<Expression<E>> rootItemElt =
			new ExpressionTreeItem<E>(rootExpression);
		rootItem = new SimpleObjectProperty<>(rootItemElt);
		rootItemElt.setExpanded(true);

		expressions = FXCollections.<Expression<E>>observableArrayList();
		/*
		 * Initialize variablesMap as an observable map upon
		 * VariableExpression.getValues() using FXCollections
		 */
		variablesMap = FXCollections.<String, Optional<? extends Number>>observableMap(VariableExpression.getValues());
		/*
		 * Replace the intial Map<String, Optional<? extends Number>>
		 * with this ObservableMap<String, Optional<? extends Number>> which
		 * will report any changes to its observers
		 * We just switched a map for an observable map for all
		 * VariableExpressions
		 */
		VariableExpression.setValues(variablesMap);

		/*
		 * Set default #predicate to filter nothing
		 * changed in #setPredicate
		 */
		predicate = new Predicate<Expression<E>>()
		{
			@Override
			public boolean test(Expression<E> expression)
			{
				return true;
			}
		};
		operatorFiltering = new SimpleObjectProperty<BinaryOperatorRules>(BinaryOperatorRules.ANY);
		operandFiltering = new SimpleObjectProperty<TerminalType>(TerminalType.ALL);
		nameFiltering = new SimpleStringProperty();
		parser = new ExpressionParser<E>(specimen);
		file = null;
		hasFile = new ReadOnlyBooleanWrapper(false);
		filteredExpressions = new FilteredList<>(expressions, predicate);

		/*
		 * Whenever #operandFiltering, #operatorFiltering or #nameFiltering
		 * changes, #predicate has to be updated using #setPredicate
		 * So we'll add ChangeListeners to #operandFiltering, #operatorFiltering
		 * and #nameFiltering to do so.
		 */
		operatorFiltering
		    .addListener((ObservableValue<? extends BinaryOperatorRules> observable,
		                  BinaryOperatorRules oldValue,
		                  BinaryOperatorRules newvalue) -> {
				/*
				 * TODO Set the predicate to filter expressions based on the
				 * - newvalue of #operatorFiltering
				 * - current value of operandFiltering and
				 * - current value of nameFiltering
				 */
		    });
		operandFiltering
		    .addListener((ObservableValue<? extends TerminalType> observable,
		                  TerminalType oldValue,
		                  TerminalType newvalue) -> {
				/*
				 * TODO Set the predicate to filter expressions based on the
				 * - current value of operatorFiltering
				 * - the newvalue of #operandFiltering and
				 * - current value of nameFiltering
				 */
		    });
		nameFiltering.addListener((ObservableValue<? extends String> observable,
		                           String oldValue,
		                           String newvalue) -> {
			/*
			 * TODO Set the predicate to filter expressions based on the
			 * - current value of operatorFiltering
			 * - current value of operandFiltering and
			 * - newvalue of nameFiltering
			 */
		});
	}

	/**
	 * Filtered Expressions accessor
	 * @return the expressions list filtered by predicate
	 * @see application.Controller#initialize(java.net.URL, java.util.ResourceBundle)
	 * @see application.Controller#onSelectAllAction(javafx.event.ActionEvent)
	 */
	public ObservableList<Expression<E>> getExpressions()
	{
		return filteredExpressions;
	}

	/**
	 * Variables accessor
	 * @return the variables map
	 * @see application.Controller#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	public ObservableMap<String, Optional<? extends Number>> getVariables()
	{
		return variablesMap;
	}

	/**
	 * Get expression from {@link #expressions} corresponding to the provided
	 * expression.
	 * @param expression the expression to search in {@link #expressions}
	 * @return the corresponding expression from {@link #expressions} or null
	 * if there is no such expression among {@link #expressions}
	 * @deprecated never used
	 * @see #getExpressions()
	 */
	@Deprecated
	private Expression<E> getExpression(Expression<E> expression)
	{
		Expression<E> result = null;
		for (Expression<E> registered : expressions)
		{
			if (registered.equals(expression))
			{
				result = registered;
				break;
			}
		}
		return result;
	}

	/**
	 * The current file to read from or save to
	 * @return the current file descriptor or null if there is no file set.
	 * @see application.Controller#onRevertAction(javafx.event.ActionEvent)
	 * @see application.Controller#onSaveAction(javafx.event.ActionEvent)
	 * @see application.Controller#onSaveAsAction(javafx.event.ActionEvent)
	 */
	public File getFile()
	{
		return file;
	}

	/**
	 * Reset file attributes to default values.
	 * {@link #file} is set to null.
	 * {@link #hasFile} property is set to false
	 * This can be useful when clearing model from previous loaded file to
	 * restart from scratch.
	 * @see application.Controller#onNewAction(javafx.event.ActionEvent)
	 */
	public void resetFile()
	{
		file = null;
		hasFile.set(false);
	}

	/**
	 * Set number type and set new parser
	 * @param specimen the specimen number used to create a parser
	 * @throws NullPointerException if provided specimen is null
	 * @see #load(File, boolean)
	 * @see application.Controller#onSelectNumberType(javafx.event.ActionEvent)
	 */
	@SuppressWarnings("unchecked") // because specimen might be casted to E
	public void setNumberType(Number specimen) throws NullPointerException
	{
		/*
		 * TODO Set new specimen from non null argument and create a new parser
		 * Caution : Check if this.specimen is not bound before trying to change
		 * its value.
		 */
	}

	/**
	 * Clear all expressions and variables
	 * @see #load(File, boolean)
	 * @see application.Controller#onNewAction(javafx.event.ActionEvent)
	 * @see application.Controller#onSelectNumberType(javafx.event.ActionEvent)
	 */
	public void clear()
	{
		expressions.clear();
		variablesMap.clear();
		rootExpression.clear();
		rootItem.get().getChildren().clear();
	}

	/**
	 * Parse context using {@link #parser} and merge the resulting expression
	 * list with {@link #expressions} with <b>no duplicates</b>
	 * @param context the context to parse
	 * @return true if {@link #expressions} has been modified after parsing
	 * (indicating that new expressions have been added to {@link #expressions}).
	 * @throws ParserException if parsing fails
	 * @see #load(File, boolean)
	 * @see #merge(List)
	 * @see application.Controller#onAddAction(javafx.event.ActionEvent)
	 * @see application.Controller#onSelectNumberType(javafx.event.ActionEvent)
	 */
	public boolean parse(String context) throws ParserException
	{
		/*
		 * TODO Parse context using #parser and merge the resulting expressions
		 */

		/*
		 * TODO Merge parsed expressions with #expressions without doubles
		 */
		return false;
	}

	/**
	 * Replace the provided expression with the parsing of the provided context
	 * @param expression The expression to replace
	 * @param context the new context to parse. If provided context is null or
	 * empty then try to remove provided expression from {@link #expressions}
	 * @return true if {@link #expressions} have been modified after reparsing
	 * indicating that expression has been modified or removed and/or
	 * expressions have been added.
	 * @throws NullPointerException if provided epxression is null
	 * @throws IllegalArgumentException if provided expression can not be found
	 * among {@link #expressions}
	 * @throws ParserException if the parsing of the new context failed
	 * @implNote if the new context contains more than 1 expression, then use
	 * the first parsed expression to replace the provided expression and
	 * add all remaining expressions
	 * @see application.Controller#onEditCommitAction(javafx.scene.control.TableColumn.CellEditEvent)
	 */
	public boolean reparse(Expression<? extends Number> expression, String context)
	    throws NullPointerException,
	    IllegalArgumentException,
	    ParserException
	{
		Objects.requireNonNull(expression);
		/*
		 * TODO Search for expression's index in #expressions anf throw
		 * IllegalArgumentException if not found
		 */

		/*
		 * TODO If context is null or empty then remove expression
		 */

		/*
		 * TODO Parse context using #parser
		 */

		/*
		 * TODO Set the first parsed expression at index "index" in #expressions
		 */

		/*
		 * TODO Remove the first expression from parsed expressions
		 * and merge the rest of parsed expressions
		 */

		/*
		 * TODO Cleanup #variablesMap and refresh #rootItem
		 */
		return false;
	}

	/**
	 * Try to remove provided expression from {@link #expressions}
	 * @param expression the expression to remove from {@link #expressions}
	 * @return true if the provided expression has been found and removed from
	 * {@link #expressions}
	 * @see #reparse(Expression, String)
	 * @see application.Controller#onDeleteSelectedAction(javafx.event.ActionEvent)
	 */
	public boolean remove(Expression<? extends Number> expression)
	{
		/*
		 * TODO Search for expression's index in #expressions and
		 * return false if not found
		 */

		/*
		 * TODO Remove expression from #expressions
		 * cleanup #variablesMap and refresh #rootItem
		 */
		return false;
	}

	/**
	 * Read all expressions from a file (with or without clearing expressions
	 * first)
	 * @param file the file to read
	 * @param append Flag indicating expressions read from file shall be added
	 * to the existing expressions. In such case the type of numbers read from
	 * file MUST match the current type number otherwise inconsistencies might
	 * occur
	 * (without duplicates) or expressions read from file should replace
	 * expressions in the model
	 * @return true if {@link #expressions} or {@link #variablesMap} have
	 * changed
	 * after parsing expressions from file. Which mean, loading the same file
	 * twice should result in a false return value.
	 * @throws NullPointerException if provided file is null
	 * @throws IOException if any file operation fails
	 * @throws ParserException if parsing fails
	 * @throws UnsupportedNumberClassException if append mode is true and the
	 * type of Number read from file differs from the current type of number
	 * @implSpec Provided file is also used to set #file and #hasFile
	 * @implSpec The file to read can contain one or more expression per line.
	 * Expression separator within a line is ";"
	 * If a line in the file starts with "type" then we shall search for the
	 * last word in this line which contains the type of numbers used in
	 * expressions and call {@link #setNumberType(Number)}: Valid number type
	 * are int, float and double.
	 * @see application.Controller#loadFile
	 */
	public boolean load(File file, boolean append)
	    throws NullPointerException,
	    IOException,
	    ParserException
	{
		Objects.requireNonNull(file);
		if (!append)
		{
			clear();
		}
		// Checkpoint for later comparison
		int expressionsHash = expressions.hashCode();
		int variablesHash = variablesMap.hashCode();

		/*
		 * TODO Read text file lines to parse expressions
		 * If a line starts with "type" then it shall end by either int, float
		 * or double to indicate the type of numbers used in expressions.
		 * Throw a UnsupportedNumberClassException in any other case
		 * If a line does not start with "type" then it is a context to parse
		 */

		/*
		 * TODO If everything went fine, set file & hasFile attribute
		 */

		/*
		 * TODO Returns true if #expressions or #variables have changed
		 * by comparing with checkpoint values
		 */
		return false;
	}

	/**
	 * Save all expressions to file (as plain text).
	 * @param file the file to save to
	 * @return true if expressions have been saved to file, false otherwise
	 * @throws IOException if any file operation fails
	 * @implNote If there are variables into {@link #variablesMap} featuring a
	 * value which are <b>not</b> also defined by an assignment in
	 * {@link #expressions}, then these variables should also be written in the
	 * file as an {@link AssignmentExpression}
	 * @implSpec Provided file is also used to set #file and #hasFile
	 * @implSpec after saving the file contains: a line containing the "type
	 * int|float|double" indicating the type of numbers to expect in expressions
	 * followed by one expression per line (although expressions separator ";"
	 * are allowed during file reading).
	 * @see application.Controller#saveFile
	 */
	public boolean save(File file) throws IOException
	{
		if (file == null)
		{
			return false;
		}

		/*
		 * TODO Save all expressions to file
		 * If there are variables with values without expressions to
		 * define these values then build local AssignmentExpression and
		 * print them to file (one assignment per line)
		 * Then print expressions to file (one expression per line)
		 */

		// TODO If everything went well then set the #file & #hasFile and return true
		return false;
	}

	/**
	 * Create a string representation of all expressions.
	 * @return a new String containing all expressions separated by ";"
	 * @implNote If there is a variable within {@link #variablesMap} featuring a
	 * value with no corrersponding expression in {@link #expressions} to
	 * provide such a value, then an assignment expression can be locally
	 * created to reflect this variable in the output
	 * @see application.Controller#onSelectNumberType(javafx.event.ActionEvent)
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		/*
		 * TODO If there are variables with values without expressions to
		 * define these values then build local AssignmentExpression and
		 * add them to Stringbuilder
		 */

		/*
		 * TODO Add Expressions toString() to StringBuilder
		 */

		return sb.toString();
	}

	/**
	 * Accessor to {@link #specimen} property
	 * @return the {@link #specimen} property
	 * @see application.Controller#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	public final ObjectProperty<E> specimenProperty()
	{
		return specimen;
	}

	/**
	 * Accessor to {@link #specimen}'s value
	 * @return {@link #specimen}'s value
	 */
	public final E getSpecimen()
	{
		return specimen.get();
	}

	/**
	 * Setter of {@link #specimen}
	 * @param value a new value to {@link #specimen}
	 * @implSpec might fail (with a log warning) if {@link #specimen}
	 * is already bound to another property
	 * @deprecated use {@link #setNumberType(Number)}
	 * @see #setNumberType(Number)
	 */
	public final void setSpecimen(final E value)
	{
		if (specimen.isBound())
		{
			logger.warning("can't set bound specimen property");
			return;
		}
			specimen.set(value);
		}

	/**
	 * Accessor to {@link #rootItem} property
	 * @return the {@link #rootItem} property
	 * @see application.Controller#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	public final ObjectProperty<TreeItem<Expression<E>>> rootItemProperty()
	{
		return rootItem;
	}

	/**
	 * Accessor to {@link #rootItem}'s value
	 * @return {@link #rootItem}'s value
	 */
	public final TreeItem<Expression<E>> getRootItem()
	{
		return rootItem.get();
	}

	/**
	 * Setter of {@link #rootItem}
	 * @param value a new value to {@link #specimen}
	 * @implSpec might fail (with a log warning) if {@link #rootItem}
	 * is already bound to another property
	 */
	public final void setRootItem(final ExpressionTreeItem<E> value)
	{
		if (rootItem.isBound())
		{
			logger.warning("Can't set bound rootItem property");
			return;
		}
		rootItem.set(value);
	}

	/**
	 * Accessor to {@link #operatorFiltering} property
 	 * @return the {@link #operatorFiltering} property
 	 * @see application.Controller#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	public final ObjectProperty<BinaryOperatorRules> operatorFilteringProperty()
	{
		return operatorFiltering;
	}

	/**
	 * Accessor to {@link #operatorFiltering}'s value
	 * @return {@link #operatorFiltering}'s value
	 * @see #ExpressionsModel(Number, Logger)
	 */
	public final BinaryOperatorRules getOperatorFiltering()
	{
		return operatorFiltering.get();
	}

	/**
	 * Setter of {@link #operatorFiltering}
	 * @param value a new value to {@link #operatorFiltering}
	 * @implSpec might fail (with a log warning) if {@link #operatorFiltering}
	 * is already bound to another property
	 */
	public final void setOperatorFiltering(final BinaryOperatorRules value)
	{
		if (operatorFiltering.isBound())
		{
			logger.warning("Can't set bound operatorFiltering property");
			return;
		}
		operatorFiltering.set(value);
	}

	/**
	 * Accessor to {@link #operandFiltering} property
 	 * @return the {@link #operandFiltering} property
 	 * @see application.Controller#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	public final ObjectProperty<TerminalType> operandFilteringProperty()
	{
		return operandFiltering;
	}

	/**
	 * Accessor to {@link #operandFiltering}'s value
	 * @return {@link #operandFiltering}'s value
	 * @see #ExpressionsModel(Number, Logger)
	 */
	public final TerminalType getOperandFiltering()
	{
		return operandFiltering.get();
	}

	/**
	 * Setter of {@link #operandFiltering}
	 * @param value a new value to {@link #operandFiltering}
	 * @implSpec might fail (with a log warning) if {@link #operandFiltering}
	 * is already bound to another property
	 */
	public final void setOperandFiltering(final TerminalType value)
	{
		if (operandFiltering.isBound())
		{
			logger.warning("Can't set bound operandFiltering property");
			return;
		}
		operandFiltering.set(value);
	}

	/**
	 * Accessor to {@link #nameFiltering} property
 	 * @return the {@link #nameFiltering} property
 	 * @see application.Controller#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	public final StringProperty nameFilteringProperty()
	{
		return nameFiltering;
	}

	/**
	 * Accessor to {@link #nameFiltering}'s value
	 * @return {@link #nameFiltering}'s value
	 * @see #ExpressionsModel(Number, Logger)
	 */
	public final String getNameFiltering()
	{
		return nameFiltering.get();
	}

	/**
	 * Setter of {@link #nameFiltering}
	 * @param value a new value to {@link #nameFiltering}
	 * @implSpec might fail (with a log warning) if {@link #nameFiltering}
	 * is already bound to another property
	 */
	public final void setNameFiltering(final String value)
	{
		if (nameFiltering.isBound())
		{
			logger.warning("Can't set bound nameFiltering property");
			return;
		}
		nameFiltering.set(value);
	}

	/**
	 * Accessor to the {@link #hasFile} property
	 * @return the {@link #hasFile} property
	 * @see application.Controller#initialize(java.net.URL, java.util.ResourceBundle)
	 */
	public final ReadOnlyBooleanProperty hasFileProperty()
	{
		return hasFile.getReadOnlyProperty();
	}

	/**
	 * Accessor to the value of {@link #hasFile} property
	 * @return the value of {@link #hasFile} property
	 * @see application.Controller#onRevertAction(javafx.event.ActionEvent)
	 * @see application.Controller#onSaveAction(javafx.event.ActionEvent)
	 */
	public final boolean hasFile()
	{
		return hasFile.get();
	}

	/**
	 * Refresh root item
	 * @see #merge(List)
	 * @see #remove(Expression)
	 * @see #reparse(Expression, String)
	 */
	public void refreshRoot()
	{
		rootExpression.clear();
		rootExpression.addAll(expressions);
		TreeItem<Expression<E>> rootNode = rootItem.get();
		((ExpressionTreeItem<E>)rootNode).reset();
		rootNode.setValue(rootExpression);
		rootNode.setExpanded(true);
		rootItem.set(rootNode);
		Event.fireEvent(rootNode, new TreeItem.TreeModificationEvent<Expression<E>>(
		    TreeItem.valueChangedEvent(), rootNode));
	}

	// ------------------------------------------------------------------------
	// Utility methods
	// ------------------------------------------------------------------------

	/**
	 * Merge the provided list of expressions with {@link #expressions}
	 * @param expressions the list of (new) expressions to merge with
	 * {@link #expressions}
	 * @return true if at least one expression from provided expression list has
	 * been added to {@link #expressions}
	 * @implNote Each expression from provided list is added only if it not
	 * already contained in {@link #expressions}.
	 * @implNote {@link #rootExpression} has been updated
	 * @implNote {@link #rootItem} has been updated
	 * @see #parse(String)
	 * @see #reparse(Expression, String)
	 */
	private boolean merge(List<Expression<E>> expressions)
	{
		boolean added = false;

		/*
		 * TODO Merge the provided expressions with #expressions
		 * If an expression is already contained in #expressions then
		 * it should not be added again.
		 */

		/*
		 * TODO Update root if at least one expression has been added to
		 * #expressions
		 */

		return added;
	}

	/**
	 * Search for an {@link AssignmentExpression} containing a
	 * {@link VariableExpression} with the provided name as left side.
	 * @param name The name of the {@link VariableExpression} we're searching
	 * @param expressions the expression collection to search in
	 * @return The {@link AssignmentExpression} containing a
	 * {@link VariableExpression} called with the provided name as a left side
	 * or null if there is no such assignement.
	 * @throws NullPointerException if either provided name or expressions are
	 * null
	 * @implNote Since {@link AssignmentExpression} are root in an expression
	 * tree there is no need to explore provided expressions in depth
	 * @see #getAssignmentFor(String)
	 */
	private AssignmentExpression<E>
	    getAssignmentFor(String name, Collection<Expression<E>> expressions)
	{
		Objects.requireNonNull(name);
		Objects.requireNonNull(expressions);
		/*
		 * TODO Search for an AssignmentExpression containing a
		 * VariableExpression with the provided name as left side
		 * among provided expressions
		 */
		return null;
	}

	/**
	 * Search for an {@link AssignmentExpression} containing a
	 * {@link VariableExpression} with the provided name as left side
	 * among {@link #expressions}.
	 * @param name The name of the {@link VariableExpression} we're searching
	 * @return The {@link AssignmentExpression} containing a
	 * {@link VariableExpression} called with the provided name as a left side
	 * or null if there is no such assignement.
	 * @throws NullPointerException if either provided name or expressions are
	 * null
	 * @implNote Since {@link AssignmentExpression} are root in an expression
	 * tree there is no need to explore provided expressions in depth
	 * @see #save(File)
	 * @see #toString()
	 */
	private AssignmentExpression<E> getAssignmentFor(String name)
	{
		return getAssignmentFor(name, expressions);
	}

	/**
	 * Search for a specific type of terminal expression within an expression
	 * @param expression the expression to search into
	 * @param type the type of terminal expression to search for
	 * @return true if the provided type of terminal expression has been
	 * found in provided expression (including sub-expressions)
	 * @see #setPredicate(BinaryOperatorRules, TerminalType, String)
	 */
	private static <E extends Number> boolean
	    searchFor(Expression<E> expression, TerminalType type)
	{
		/*
		 * TODO if provided expression is null return false
		 */

		/*
		 * TODO if provided expression is a TerminalExpression
		 * return true if the type of expression is the same as provided type
		 * return false otherwise
		 */

		/*
		 * TODO if provided expression is a GroupExpression
		 * return true if at least one of the sub-expressions
		 * contains the provided type of terminal expression
		 * return false otherwise
		 */

		return false;
	}

	/**
	 * Setup new {@link #predicate} and apply it to {@link #filteredExpressions}
	 * @param operatorFilter value for {@link BinaryExpression}s filtering.
	 * If opertorFilter is set to {@link BinaryOperatorRules#ANY} this validates
	 * the operatorFilter criterium.
	 * If an expression String content contains the operatorFilter string then
	 * this validates the operatorFilter criterium.
	 * @param operandFilter value for {@link TerminalExpression} filtering
	 * If operandFilter is set to {@link TerminalType#ALL} this validates the
	 * operandFilter crtiterium.
	 * If an expression contains the type of Terminal expression of
	 * operandFitler this validates the operandFilter criterium.
	 * @param searchName value for names filtering in expressions.
	 * If searchName is null or empty this validates the searchName criterium.
	 * If an expression String content contains the searchName this validates
	 * the searchName criterium.
	 * The {@link #predicate} examining {@link Expression}s set by this method
	 * returns true when all 3 criteria are true.
	 * @see #ExpressionsModel(Number, Logger)
	 */
	private void setPredicate(BinaryOperatorRules operatorFilter,
	                          TerminalType operandFilter,
	                          String searchName)
	{
		predicate = new Predicate<Expression<E>>()
		{
			@Override
			public boolean test(Expression<E> e)
			{
				String expressionString = e.toString();

				/*
				 * TODO Search criterium is true when
				 * 	- searchName is null or when
				 * 	- searchName is empty or when
				 * 	- searchName is found within expression's toString
				 */
				boolean searchOk = false;
				/*
				 * TODO operator criterium is true when
				 * 	- operatorFilter is BinaryOperatorRules.ANY or when
				 * 	- operatorFilter symbol is found in expression's toString
				 */
				boolean operatorOk = false;
				/*
				 * TODO operand criterium is true when
				 * 	- operandFilter is TerminalType.ALL or when
				 * 	- operandFilter has been found within expression using searchFor method
				 */
				boolean operandOk = false;

				/*
				 * TODO Predicate result is true when all criteria are true
				 */
				return true;
			}

		};

		// update the predicate on #filteredExpressions
		filteredExpressions.setPredicate(predicate);
	}

	/**
	 * Search for the presence of a named variable within provided expression
	 * @param expression The expression to search into
	 * @param name the name of the variable to search
	 * @return true if the provided expression contains a variable with the provided name
	 * @see #cleanupVariablesMap()
	 */
	private static <E extends Number> boolean containsVariable(Expression<E> expression, String name)
	{
		/*
		 * TODO if provided expression is null return false
		 */

		/*
		 * TODO if provided expression is a VariableExpression
		 * return true if the name of the variable is the same as provided name
		 * return false otherwise
		 */

		/*
		 * TODO if provided expression is BinaryExpression
		 * return true if at least one of the sub-expressions
		 * contains the provided named variable
		 */

		/*
		 * TODO if provided expression is a GroupExpression
		 * return true if at least one of the sub-expressions
		 * contains the provided named variable
		 */

		return false;
	}

	/**
	 * Removes from {@link #variablesMap} all entries not found in
	 * {@link #expressions}
	 * @return true if at least one entry of {@link #variablesMap} has been
	 * removed
	 * @implNote Caution : You can't remove a key from a set while iterating
	 * over keys, you have to build a subset of keys to remove during iteration
	 * and removes the subset keys in another loop.
	 * @see #remove(Expression)
	 * @see #reparse(Expression, String)
	 */
	private boolean cleanupVariablesMap()
	{
		Set<String> keys = variablesMap.keySet();
		Set<String> keysToRemove = new HashSet<>();
		/*
		 * TODO Search for keys (names) to remove from variablesMap
		 * by searching for named variables in expressions : If no expressions
		 * contains a variable with the provided name then this key should be
		 * removed from variablesMap
		 */

		/*
		 * TODO Remove the keys not found in #expressions from #variablesMap
		 */

		/*
		 * TODO return true if at least one key has been removed from #variablesMap
		 */
		return false;
	}
}
