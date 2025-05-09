package expressions.models;

import expressions.terminal.VariableExpression;

/**
 * A simple Data class to show <variable name, value> contained in
 * {@link VariableExpression#getValues()} dictionary and displayed in a
 * {@link javafx.scene.control.TableView}. First column contains the variable
 * name and second column contains the variable value (if any).
 * @param <E> the type of numbers to display
 */
public class VariableDisplay<E extends Number> extends NamedDataDisplay<E>
{
	/**
	 * Valued constructor
	 * @param name the name of the variable to show
	 */
	public VariableDisplay(String name)
	{
		super(name,
		      VariableExpression.getValues().get(name) == null ?
		    	  null : VariableExpression.getValues().get(name).isEmpty() ?
		    		  null : VariableExpression.getValues().get(name).get());
	}
}
