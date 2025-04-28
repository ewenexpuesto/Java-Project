package expressions.terminal;

import java.util.ArrayList;
import java.util.List;

/**
 * Terminal expression type enum
 * @implNote used in {@link javafx.scene.control.ComboBox}
 */
public enum TerminalType
{
	/**
	 * All possible {@link TerminalExpression}s
	 */
	ALL("all"),
	/**
	 * Only {@link ConstantExpression}s
	 */
	CONSTANTS("constants"),
	/**
	 * Only {@link VariableExpression}s
	 */
	VARIABLES("variables");

	/**
	 * String content
	 */
	private final String value;

	/**
	 * Constructor from string
	 * @param value the string
	 */
	private TerminalType(String value)
	{
		this.value = value;
	}

	/**
	 * @return a String representation fot this enum
	 */
	@Override
	public String toString()
	{
		return value.charAt(0) + value.substring(1).toLowerCase();
	}

	/**
	 * List of all possible values
	 * @return a list of all possible values of this enum
	 */
	public static List<TerminalType> all()
	{
		List<TerminalType> list = new ArrayList<>();
		for (TerminalType value : values())
		{
			list.add(value);
		}
		return list;
	}
}
