/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.math.expr;

import com.google.common.collect.ImmutableMap;
import org.apache.druid.common.config.NullHandling;
import org.apache.druid.testing.InitializedNullHandlingTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;

public class FunctionTest extends InitializedNullHandlingTest
{
  private Expr.ObjectBinding bindings;

  @Before
  public void setup()
  {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    builder.put("x", "foo");
    builder.put("y", 2);
    builder.put("z", 3.1);
    builder.put("a", new String[] {"foo", "bar", "baz", "foobar"});
    builder.put("b", new Long[] {1L, 2L, 3L, 4L, 5L});
    builder.put("c", new Double[] {3.1, 4.2, 5.3});
    bindings = Parser.withMap(builder.build());
  }

  @Test
  public void testCaseSimple()
  {
    assertExpr("case_simple(x,'baz','is baz','foo','is foo','is other')", "is foo");
    assertExpr("case_simple(x,'baz','is baz','bar','is bar','is other')", "is other");
    assertExpr("case_simple(y,2,'is 2',3,'is 3','is other')", "is 2");
    assertExpr("case_simple(z,2,'is 2',3,'is 3','is other')", "is other");
  }

  @Test
  public void testCaseSearched()
  {
    assertExpr("case_searched(x=='baz','is baz',x=='foo','is foo','is other')", "is foo");
    assertExpr("case_searched(x=='baz','is baz',x=='bar','is bar','is other')", "is other");
    assertExpr("case_searched(y==2,'is 2',y==3,'is 3','is other')", "is 2");
    assertExpr("case_searched(z==2,'is 2',z==3,'is 3','is other')", "is other");
  }

  @Test
  public void testConcat()
  {
    assertExpr("concat(x,' ',y)", "foo 2");
    if (NullHandling.replaceWithDefault()) {
      assertExpr("concat(x,' ',nonexistent,' ',y)", "foo  2");
    } else {
      assertArrayExpr("concat(x,' ',nonexistent,' ',y)", null);
    }

    assertExpr("concat(z)", "3.1");
    assertArrayExpr("concat()", null);
  }

  @Test
  public void testReplace()
  {
    assertExpr("replace(x,'oo','ab')", "fab");
    assertExpr("replace(x,x,'ab')", "ab");
    assertExpr("replace(x,'oo',y)", "f2");
  }

  @Test
  public void testSubstring()
  {
    assertExpr("substring(x,0,2)", "fo");
    assertExpr("substring(x,1,2)", "oo");
    assertExpr("substring(x,y,1)", "o");
    assertExpr("substring(x,0,-1)", "foo");
    assertExpr("substring(x,0,100)", "foo");
  }

  @Test
  public void testStrlen()
  {
    assertExpr("strlen(x)", 3L);
    assertExpr("strlen(nonexistent)", NullHandling.defaultLongValue());
  }

  @Test
  public void testStrpos()
  {
    assertExpr("strpos(x, 'o')", 1L);
    assertExpr("strpos(x, 'o', 0)", 1L);
    assertExpr("strpos(x, 'o', 1)", 1L);
    assertExpr("strpos(x, 'o', 2)", 2L);
    assertExpr("strpos(x, 'o', 3)", -1L);
    assertExpr("strpos(x, '')", 0L);
    assertExpr("strpos(x, 'x')", -1L);
  }

  @Test
  public void testLower()
  {
    assertExpr("lower('FOO')", "foo");
  }

  @Test
  public void testUpper()
  {
    assertExpr("upper(x)", "FOO");
  }

  @Test
  public void testIsNull()
  {
    assertExpr("isnull(null)", 1L);
    assertExpr("isnull('abc')", 0L);
  }

  @Test
  public void testIsNotNull()
  {
    assertExpr("notnull(null)", 0L);
    assertExpr("notnull('abc')", 1L);
  }

  @Test
  public void testLpad()
  {
    assertExpr("lpad(x, 5, 'ab')", "abfoo");
    assertExpr("lpad(x, 4, 'ab')", "afoo");
    assertExpr("lpad(x, 2, 'ab')", "fo");
    assertArrayExpr("lpad(x, 0, 'ab')", null);
    assertArrayExpr("lpad(x, 5, null)", null);
    assertArrayExpr("lpad(null, 5, x)", null);
  }

  @Test
  public void testRpad()
  {
    assertExpr("rpad(x, 5, 'ab')", "fooab");
    assertExpr("rpad(x, 4, 'ab')", "fooa");
    assertExpr("rpad(x, 2, 'ab')", "fo");
    assertArrayExpr("rpad(x, 0, 'ab')", null);
    assertArrayExpr("rpad(x, 5, null)", null);
    assertArrayExpr("rpad(null, 5, x)", null);
  }

  @Test
  public void testArrayConstructor()
  {
    assertArrayExpr("array(1, 2, 3, 4)", new Long[]{1L, 2L, 3L, 4L});
    assertArrayExpr("array(1, 2, 3, 'bar')", new Long[]{1L, 2L, 3L, null});
    assertArrayExpr("array(1.0)", new Double[]{1.0});
    assertArrayExpr("array('foo', 'bar')", new String[]{"foo", "bar"});
  }

  @Test
  public void testArrayLength()
  {
    assertExpr("array_length([1,2,3])", 3L);
    assertExpr("array_length(a)", 4);
  }

  @Test
  public void testArrayOffset()
  {
    assertExpr("array_offset([1, 2, 3], 2)", 3L);
    assertArrayExpr("array_offset([1, 2, 3], 3)", null);
    assertExpr("array_offset(a, 2)", "baz");
  }

  @Test
  public void testArrayOrdinal()
  {
    assertExpr("array_ordinal([1, 2, 3], 3)", 3L);
    assertArrayExpr("array_ordinal([1, 2, 3], 4)", null);
    assertExpr("array_ordinal(a, 3)", "baz");
  }

  @Test
  public void testArrayOffsetOf()
  {
    assertExpr("array_offset_of([1, 2, 3], 3)", 2L);
    assertExpr("array_offset_of([1, 2, 3], 4)", NullHandling.replaceWithDefault() ? -1L : null);
    assertExpr("array_offset_of(a, 'baz')", 2);
  }

  @Test
  public void testArrayOrdinalOf()
  {
    assertExpr("array_ordinal_of([1, 2, 3], 3)", 3L);
    assertExpr("array_ordinal_of([1, 2, 3], 4)", NullHandling.replaceWithDefault() ? -1L : null);
    assertExpr("array_ordinal_of(a, 'baz')", 3);
  }

  @Test
  public void testArrayContains()
  {
    assertExpr("array_contains([1, 2, 3], 2)", 1L);
    assertExpr("array_contains([1, 2, 3], 4)", 0L);
    assertExpr("array_contains([1, 2, 3], [2, 3])", 1L);
    assertExpr("array_contains([1, 2, 3], [3, 4])", 0L);
    assertExpr("array_contains(b, [3, 4])", 1L);
  }

  @Test
  public void testArrayOverlap()
  {
    assertExpr("array_overlap([1, 2, 3], [2, 4, 6])", 1L);
    assertExpr("array_overlap([1, 2, 3], [4, 5, 6])", 0L);
  }

  @Test
  public void testArrayAppend()
  {
    assertArrayExpr("array_append([1, 2, 3], 4)", new Long[]{1L, 2L, 3L, 4L});
    assertArrayExpr("array_append([1, 2, 3], 'bar')", new Long[]{1L, 2L, 3L, null});
    assertArrayExpr("array_append([], 1)", new String[]{"1"});
    assertArrayExpr("array_append(<LONG>[], 1)", new Long[]{1L});
  }

  @Test
  public void testArrayConcat()
  {
    assertArrayExpr("array_concat([1, 2, 3], [2, 4, 6])", new Long[]{1L, 2L, 3L, 2L, 4L, 6L});
    assertArrayExpr("array_concat([1, 2, 3], 4)", new Long[]{1L, 2L, 3L, 4L});
    assertArrayExpr("array_concat(0, [1, 2, 3])", new Long[]{0L, 1L, 2L, 3L});
    assertArrayExpr("array_concat(map(y -> y * 3, b), [1, 2, 3])", new Long[]{3L, 6L, 9L, 12L, 15L, 1L, 2L, 3L});
    assertArrayExpr("array_concat(0, 1)", new Long[]{0L, 1L});
  }

  @Test
  public void testArrayToString()
  {
    assertExpr("array_to_string([1, 2, 3], ',')", "1,2,3");
    assertExpr("array_to_string([1], '|')", "1");
    assertExpr("array_to_string(a, '|')", "foo|bar|baz|foobar");
  }

  @Test
  public void testStringToArray()
  {
    assertArrayExpr("string_to_array('1,2,3', ',')", new String[]{"1", "2", "3"});
    assertArrayExpr("string_to_array('1', ',')", new String[]{"1"});
    assertArrayExpr("string_to_array(array_to_string(a, ','), ',')", new String[]{"foo", "bar", "baz", "foobar"});
  }

  @Test
  public void testArrayCast()
  {
    assertArrayExpr("cast([1, 2, 3], 'STRING_ARRAY')", new String[]{"1", "2", "3"});
    assertArrayExpr("cast([1, 2, 3], 'DOUBLE_ARRAY')", new Double[]{1.0, 2.0, 3.0});
    assertArrayExpr("cast(c, 'LONG_ARRAY')", new Long[]{3L, 4L, 5L});
    assertArrayExpr("cast(string_to_array(array_to_string(b, ','), ','), 'LONG_ARRAY')", new Long[]{1L, 2L, 3L, 4L, 5L});
    assertArrayExpr("cast(['1.0', '2.0', '3.0'], 'LONG_ARRAY')", new Long[]{1L, 2L, 3L});
  }

  @Test
  public void testArraySlice()
  {
    assertArrayExpr("array_slice([1, 2, 3, 4], 1, 3)", new Long[] {2L, 3L});
    assertArrayExpr("array_slice([1.0, 2.1, 3.2, 4.3], 2)", new Double[] {3.2, 4.3});
    assertArrayExpr("array_slice(['a', 'b', 'c', 'd'], 4, 6)", new String[] {null, null});
    assertArrayExpr("array_slice([1, 2, 3, 4], 2, 2)", new Long[] {});
    assertArrayExpr("array_slice([1, 2, 3, 4], 5, 7)", null);
    assertArrayExpr("array_slice([1, 2, 3, 4], 2, 1)", null);
  }

  @Test
  public void testArrayPrepend()
  {
    assertArrayExpr("array_prepend(4, [1, 2, 3])", new Long[]{4L, 1L, 2L, 3L});
    assertArrayExpr("array_prepend('bar', [1, 2, 3])", new Long[]{null, 1L, 2L, 3L});
    assertArrayExpr("array_prepend(1, [])", new String[]{"1"});
    assertArrayExpr("array_prepend(1, <LONG>[])", new Long[]{1L});
    assertArrayExpr("array_prepend(1, <DOUBLE>[])", new Double[]{1.0});
  }

  @Test
  public void testGreatest()
  {
    // Same types
    assertExpr("greatest(y, 0)", 2L);
    assertExpr("greatest(34.0, z, 5.0, 767.0", 767.0);
    assertExpr("greatest('B', x, 'A')", "foo");

    // Different types
    assertExpr("greatest(-1, z, 'A')", "A");
    assertExpr("greatest(-1, z)", 3.1);
    assertExpr("greatest(1, 'A')", "A");

    // Invalid types
    try {
      assertExpr("greatest(1, ['A'])", null);
      Assert.fail("Did not throw IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      Assert.assertEquals("Function[greatest] does not accept STRING_ARRAY types", e.getMessage());
    }

    // Null handling
    assertExpr("greatest()", null);
    assertExpr("greatest(null, null)", null);
    assertExpr("greatest(1, null, 'A')", "A");
  }

  @Test
  public void testLeast()
  {
    // Same types
    assertExpr("least(y, 0)", 0L);
    assertExpr("least(34.0, z, 5.0, 767.0", 3.1);
    assertExpr("least('B', x, 'A')", "A");

    // Different types
    assertExpr("least(-1, z, 'A')", "-1");
    assertExpr("least(-1, z)", -1.0);
    assertExpr("least(1, 'A')", "1");

    // Invalid types
    try {
      assertExpr("least(1, [2, 3])", null);
      Assert.fail("Did not throw IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      Assert.assertEquals("Function[least] does not accept LONG_ARRAY types", e.getMessage());
    }

    // Null handling
    assertExpr("least()", null);
    assertExpr("least(null, null)", null);
    assertExpr("least(1, null, 'A')", "1");
  }

  private void assertExpr(final String expression, @Nullable final Object expectedResult)
  {
    final Expr expr = Parser.parse(expression, ExprMacroTable.nil());
    Assert.assertEquals(expression, expectedResult, expr.eval(bindings).value());

    final Expr exprNoFlatten = Parser.parse(expression, ExprMacroTable.nil(), false);
    final Expr roundTrip = Parser.parse(exprNoFlatten.stringify(), ExprMacroTable.nil());
    Assert.assertEquals(expr.stringify(), expectedResult, roundTrip.eval(bindings).value());

    final Expr roundTripFlatten = Parser.parse(expr.stringify(), ExprMacroTable.nil());
    Assert.assertEquals(expr.stringify(), expectedResult, roundTripFlatten.eval(bindings).value());

    Assert.assertEquals(expr.stringify(), roundTrip.stringify());
    Assert.assertEquals(expr.stringify(), roundTripFlatten.stringify());
  }

  private void assertArrayExpr(final String expression, @Nullable final Object[] expectedResult)
  {
    final Expr expr = Parser.parse(expression, ExprMacroTable.nil());
    Assert.assertArrayEquals(expression, expectedResult, expr.eval(bindings).asArray());

    final Expr exprNoFlatten = Parser.parse(expression, ExprMacroTable.nil(), false);
    final Expr roundTrip = Parser.parse(exprNoFlatten.stringify(), ExprMacroTable.nil());
    Assert.assertArrayEquals(expression, expectedResult, roundTrip.eval(bindings).asArray());

    final Expr roundTripFlatten = Parser.parse(expr.stringify(), ExprMacroTable.nil());
    Assert.assertArrayEquals(expression, expectedResult, roundTripFlatten.eval(bindings).asArray());

    Assert.assertEquals(expr.stringify(), roundTrip.stringify());
    Assert.assertEquals(expr.stringify(), roundTripFlatten.stringify());
  }
}
