package com.redfin.fuzzy;

import com.redfin.fuzzy.cases.ExcludingCase;
import com.redfin.fuzzy.cases.StringCase;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cases are the principal building block of the fuzzy engine. They describe tests by producing instances of specific
 * values according to a set of <em>subcases</em> (also known as
 * <em><a href="https://en.wikipedia.org/wiki/Equivalence_partitioning">equivalency classes</a></em>).
 *
 * <p>For example, the {@link StringCase} describes all of the different types of string that an application should be
 * able to handle. It provides several subcases, including the empty string, strings with non-ASCII characters, and
 * strings that contain characters with special meanings in various languages.
 * </p>
 * <p>When a test consumes a particular case, it is making a broad statement that the code under test should be able to
 * handle the values of each of the equivalency classes defined by that case.
 * </p>
 * <p><strong>Note to implementors:</strong> consider overriding the default implementations for the methods listed
 * below. See comments on those methods for explanations.</p>
 * <ul>
 *     <li>{@link #excluding(Iterable)}</li>
 * </ul>
 *
 * @param <T> the type of values created by this case.
 *
 * @see Cases
 * @see Literal
 * @see Subcases
 */
public interface Case<T>
{

	/**
	 * Returns the specific set of subcases that describe all equivalency classes for this case.
	 *
	 * <p>Cases are expected to return <em>at least one</em> subcase. When the fuzzy engine is determining how many test
	 * cases to execute, it does so in terms of subcases, not cases. Thus, when using the pairwise permutation
	 * algorithm, the fuzzy library works to ensure that each possible combination of two subcases are executed.</p>
	 *
	 * <p><strong>Note to implementors:</strong> cases should generally <em>not</em> include {@code null} values as
	 * possible outputs. Instead, consumers are expected to use the {@link #orNull()} method (or, equivalently,
	 * {@link Any#nullableOf}) to declare that their cases should also generate null values.</p>
	 */
	Set<Subcase<T>> getSubcases();

	/**
	 * Returns a new case that combines the subcases of this case and the provided {@code other} case. This method can
	 * be used to combine different cases for a value into a single {@linkplain Generator generator}.
	 */
	default Case<T> or(Case<T> other) { return Any.of(this, other); }

	/**
	 * Returns a new case that combines the subcases of this case and a subcase specifically generating the value
	 * {@code null}.
	 */
	default Case<T> orNull() { return Any.nullableOf(this); }

	/**
	 * Constrains the
	 */
	default Case<T> excluding(T value) { return excluding(Collections.singleton(value)); }
	default Case<T> excluding(T... values) { return excluding(values == null ? null : Arrays.asList(values)); }
	default Case<T> excluding(Iterable<T> values) { return new ExcludingCase<>(this, values); }

	/**
	 * Arbitrarily selects and returns the value of one of this case's subcases.
	 */
	default T generateAnyOnce() { return generateAnyOnce(new Random()); }

	/**
	 * Arbitrarily selects and returns the value of one of this case's subcases.
	 *
	 * @param random the random number generator that will be used to create the returned value.
	 */
	default T generateAnyOnce(Random random) {
		FuzzyPreconditions.checkNotNull(random);

		Set<Subcase<T>> subcases = getSubcases();

		if(subcases == null || subcases.isEmpty())
			throw new IllegalStateException(String.format("Case of type %s generated zero suppliers.", getClass()));

		return subcases.stream().findAny().orElse(null).generate(random);
	}

	/**
	 * Requests each of this case's subcases to generate and return a value.
	 */
	default Set<T> generateAllOnce() { return generateAllOnce(new Random()); }

	/**
	 * Requests each of this case's subcases to generate and return a value.
	 *
	 * @param random the random number generator that will be used to create the returned values.
	 */
	default Set<T> generateAllOnce(Random random) {
		FuzzyPreconditions.checkNotNull(random);

		Set<Subcase<T>> subcases = getSubcases();

		if(subcases == null || subcases.isEmpty())
			throw new IllegalStateException(String.format("Case of type %s generated zero suppliers.", getClass()));

		return subcases.stream().map(s -> s.generate(random)).collect(Collectors.toSet());
	}

}
