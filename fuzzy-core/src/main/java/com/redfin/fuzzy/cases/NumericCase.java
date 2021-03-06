package com.redfin.fuzzy.cases;

import com.redfin.fuzzy.Any;
import com.redfin.fuzzy.Case;
import com.redfin.fuzzy.FuzzyPreconditions;
import com.redfin.fuzzy.Generator;
import com.redfin.fuzzy.Literal;
import com.redfin.fuzzy.Subcase;
import com.redfin.fuzzy.Subcases;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class NumericCase<T extends Number> implements Case<T> {

	private static final int MAX_ATTEMPTS = 100;

	private T min;
	private T max;

	private final Set<T> excluded = new HashSet<T>();

	public Case<T> inRange(T minInclusive, T maxInclusive) {
		FuzzyPreconditions.checkNotNull(minInclusive);
		FuzzyPreconditions.checkNotNull(maxInclusive);

		if(lt(maxInclusive, minInclusive) || minInclusive.equals(maxInclusive)) {
			throw new IllegalArgumentException("minInclusive must be less than maxInclusive.");
		}

		min = minInclusive;
		max = maxInclusive;

		return this;
	}

	protected T getMax() { return max; }
	protected T getMin() { return min; }

	public Case<T> lessThanOrEqualTo(T maxInclusive) {
		FuzzyPreconditions.checkNotNull(maxInclusive);
		min = null;
		max = maxInclusive;
		return this;
	}

	public Case<T> greaterThanOrEqualTo(T minInclusive) {
		FuzzyPreconditions.checkNotNull(minInclusive);
		min = minInclusive;
		max = null;
		return this;
	}

	@Override
	public Case<T> excluding(Iterable<T> values) {
		if(values != null) {
			for(T t : values) {
				excluded.add(t);
			}
		}
		return this;
	}

	public Case<T> greaterThanOrEqualTo(Generator<T> minInclusive) {
		FuzzyPreconditions.checkNotNull(minInclusive);
		return () -> Subcases.pairwisePermutations(
			Collections.<Subcase<T>>singleton(random -> minInclusive.get()),
			newCase().greaterThanOrEqualTo(i2t(1)).getSubcases(),
			(random, base, distance) -> add(base, distance)
		);
	}

	public Case<T> lessThanOrEqualTo(Generator<T> maxInclusive) {
		FuzzyPreconditions.checkNotNull(maxInclusive);
		return () -> Subcases.pairwisePermutations(
			Collections.<Subcase<T>>singleton(random -> maxInclusive.get()),
			newCase().greaterThanOrEqualTo(i2t(1)).getSubcases(),
			(random, base, distance) -> add(base, negate(distance))
		);
	}

	public WithinChain<T> within(T range) {
		FuzzyPreconditions.checkNotNull(range);
		if(lt(range, i2t(1)))
			throw new IllegalArgumentException();

		return new WithinChain<T>(newCase(), range);
	}

	public static class WithinChain<T extends Number> {
		private final NumericCase<T> baseCase;
		private final T range;

		private WithinChain(NumericCase<T> baseCase, T range) { this.baseCase = baseCase; this.range = range; }

		public Case<T> butExcludingValueOf(Generator<T> number) {
			FuzzyPreconditions.checkNotNull(number);
			if(range.equals(baseCase.i2t(1))) {
				return () -> Subcases.pairwisePermutations(
					Collections.<Subcase<T>>singleton(random -> number.get()),
					Any.of(-1, 1).getSubcases(),
					(random, base, distance) -> baseCase.add(base, baseCase.i2t(distance))
				);
			}
			else {
				return () -> Subcases.pairwisePermutations(
					Collections.<Subcase<T>>singleton(random -> number.get()),
					Any.of(
						baseCase.newCase().inRange(baseCase.negate(range), baseCase.i2t(-1)),
						baseCase.newCase().inRange(baseCase.i2t(1), range)
					).getSubcases(),
					(random, base, distance) -> baseCase.add(base, distance)
				);
			}
		}

		public Case<T> of(Generator<T> number) {
			FuzzyPreconditions.checkNotNull(number);

			if(range.equals(baseCase.i2t(1))) {
				return () -> Subcases.pairwisePermutations(
					Collections.<Subcase<T>>singleton(random -> number.get()),
					Any.of(-1, 0, 1).getSubcases(),
					(random, base, distance) -> baseCase.add(base, baseCase.i2t(distance))
				);
			}
			else {
				return () -> Subcases.pairwisePermutations(
					Collections.<Subcase<T>>singleton(random -> number.get()),
					Any.of(
						baseCase.newCase().inRange(baseCase.negate(range), baseCase.i2t(-1)),
						Literal.value(baseCase.i2t(0)),
						baseCase.newCase().inRange(baseCase.i2t(1), range)
					).getSubcases(),
					(random, base, distance) -> baseCase.add(base, distance)
				);
			}
		}
	}

	private Subcase<T> exclude(Subcase<T> subcase) {
		return r -> {
			for(int i = 0; i < MAX_ATTEMPTS; i++) {
				T t = subcase.generate(r);
				if(!excluded.contains(t)) return t;
			}
			throw new IllegalStateException("Numeric case could not generate a value that was not marked as excluded.");
		};
	}

	@Override
	public Set<Subcase<T>> getSubcases() {
		Set<Subcase<T>> subcases = new HashSet<>(3);
		final T zero = i2t(0);

		// Negative
		if(min != null && max != null && lt(max, zero)) {
			final T d = add(max, negate(min));
			subcases.add(r -> add(min, rngLessThan(r, d)));
		}
		else if(min == null || lt(min, zero)) {
			if(min == null) {
				subcases.add(r -> negate(abs(rng(r))));
			}
			else {
				final T max = negate(min);
				subcases.add(r -> negate(rngLessThan(r, max)));
			}
		}

		// Positive
		if(max != null && min != null && lt(zero, min)) {
			final T d = add(max, negate(min));
			subcases.add(r -> add(min, rngLessThan(r, d)));
		}
		else if(max == null || lt(zero, max)) {
			if(max == null) {
				subcases.add(r -> abs(rng(r)));
			}
			else {
				final T max = this.max;
				subcases.add(r -> rngLessThan(r, max));
			}
		}

		// Zero
		if(
			(min == null || lt(min, zero) || zero.equals(min)) &&
			(max == null || lt(zero, max) || zero.equals(max)) &&
			!excluded.contains(zero)
		) {
			subcases.add(r -> zero);
		}

		// Cover the specific boundaries
		if(min != null && !min.equals(zero) && !excluded.contains(min)) subcases.add(r -> min);
		if(max != null && !max.equals(zero) && !excluded.contains(max)) subcases.add(r -> max);

		// Let the subclass add additional cases if it wants to.
		addAdditionalSubcases(subcases);

		return subcases.stream().map(this::exclude).collect(Collectors.toSet());
	}

	protected abstract NumericCase<T> newCase();

	protected abstract T add(T a, T b);
	protected abstract T negate(T t);
	protected abstract T abs(T t);
	protected abstract T i2t(int i);
	protected abstract boolean lt(T a, T b);

	protected abstract T rng(Random random);
	protected abstract T rngLessThan(Random random, T maxInclusive);

	protected void addAdditionalSubcases(Set<Subcase<T>> subcases) {}

	public static NumericCase<Byte> ofBytes() {
		return new NumericCase<Byte>() {
			@Override protected NumericCase<Byte> newCase() { return Any.byteInteger(); }

			@Override protected Byte add(Byte a, Byte b) {
				long r = a.longValue() + b.longValue();
				return r < Byte.MIN_VALUE ? Byte.MIN_VALUE : (r > Byte.MAX_VALUE ? Byte.MAX_VALUE : (byte)r);
			}

			@Override protected Byte negate(Byte b) { return (byte) -b; }
			@Override protected Byte abs(Byte b) { return (byte) (b < 0 ? -b : b); }
			@Override protected Byte i2t(int i) { return (byte)i; }
			@Override protected boolean lt(Byte a, Byte b) { return a < b; }
			@Override protected Byte rng(Random random) {
				byte[] b = new byte[1];
				random.nextBytes(b);
				return b[0] == 0 ? (byte)1 : b[0];
			}

			@Override
			protected Byte rngLessThan(Random random, Byte maxInclusive) {
				// Uniformity of distribution is not a huge deal here
				return (byte)((1 + random.nextInt(maxInclusive)) & 0x7F);
			}
		};
	}

	public static NumericCase<Short> ofShorts() {
		return new NumericCase<Short>() {
			@Override protected NumericCase<Short> newCase() { return Any.shortInteger(); }

			@Override protected Short add(Short a, Short b) {
				long r = a.longValue() + b.longValue();
				return r < Short.MIN_VALUE ? Short.MIN_VALUE : (r > Short.MAX_VALUE ? Short.MAX_VALUE : (short)r);
			}

			@Override protected Short negate(Short s) { return (short) -s; }
			@Override protected Short abs(Short s) { return (short) (s < 0 ? -s : s); }
			@Override protected Short i2t(int i) { return (short)i; }
			@Override protected boolean lt(Short a, Short b) { return a < b; }

			@Override
			protected Short rng(Random random) {
				byte[] b = new byte[2]; random.nextBytes(b);
				short s = (short)(b[0] << 8 & b[1]);
				return s == 0 ? (short)1 : s;
			}

			@Override
			protected Short rngLessThan(Random random, Short maxInclusive) {
				// Uniformity of distribution is not a huge deal here
				return (short)((1 + random.nextInt(maxInclusive)) & 0x7FFF);
			}
		};
	}

	public static NumericCase<Integer> ofIntegers() {
		return new NumericCase<Integer>() {
			@Override protected NumericCase<Integer> newCase() { return Any.integer(); }

			@Override protected Integer add(Integer a, Integer b) {
				long r = a.longValue() + b.longValue();
				return r < Integer.MIN_VALUE ? Integer.MIN_VALUE : (r > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)r);
			}

			@Override protected Integer negate(Integer integer) { return -integer; }
			@Override protected Integer abs(Integer integer) { int i = integer; return i < 0 ? -i : i; }
			@Override protected Integer i2t(int i) { return i; }
			@Override protected boolean lt(Integer a, Integer b) { return a < b; }
			@Override protected Integer rng(Random random) {
				int i = random.nextInt();
				return i == 0 ? 1 : i;
			}

			@Override
			protected Integer rngLessThan(Random random, Integer maxInclusive) {
				return 1 + random.nextInt(maxInclusive);
			}
		};
	}

	public static NumericCase<Long> ofLongs() {
		return new NumericCase<Long>() {
			@Override protected NumericCase<Long> newCase() { return Any.longInteger(); }

			// TODO: cap overflow
			@Override protected Long add(Long a, Long b) { return a + b; }

			@Override protected Long negate(Long lng) { return -lng; }
			@Override protected Long abs(Long lng) { long l = lng; return l < 0 ? -l : l; }
			@Override protected Long i2t(int i) { return (long) i; }
			@Override protected boolean lt(Long a, Long b) { return a < b; }
			@Override protected Long rng(Random random) {
				long l = random.nextLong();
				return l == 0 ? 1L : l;
			}

			@Override
			protected Long rngLessThan(Random random, Long maxInclusive) {
				// Deliberately pick a long integer that needs more than 32 bits, if appropriate
				if(maxInclusive > 0x7FFFFFFF) {
					// Uniformity of distribution isn't a huge deal here, so we'll just get a random nonnegative long
					// and shift it right until it's >= max.
					long l = random.nextLong() & 0x7FFFFFFFFFFFFFFFL;
					while(l >= maxInclusive) {
						l >>= 1;
					}
					return l;
				}
				else {
					return (long) 1 + random.nextInt(maxInclusive.intValue());
				}
			}

			@Override
			protected void addAdditionalSubcases(Set<Subcase<Long>> subcases) {
				// If supported by their bounds, make sure to add negative and positive cases for values larger
				// than regular integers can handle. We don't need to do this if the opposite bound already lies outside
				// the capacity of an integer.

				if(getMax() == null || getMax() > Integer.MIN_VALUE) {
					if (getMin() == null) {
						subcases.add(r -> (long) Integer.MIN_VALUE - (1L + (r.nextInt() & 0x3FFFFFFFL)));
					}
					// if a minimum bound is set and it's less than Integer.MIN_VALUE, it will already be included as
					// a case
				}

				if(getMin() == null || getMin() < Integer.MAX_VALUE) {
					if (getMax() == null) {
						subcases.add(r -> (long) Integer.MAX_VALUE + 1L + (r.nextInt() & 0x3FFFFFFFL));
					}
					// if a maximum bound is set and it's greater than Integer.MAX_VALUE, it will already be included as
					// a case
				}
			}
		};
	}

}
