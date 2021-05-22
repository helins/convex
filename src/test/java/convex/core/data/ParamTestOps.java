package convex.core.data;

import static convex.test.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import convex.core.State;
import convex.core.data.prim.CVMBool;
import convex.core.exceptions.BadFormatException;
import convex.core.exceptions.InvalidDataException;
import convex.core.exceptions.ValidationException;
import convex.core.lang.AOp;
import convex.core.lang.Context;
import convex.core.lang.RT;
import convex.core.lang.TestState;
import convex.core.lang.ops.Cond;
import convex.core.lang.ops.Constant;
import convex.core.lang.ops.Def;
import convex.core.lang.ops.Do;
import convex.core.lang.ops.Invoke;
import convex.core.lang.ops.Lookup;

@RunWith(Parameterized.class)
public class ParamTestOps {
	private AOp<?> op;
	private Object expected;

	private static final State INITIAL_STATE = TestState.STATE;

	public ParamTestOps(String label, AOp<?> v, Object expected) {
		this.op = v;
		this.expected = expected;
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> dataExamples() throws BadFormatException {
		return Arrays
				.asList(new Object[][] { 
					    { "Constant", Constant.of(1L), RT.cvm(1L) },
						{ "Lookup", Do.create(Def.create("foo", Constant.of(13)), 
								Lookup.create("foo")), RT.cvm(13) },
						{ "Def", Def.create("foo", Constant.createString("bar")), Strings.create("bar") },
						{ "Vector", Invoke.create("vector", Constant.createString("foo"), Constant.createString("bar")),
								Vectors.of(Strings.create("foo"), Strings.create("bar")) },

						{ "Do", Do.create(Constant.createString("foo"), Constant.createString("bar")), Strings.create("bar") },
						{ "Cond",
								Cond.create(Constant.of(CVMBool.TRUE), Constant.createString("truthy"),
										Constant.createString("falsey")),
								Strings.create("truthy") },
						{ "Def", Def.create("foo", Constant.of(1L)), 1L } });
	}

	@Test
	public void testExpectedResult() {
		long JUICE = 10000;
		Context<?> c = Context.createInitial(INITIAL_STATE, TestState.HERO, JUICE);
		Context<?> c2 = c.execute(op);

		assertCVMEquals(expected, c2.getResult());
	}

	@Test
	public void testCanonical() {
		assertTrue(op.isCanonical());
	}

	@Test
	public void testGeneric() throws InvalidDataException, ValidationException {
		ObjectsTest.doCellTests(op);
	}
}
