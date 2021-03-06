package com.redfin.fuzzy;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class FuzzyUtilTest {

	@Test
	public void testSetOfNothing() {
		assertEquals(new HashSet<>(), FuzzyUtil.setOf());
	}

	@Test
	public void testToCharSetOfNull() {
		assertEquals(new HashSet<>(), FuzzyUtil.toCharSet(null));
	}

	@Test
	public void testToCharSetOfEmpty() {
		assertEquals(new HashSet<>(), FuzzyUtil.toCharSet(""));
	}

	@Test
	public void testInspectNull() {
		assertEquals("{null}", FuzzyUtil.inspect(null));
	}

	@Test
	public void testInspectString() {
		assertEquals("\"asdf\"", FuzzyUtil.inspect("asdf"));
		assertEquals("\"\\\\ \\b \\n \\t \\f \\r \\\"\"", FuzzyUtil.inspect("\\ \b \n \t \f \r \""));
		assertEquals("\"\\u000F\\u7FFF\"", FuzzyUtil.inspect("\u000F\u7FFF"));

		assertEquals(
			"\"Hello, \\uD83C\\uDF35!\"",
			FuzzyUtil.inspect("Hello, " + new String(Character.toChars(0x1F335)) + "!")
		);
	}

	@Test
	public void testInspectNumbers() {
		assertEquals("123", FuzzyUtil.inspect((byte)123));
		assertEquals("1234", FuzzyUtil.inspect((short)1234));
		assertEquals("12345", FuzzyUtil.inspect(12345));
		assertEquals("123456", FuzzyUtil.inspect(123456L));

		assertEquals("12.34", FuzzyUtil.inspect(12.34f));
		assertEquals("12.345", FuzzyUtil.inspect(12.345));
	}

	@Test
	public void testInspectMap() {
		TreeMap<String, Integer> map = new TreeMap<>();
		map.put("A", 1);
		map.put("B", 2);
		map.put("C", 3);

		assertEquals("{\"A\": 1, \"B\": 2, \"C\": 3}", FuzzyUtil.inspect(map));

		map.remove("A");
		map.remove("C");

		assertEquals("{\"B\": 2}", FuzzyUtil.inspect(map));

		map.clear();

		assertEquals("{}", FuzzyUtil.inspect(map));
	}

	@Test
	public void testInspectMapMaxLength() {
		TreeMap<Integer, Integer> map = new TreeMap<>();
		for(int i = 1; i <= 100; i++) map.put(i, i + 1);

		assertEquals(
			"{1: 2, 2: 3, 3: 4, 4: 5, 5: 6, 6: 7, 7: 8, 8: 9, 9: 10, 10: 11, 11: 12, 12: 13, 13: 14, 14: 15, 15: 16, 16: 17, 17: 18, 18: 19, 19: 20, 20: 21, 21: 22, 22: 23, 23: 24, 24: 25, 25: 26, 26: 27, 27: 28, 28: 29, 29: 30, 30: 31, 31: 32, 32: 33, 33: 34, 34: 35, 35: 36, 36: 37, 37: 38, 38: 39, 39: 40, 40: 41, 41: 42, 42: 43, 43: 44, 44: 45, 45: 46, 46: 47, 47: 48, 48: 49, 49: 50, 50: 51 ...}",
			FuzzyUtil.inspect(map)
		);
	}

	@Test
	public void testInspectArray() {
		assertEquals("[1, 2, 3, 4]", FuzzyUtil.inspect(new int[] {1, 2, 3, 4}));
		assertEquals("[100]", FuzzyUtil.inspect(new int[] {100}));
		assertEquals("[]", FuzzyUtil.inspect(new int[0]));
	}

	@Test
	public void testInspectIterable() {
		assertEquals("[5, 6, 7, 8]", FuzzyUtil.inspect(Arrays.asList(5, 6, 7, 8)));
		assertEquals("[200]", FuzzyUtil.inspect(Collections.singleton(200)));
		assertEquals("[]", FuzzyUtil.inspect(Collections.emptyList()));
	}

	@Test
	public void testInspectIterableMaxLength() {
		List<Integer> list = new ArrayList<>();
		for(int i = 1; i <= 100; i++) list.add(i);

		assertEquals(
			"[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50 ...]",
			FuzzyUtil.inspect(list)
		);
	}

	@Test
	public void testInspectOtherObject() {
		Object o = new Object() { @Override public String toString() { return "Hello, world!"; } };
		assertEquals("Hello, world!", FuzzyUtil.inspect(o));
	}

	@Test
	public void testInspectRecursion() {
		Map<Integer, Object> map = new TreeMap<>();

		map.put(1, Arrays.asList(Collections.emptyMap(), Collections.emptyMap()));
		map.put(2, "Hello!");
		map.put(3, Collections.emptyMap());

		assertEquals("{1: [{}, {}], 2: \"Hello!\", 3: {}}", FuzzyUtil.inspect(map));
	}

	@Test
	public void testInspectMaxDepth() {
		assertEquals("[[[...]]]", FuzzyUtil.inspect(new int[][][][] {{{{}}}}));

		List<Object> l = new ArrayList<>();
		l.add(l);
		assertEquals("[[[...]]]", FuzzyUtil.inspect(l));

		Map<Integer, Object> m = new TreeMap<>();
		m.put(1, m);
		m.put(2, "Hi");
		assertEquals("{1: {1: {1: ..., 2: \"Hi\"}, 2: \"Hi\"}, 2: \"Hi\"}", FuzzyUtil.inspect(m));
	}

	@Test
	public void testConstructorForCoverage() { new FuzzyUtil(); }

}
