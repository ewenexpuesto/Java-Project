package tests;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import expressions.terminal.TerminalExpression;
import expressions.terminal.VariableExpression;

/**
 * Test class for {@link VariableExpression}
 * @author davidroussel
 */
@DisplayName("VariableExpression<E>")
public class VariableExpressionTest
{

	/**
	 * The filled expression to test
	 */
	private VariableExpression<? extends Number> testFilledExpression;

	/**
	 * The empty expression to test
	 */
	private VariableExpression<? extends Number> testEmptyExpression;

	/**
	 * Name of the value
	 */
	private String testValueName = null;

	/**
	 * Value natures to test
	 */
	@SuppressWarnings("unchecked")
	private final static Class<? extends Number>[] valueClasses =
	    (Class<? extends Number>[]) new Class<?>[] {
		Integer.class,
		Float.class,
		Double.class,
		BigDecimal.class	// Should trigger exceptions
	};

	/**
	 * Names for variables
	 */
	private final static String[] names = new String[] {
		"a",
		"b",
		"c",
		"d"
	};

	/**
	 * Suffix for variables
	 */
	private final static String[] suffixes = new String[] {
		"i",
		"f",
		"d",
		"b"
	};

	/**
	 * Index a  particular type in {@link #valueClasses}
	 * @param type the type to search
	 * @return the index of this type in various arrays
	 */
	private static int indexOf(Class<? extends Number> type)
	{
		if (type == Integer.class)
		{
			return 0;
		}
		else if (type == Float.class)
		{
			return 1;
		}
		else if (type == Double.class)
		{
			return 2;
		}
		else if (type == BigDecimal.class)
		{
			return 3;
		}
		else
		{
			return -1;
		}
	}

	/**
	 * List of all possible value types for expressions
	 */
	private static List<Class<? extends Number>> valueClassList = new Vector<Class<? extends Number>>();

	/**
	 * Array of all possible expressions (filled with values)
	 */
	private final static VariableExpression<? extends Number>[] filledExpressions =
		new VariableExpression<?>[valueClasses.length];
	/**
	 * Array of all possible expressions (NOT filled with values)
	 */
	private final static VariableExpression<? extends Number>[] emptyExpressions =
		new VariableExpression<?>[valueClasses.length];

	/**
	 * Map to get a filled expression from value type
	 * @see #setUpBeforeClass()
	 */
	private static Map<Class<? extends Number>, VariableExpression<? extends Number>> filledExpressionsMap =
	    new ConcurrentHashMap<Class<? extends Number>, VariableExpression<? extends Number>>();

	/**
	 * Map to get an empty expression from value type
	 * @see #setUpBeforeClass()
	 */
	private static Map<Class<? extends Number>, VariableExpression<? extends Number>> emptyExpressionsMap =
	    new ConcurrentHashMap<Class<? extends Number>, VariableExpression<? extends Number>>();

	/**
	 * Values to set
	 */
	private final static Number[] values = new Number[] {
		Integer.valueOf(2),
		Float.valueOf(2.0f),
		Double.valueOf(2.0),
		BigDecimal.valueOf(2)
	};

	// /**
	//  * Other Values to set
	//  */
	// private final static Number[] altValues = new Number[] {
	// 	Integer.valueOf(3),
	// 	Float.valueOf(3.0f),
	// 	Double.valueOf(3.0),
	// 	BigDecimal.valueOf(3)
	// };

	/**
	 * Map get get values from type of values
	 */
	private static Map<Class<? extends Number>, Number> valuesMap =
		new ConcurrentHashMap<Class<? extends Number>, Number>();

	/**
	 * Setup before all tests
	 * @throws java.lang.Exception if setup fails
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception
	{
		for (int i = 0; i < valueClasses.length; i++)
		{
			Class<? extends Number> currentValueClass = valueClasses[i];
			int index = indexOf(currentValueClass);
			if (index == -1)
			{
				fail("Unknown index for " + currentValueClass.getSimpleName());
			}

			Number value = values[indexOf(currentValueClass)];

			valueClassList.add(currentValueClass);
			valuesMap.put(currentValueClass, value);
		}
	}

	/**
	 * Value classes provider used in each Parameterized test
	 * @return a stream of value classes to use in each @ParameterizedTest
	 */
	private static Stream<Class<? extends Number>> valueClassesProvider()
	{
		return valueClassList.stream();
	}

	/**
	 * Tear down after all tests
	 * @throws java.lang.Exception if tear down fails
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception
	{
		valueClassList.clear();
		valuesMap.clear();
		filledExpressionsMap.clear();
		emptyExpressionsMap.clear();
		VariableExpression.clearAll();
	}

	/**
	 * Check constructor and build instance
	 * @param type The type of Number contained in variable
	 * @param name the name of the variable (or null if no name is to be set)
	 * @param value the value of the variable (or null if no value is to be set)
	 * @param testName Name of the test where this method is used
	 * @return a {@link VariableExpression} instance of null if such instance
	 * could not be build.
	 */
	@SuppressWarnings("unchecked")
	private static VariableExpression<? extends Number>
	    buildVariable(Class<? extends Number> type,
	                  String name,
	                  Number value,
	                  String testName)
	{
		@SuppressWarnings("rawtypes")
		Constructor<VariableExpression> constructor = null;
		final int nbArgs = (name == null ? 0 : 1) + (value == null ? 0 : 1);
		Class<?>[] argumentTypes = new Class<?>[nbArgs];
		if (nbArgs > 0)
		{
			argumentTypes[0] = String.class;
		}
		if (nbArgs > 1)
		{
			argumentTypes[1] = Number.class;
		}

		try
		{
			constructor = VariableExpression.class.getConstructor(argumentTypes);
		}
		catch (NoSuchMethodException e)
		{
			fail(testName + " missing constructor " + type.getSimpleName() +
			    "(" + argumentTypes + ")" + e.getLocalizedMessage());
		}
		catch (SecurityException e)
		{
			fail(testName + " inaccessible constructor "
			    + e.getLocalizedMessage());
		}

		assertNotNull(constructor,
		              testName + " unexpected null valued constructor");

		Object[] arguments = new Object[nbArgs];
		if (nbArgs > 0)
		{
			arguments[0] = name;
		}
		if (nbArgs > 1)
		{
			arguments[1] = value;
		}
		Object instance = null;
		try
		{
			instance = constructor.newInstance(arguments);
		}
		catch (InstantiationException e)
		{
			fail(testName + " instantiation exception : Abstract class "
			    + e.getLocalizedMessage());
		}
		catch (IllegalAccessException e)
		{
			fail(testName + " valued constructor is inaccessible "
			    + e.getLocalizedMessage());
		}
		catch (IllegalArgumentException e)
		{
			fail(testName + " valued constructor illegal argument "
			    + e.getLocalizedMessage());
		}
		catch (InvocationTargetException e)
		{
			fail(testName + " invoked valued constructor threw an exception "
			    + e.getCause().getLocalizedMessage());
		}

		return (VariableExpression<? extends Number>) instance;
	}

	/**
	 * Setup empty expressions with names and no values.
	 * If {@link #setupEmptyExpressions()} occurs before
	 * {@link #setupFilledExpressions()} then variables in
	 * {@link #emptyExpressionsMap} will have no values.
	 * If {@link #setupEmptyExpressions()} occurs after
	 * {@link #setupFilledExpressions()} and since all variables with the same
	 * name share the same values then variables in {@link #emptyExpressionsMap}
	 * will have values
	 */
	private void setupEmptyExpressions()
	{
		emptyExpressionsMap.clear();

		// Refill expressions cause they might have been modified
		for (int i = 0; i < valueClasses.length; i++)
		{
			Class<? extends Number> currentValueClass = valueClasses[i];
			int index = indexOf(currentValueClass);
			if (index == -1)
			{
				fail("Unknown index for " + currentValueClass.getSimpleName());
			}

			String varName = names[indexOf(currentValueClass)];

			if (currentValueClass == Integer.class)
			{
				emptyExpressions[index] = new VariableExpression<Integer>(varName+"i");
			}
			else if (currentValueClass == Float.class)
			{
				emptyExpressions[index] = new VariableExpression<Float>(varName+"f");
			}
			else if (currentValueClass == Double.class)
			{
				emptyExpressions[index] = new VariableExpression<Double>(varName+"d");
			}
			else if (currentValueClass == BigDecimal.class)
			{
				emptyExpressions[index] = new VariableExpression<BigDecimal>(varName+"b");
			}
			else
			{
				index = 0; // safe but useless
				fail("Unknown expression type : " + currentValueClass.getSimpleName());
			}

			emptyExpressionsMap.put(currentValueClass, emptyExpressions[index]);
		}
	}

	/**
	 * Setup filled expressions with names and values
	 */
	private void setupFilledExpressions()
	{
		filledExpressionsMap.clear();

		// Refill expressions cause they might have been modified
		for (int i = 0; i < valueClasses.length; i++)
		{
			Class<? extends Number> currentValueClass = valueClasses[i];
			int index = indexOf(currentValueClass);
			if (index == -1)
			{
				fail("Unknown index for " + currentValueClass.getSimpleName());
			}

			Number value = values[indexOf(currentValueClass)];
			String varName = names[indexOf(currentValueClass)];

			if (currentValueClass == Integer.class)
			{
				filledExpressions[index] = new VariableExpression<Integer>(varName+"i", (Integer) value);
			}
			else if (currentValueClass == Float.class)
			{
				filledExpressions[index] = new VariableExpression<Float>(varName+"f", (Float) value);
			}
			else if (currentValueClass == Double.class)
			{
				filledExpressions[index] = new VariableExpression<Double>(varName+"d", (Double) value);
			}
			else if (currentValueClass == BigDecimal.class)
			{
				filledExpressions[index] = new VariableExpression<BigDecimal>(varName+"b", (BigDecimal) value);
			}
			else
			{
				index = 0; // safe but useless
				fail("Unknown expression type : " + currentValueClass.getSimpleName());
			}

			filledExpressionsMap.put(currentValueClass, filledExpressions[index]);
		}
	}

	/**
	 * Setup before each test
	 * @throws java.lang.Exception if setup fails
	 */
	@BeforeEach
	void setUp() throws Exception
	{
		setupEmptyExpressions();
		setupFilledExpressions();
	}

	/**
	 * Tear down after each test
	 * @throws java.lang.Exception if tear down fails
	 */
	@AfterEach
	void tearDown() throws Exception
	{
		testEmptyExpression = null;
		testFilledExpression = null;
		testValueName = null;
		VariableExpression.clearAll();
	}
	/**
	 * Expression factory based on the value class
	 * @param valueClass the value class to use
	 * @param filled indeicates if the Varriable should be filled with value or not
	 * @return a new {@link VariableExpression} instance with the specified
	 * value class or null if the valueClass is not Integer, Float or Double
	 */
	private VariableExpression<? extends Number>
	    variableFactory(Class<? extends Number> valueClass, boolean filled)
	{
		String varName = names[indexOf(valueClass)];
		Number varValue = values[indexOf(valueClass)];
		if (valueClass == Integer.class)
		{
			if (filled)
			{
				return new VariableExpression<Integer>(varName+"i", (Integer) varValue);
			}
			else
			{
				return new VariableExpression<Integer>(varName+"i");
			}
		}
		else if (valueClass == Float.class)
		{
			if (filled)
			{
				return new VariableExpression<Float>(varName+"f", (Float) varValue);
			}
			else
			{
				return new VariableExpression<Float>(varName+"f");
			}
		}
		else if (valueClass == Double.class)
		{
			if (filled)
			{
				return new VariableExpression<Double>(varName+"d", (Double) varValue);
			}
			else
			{
				return new VariableExpression<Double>(varName+"d");
			}
		}
		else if (valueClass == BigDecimal.class)
		{
			if (filled)
			{
				return new VariableExpression<BigDecimal>(varName+"b", (BigDecimal) varValue);
			}
			else
			{
				return new VariableExpression<BigDecimal>(varName+"b");
			}
		}
		else
		{
			return null;
		}
	}

	/**
	 * Setup expressions for testing with a specific Number type
	 * @param type the number type
	 */
	private void setupTestFor(Class<? extends Number> type)
	{
		testFilledExpression = filledExpressionsMap.get(type);
		testEmptyExpression = emptyExpressionsMap.get(type);
		testValueName = type.getSimpleName();
	}

	/**
	 * Test method for {@link VariableExpression#VariableExpression(String)}.
	 * @param type number type for setting up the test
	 */
	@ParameterizedTest
	@MethodSource("valueClassesProvider")
	@DisplayName("VariableExpression<Number>(String)")
	final void testVariableExpressionOfString(Class<? extends Number> type)
	{
		setupTestFor(type);
		String testName = new String("VariableExpression<" + testValueName
	    + ">(String)");
		System.out.println(testName);

		// Build variable with no name and no value : constructor inaccessible
		String name = "a";
		VariableExpression<? extends Number> variable = buildVariable(type, name, null, testName);
		assertNotNull(variable, testName + " unexpected null instance");
		assertEquals(name,
		             variable.getName(),
		             testName + " unexpected name");
		assertFalse(variable.hasValue(), testName + " unexpected value status");
		assertThrows(IllegalStateException.class, () -> {
			variable.value();
		});

		// Building another variable with the same name should be ok
		try
		{
			VariableExpression<? extends Number> other =
			    buildVariable(type, name, null, testName);
			assertNotNull(other, testName + " unexpected null instance");
			assertFalse(other.hasValue(), testName + " unexpected value status");
			assertThrows(IllegalStateException.class, () -> {
				other.value();
			});
		}
		catch (IllegalArgumentException e)
		{
			fail(testName + " unexpected construction failure: "
			    + e.getLocalizedMessage());
		}
	}

	/**
	 * Test method for {@link VariableExpression#VariableExpression(String, Number)}.
	 * @param type number type for setting up the test
	 */
	@ParameterizedTest
	@MethodSource("valueClassesProvider")
	@DisplayName("VariableExpression<Number>(String, Number)")
	final void testVariableExpressionOfStringOfNumber(Class<? extends Number> type)
	{
		setupTestFor(type);
		String testName = new String("VariableExpression<" + testValueName
	    + ">(String, " + testValueName + ")");
		System.out.println(testName);

		// Build variable with no name and no value : constructor inaccessible
		Number value;
		final Number otherValue;
		if (type == Integer.class)
		{
			value = 2;
			otherValue = 3;
		}
		else if (type == Float.class)
		{
			value = 2.0f;
			otherValue = 3.0f;
		}
		else if (type == Double.class)
		{
			value = 2.0;
			otherValue = 3.0;
		}
		else if (type == BigDecimal.class)
		{
			value = new BigDecimal(2);
			otherValue = new BigDecimal(3);
		}
		else
		{
			value = null;
			otherValue = null;
			fail(testName + " unknown type: " + type.getSimpleName());
		}

		String name = names[indexOf(type)];

		VariableExpression<? extends Number> variable = buildVariable(type, name, value, testName);
		assertNotNull(variable, testName + " unexpected null instance");
		assertEquals(name,
		             variable.getName(),
		             testName + " unexpected name");
		assertTrue(variable.hasValue(), testName + " unexpected value status");
		assertEquals(value, variable.value(), testName + " unexpected value");

		// building the same variable with the same value should be ok
		try
		{
			VariableExpression<? extends Number> other = buildVariable(type, name, value, testName);
			assertNotNull(other, testName + " unexpected null instance");
			assertEquals(variable.getName(),
			             other.getName(),
			             testName + " unexpected name");
			assertEquals(variable.hasValue(),
			             other.hasValue(),
			             testName + " unexpected value status inequality");
			assertEquals(variable.value(),
			             other.value(),
			             testName + " unexpected value inequality");
		}
		catch (IllegalArgumentException e)
		{
			fail(testName + " unexpected construction failure building a second "
				+ "variable with the same name and value");
		}

		// building the same variable with another value should fail
		assertThrows(IllegalArgumentException.class, () -> {
			VariableExpression<? extends Number> impossible = null;
			if (type == Integer.class)
			{
				impossible = new VariableExpression<Integer>(name, (Integer) otherValue);
			}
			if (type == Float.class)
			{
				impossible = new VariableExpression<Float>(name, (Float) otherValue);
			}
			if (type == Double.class)
			{
				impossible = new VariableExpression<Double>(name, (Double) otherValue);
			}
			if (type == BigDecimal.class)
			{
				impossible = new VariableExpression<BigDecimal>(name, (BigDecimal) otherValue);
			}
			assertNotNull(impossible, testName + " unexpected null isntance");
		});

		// building the same variable with the same value but another type should fail
		assertThrows(IllegalArgumentException.class, () -> {
			VariableExpression<? extends Number> impossible = null;
			if (type == Integer.class)
			{
				impossible = new VariableExpression<Float>(name,
					(Float)values[indexOf(Float.class)]);
			}
			if (type == Float.class)
			{
				impossible = new VariableExpression<Double>(name,
					(Double)values[indexOf(Double.class)]);
			}
			if (type == Double.class)
			{
				impossible = new VariableExpression<BigDecimal>(name,
					(BigDecimal)values[indexOf(BigDecimal.class)]);
			}
			if (type == BigDecimal.class)
			{
				impossible = new VariableExpression<Integer>(name,
					(Integer)values[indexOf(Integer.class)]);
			}
			assertNotNull(impossible, testName + " unexpected null isntance");
		});

		testFilledExpression = filledExpressionsMap.get(type);
		assertNotNull(testFilledExpression, testName + " failed with null instance");

		testFilledExpression = variableFactory(type, true);
		assertNotNull(testFilledExpression, testName + " failed with null instance");
	}

	/**
	 * Test method for {@link VariableExpression#VariableExpression(String)}
	 * @param type number type for setting up the test
	 */
	@ParameterizedTest
	@MethodSource("valueClassesProvider")
	@DisplayName("VariableExpression<Number>(String)")
	final void testVariableExpressionOfStringOfValue(Class<? extends Number> type)
	{
		setupTestFor(type);
		String testName = new String("VariableExpression<" + testValueName
	    + ">(String)");
		System.out.println(testName);
		setupEmptyExpressions();
		testEmptyExpression = emptyExpressionsMap.get(type);

		assertNotNull(testEmptyExpression, testName + " failed with null instance");

		testEmptyExpression = variableFactory(type, false);
		assertNotNull(testEmptyExpression, testName + " failed with null instance");
	}

	/**
	 * Test method for {@link VariableExpression#getName()}.
	 * @param type number type for setting up the test
	 */
	@ParameterizedTest
	@MethodSource("valueClassesProvider")
	@DisplayName("String getName()")
	final void testGetName(Class<? extends Number> type)
	{
		setupTestFor(type);
		String testName = new String("String getName() [" + type.getSimpleName() + "]");
		System.out.println(testName);

		assertNotNull(testFilledExpression.getName(),
		              testName + " failed with null name");
		assertNotNull(testEmptyExpression.getName(),
		              testName + " failed with null name");

		assertEquals(names[indexOf(type)] + suffixes[indexOf(type)],
		             testFilledExpression.getName(),
		             testName + " failed with wrong name");
		assertEquals(names[indexOf(type)] + suffixes[indexOf(type)],
		             testEmptyExpression.getName(),
		             testName + " failed with wrong name");

		testFilledExpression = variableFactory(type, true);
		assertEquals(names[indexOf(type)] + suffixes[indexOf(type)],
		             testFilledExpression.getName(),
		             testName + " failed with wrong name");

		testEmptyExpression = variableFactory(type, false);
		assertEquals(names[indexOf(type)] + suffixes[indexOf(type)],
		             testEmptyExpression.getName(),
		             testName + " failed with wrong name");
	}

	/**
	 * Test method for {@link VariableExpression#hasValue()}.
	 * @param type number type for setting up the test
	 */
	@ParameterizedTest
	@MethodSource("valueClassesProvider")
	@DisplayName("boolean hasValue()")
	final void testHasValue(Class<? extends Number> type)
	{
		setupTestFor(type);
		String testName = new String("boolean hasValue() [" + type.getSimpleName() + "]");
		System.out.println(testName);
		VariableExpression.clearAllValues();

		testEmptyExpression = variableFactory(type, false);
		assertFalse(testEmptyExpression.hasValue(),
		            testName + " failed with evaluable expression");
		testFilledExpression = variableFactory(type, true);
		assertTrue(testFilledExpression.hasValue(),
		           testName + " failed with non evaluable expression");
		assertTrue(testEmptyExpression.hasValue(),
		           testName + " failed with evaluable expression");
	}

	/**
	 * Test method for {@link VariableExpression#value()}.
	 * @param type number type for setting up the test
	 */
	@ParameterizedTest
	@MethodSource("valueClassesProvider")
	@DisplayName("Number value()")
	final void testValue(Class<? extends Number> type)
	{
		setupTestFor(type);
		String testName = new String("Number evaluate() [" + type.getSimpleName() + "]");
		System.out.println(testName);

		assertEquals(valuesMap.get(type),
		             testFilledExpression.value(),
		             testName + " failed with wrong evaluation");

		testFilledExpression = variableFactory(type, true);

		assertEquals(valuesMap.get(type),
		             testFilledExpression.value(),
		             testName + " failed with wrong evaluation");

		testEmptyExpression = buildVariable(type, "empty", null, testName);

		assertThrows(IllegalStateException.class,
		             () -> {testEmptyExpression.value();},
		             testName + " failed with no exception thrown");

		// since testFilledExpression has a value it will be transfered to testEmptyExpression
		testEmptyExpression = variableFactory(type, false);
		assertEquals(valuesMap.get(type),
		             testEmptyExpression.value(),
		             testName + " failed with wrong evaluation");
	}

	/**
	 * Test method for {@link VariableExpression#setValue(Number)}.
	 * @param type number type for setting up the test
	 */
	@ParameterizedTest
	@MethodSource("valueClassesProvider")
	@DisplayName("void setValue(Number)")
	final void testSetValueOfNumber(Class<? extends Number> type)
	{
		setupTestFor(type);
		String testName = new String("void setValue("+type.getSimpleName()+")");
		System.out.println(testName);

		if (type == Integer.class)
		{
			Integer value = (Integer) valuesMap.get(type);
			@SuppressWarnings("unchecked")
			VariableExpression<Integer> castedExpression =
			    (VariableExpression<Integer>) testEmptyExpression;
			castedExpression.setValue(value);
			assertEquals(value,
			             castedExpression.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");
			final Integer nullValue = null;
			assertThrows(NullPointerException.class,
			             () -> {castedExpression.setValue(nullValue);},
			             testName + " failed with no exception thrown");

		}
		if (type == Float.class)
		{
			Float value = (Float) valuesMap.get(type);
			@SuppressWarnings("unchecked")
			VariableExpression<Float> castedExpression =
			    (VariableExpression<Float>) testEmptyExpression;
			castedExpression.setValue(value);
			assertEquals(value,
			             castedExpression.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");
			final Float nullValue = null;
			assertThrows(NullPointerException.class,
			             () -> {castedExpression.setValue(nullValue);},
			             testName + " failed with no exception thrown");
		}
		if (type == Double.class)
		{
			Double value = (Double) valuesMap.get(type);
			@SuppressWarnings("unchecked")
			VariableExpression<Double> castedExpression =
			    (VariableExpression<Double>) testEmptyExpression;
			castedExpression.setValue(value);
			assertEquals(value,
			             castedExpression.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");
			final Double nullValue = null;
			assertThrows(NullPointerException.class,
			             () -> {castedExpression.setValue(nullValue);},
			             testName + " failed with no exception thrown");

		}
		if (type == BigDecimal.class)
		{
			BigDecimal value = (BigDecimal) valuesMap.get(type);
			@SuppressWarnings("unchecked")
			VariableExpression<BigDecimal> castedExpression =
			    (VariableExpression<BigDecimal>) testEmptyExpression;
			castedExpression.setValue(value);
			assertEquals(value,
			             castedExpression.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");
			final BigDecimal nullValue = null;
			assertThrows(NullPointerException.class,
			             () -> {castedExpression.setValue(nullValue);},
			             testName + " failed with no exception thrown");

		}
	}

	/**
	 * Test method for {@link VariableExpression#setValue(TerminalExpression)}.
	 * @param type number type for setting up the test
	 */
	@ParameterizedTest
	@MethodSource("valueClassesProvider")
	@DisplayName("void setValue(TerminalExpression<Number>)")
	final void testSetValueOfTerminalExpression(Class<? extends Number> type)
	{
		setupTestFor(type);
		String testName = new String("void setValue(TerminalExpression<" + type.getSimpleName() + ">)");
		System.out.println(testName);

		if (type == Integer.class)
		{
			Integer value = (Integer) valuesMap.get(type);
			@SuppressWarnings("unchecked")
			VariableExpression<Integer> castedEmptyExpression =
			    (VariableExpression<Integer>) testEmptyExpression;

			@SuppressWarnings("unchecked")
			VariableExpression<Integer> castedFilleExpression =
				(VariableExpression<Integer>) testFilledExpression;

			VariableExpression<Integer> empty = new VariableExpression<>("empty");

			assertFalse(castedFilleExpression.setValue(empty),
			            testName + "[" + type.getSimpleName()
		                 + "] failed with filled set to empty");

			assertTrue(castedFilleExpression.setValue(castedEmptyExpression),
			            testName + "[" + type.getSimpleName()
		                 + "] failed to set filled set with (no more) empty");

			assertTrue(castedEmptyExpression.setValue(castedFilleExpression),
			           testName + "[" + type.getSimpleName()
		                 + "] failed with empty not set to filled");

			assertTrue(empty.setValue(castedFilleExpression),
			           testName + "[" + type.getSimpleName()
		                 + "] failed with empty not set to filled");

			assertEquals(value,
			             castedEmptyExpression.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");
			assertEquals(value,
			             empty.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");

			final TerminalExpression<Integer> nullExpr = null;
			assertFalse(castedEmptyExpression.setValue(nullExpr),
			            testName + "[" + type.getSimpleName()
			                + "] failed with true");

		}
		if (type == Float.class)
		{
			Float value = (Float) valuesMap.get(type);
			@SuppressWarnings("unchecked")
			VariableExpression<Float> castedEmptyExpression =
			    (VariableExpression<Float>) testEmptyExpression;

			@SuppressWarnings("unchecked")
			VariableExpression<Float> castedFilleExpression =
				(VariableExpression<Float>) testFilledExpression;

			VariableExpression<Float> empty = new VariableExpression<>("empty");

			assertFalse(castedFilleExpression.setValue(empty),
			            testName + "[" + type.getSimpleName()
		                 + "] failed with filled set to empty");

			assertTrue(castedFilleExpression.setValue(castedEmptyExpression),
			            testName + "[" + type.getSimpleName()
		                 + "] failed to set filled set with (no more) empty");

			assertTrue(castedEmptyExpression.setValue(castedFilleExpression),
			           testName + "[" + type.getSimpleName()
		                 + "] failed with empty not set to filled");

			assertTrue(empty.setValue(castedFilleExpression),
			           testName + "[" + type.getSimpleName()
		                 + "] failed with empty not set to filled");

			assertEquals(value,
			             castedEmptyExpression.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");
			assertEquals(value,
			             empty.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");

			final TerminalExpression<Float> nullExpr = null;
			assertFalse(castedEmptyExpression.setValue(nullExpr),
			            testName + "[" + type.getSimpleName()
			                + "] failed with true");
		}
		if (type == Double.class)
		{
			Double value = (Double) valuesMap.get(type);
			@SuppressWarnings("unchecked")
			VariableExpression<Double> castedEmptyExpression =
			    (VariableExpression<Double>) testEmptyExpression;

			@SuppressWarnings("unchecked")
			VariableExpression<Double> castedFilleExpression =
				(VariableExpression<Double>) testFilledExpression;

			VariableExpression<Double> empty = new VariableExpression<>("empty");

			assertFalse(castedFilleExpression.setValue(empty),
			            testName + "[" + type.getSimpleName()
		                 + "] failed with filled set to empty");

			assertTrue(castedFilleExpression.setValue(castedEmptyExpression),
			            testName + "[" + type.getSimpleName()
		                 + "] failed to set filled set with (no more) empty");

			assertTrue(castedEmptyExpression.setValue(castedFilleExpression),
			           testName + "[" + type.getSimpleName()
		                 + "] failed with empty not set to filled");

			assertTrue(empty.setValue(castedFilleExpression),
			           testName + "[" + type.getSimpleName()
		                 + "] failed with empty not set to filled");

			assertEquals(value,
			             castedEmptyExpression.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");
			assertEquals(value,
			             empty.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");

			final TerminalExpression<Double> nullExpr = null;
			assertFalse(castedEmptyExpression.setValue(nullExpr),
			            testName + "[" + type.getSimpleName()
			                + "] failed with true");
		}
		if (type == BigDecimal.class)
		{
			BigDecimal value = (BigDecimal) valuesMap.get(type);
			@SuppressWarnings("unchecked")
			VariableExpression<BigDecimal> castedEmptyExpression =
			    (VariableExpression<BigDecimal>) testEmptyExpression;

			@SuppressWarnings("unchecked")
			VariableExpression<BigDecimal> castedFilleExpression =
				(VariableExpression<BigDecimal>) testFilledExpression;

			VariableExpression<BigDecimal> empty = new VariableExpression<>("empty");

			assertFalse(castedFilleExpression.setValue(empty),
			            testName + "[" + type.getSimpleName()
		                 + "] failed with filled set to empty");

			assertTrue(castedFilleExpression.setValue(castedEmptyExpression),
			            testName + "[" + type.getSimpleName()
		                 + "] failed to set filled set with (no more) empty");

			assertTrue(castedEmptyExpression.setValue(castedFilleExpression),
			           testName + "[" + type.getSimpleName()
		                 + "] failed with empty not set to filled");

			assertTrue(empty.setValue(castedFilleExpression),
			           testName + "[" + type.getSimpleName()
		                 + "] failed with empty not set to filled");

			assertEquals(value,
			             castedEmptyExpression.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");
			assertEquals(value,
			             empty.value(),
			             testName + "[" + type.getSimpleName()
			                 + "] failed with wrong evaluation");

			final TerminalExpression<BigDecimal> nullExpr = null;
			assertFalse(castedEmptyExpression.setValue(nullExpr),
			            testName + "[" + type.getSimpleName()
			                + "] failed with true");
		}
	}

	// TODO add test for contains(expr) method

	/**
	 * Test method for {@link VariableExpression#equals(java.lang.Object)}.
	 * @param type number type for setting up the test
	 */
	@ParameterizedTest
	@MethodSource("valueClassesProvider")
	@DisplayName("boolean equals(Object)")
	final void testEqualsObject(Class<? extends Number> type)
	{
		setupTestFor(type);
		String testName = new String("boolean equals(Object) [" + type.getSimpleName() + "]");
		System.out.println(testName);

		// Non equality to null
		assertNotEquals(null,
		                testFilledExpression,
		                testName + " failed with equality to null");
		assertNotEquals(null,
		                testEmptyExpression,
		                testName + " failed with equality to null");

		// Equality to self
		assertEquals(testFilledExpression,
		             testFilledExpression,
		             testName + " failed with non equality to self");
		assertEquals(testEmptyExpression,
		             testEmptyExpression,
		             testName + " failed with non equality to self");

		// Equality between filled and unfilled since only name matters
		assertEquals(testFilledExpression,
		             testEmptyExpression,
		             testName + " failed with inequality between filled and "
		             	+ "unfilled");

		// Equality to other with same content
		VariableExpression<? extends Number> other = variableFactory(type, true);
		assertEquals(other,
		             testFilledExpression,
		             testName + " failed with non equality to other with same "
		             	+ "content");

		// Equality with other with no content
		other = variableFactory(type, false);
		assertEquals(other,
		             testEmptyExpression,
		             testName + " failed with inequality to other with same "
		                 + "content but unevaluable");
	}

	/**
	 * Test method for {@link VariableExpression#toString()}.
	 * @param type number type for setting up the test
	 */
	@ParameterizedTest
	@MethodSource("valueClassesProvider")
	@DisplayName("String toString()")
	final void testToString(Class<? extends Number> type)
	{
		setupTestFor(type);
		String testName = new String("String toString() [" + type.getSimpleName() + "]");
		System.out.println(testName);
		setupEmptyExpressions();
		testEmptyExpression = emptyExpressionsMap.get(type);

		assertEquals(testEmptyExpression.getName(),
		             testEmptyExpression.toString(),
		             testName + " failed with wrong toString");

		setupFilledExpressions();
		testFilledExpression = filledExpressionsMap.get(type);

//		String expected = new String(testFilledExpression.getName() + "("
//		    + valuesMap.get(type).toString() + ")");
		String expected = new String(testFilledExpression.getName());
		assertEquals(expected,
		             testFilledExpression.toString(),
		             testName + " failed with different strings");
	}

	/**
	 * Test method for {@link VariableExpression#hashCode()}.
	 * @param type number type for setting up the test
	 */
	@ParameterizedTest
	@MethodSource("valueClassesProvider")
	@DisplayName("int hashCode()")
	final void testHashCode(Class<? extends Number> type)
	{
		setupTestFor(type);
		String testName = new String("int hashCode() [" + type.getSimpleName() + "]");
		System.out.println(testName);

		assertEquals(testFilledExpression.toString().hashCode(),
		             testFilledExpression.hashCode(),
		             testName + " failed with different hashcode");
	}

}
