package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import expressions.Expression;
import expressions.binary.AssignmentExpression;
import expressions.binary.BinaryOperatorRules;
import expressions.models.ExpressionTreeItem;
import expressions.models.ExpressionsModel;
import expressions.special.GroupExpression;
import expressions.terminal.ConstantExpression;
import expressions.terminal.TerminalType;
import expressions.terminal.VariableExpression;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import logger.LoggerFactory;
import parser.ExpressionParser;
import parser.exceptions.MissingRightOperandException;
import parser.exceptions.ParserException;

/**
 * Test class for {@link ExpressionsModel}
 */
@DisplayName("ExpressionsModelTest")
@TestMethodOrder(OrderAnnotation.class)
class ExpressionsModelTest
{

	/**
	 * The model to test
	 */
	private ExpressionsModel<Number> testModel = null;

	/**
	 * Number property to set parser
	 */
	private ObjectProperty<Number> specimen;

	/**
	 * Global Logger
	 */
	private static Logger globalLogger = null;

	/**
	 * Logger to use during each test
	 */
	private Logger parentLogger;

	/**
	 * Log handler to assert messages sent to parentLogger
	 */
	private LogHandler logHandler;

	/**
	 * Simple valid contexts to parse
	 */
	private static final String[] validContexts = new String[]
	{
		"a+b",
		"a=1",
		"b=2",
		"c = 3.3"
	};

	/**
	 * expected variables names
	 */
	private static final String[] variablesNames = new String[]
	{
		"a",
		"b",
		"c"
	};

	/**
	 * Simple valid other contexts to parse
	 */
	private static final String[] otherValidContexts = new String[]
	{
		"d=2",
		"d+3",
		"e-d",
		"e=5"
	};

	/**
	 * expected other variables names
	 */
	private static final String[] otherVariablesNames = new String[]
	{
		"d",
		"e"
	};

	/**
	 * Simple invalid contexts to parse
	 */
	private static final String[] invalidContexts = new String[]
	{
		"a!b",
		"a+(b-3",
		"a+b = 3"
	};

	/**
	 * Setup before all tests
	 * @throws Exception if setup fails
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception
	{
		globalLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		System.out.println("-------------------------------------------------");
		System.out.println("Start of ExpressionsModel Tests");
		System.out.println("-------------------------------------------------");
		// JavaFX platform startup
		Platform.startup(()->{});
	}

	/**
	 * Teardown after all tests
	 * @throws Exception if teardown fails
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception
	{
		System.out.println("-------------------------------------------------");
		System.out.println("End of ExpressionsModel Tests");
		System.out.println("-------------------------------------------------");
	}

	/**
	 * Setup before each test
	 * @throws Exception if setpu fails
	 */
	@BeforeEach
	void setUp() throws Exception
	{
		// Initial specimen is double
		specimen = new SimpleObjectProperty<>(Float.valueOf(0.0f));
		parentLogger = null;
		Class<?> runningClass = getClass();
		try
		{
			parentLogger = LoggerFactory.getLogger(runningClass,
			                                       true,
			                                       null,
			                                       false,
			                                       globalLogger,
			                                       Level.WARNING);
			// handler handles 2 messages per test
			logHandler = new LogHandler(2);
			parentLogger.addHandler(logHandler);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.exit(ex.hashCode());
		}
	}

	/**
	 * Teardown after each test
	 * @throws Exception if teardown fails
	 */
	@AfterEach
	void tearDown() throws Exception
	{
		VariableExpression.clearAll();
		parentLogger.removeHandler(logHandler);
	}

	/**
	 * Possible specimens values
	 */
	private static final Number[] specimenValues = new Number[]
	{
		Integer.valueOf(0),
		Float.valueOf(0.0f),
		Double.valueOf(0.0)
	};

	/**
	 * Stream of numbers
	 * @return a stream of numbers
	 */
	private static Stream<? extends Number> numberProvider()
	{
		return Stream.of(specimenValues);
	}

	/**
	 * Test method for {@link ExpressionsModel#ExpressionsModel(Number, Logger)}
	 * @param specimen The specimen number
	 * @param info Test infos
	 */
	@ParameterizedTest(name="ExpressionModel<>({0}, Logger)")
	@MethodSource("numberProvider")
	@DisplayName("ExpressionModel(Number, Logger)")
	@Order(1)
	final void testExpressionsModel(Number specimen, TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen, parentLogger);
		assertNotNull(testModel);
		// logger should not be null
		// specimen should be equal to provided specimen
		Number modelSpecimen = testModel.getSpecimen();
		assertEquals(specimen,
		             modelSpecimen,
		             testName + " unexpected sepcimen value");
		boolean fileStatus = testModel.hasFile();
		assertFalse(fileStatus,
		            testName + " unexpected file status");
		ObservableList<Expression<Number>> list = testModel.getExpressions();
		assertNotNull(list, testName + " unexpected expressions list");
		assertTrue(list.isEmpty(),
		           testName + " unexpected expressions list size");
		File file = testModel.getFile();
		assertNull(file, testName + " unexpected non null file");
		String nameFiltering = testModel.getNameFiltering();
		assertNull(nameFiltering,
		           testName + " unexpected non null name filtering");
		TerminalType operandFiltering = testModel.getOperandFiltering();
		assertEquals(TerminalType.ALL,
		             operandFiltering,
		             testName + " unexpected operand filtering");
		BinaryOperatorRules operatorFiltering =
		    testModel.getOperatorFiltering();
		assertEquals(BinaryOperatorRules.ANY,
		             operatorFiltering,
		             testName + " unexpected operator filtering");
		TreeItem<Expression<Number>> item = testModel.getRootItem();
		assertTrue(item.getChildren().isEmpty(),
		           testName + " unexpected non empty root item");
		Expression<Number> rootExpression = item.getValue();
		assertNotNull(rootExpression,
		              testName + " unexpected null root expression");
		assertEquals(GroupExpression.class,
		             rootExpression.getClass(),
		             testName + " unexpected root expression");
	}

	/**
	 * Test method for {@link ExpressionsModel#getExpressions()}
	 * (Which is obsolete)
	 * @param info Test infos
	 */
	@DisplayName("getExpressions()")
	@Test
	@Order(9)
	final void testGetExpressions(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);
		ObservableList<Expression<Number>> list = testModel.getExpressions();
		assertNotNull(list, testName + " unexpected expressions list");
		assertTrue(list.isEmpty(),
		           testName + " unexpected expressions list size");
		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				fail(testName + " parsing failed for context: "
				           + context + " with exception: "
				           + e.getMessage());
			}
		}
		assertNotNull(list, testName + " unexpected expressions list");
		assertFalse(list.isEmpty(),
		            testName + " unexpected empty expressions list");
		assertEquals(validContexts.length,
		             list.size(),
		             testName + " unexpected expressions list size");
	}

	/**
	 * Test method for {@link ExpressionsModel#getVariables()}
	 * @param info Test infos
	 */
	@DisplayName("getVariables()")
	@Test
	@Order(10)
	final void testGetVariables(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);
		Map<String, Optional<? extends Number>> modelVariables =
		    testModel.getVariables();
		assertNotNull(modelVariables, testName + " unexpected variables map");
		assertTrue(modelVariables.isEmpty(),
		           testName + " unexpected non empty variables map");
		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				fail(testName + " parsing failed for context: "
				           + context + " with exception: "
				           + e.getMessage());
			}
		}
		assertNotNull(modelVariables, testName + " unexpected variables map");
		assertFalse(modelVariables.isEmpty(),
		            testName + " unexpected empty variables map");
		assertEquals(variablesNames.length,
		             modelVariables.size(),
		             testName + " unexpected variables map size");
		VariableExpression.clearAll();
		assertTrue(modelVariables.isEmpty(),
		           testName + " unexpected non empty variables map");
	}

	/**
	 * Test method for {@link ExpressionsModel#getFile()}
	 * @param info Test infos
	 */
	@DisplayName("getFile()")
	@Test
	@Order(11)
	final void testGetFile(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);
		File file = testModel.getFile();
		assertNull(file, testName + " unexpected non null file");
		File loadedFile = new File("assets/Test1.txt");
		assertTrue(loadedFile.exists(),
		           testName + " unexpected non existant file: "
		               + loadedFile.getAbsolutePath());
		boolean loaded = false;
		try
		{
			loaded = testModel.load(loadedFile, false);
		}
		catch (NullPointerException e)
		{
			fail(testName
			    + " unexpected NullPointerException loading file "
			    + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			fail(testName
			    + " unexpected IOException loading file "
			    + e.getLocalizedMessage());
		}
		catch (ParserException e)
		{
			fail(testName
			    + " unexpected ParserException loading file "
			    + e.getLocalizedMessage());
		}

		assertTrue(loaded,
		           testName + " loading file should have succeeded");
		file = testModel.getFile();
		assertNotNull(file, testName + " unexpected null file");
		assertEquals(loadedFile.getAbsolutePath(),
		             file.getAbsolutePath(),
		             testName + " unexpected file");
	}

	/**
	 * Test method for {@link ExpressionsModel#resetFile()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("resetFile()")
	@Order(12)
	final void testResetFile(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);
		File file = testModel.getFile();
		assertNull(file, testName + " unexpected non null file");
		File loadedFile = new File("assets/Test1.txt");
		assertTrue(loadedFile.exists(),
		           testName + " unexpected non existant file: "
		               + loadedFile.getAbsolutePath());
		boolean loaded = false;
		try
		{
			loaded = testModel.load(loadedFile, false);
		}
		catch (NullPointerException e)
		{
			fail(testName + " unexpected NullPointerException loading file "
			    + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			fail(testName + " unexpected IOException loading file "
			    + e.getLocalizedMessage());
		}
		catch (ParserException e)
		{
			fail(testName + " unexpected ParserException loading file "
			    + e.getLocalizedMessage());
		}

		assertTrue(loaded, testName + " loading file should have succeeded");
		file = testModel.getFile();
		assertNotNull(file, testName + " unexpected null file");
		assertEquals(loadedFile.getAbsolutePath(),
		             file.getAbsolutePath(),
		             testName + " unexpected file");

		testModel.resetFile();
		file = testModel.getFile();
		assertNull(file, testName + " unexpected non null file");
		assertFalse(testModel.hasFile(),
		            testName + " unexpected file status");
	}

	/**
	 * Test method for {@link ExpressionsModel#setNumberType(Number)}
	 * @param specimen Number specimen
	 * @param info Test infos
	 */
	@ParameterizedTest(name = "setNumberType<>({0})")
	@MethodSource("numberProvider")
	@DisplayName("setNumberType(Number)")
	@Order(13)
	final void testSetNumberType(Number specimen, TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen, parentLogger);
		assertNotNull(testModel);

		/*
		 * Setting a null specimen should throw a NullPointerException
		 */
		assertThrows(NullPointerException.class, () ->
		{
			testModel.setNumberType(null);
		}, testName + " setting null specimen should have failed");

		for (int i=0; i < otherValidContexts.length; i++)
		{
			String context = otherValidContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				fail(testName + " parsing failed for context: "
				           + context + " with exception: "
				           + e.getMessage());
			}
		}
		/*
		 * Save current expressions to a buffer
		 */
		String buffer = testModel.toString();

		/*
		 * Set the number type to a new one
		 */
		Number oldSpecimen = testModel.getSpecimen();
		Number newSpecimen = null;
		int i = 0;
		while (newSpecimen == null)
		{
			int j = i % specimenValues.length;
			if (specimenValues[j].getClass() != specimen.getClass())
			{
				newSpecimen = specimenValues[j];
				break;
			}
			i++;
		}

		testModel.setNumberType(newSpecimen);
		assertNotEquals(oldSpecimen,
		                testModel.getSpecimen(),
		                testName + " specimen should have changed");
		assertEquals(newSpecimen,
		             testModel.getSpecimen(),
		             testName + " unexpected specimen value");
		boolean parsed = false;
		try
		{
			parsed = testModel.parse(buffer);
			assertTrue(parsed,
			           testName + " parsing failed for context: "
			               + buffer);
		}
		catch (ParserException e)
		{
			if ((oldSpecimen.getClass() == Integer.class) && (newSpecimen.getClass() != Integer.class))
			{
				// Float and Double parsing should not fail
				fail(testName + " parsing failed for context: " + buffer
				    + " with exception: " + e.getMessage());
			}
		}
	}

	/**
	 * Test method for {@link ExpressionsModel#clear()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("clear()")
	@Order(5)
	final void testClear(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		assertFalse(testModel.getExpressions().isEmpty(),
		            testName + " unexpected empty expressions");
		assertFalse(testModel.getVariables().isEmpty(),
		            testName + " unexpected empty variables");
		TreeItem<Expression<Number>> rootItem = testModel.getRootItem();
		Expression<Number> rootExpression = rootItem.getValue();
		assertEquals(GroupExpression.class,
		             rootExpression.getClass(),
		             testName + " unexpected root expression class");
		GroupExpression<Number> groupExpression =
		    (GroupExpression<Number>) rootExpression;
		assertFalse(groupExpression.isEmpty(),
		            testName + " unexpected empty root expression");
		assertFalse(rootItem.getChildren().isEmpty(),
		            testName + " unexpected root item empty children");

		testModel.clear();

		assertTrue(testModel.getExpressions().isEmpty(),
		           testName + " unexpected non empty expressions");
		assertTrue(testModel.getVariables().isEmpty(),
		           testName + " unexpected non empty variables");
		TreeItem<Expression<Number>> newRootItem = testModel.getRootItem();
		assertSame(rootItem, newRootItem, testName + " unexpected root item");
		assertTrue(rootItem.getChildren().isEmpty(),
		           testName + " unexpected root item non empty children");
		assertEquals(GroupExpression.class,
		             rootExpression.getClass(),
		             testName + " unexpected root expression class");
		assertTrue(groupExpression.isEmpty(),
		           testName + " unexpected non empty root expression");
	}

	/**
	 * Test method for {@link ExpressionsModel#parse(String)}
	 * @param specimen number specimen
	 * @param info Test infos
	 */
	@ParameterizedTest(name = "parse(String)")
	@MethodSource("numberProvider")
	@DisplayName("parse(String)")
	@Order(2)
	final void testParse(Number specimen, TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen, parentLogger);
		assertNotNull(testModel);

		/*
		 * Parsing null context should lead to NullPpointerException
		 */
		assertThrows(NullPointerException.class, () ->
		{
			testModel.parse(null);
		}, testName + " parsing should have failed for null context");
		/*
		 * Parsing empty context should lead to MissingRightOperandException
		 */
		assertThrows(MissingRightOperandException.class, () ->
		{
			testModel.parse("");
		}, testName + " parsing should have failed for empty context");


		int expectedSize = 0;
		int i = 0;
		for (i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
				expectedSize = expectedSize + (parsed ? 1 : 0);
				// Parsing the same context a second time should not add
				// expressions to the list
				parsed = testModel.parse(context);
				assertFalse(parsed,
				            testName + " parsing should not have added context: "
				                + context);
				assertEquals(expectedSize,
				             testModel.getExpressions().size(),
				             testName + " unexpected expressions list size");
			}
			catch (ParserException e)
			{
				if (specimen.getClass() != Integer.class)
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: "
					           + context + " with exception: "
					           + e.getMessage());
				}
				else
				{
					if (i != (validContexts.length - 1))
					{
						// Integer parsing should fail for the last context
						fail(testName + " parsing failed for context: "
						           + context + " with exception: "
						           + e.getMessage());
					}
					// Nothing : expected
					globalLogger.log(Level.WARNING,
					                 testName + " parsing failed for context: "
					                     + context + " with exception: "
					                     + e.getMessage());
				}
			}
		}

		ObservableList<Expression<Number>> list = testModel.getExpressions();
		assertNotNull(list, testName + " unexpected expressions list");
		assertFalse(list.isEmpty(),
		            testName + " unexpected empty expressions list");
		assertEquals(expectedSize,
		             list.size(),
		             testName + " unexpected expressions list size");
		Map<String, Optional<? extends Number>> modelVariables =
		    testModel.getVariables();
		assertNotNull(modelVariables, testName + " unexpected variables map");
		assertFalse(modelVariables.isEmpty(),
		            testName + " unexpected empty variables map");
		/*
		 * variables map has already registered the c variable name before
		 * paring failed on c = 3.3 for Integers
		 */
		assertEquals(variablesNames.length,
		             modelVariables.size(),
		             testName + " unexpected variables map size");
		for (int j = 0; j < variablesNames.length; j++)
		{
			String name = variablesNames[j];
			assertTrue(modelVariables.containsKey(name),
			           testName + " unexpected missing variable: " + name);
		}

		for (int j = 0; j < invalidContexts.length; j++)
		{
			final String context = invalidContexts[j];
			assertThrows(ParserException.class, () ->
			{
				testModel.parse(context);
			}, testName + " parsing should have failed for context: "
			           + context);
		}
	}

	/**
	 * Test method for {@link ExpressionsModel#reparse(Expression, String)}
	 * @param specimen number specimen
	 * @param info tesst infos
	 */
	@ParameterizedTest(name = "reparse(Expression, String)")
	@MethodSource("numberProvider")
	@DisplayName("reparse(Expression, String)")
	@Order(3)
	final void testReparse(Number specimen, TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen, parentLogger);
		assertNotNull(testModel);

		/*
		 * First parse the valid contexts
		 */
		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.getClass() != Integer.class) || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: "
					           + context + " with exception: "
					           + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}
		/*
		 * Then reparse each expression with other valid contexts
		 */
		List<Expression<Number>> expressions = testModel.getExpressions();
		int nbContexts = expressions.size();
		for (int i = 0; i < nbContexts; i++)
		{
			Expression<Number> oldExpression = expressions.get(i);
			String context = otherValidContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.reparse(oldExpression, context);
				assertTrue(parsed,
				           testName + "re-parsing failed for context: "
				               + context);
				assertEquals(nbContexts,
				             testModel.getExpressions().size(),
				             testName + " unexpected expressions list size");
			}
			catch (ParserException e)
			{
				fail(testName + " parsing failed for context: "
				           + context + " with exception: "
				           + e.getMessage());
			}
		}

		Map<String, Optional<? extends Number>> modelVariables = testModel.getVariables();
		assertNotNull(modelVariables, testName + " unexpected variables map");
		assertFalse(modelVariables.isEmpty(),
		            testName + " unexpected empty variables map");
		/*
		 * variableNames should not be part of variables anymore
		 */
		for (int j = 0; j < variablesNames.length; j++)
		{
			String name = variablesNames[j];
			assertFalse(modelVariables.containsKey(name),
			            testName + " unexpected variable: " + name);
		}
		assertEquals(otherVariablesNames.length,
		             modelVariables.size(),
		             testName + " unexpected variables map size");
		/*
		 * otherVariablesNames should be part of variables
		 */
		for (int j = 0; j < otherVariablesNames.length; j++)
		{
			String name = otherVariablesNames[j];
			assertTrue(modelVariables.containsKey(name),
			           testName + " unexpected missing variable: " + name);
		}

		/*
		 * Reparsing a null expression should throw a NullPointerException
		 */
		assertThrows(NullPointerException.class, () ->
		{
			testModel.reparse(null, null);
		}, testName + " reparsing should have failed with null expression");

		/*
		 * Reparsing a null context should remove the expression
		 */
		boolean reparsed = false;
		Expression<Number> expression = expressions.get(0);
		try
		{
			reparsed = testModel.reparse(expression, null);
		}
		catch (NullPointerException e)
		{
			fail(testName + " unexpected NullPointerException reparsing with null context "
			    + e.getLocalizedMessage());
		}
		catch (IllegalArgumentException e)
		{
			// Nothing : expected
		}
		catch (ParserException e)
		{
			fail(testName + " unexpected ParserException reparsing with null context "
			    + e.getLocalizedMessage());
		}
		assertTrue(reparsed,
		           testName + " reparsing failed for null context");
		assertEquals(nbContexts - 1,
		             testModel.getExpressions().size(),
		             testName + " unexpected expressions list size");
		assertFalse(expressions.contains(expression),
		            testName + " reparsing should have removed expression "
		                + expression.toString());

		/*
		 * Reparsing an empty context should remove the expression
		 */
		expression = expressions.get(0);
		try
		{
			reparsed = testModel.reparse(expression, "");
		}
		catch (NullPointerException e)
		{
			fail(testName
			    + " unexpected NullPointerException reparsing with empty context "
			    + e.getLocalizedMessage());
		}
		catch (IllegalArgumentException e)
		{
			// Nothing : expected
		}
		catch (ParserException e)
		{
			fail(testName
			    + " unexpected ParserException reparsing with empty context "
			    + e.getLocalizedMessage());
		}
		assertTrue(reparsed,
		           testName + " reparsing failed for empty context");
		assertEquals(nbContexts - 2,
		             testModel.getExpressions().size(),
		             testName + " unexpected expressions list size");
		assertFalse(expressions.contains(expression),
		            testName + " reparsing should have removed expression "
		                + expression.toString());
	}

	/**
	 * Test method for {@link ExpressionsModel#remove(Expression)}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("remove(Expression)")
	@Order(4)
	final void testRemove(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		/*
		 * Removing a null expression should always return false
		 */
		assertFalse(testModel.remove(null),
		            testName + " removing null expression should have failed");

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		/*
		 * Copy expressions in a new list and shuffle them
		 * Then remove them one by one
		 */
		List<Expression<Number>> expressionsCopy = new ArrayList<>(testModel.getExpressions());
		Collections.shuffle(expressionsCopy);
		for (Expression<Number> expression : expressionsCopy)
		{
			boolean removed = testModel.remove(expression);
			assertTrue(removed,
			           testName + " removing expression should have succeeded");
			assertFalse(testModel.getExpressions().contains(expression),
			            testName + " removing expression should have removed it");
			/*
			 * Trying to remove the same expression again should fail
			 */
			removed = testModel.remove(expression);
			assertFalse(removed,
			           testName + " removing expression a second time should" +
					   " have failed");
		}
		assertTrue(testModel.getExpressions().isEmpty(),
		           testName + " unexpected non empty expressions list");
		assertTrue(testModel.getVariables().isEmpty(),
		           testName + " unexpected non empty variables list");
	}

	/**
	 * Test method for {@link ExpressionsModel#load(File, boolean)}
	 * @param info Test infos
	 */
	@Test
	@Order(6)
	@DisplayName("load(File, boolean)")
	final void testLoad(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		/*
		 * Loading a null file should throw a NullPointerException
		 */
		assertThrows(NullPointerException.class, () ->
		{
			testModel.load(null, false);
		}, testName + " loading should have failed with null file");

		/*
		 * Loading a non existant file should throw a FileNotFoundException
		 */
		assertThrows(FileNotFoundException.class, () ->
		{
			testModel.load(new File("assets/NonExistant.txt"), false);
		}, testName + " loading should have failed with non existant file");

		/*
		 * Loading an existing file should succeed
		 */
		File loadedFile1 = new File("assets/Test1.txt");
		assertTrue(loadedFile1.exists(),
		           testName + " unexpected non existant file: "
		               + loadedFile1.getAbsolutePath());
		boolean loaded = false;
		try
		{
			loaded = testModel.load(loadedFile1, false);
		}
		catch (NullPointerException e)
		{
			fail(testName + " unexpected NullPointerException loading file "
			    + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			fail(testName + " unexpected IOException loading file "
			    + e.getLocalizedMessage());
		}
		catch (ParserException e)
		{
			fail(testName + " unexpected ParserException loading file "
			    + e.getLocalizedMessage());
		}

		assertTrue(loaded, testName + " loading file should have succeeded");
		File file = testModel.getFile();
		assertNotNull(file, testName + " unexpected null file");
		assertEquals(loadedFile1.getAbsolutePath(),
		             file.getAbsolutePath(),
		             testName + " unexpected file");
		/*
		 * Appeding an existing file should succeed
		 * 	Test4.txt Should add 4 expressions
		 */
		File loadedFile2 = new File("assets/Test4.txt");
		assertTrue(loadedFile2.exists(),
		           testName + " unexpected non existant file: "
		               + loadedFile2.getAbsolutePath());
		loaded = false;
		try
		{
			loaded = testModel.load(loadedFile2, true);
		}
		catch (NullPointerException e)
		{
			fail(testName + " unexpected NullPointerException loading file "
			    + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			fail(testName + " unexpected IOException loading file "
			    + e.getLocalizedMessage());
		}
		catch (ParserException e)
		{
			fail(testName + " unexpected ParserException loading file "
			    + e.getLocalizedMessage());
		}

		assertTrue(loaded, testName + " loading file should have succeeded");
		file = testModel.getFile();
		assertNotNull(file, testName + " unexpected null file");
		assertEquals(loadedFile2.getAbsolutePath(),
		             file.getAbsolutePath(),
		             testName + " unexpected file");
	}

	/**
	 * Test method for {@link ExpressionsModel#save(File)}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("save(File)")
	@Order(7)
	final void testSave(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);
		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				fail(testName + " parsing failed for context: " + context
				    + " with exception: " + e.getMessage());
			}
		}

		/*
		 * Saving a null file should return false
		 */
		boolean saved = false;
		try
		{
			saved = testModel.save(null);
		}
		catch (IOException e)
		{
			fail(testName + " unexpected IOException saving to null file "
			    + e.getLocalizedMessage());
		}
		assertFalse(saved,
		            testName + " saving should have failed with null file");

		/*
		 * Remove expression 1 so that no expression provide a value for
		 * variable "a" forcing save to create assignments
		 */
		boolean removed = testModel.remove(testModel.getExpressions().get(1));
		assertTrue(removed,
		           testName + " removing expression should have succeeded");

		/*
		 * Saving to a non existing file should succeed
		 */
		File saveFile = new File("assets/SaveTest0.txt");
		if (saveFile.exists())
		{
			saveFile.delete();
		}
		assertFalse(saveFile.exists(),
		           testName + " unexpected non existant file: "
		               + saveFile.getAbsolutePath());
		saved = false;
		try
		{
			saved = testModel.save(saveFile);
		}
		catch (IOException e)
		{
			fail(testName + " unexpected IOException saving file "
			    + saveFile.getAbsolutePath() + " with exception: "
			    + e.getLocalizedMessage());
		}
		assertTrue(saved,
		           testName + " saving file should have succeeded");
		File file = testModel.getFile();
		assertTrue(file.exists(),
			       testName + " unexpected non existant file: " +
				   file.getAbsolutePath());
		assertNotNull(file, testName + " unexpected null file");
		assertEquals(saveFile.getAbsolutePath(),
		             file.getAbsolutePath(),
		             testName + " unexpected file");
		File referenceFile = new File("assets/SaveReference0.txt");
		assertTrue(referenceFile.exists(),
		           testName + " unexpected non existant file: "
		               + referenceFile.getAbsolutePath());
		try
		{
			assertEquals(Files.readString(referenceFile.toPath()),
			             Files.readString(saveFile.toPath()),
			             testName + " unexpected file content");
		}
		catch (IOException e)
		{
			fail(testName + " unexpected IOException reading file "
			    + referenceFile.getAbsolutePath() + " with exception: "
			    + e.getLocalizedMessage());
		}
	}

	/**
	 * Test method for {@link ExpressionsModel#toString()}
	 * @param specimen numbre specimen
	 * @param info Test infos
	 */
	@ParameterizedTest(name = "toString()")
	@MethodSource("numberProvider")
	@DisplayName("toString()")
	@Order(8)
	final void testToString(Number specimen, TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen, parentLogger);
		assertNotNull(testModel);

		/*
		 * First parse the valid contexts
		 */
		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		/*
		 * Store individual expressions strings
		 */
		Collection<String> expressionsStrings =
		    new ArrayList<>(testModel.getExpressions().size());
		for (Expression<Number> expression : testModel.getExpressions())
		{
			expressionsStrings.add(expression.toString());
		}
		String expectedString = String.join(ExpressionParser.Separator, expressionsStrings);
		expectedString = expectedString + ExpressionParser.Separator;

		/*
		 * Tests the model string
		 */
		String modelString = testModel.toString();
		assertNotNull(modelString,
		              testName + " unexpected null model string");
		assertFalse(modelString.isEmpty(),
		           testName + " unexpected empty model string");
		assertEquals(expectedString,
		             modelString,
		             testName + " unexpected model string");

		/*
		 * Remove expression 1 so that no expression provide a value for "a"
		 */
		AssignmentExpression<Number> removedAssignment = (AssignmentExpression<Number>)testModel.getExpressions().get(1);
		boolean removed = testModel.remove(removedAssignment);
		assertTrue(removed,
		           testName + " removing expression should have succeeded");
		StringBuilder sb = new StringBuilder();
		sb.append(removedAssignment.toString());
		for (Expression<Number> expression : testModel.getExpressions())
		{
			sb.append(ExpressionParser.Separator);
			sb.append(expression.toString());
		}
		sb.append(ExpressionParser.Separator);
		expectedString = sb.toString();
		modelString = testModel.toString();
		assertNotNull(modelString,
		              testName + " unexpected null model string");
		assertFalse(modelString.isEmpty(),
		           testName + " unexpected empty model string");
		assertEquals(expectedString,
		             modelString,
		             testName + " unexpected model string");
	}

	/**
	 * Test method for {@link ExpressionsModel#specimenProperty()}
	 * @param specimen number specimen
	 * @param info Test infos
	 */
	@ParameterizedTest(name = "specimenProperty()")
	@MethodSource("numberProvider")
	@DisplayName("specimenProperty(")
	@Order(14)
	final void testSpecimenProperty(Number specimen, TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen, parentLogger);
		assertNotNull(testModel);

		ObjectProperty<Number> specimenProperty = testModel.specimenProperty();
		assertNotNull(specimenProperty, testName + " unexpected null property");
		assertFalse(specimenProperty.isBound(),
		            testName + " unexpected bound property");
		assertEquals(specimen,
		             specimenProperty.get(),
		             testName + " unexpected property value");

		for (int i = 0; i < specimenValues.length; i++)
		{
			testModel.setNumberType(specimenValues[i]);
			assertTrue(logHandler.isEmpty(), testName + " unexpected log message");
			assertEquals(specimenValues[i],
			             specimenProperty.get(),
			             testName + " unexpected property value");
		}

		specimenProperty.bind(this.specimen);
		this.specimen.set(12); // Any value not in specimenValues
		assertTrue(specimenProperty.isBound(),
		           testName + " specimen property should be bound");
		Number currentValue = specimenProperty.get();
		for (int i = 0; i < specimenValues.length; i++)
		{
			testModel.setNumberType(specimenValues[i]);
			assertFalse(logHandler.isEmpty(), testName + " unexpected no log message");
			LogRecord lastRecord = logHandler.pop();
			assertEquals(Level.WARNING,
			             lastRecord.getLevel(),
			             testName + " unexpected log level");
			logHandler.flush();
			assertNotEquals(specimenValues[i],
			                specimenProperty.get(),
			                testName + " unexpectedly changed bound property");
			assertEquals(currentValue,
			             specimenProperty.get(),
			             testName + " unexpected inequality");
		}
	}

	/**
	 * Test method for {@link ExpressionsModel#getSpecimen()}
	 * @param specimen number specimen
	 * @param info Test infos
	 */
	@ParameterizedTest(name = "getSpecimen()")
	@MethodSource("numberProvider")
	@DisplayName("getSpecimen()")
	@Order(15)
	final void testGetSpecimen(Number specimen, TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen, parentLogger);
		assertNotNull(testModel);

		assertEquals(specimen,
		             testModel.getSpecimen(),
		             testName + " unexpected specimen value");

		for (int i = 0; i < specimenValues.length; i++)
		{
			testModel.setNumberType(specimenValues[i]);
			assertTrue(logHandler.isEmpty(), testName + " unexpected log message");
			assertEquals(specimenValues[i],
			             testModel.getSpecimen(),
			             testName + " unexpected specimen value");
		}
	}

	/**
	 * Test method for {@link ExpressionsModel#setSpecimen(Number)}
	 * @param specimen number specimen
	 * @apiNote setSpecimen is deprecated
	 * @param info Test infos
	 */
	@SuppressWarnings({ "javadoc", "deprecation" }) // Because setSpecimen is deprecated
	@ParameterizedTest(name = "setSpecimen({0})")
	@MethodSource("numberProvider")
	@DisplayName("setSpecimen(E)")
	@Order(16)
	final void testSetSpecimen(Number specimen, TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen, parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < specimenValues.length; i++)
		{
			testModel.setSpecimen(specimenValues[i]);
			assertTrue(logHandler.isEmpty(), testName + " unexpected log message");
			assertEquals(specimenValues[i],
			             testModel.getSpecimen(),
			             testName + " unexpected specimen value");
		}

		testModel.specimenProperty().bind(this.specimen);
		this.specimen.set(12); // Any value not in specimenValues
		assertTrue(testModel.specimenProperty().isBound(),
		           testName + " specimen property should be bound");

		for (int i = 0; i < specimenValues.length; i++)
		{
			testModel.setSpecimen(specimenValues[i]);
			assertFalse(logHandler.isEmpty(),
			            testName + " unexpected no log message");
			LogRecord record = logHandler.pop();
			assertEquals(Level.WARNING,
			             record.getLevel(),
			             testName + " unexpected log level");
			logHandler.flush();
			assertNotEquals(specimenValues[i],
			                testModel.getSpecimen(),
			                testName + " unexpected specimen value");
		}
	}

	/**
	 * Test method for {@link ExpressionsModel#rootItemProperty()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("rootItemProperty()")
	@Order(17)
	final void testRootItemProperty(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		ObjectProperty<TreeItem<Expression<Number>>> rootProperty =
		    testModel.rootItemProperty();
		assertNotNull(rootProperty, testName + " unexpcted null property");
		assertFalse(rootProperty.isBound(), testName + " unexpected bound property");

		TreeItem<Expression<Number>> rootItem = rootProperty.get();
		assertNotNull(rootItem, testName + " unexpected null root item");

		int parsedCount = 0;
		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
				parsedCount += parsed ? 1 : 0;
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		/*
		 * rootItem itself doesn't change but its content changes
		 */
		assertSame(rootItem,
		           rootProperty.get(),
		           testName + " unexpected changed rootItem");
		assertEquals(parsedCount,
		             rootItem.getChildren().size(),
		             testName + " unexpected rootItem children count");

		/*
		 * Binding to another property
		 */
		Expression<Number> newRoot =
		    new AssignmentExpression<>(new VariableExpression<Number>("what ?"),
		                               new ConstantExpression<Number>(0.0f));
		TreeItem<Expression<Number>> newRootItem = new ExpressionTreeItem<>(newRoot);
		ObjectProperty<TreeItem<Expression<Number>>> newRootProperty =
		    new SimpleObjectProperty<TreeItem<Expression<Number>>>(newRootItem);
		rootProperty.bind(newRootProperty);
		assertTrue(rootProperty.isBound(),
		           testName + " unexpected unbound root item property");

		/*
		 * Setting values on rootProperty should fail (with a log message) now
		 */
		Expression<Number> otherRoot =
		    new VariableExpression<Number>("testName", 3);
		ExpressionTreeItem<Number> otherItem = new ExpressionTreeItem<>(otherRoot);

		testModel.setRootItem(otherItem);

		assertNotEquals(otherRoot,
		                rootProperty.get(),
		                testName + " unexpected root property value");
		assertFalse(logHandler.isEmpty(),
		            testName + " unexpectedly no log messages");
		LogRecord lastRecord = logHandler.pop();
		assertEquals(Level.WARNING,
		             lastRecord.getLevel(),
		             testName + " unexpected log message level");
		assertEquals(newRootItem,
		             rootProperty.get(),
		             testName + " unexpected root property value");
	}

	/**
	 * Test method for {@link ExpressionsModel#getRootItem()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("getRootItem()")
	@Order(18)
	final void testGetRootItem(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		TreeItem<Expression<Number>> rootItem = testModel.getRootItem();
		assertNotNull(rootItem, testName + " unexpected null root item");

		int parsedCount = 0;
		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
				parsedCount += parsed ? 1 : 0;
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		/*
		 * rootItem itself doesn't change but its content changes
		 */
		assertSame(rootItem,
		           testModel.getRootItem(),
		           testName + " unexpected changed rootItem");
		assertEquals(parsedCount,
		             rootItem.getChildren().size(),
		             testName + " unexpected rootItem children count");
	}

	/**
	 * Test method for {@link ExpressionsModel#setRootItem(ExpressionTreeItem)}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("setRootItem(ExpressionTreeItem)")
	@Order(19)
	final void testSetRootItem(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		TreeItem<Expression<Number>> rootItem = testModel.getRootItem();

		/*
		 * Binding to another property
		 */
		Expression<Number> newRoot =
		    new AssignmentExpression<>(new VariableExpression<Number>("root"),
		                               new ConstantExpression<Number>(1.0f));
		ExpressionTreeItem<Number> newRootItem =
		    new ExpressionTreeItem<>(newRoot);

		ObjectProperty<TreeItem<Expression<Number>>> newRootProperty =
		    new SimpleObjectProperty<TreeItem<Expression<Number>>>(newRootItem);

		testModel.rootItemProperty().bind(newRootProperty);
		assertTrue(testModel.rootItemProperty().isBound(),
		           testName + " unexpected unbound root item property");
		assertNotEquals(rootItem,
		                testModel.getRootItem(),
		                testName + " unexpected root item");

		assertEquals(newRootItem,
		             testModel.getRootItem(),
		             testName + " unexpected root item");

		/*
		 * Setting another rootItem should fail (with a log message) now
		 */
		Expression<Number> otherRoot =
		    new AssignmentExpression<>(new VariableExpression<Number>("what ?"),
		                               new ConstantExpression<Number>(0.0f));
		ExpressionTreeItem<Number> otherRootItem =
		    new ExpressionTreeItem<>(otherRoot);

		testModel.setRootItem(otherRootItem);

		assertNotEquals(otherRootItem,
		                testModel.getRootItem(),
		                testName + " unexpected root item");
		assertEquals(newRootItem,
		             testModel.getRootItem(),
		             testName + " unexpected root item");
		assertFalse(logHandler.isEmpty(),
		            testName + " unexpectedly no log messages");
		LogRecord lastRecord = logHandler.pop();
		assertEquals(Level.WARNING,
		             lastRecord.getLevel(),
		             testName + " unexpected log message level");
	}

	/**
	 * Test method for {@link ExpressionsModel#operatorFilteringProperty()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("operatorFilteringProperty()")
	@Order(20)
	final void testOperatorFilteringProperty(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		ObjectProperty<BinaryOperatorRules> operatorFiltering =
		    testModel.operatorFilteringProperty();
		assertNotNull(operatorFiltering,
		              testName + " unexpected null operator filtering property");
		assertFalse(operatorFiltering.isBound(),
		           testName + " unexpected bound operator filtering property");
		assertEquals(BinaryOperatorRules.ANY,
		             operatorFiltering.get(),
		             testName + " unexpected operator filtering value");

		/*
		 * Valid contexts should be:
		 * "a+b",
		 * "a=1",
		 * "b=2",
		 * "c = 3.3"
		 * filtering on "=" should remove "a+b"
		 */
		List<Expression<Number>> allExpressions = new ArrayList<>(testModel.getExpressions());
		int allExpressionsCount = allExpressions.size();
		Expression<Number> firstExpression = allExpressions.get(0);

		operatorFiltering.set(BinaryOperatorRules.ASSIGNMENT);
		List<Expression<Number>> filteredExpressions = testModel.getExpressions();
		assertEquals(allExpressionsCount - 1,
		             filteredExpressions.size(),
		             testName + " unexpected expressions list size");
		assertFalse(filteredExpressions.contains(firstExpression),
		            testName + " unexpected expression: " + firstExpression);
		assertTrue(allExpressions.containsAll(filteredExpressions),
		           testName + " unexpected filtered expressions not contained "
		               + "in all expressions");

		operatorFiltering.set(BinaryOperatorRules.ADDITION);
		filteredExpressions = testModel.getExpressions();
		assertEquals(1,
		             filteredExpressions.size(),
		             testName + " unexpected expressions list size");
		assertTrue(filteredExpressions.contains(firstExpression),
		           testName + " expected expression: " + firstExpression);

		/*
		 * Binding to another property
		 */
		ObjectProperty<BinaryOperatorRules> myProperty =
		    new SimpleObjectProperty<BinaryOperatorRules>(BinaryOperatorRules.DIVISION);
		operatorFiltering.bind(myProperty);

		assertEquals(BinaryOperatorRules.DIVISION,
		             operatorFiltering.get(),
		             testName + " unexpected operator filtering value");
		assertTrue(operatorFiltering.isBound(),
		           testName + " unexpected unbound operator filtering property");
	}

	/**
	 * Test method for {@link ExpressionsModel#getOperatorFiltering()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("getOperatorFiltering()")
	@Order(21)
	final void testGetOperatorFiltering(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		ObjectProperty<BinaryOperatorRules> operatorFiltering =
		    testModel.operatorFilteringProperty();
		assertEquals(operatorFiltering.get(),
		             testModel.getOperatorFiltering(),
		             testName + " unexpected operator filtering value");

		/*
		 * Binding to another property
		 */
		ObjectProperty<BinaryOperatorRules> myProperty =
		    new SimpleObjectProperty<BinaryOperatorRules>(BinaryOperatorRules.DIVISION);
		operatorFiltering.bind(myProperty);

		assertEquals(myProperty.get(),
		             testModel.getOperatorFiltering(),
		             testName + " unexpected operator filtering value");
	}

	/**
	 * Test method for {@link ExpressionsModel#setOperatorFiltering(BinaryOperatorRules)}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("setOperatorFiltering(BinaryOperatorRules)")
	@Order(22)
	final void testSetOperatorFiltering(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		/*
		 * Valid contexts should be:
		 * "a+b",
		 * "a=1",
		 * "b=2",
		 * "c = 3.3"
		 * filtering on "=" should remove "a+b"
		 */
		List<Expression<Number>> allExpressions =
		    new ArrayList<>(testModel.getExpressions());
		int allExpressionsCount = allExpressions.size();
		Expression<Number> firstExpression = allExpressions.get(0);

		testModel.setOperatorFiltering(BinaryOperatorRules.ASSIGNMENT);
		List<Expression<Number>> filteredExpressions =
		    testModel.getExpressions();
		assertEquals(allExpressionsCount - 1,
		             filteredExpressions.size(),
		             testName + " unexpected expressions list size");
		assertFalse(filteredExpressions.contains(firstExpression),
		            testName + " unexpected expression: " + firstExpression);

		testModel.setOperatorFiltering(BinaryOperatorRules.ADDITION);
		filteredExpressions = testModel.getExpressions();
		assertEquals(1,
		             filteredExpressions.size(),
		             testName + " unexpected expressions list size");
		assertTrue(filteredExpressions.contains(firstExpression),
		           testName + " expected expression: " + firstExpression);

		/*
		 * Binding to another property should prevent setting the value
		 */
		BinaryOperatorRules expectedValue = BinaryOperatorRules.DIVISION;
		ObjectProperty<BinaryOperatorRules> myProperty =
		    new SimpleObjectProperty<BinaryOperatorRules>(expectedValue);
		testModel.operatorFilteringProperty().bind(myProperty);

		BinaryOperatorRules testValue = BinaryOperatorRules.MULTIPLICATION;
		testModel.setOperatorFiltering(testValue);
		assertNotEquals(testValue,
		                testModel.getOperatorFiltering(),
		                testName + " unexpected operator filtering value");
		assertEquals(expectedValue,
		             testModel.getOperatorFiltering(),
		             testName + " unexpected operator filtering value");
		assertFalse(logHandler.isEmpty(),
		            testName + " unexpectedly no log messages");
		LogRecord lastRecord = logHandler.pop();
		assertEquals(Level.WARNING,
		             lastRecord.getLevel(),
		             testName + " unexpected log message level");
	}

	/**
	 * Test method for {@link ExpressionsModel#operandFilteringProperty()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("operandFilteringProperty()")
	@Order(23)
	final void testOperandFilteringProperty(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		ObjectProperty<TerminalType> operandFiltering =
		    testModel.operandFilteringProperty();
		assertNotNull(operandFiltering,
		              testName
		                  + " unexpected null operand filtering property");
		assertFalse(operandFiltering.isBound(),
		            testName + " unexpected bound operand filtering property");
		assertEquals(TerminalType.ALL,
		             operandFiltering.get(),
		             testName + " unexpected operand filtering value");

		/*
		 * Valid contexts should be:
		 * "a+b",
		 * "a=1",
		 * "b=2",
		 * "c = 3.3"
		 * filtering on Variable "a" should leave "a+b" and "a=1"
		 * filtering on Constants should remove "a+b"
		 */
		List<Expression<Number>> allExpressions =
		    new ArrayList<>(testModel.getExpressions());
		int allExpressionsCount = allExpressions.size();
		Expression<Number> firstExpression = allExpressions.get(0);

		operandFiltering.set(TerminalType.VARIABLES);
		List<Expression<Number>> filteredExpressions =
		    testModel.getExpressions();
		// All expressions have variables to filtered should be unchanged
		assertIterableEquals(allExpressions,
		                     filteredExpressions,
		                     testName + " unexpected filtered expressions list");

		operandFiltering.set(TerminalType.CONSTANTS);
		filteredExpressions = testModel.getExpressions();
		assertEquals(allExpressionsCount - 1,
		             filteredExpressions.size(),
		             testName + " unexpected filtered expressions list size");
		assertFalse(filteredExpressions.contains(firstExpression),
		            testName + " unexpected expression: " + firstExpression);
		assertTrue(allExpressions.containsAll(filteredExpressions),
		           testName + " unexpected filtered expressions not contained "
		               + "in all expressions");

		/*
		 * Binding to another property
		 */
		ObjectProperty<TerminalType> myProperty =
		    new SimpleObjectProperty<TerminalType>(TerminalType.VARIABLES);
		operandFiltering.bind(myProperty);
		assertEquals(TerminalType.VARIABLES,
		             operandFiltering.get(),
		             testName + " unexpected operand filtering value");
		assertTrue(operandFiltering.isBound(),
		           testName + " unexpected unbound operand filtering property");

		/*
		 * Setting a value to operandFiltering should fail (with a log message)
		 */
		testModel.setOperandFiltering(TerminalType.ALL);
		assertNotEquals(TerminalType.ALL,
		                operandFiltering.get(),
		                testName + " unexpected operand filtering value");
		assertEquals(TerminalType.VARIABLES,
		             operandFiltering.get(),
		             testName + " unexpected operand filtering value");
		assertFalse(logHandler.isEmpty(),
		            testName + " unexpectedly no log messages");
		LogRecord lastRecord = logHandler.pop();
		assertEquals(Level.WARNING,
		             lastRecord.getLevel(),
		             testName + " unexpected log message level");
	}

	/**
	 * Test method for {@link ExpressionsModel#getOperandFiltering()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("getOperandFiltering(")
	@Order(24)
	final void testGetOperandFiltering(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		assertEquals(TerminalType.ALL,
		             testModel.getOperandFiltering(),
		             testName + " unexpected operand filtering value");


		/*
		 * Binding to another property
		 */
		ObjectProperty<TerminalType> myProperty =
		    new SimpleObjectProperty<TerminalType>(TerminalType.VARIABLES);
		testModel.operandFilteringProperty().bind(myProperty);
		assertEquals(TerminalType.VARIABLES,
		             testModel.getOperandFiltering(),
		             testName + " unexpected operand filtering value");
	}

	/**
	 * Test method for {@link ExpressionsModel#setOperandFiltering(TerminalType)}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("setOperandFiltering(TerminalType)")
	@Order(25)
	final void testSetOperandFiltering(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		/*
		 * Valid contexts should be:
		 * "a+b",
		 * "a=1",
		 * "b=2",
		 * "c = 3.3"
		 * filtering on Variable "a" should leave "a+b" and "a=1"
		 * filtering on Constants should remove "a+b"
		 */
		List<Expression<Number>> allExpressions =
		    new ArrayList<>(testModel.getExpressions());
		int allExpressionsCount = allExpressions.size();
		Expression<Number> firstExpression = allExpressions.get(0);

		testModel.setOperandFiltering(TerminalType.VARIABLES);
		assertEquals(TerminalType.VARIABLES,
		             testModel.getOperandFiltering(),
		             testName + " unexpected operand filtering value");
		List<Expression<Number>> filteredExpressions =
		    testModel.getExpressions();
		// All expressions have variables to filtered should be unchanged
		assertIterableEquals(allExpressions,
		                     filteredExpressions,
		                     testName
		                         + " unexpected filtered expressions list");

		testModel.setOperandFiltering(TerminalType.CONSTANTS);
		assertEquals(TerminalType.CONSTANTS,
		             testModel.getOperandFiltering(),
		             testName + " unexpected operand filtering value");
		filteredExpressions = testModel.getExpressions();
		assertEquals(allExpressionsCount - 1,
		             filteredExpressions.size(),
		             testName + " unexpected filtered expressions list size");
		assertFalse(filteredExpressions.contains(firstExpression),
		            testName + " unexpected expression: " + firstExpression);

		/*
		 * Binding to another property
		 */
		ObjectProperty<TerminalType> myProperty =
		    new SimpleObjectProperty<TerminalType>(TerminalType.VARIABLES);
		testModel.operandFilteringProperty().bind(myProperty);
		assertEquals(TerminalType.VARIABLES,
		             testModel.getOperandFiltering(),
		             testName + " unexpected operand filtering value");

		/*
		 * Setting a value to operandFiltering should fail (with a log message)
		 */
		testModel.setOperandFiltering(TerminalType.ALL);
		assertNotEquals(TerminalType.ALL,
		                testModel.getOperandFiltering(),
		                testName + " unexpected operand filtering value");
		assertEquals(TerminalType.VARIABLES,
		             testModel.getOperandFiltering(),
		             testName + " unexpected operand filtering value");
		assertFalse(logHandler.isEmpty(),
		            testName + " unexpectedly no log messages");
		LogRecord lastRecord = logHandler.pop();
		assertEquals(Level.WARNING,
		             lastRecord.getLevel(),
		             testName + " unexpected log message level");
	}

	/**
	 * Test method for {@link ExpressionsModel#nameFilteringProperty()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("nameFilteringProperty()")
	@Order(26)
	final void testNameFilteringProperty(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		StringProperty nameFiltering =
		    testModel.nameFilteringProperty();
		assertNotNull(nameFiltering,
		              testName + " unexpected null name filtering property");
		assertFalse(nameFiltering.isBound(),
		           testName + " unexpected bound name filtering property");
		assertTrue(nameFiltering.isEmpty().getValue(),
		           testName + " unexpected non empty name filtering value");
		assertNull(nameFiltering.get(),
		           testName + " unexpected non null name filtering value"
		               + " --> Initialize nameFiltering to \"no value\"");
		/*
		 * Valid contexts should be:
		 * "a+b",
		 * "a=1",
		 * "b=2",
		 * "c = 3.3"
		 * filtering on "a" should leave "a+b" and "a=1"
		 */
		List<Expression<Number>> allExpressions =
		    new ArrayList<>(testModel.getExpressions());
		Expression<Number> firstExpression = allExpressions.get(0);
		Expression<Number> secondExpression = allExpressions.get(1);

		nameFiltering.set("a");
		List<Expression<Number>> filteredExpressions =
		    testModel.getExpressions();
		assertEquals(2,
		             filteredExpressions.size(),
		             testName + " unexpected filtered expressions list size");
		assertTrue(filteredExpressions.contains(firstExpression),
		           testName + " expected expression: " + firstExpression);
		assertTrue(filteredExpressions.contains(secondExpression),
		           testName + " expected expression: " + secondExpression);
		assertTrue(allExpressions.containsAll(filteredExpressions),
		           testName + " unexpected filtered expressions not contained "
		           	+ "in all expressions");

		/*
		 * Binding to another property
		 */
		StringProperty myProperty =
		    new SimpleStringProperty("b");
		nameFiltering.bind(myProperty);
		assertEquals("b",
		             nameFiltering.get(),
		             testName + " unexpected name filtering value");
		assertTrue(nameFiltering.isBound(),
		           testName + " unexpected unbound name filtering property");
		/*
		 * Setting a value to nameFiltering should fail (with a log message)
		 */
		testModel.setNameFiltering("a");
		assertNotEquals("a",
		                nameFiltering.get(),
		                testName + " unexpected name filtering value");
		assertEquals("b",
		             nameFiltering.get(),
		             testName + " unexpected name filtering value");
		assertFalse(logHandler.isEmpty(),
		            testName + " unexpectedly no log messages");
		LogRecord lastRecord = logHandler.pop();
		assertEquals(Level.WARNING,
		             lastRecord.getLevel(),
		             testName + " unexpected log message level");
	}

	/**
	 * Test method for {@link ExpressionsModel#getNameFiltering()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("getNameFiltering()")
	@Order(27)
	final void testGetNameFiltering(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		assertNull(testModel.getNameFiltering(),
		           testName + " unexpected non null name filtering value");
		/*
		 * Valid contexts should be:
		 * "a+b",
		 * "a=1",
		 * "b=2",
		 * "c = 3.3"
		 * filtering on "a" should leave "a+b" and "a=1"
		 */

		testModel.nameFilteringProperty().set("a");
		assertEquals("a",
		             testModel.getNameFiltering(),
		             testName + " unexpected name filtering value");

		/*
		 * Binding to another property
		 */
		StringProperty myProperty = new SimpleStringProperty("b");
		testModel.nameFilteringProperty().bind(myProperty);
		assertEquals("b",
		             testModel.getNameFiltering(),
		             testName + " unexpected name filtering value");
	}

	/**
	 * Test method for {@link ExpressionsModel#setNameFiltering(String)}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("setNameFiltering(String)")
	@Order(28)
	final void testSetNameFiltering(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}

		/*
		 * Valid contexts should be:
		 * "a+b",
		 * "a=1",
		 * "b=2",
		 * "c = 3.3"
		 * filtering on "a" should leave "a+b" and "a=1"
		 */
		List<Expression<Number>> allExpressions =
		    new ArrayList<>(testModel.getExpressions());
		Expression<Number> firstExpression = allExpressions.get(0);
		Expression<Number> secondExpression = allExpressions.get(1);

		testModel.setNameFiltering("a");
		assertEquals("a",
		             testModel.getNameFiltering(),
		             testName + " unexpected name filtering value");
		List<Expression<Number>> filteredExpressions =
		    testModel.getExpressions();
		assertEquals(2,
		             filteredExpressions.size(),
		             testName + " unexpected filtered expressions list size");
		assertTrue(filteredExpressions.contains(firstExpression),
		           testName + " expected expression: " + firstExpression);
		assertTrue(filteredExpressions.contains(secondExpression),
		           testName + " expected expression: " + secondExpression);
		assertTrue(allExpressions.containsAll(filteredExpressions),
		           testName + " unexpected filtered expressions not contained "
		               + "in all expressions");

		/*
		 * Binding to another property
		 */
		StringProperty myProperty = new SimpleStringProperty("b");
		testModel.nameFilteringProperty().bind(myProperty);
		assertEquals("b",
		             testModel.getNameFiltering(),
		             testName + " unexpected name filtering value");
		/*
		 * Setting a value to nameFiltering should fail (with a log message)
		 */
		testModel.setNameFiltering("a");
		assertNotEquals("a",
		                testModel.getNameFiltering(),
		                testName + " unexpected name filtering value");
		assertEquals("b",
		             testModel.getNameFiltering(),
		             testName + " unexpected name filtering value");
		assertFalse(logHandler.isEmpty(),
		            testName + " unexpectedly no log messages");
		LogRecord lastRecord = logHandler.pop();
		assertEquals(Level.WARNING,
		             lastRecord.getLevel(),
		             testName + " unexpected log message level");
	}

	/**
	 * Test method for {@link ExpressionsModel#hasFileProperty()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("hasFileProperty()")
	@Order(29)
	final void testHasFileProperty(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		ReadOnlyBooleanProperty hasFileProperty = testModel.hasFileProperty();
		assertNotNull(hasFileProperty,
		              testName + " unexpected null hasFile property");
		assertFalse(hasFileProperty.get(),
		            testName + " unexpected non false hasFile property");

		/*
		 * Loading an existing file should succeed
		 */
		File loadedFile1 = new File("assets/Test1.txt");
		assertTrue(loadedFile1.exists(),
		           testName + " unexpected non existant file: "
		               + loadedFile1.getAbsolutePath());
		boolean loaded = false;
		try
		{
			loaded = testModel.load(loadedFile1, false);
		}
		catch (NullPointerException e)
		{
			fail(testName + " unexpected NullPointerException loading file "
			    + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			fail(testName + " unexpected IOException loading file "
			    + e.getLocalizedMessage());
		}
		catch (ParserException e)
		{
			fail(testName + " unexpected ParserException loading file "
			    + e.getLocalizedMessage());
		}
		assertTrue(loaded, testName + " loading file should have succeeded");
		assertTrue(hasFileProperty.get(),
		           testName + " unexpected false hasFile property");

		testModel.resetFile();
		assertFalse(hasFileProperty.get(),
		            testName + " unexpected non false hasFile property");
	}

	/**
	 * Test method for {@link ExpressionsModel#hasFile()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("hasFile()")
	@Order(30)
	final void testHasFile(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);
		assertFalse(testModel.hasFile(),
		            testName + " unexpected file status");

		File loadedFile = new File("assets/Test1.txt");
		assertTrue(loadedFile.exists(),
		           testName + " unexpected non existant file: "
		               + loadedFile.getAbsolutePath());
		boolean loaded = false;
		try
		{
			loaded = testModel.load(loadedFile, false);
		}
		catch (NullPointerException e)
		{
			fail(testName + " unexpected NullPointerException loading file "
			    + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			fail(testName + " unexpected IOException loading file "
			    + e.getLocalizedMessage());
		}
		catch (ParserException e)
		{
			fail(testName + " unexpected ParserException loading file "
			    + e.getLocalizedMessage());
		}

		assertTrue(loaded, testName + " loading file should have succeeded");
		assertTrue(testModel.hasFile(),
		           testName + " unexpected file status");

		testModel.resetFile();
		assertFalse(testModel.hasFile(),
		            testName + " unexpected file status");
	}

	/**
	 * Test method for {@link ExpressionsModel#refreshRoot()}
	 * @param info Test infos
	 */
	@Test
	@DisplayName("refreshRoot()")
	@Order(31)
	final void testRefreshRoot(TestInfo info)
	{
		String testName = info.getDisplayName();
		testModel = new ExpressionsModel<>(specimen.get(), parentLogger);
		assertNotNull(testModel);

		/*
		 * Initial root
		 */
		TreeItem<Expression<Number>> rootItem = testModel.getRootItem();
		assertNotNull(rootItem,
		              testName + " unexpected null root item");
		assertEquals(0,
		             rootItem.getChildren().size(),
		             testName + " unexpected root item children size");
		/*
		 * root item during parsing
		 */
		int expectedChildrenCount = 0;
		for (int i = 0; i < validContexts.length; i++)
		{
			String context = validContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.parse(context);
				assertTrue(parsed,
				           testName + " parsing failed for context: "
				               + context);
				assertEquals(++expectedChildrenCount,
				             rootItem.getChildren().size(),
				             testName + " unexpected root item children size");
			}
			catch (ParserException e)
			{
				if ((specimen.get().getClass() != Integer.class)
				    || (i != (validContexts.length - 1)))
				{
					// Float and Double parsing should not fail
					fail(testName + " parsing failed for context: " + context
					    + " with exception: " + e.getMessage());
				}
				else
				{
					// Nothing : expected
				}
			}
		}
		/*
		 * Root item during reparsing
		 */
		List<Expression<Number>> expressions = testModel.getExpressions();
		int nbContexts = expressions.size();
		List<TreeItem<Expression<Number>>> childrenItems = new ArrayList<>(rootItem.getChildren());
		for (int i = 0; i < nbContexts; i++)
		{
			Expression<Number> oldExpression = expressions.get(i);
			String context = otherValidContexts[i];
			boolean parsed = false;
			try
			{
				parsed = testModel.reparse(oldExpression, context);
				assertTrue(parsed,
				           testName + "re-parsing failed for context: "
				               + context);
				List<TreeItem<Expression<Number>>> newChildrenItems =
				    new ArrayList<>(rootItem.getChildren());
				assertFalse(childrenItems.containsAll(newChildrenItems),
				            testName + " unexpected unchanged chlidren items");
				childrenItems = newChildrenItems;
			}
			catch (ParserException e)
			{
				fail(testName + " parsing failed for context: " + context
				    + " with exception: " + e.getMessage());
			}
		}

		 /*
		  * Root item during remove
		  */
		List<Expression<Number>> allExpressions =
		    new ArrayList<>(testModel.getExpressions());
		Collections.shuffle(allExpressions);
		expectedChildrenCount = rootItem.getChildren().size();
		for (Expression<Number> expression : allExpressions)
		{
			boolean removed = testModel.remove(expression);
			assertTrue(removed,
			           testName + " removing expression failed: "
			               + expression);
			assertEquals(--expectedChildrenCount,
			             rootItem.getChildren().size(),
			             testName + " unexpected root item children size");
		}
	}
}
