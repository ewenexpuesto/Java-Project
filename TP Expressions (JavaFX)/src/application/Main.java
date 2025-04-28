package application;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import expressions.Expression;
import expressions.binary.AssignmentExpression;
import expressions.binary.BinaryExpression;
import expressions.terminal.VariableExpression;
import parser.ExpressionParser;
import parser.exceptions.ParserException;

/**
 * Main program
 */
public class Main
{
	/**
	 * Extract variable set from expressions.
	 * The same variables can be encounterd multiple times within expressions
	 * but
	 * stored only once in the resulting set. Howecer a variable already stored
	 * in the resulting set can be modified for instance when a variable with no
	 * value is already stored in the set and the variable with the same name
	 * and a defined value is encountered within the expressions.
	 * @param expressions the list of expressions to explore
	 * @return a set of all variables encountered within th expressions list
	 */
	private static <E extends Number> Set<VariableExpression<E>>
	    extractVariables(Collection<Expression<E>> expressions)
	{
		Set<VariableExpression<E>> variables = new TreeSet<>();
		for (Expression<E> expression : expressions)
		{
			Set<VariableExpression<E>> expressionVariables =
			    extractVariables(expression);
			for (VariableExpression<E> expressionVariable : expressionVariables)
			{
				if (variables.contains(expressionVariable))
				{
					// find this variable in already registerd variables
					for (VariableExpression<E> registeredVariable : variables)
					{
						if (registeredVariable.equals(expressionVariable))
						{
							if (!registeredVariable.hasValue()
							    && expressionVariable.hasValue())
							{
								registeredVariable.setValue(expressionVariable.value());
							}
						}
					}
				}
				else
				{
					// Add it
					variables.add(expressionVariable);
				}
			}
		}
		return variables;
	}

	/**
	 * Extract all variables from a single expression
	 * @param expression the expression to explore
	 * @return a set of variables encountered in the provided expression
	 */
	private static <E extends Number> Set<VariableExpression<E>>
	    extractVariables(Expression<E> expression)
	{
		Set<VariableExpression<E>> variables = new TreeSet<>();

		if (expression != null)
		{
			if (expression instanceof VariableExpression<?>)
			{
				variables.add((VariableExpression<E>) expression);
			}

			if (expression instanceof BinaryExpression<?>)
			{
				BinaryExpression<E> operator =
				    (BinaryExpression<E>) expression;
				variables.addAll(extractVariables(operator.getLeft()));
				variables.addAll(extractVariables(operator.getRight()));
			}
		}

		return variables;
	}

	/**
	 * Inject a variable in an expression, replacing all occurrence of such a
	 * variable with the one provided in argument regardless of its value
	 * @param <E> the type of numbers in expressions
	 * @param variable the variable to inject in the provided expression
	 * @param expression the expression to modify
	 * @return true if provided expression has been modified because one or
	 * several instances of the provided variable have been found and replaced
	 * with the provided variable.
	 */
	private static <E extends Number> boolean
	    injectVariable(VariableExpression<E> variable, Expression<E> expression)
	{
		boolean result = false;
		if ((variable != null) && (expression != null))
		{
			if (expression instanceof BinaryExpression<?>)
			{
				BinaryExpression<E> otherOperator = (BinaryExpression<E>) expression;
				result = injectVariable(variable, otherOperator);
			}
		}
		return result;
	}

	/**
	 * Inject variable into binary expression
	 * @param <E> the type of numbers in expressions
	 * @param variable the variable to inject
	 * @param operator the binary expression to modify
	 * @return true if provided variable habe been found in provided binary
	 * expression and replaced, false otherwise
	 */
	private static <E extends Number> boolean
	    injectVariable(VariableExpression<E> variable,
	                   BinaryExpression<E> operator)
	{
		boolean result = false;
		if ((variable != null) && (operator != null))
		{
			Expression<E> left = operator.getLeft();
			Expression<E> right = operator.getRight();
			if ((variable != left))
			{
				if (variable.equals(left))
				{
					operator.setLeft(variable);
					result |= true;
				}
				else
				{
					result |= injectVariable(variable, left);
				}
			}
			if (variable != right)
			{
				if (variable.equals(right))
				{
					operator.setRight(variable);
					result |= true;
				}
				else
				{
					result |= injectVariable(variable, right);
				}
			}
		}
		return result;
	}

	/**
	 * Print expressions in tree form
	 * @param expr the expression to, print on the console
	 * @param tabLevel the number of tabulations to prepend.
	 */
	private static void printExpression(Expression<? extends Number> expr, int tabLevel)
	{
		for (int i = 0; i < tabLevel; i++)
		{
			System.out.print('\t');
		}

		if (expr instanceof BinaryExpression<?>)
		{
			BinaryExpression<? extends Number> op = (BinaryExpression<? extends Number>) expr;
			System.out.println(op.getRules().toString());
			printExpression(op.getLeft(), tabLevel + 1);
			printExpression(op.getRight(), tabLevel + 1);
			return;
		}

		System.out.println(expr);
	}

	/**
	 * Main program entrey point.
	 * Parses expressions from argument string
	 * @param args contains the expressions to parse separated by ";" but can
	 * start with a "--type {int|float|double}" to indicate the type of numbers
	 * expected in the following expressions. [default is int]
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args)
	{
		if (args.length == 0)
		{
			System.out.println("usage: java application.Main --type {int|float|double} expression1;expression2;...");
			return;
		}

		/*
		 * First contatenate all args into a single String
		 */
		StringBuilder sb = new StringBuilder();
		String numTypeString = "int";
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].startsWith("--type"))
			{
				i++;
				if (i >= args.length)
				{
					System.err.println("missing argument for --type");
					System.exit(1);
				}
				numTypeString = args[i];
			}
			else
			{
				sb.append(args[i]);
			}
		}
		String context = sb.toString();

		/*
		 * Create a parser
		 */
		ExpressionParser<?> parser = null;
		List<?> expressionsList = null;
		List<Expression<Number>> expressions = null;
		Set<VariableExpression<Number>> variables = null;
		numTypeString = numTypeString.toLowerCase();
		if (numTypeString.contains("double"))
		{
			parser = new ExpressionParser<Double>(0.0);
		}
		else if (numTypeString.contains("float"))
		{
			parser = new ExpressionParser<Float>(0.0f);
		}
		else
		{
			parser = new ExpressionParser<Integer>(0);
		}

		/*
		 * Parse expressions
		 */
		try
		{
			expressionsList = parser.parse(context);
			expressions = (List<Expression<Number>>) expressionsList;
		}
		catch (ParserException e)
		{
			e.printStackTrace();
		}

		variables = extractVariables(expressions);
		for (VariableExpression<Number> variable : variables)
		{
			for (Expression<Number> expression : expressions)
			{
				injectVariable(variable, expression);
			}
		}

		/*
		 * Print expressions
		 */
		System.out.println("Expressions : ");
		for (Expression<Number> expr : expressions)
		{
			System.out.println();
			System.out.print(expr);
			if (expr.hasValue() && !(expr instanceof AssignmentExpression<?>))
			{
				try
				{
					System.out.print(" = " + expr.value());
				}
				catch (IllegalStateException e)
				{
					e.printStackTrace();
				}
			}
			System.out.println(": ");
			printExpression(expr, 0);
		}
		/*
		 * Print variables
		 */
		System.out.println("Variables : ");
		for (VariableExpression<Number> variable : variables)
		{
			System.out.print(variable);
			if (variable.hasValue())
			{
				System.out.print(" : " + variable.value());
			}
			System.out.println();
		}
	}

}
