package expressions.terminal;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Variable expression.
 * a Variable expression is (like {@link ConstantExpression}) a numerical
 * value except its value needs to be set first before it can be evaluated.
 * Typically variables are set using the "=" operator : a = 2;
 * Multiple variables with the same name MUST share the same value (if any) to
 * avoid inconsistencies.
 * @param <E> The type of numbers in this expression
 * @author davidroussel
 */
public class VariableExpression<E extends Number> extends TerminalExpression<E>
    implements Variable<E>
{
	/**
	 * Static map to store a single {@link Optional} value for any variables
	 * with the same name.
	 * Each time a new variable is created, we must check if this name is
	 * already part of this map. In such cases the optional shall be retrived
	 * from this map which allows sharing the same (optional) value for all
	 * variables with the same name.
	 */
	private static Map<String, Optional<? extends Number>> values = new HashMap<>();

	/**
	 * Variable's name.
	 * The name of the variable is used so that when multiple epressions
	 * reference a variable's name, such a variable can be updated.
	 */
	private String name;

	/**
	 * Valued constructor.
	 * Builds a variable with a name and a value (which can be null)
	 * @param name the name of this variable
	 * @param value the value to provide to this variable (can evt be null)
	 * @throws IllegalArgumentException if there is a value associated with this
	 * name in {@link #values} which is different from the provided value since
	 * all variables with the same name shall share the same value.
	 * @implSpec This constructor ensures there is a "name" entry in
	 * {@link #values} with or without an actual value.
	 */
	public VariableExpression(String name, E value)
	{
		super(value); // Initialize this.value
		this.value = registeredValue(name, value);
		this.name = name;
	}

	/**
	 * Constructor with only name and no value
	 * @param name the name of this variable
	 */
	public VariableExpression(String name)
	{
		this(name, null);
	}

	/**
	 * Get registered value from {@link #values} if there is one or create it if
	 * there isn't.
	 * @param name The name of the value we're looking for.
	 * @param value the eventual value. If no values are found in
	 * {@link #values} a new {@link Optional} value is created with provided
	 * value (evt an empty optional if provided value is null). If a value is
	 * registered in {@link #values} and provided value is non null then both
	 * values are checked for equality. If both values differ an
	 * {@link IllegalArgumentException} is raised since all variables with the
	 * same name should share the same value.
	 * @return an optional value containing the value registered to provided
	 * name within {@link #values}.
	 * @throws IllegalArgumentException if provided non null value and
	 * registered value differ since all variables should share the same value.
	 */
	@SuppressWarnings("unchecked")
	private Optional<E> registeredValue(String name, E value)
	    throws IllegalArgumentException
	{
		Optional<E> registered = (Optional<E>) values.get(name);
		if (registered == null)
		{
			values.put(name,
			           value == null ? Optional.empty() : Optional.of(value));
			return (Optional<E>) values.get(name);
		}

		// registered is non null
		if (registered.isEmpty())
		{
			if ((value != null))
			{
				values.put(name, Optional.of(value));
				return (Optional<E>) values.get(name);
			}
		}

		// registered is non null, but may still be empty
		// If both values present compare them
		if (registered.isPresent() && (value != null))
		{
			E registeredValue = registered.get();
			if (!registeredValue.equals(value))
			{
				throw new IllegalArgumentException("Multiple values for the "
					+ "same variable " + name + ": " + registeredValue
					+ " != " + value);
			}
		}

		return registered;
	}

	/**
	 * Get registered value from {@link #values} if there is one or create an
	 * empty optional value if
	 * there isn't.
	 * @param name The name of the value we're looking for.
	 * @return an optional value containing the value registered to provided
	 * name within {@link #values} or an ampty one.
	 */
	private Optional<E> registeredValue(String name)
	{
		return registeredValue(name, null);
	}

	/**
	 * Name accessor for this variable
	 * @return the name of this variable
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/**
	 * Merge value with another variable or constant
	 * @param term the terminal expression to take value from.
	 * @return true if a value has been set, false otherwise.
	 * @implSpec If the provided expression or doesn't have a value to set then
	 * the result will always be false;
	 */
	public boolean setValue(TerminalExpression<E> term)
	{
		if (term == null)
		{
			return false;
		}

		if (!term.hasValue())
		{
			return false;
		}

		setValue(term.value());
		return true;
	}

	/**
	 * Indicate if this expression can be evaluated right now to procude a value
	 * @return true if expression can produce a value
	 * and calling {@link #value()} is legal. False otherwise
	 * @see Optional#isPresent()
	 * @implNote Always check the existence of value from {@link #values} rather
	 * than from {@link TerminalExpression#value} since it may have been changed
	 * by another variable.
	 */
	@Override
	public boolean hasValue()
	{
		return registeredValue(name).isPresent();
	}

	/**
	 * Numeric value of this expression
	 * @return the numeric value of this expression
	 * @throws IllegalStateException if a value can't be evaluated right now
	 * @see Optional#get()
	 * @implNote Always get value from {@link #values} first since it might
	 * have been changed by another variable and then go back to regular
	 * behavior (evt using super method)
	 */
	@Override
	public E value()
	{
		value = registeredValue(name);
		return super.value();
	}

	/**
	 * Set value to this variable
	 * @param value the value to set to this variable
	 * @throws NullPointerException if we try to set a null value
	 * @see Optional#of(Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setValue(E value) throws NullPointerException
	{
		Objects.requireNonNull(value, "null value");
		values.put(name, Optional.of(value));
		this.value = (Optional<E>) values.get(name);
	}

	/**
	 * Reset current value to "no value"
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void clearValue()
	{
		values.put(name, Optional.empty());
		value = (Optional<E>) values.get(name);
	}

	/**
	 * String representation of this VariableExpression
	 * @return return the "name" of this variable
	 */
	@Override
	public String toString()
	{
		/*
		 * Updated version only returns variable name
		 */
		return name;
	}

	/**
	 * Values map accessor: Get the map containing <name, value> pairs.
	 * @return The map containing values for all registered variables
	 */
	public static Map<String, Optional<? extends Number>> getValues()
	{
		return values;
	}

	/**
	 * Changes the map containing <name, value> pairs.
	 * @param map The new map to set.
	 * @implNote This situation can occur when expressions are replaced (during
	 * an undo transaction for instance) or when a more sophisticated map is
	 * required which can be the case if we need to use an observable map for
	 * instance.
	 */
	public static void setValues(Map<String, Optional<? extends Number>> map)
	{
		values = map;
	}

	/**
	 * Clear all values in {@link #value()} : replace them with empty values
	 */
	public static void clearAllValues()
	{
		Set<String> keys = values.keySet();
		for (String key : keys)
		{
			values.put(key, Optional.empty());
		}
	}

	/**
	 * Remove all elements from {@link #values} : keys and values
	 * @implNote since {@link #clearAll()} can be called atr any time, calling
	 * <code>values.get(name)</code> may not always provide a non null value.
	 */
	public static void clearAll()
	{
		values.clear();
	}
}
