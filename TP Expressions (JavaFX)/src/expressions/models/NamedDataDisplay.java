package expressions.models;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * A simple Data class to show a <Text, Value> pairs in a JavaFX
 * {@link javafx.scene.control.TableView} where 1st column contains a text
 * and second column contains a {@link Number}
 * @param <E> the type of numbers to display
 */
public class NamedDataDisplay<E extends Number>
{
	/**
	 * The string representation to show
	 */
	protected final StringProperty content;
	/**
	 * The value to show
	 */
	protected final Property<Number> value;

	/**
	 * Valued constructor
	 * @param name the string content to show in 1st column
	 * @param value the number content (evt null) to show on 2nd column
	 */
	public NamedDataDisplay(String name, Number value)
	{
		content = new SimpleStringProperty(name);
		if (value == null)
		{
			this.value = null;
		}
		else
		{
			if (value instanceof Integer)
			{
				this.value = new SimpleIntegerProperty(value.intValue());
			}
			else if (value instanceof Float)
			{
				this.value = new SimpleFloatProperty(value.floatValue());
			}
			else if (value instanceof Double)
			{
				this.value = new SimpleDoubleProperty(value.doubleValue());
			}
			else
			{
				this.value = null;
			}
		}
	}

	/**
	 * Content String property
	 * @return the content String property
	 */
	public StringProperty contentProperty()
	{
		return content;
	}

	/**
	 * Value Number property
	 * @return the value Number property
	 */
	public Property<Number> valueProperty()
	{
		return value;
	}
}
