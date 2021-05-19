package convex.core.lang;

import static convex.test.Assertions.assertCVMEquals;
import static convex.test.Assertions.assertStateError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import convex.core.Constants;
import convex.core.Init;
import convex.core.State;
import convex.core.crypto.AKeyPair;
import convex.core.data.ACell;
import convex.core.data.AccountKey;
import convex.core.data.Address;
import convex.core.data.Keyword;
import convex.core.data.prim.CVMBool;
import convex.core.data.prim.CVMDouble;
import convex.core.data.prim.CVMLong;
import convex.core.util.Utils;

/**
 * Class for building and testing a State for the unit test suite.
 * 
 * Includes example smart contracts.
 */
public class TestState {
	public static final int NUM_CONTRACTS = 5;

	public static final Address INIT = Init.INIT;
	
	public static final Address HERO = Init.HERO;
	public static final AKeyPair HERO_KP = Init.KEYPAIRS[Init.NUM_PEERS+0];

	public static final Address VILLAIN = Init.VILLAIN;
	public static final AKeyPair VILLAIN_KP = Init.KEYPAIRS[Init.NUM_PEERS+1];

	public static final Address[] CONTRACTS = new Address[NUM_CONTRACTS];

	public static final AKeyPair FIRST_PEER_KEYPAIR=Init.KEYPAIRS[0];
	public static final AccountKey FIRST_PEER_KEY=FIRST_PEER_KEYPAIR.getAccountKey();
	
	/**
	 * A test state set up with a few accounts
	 */
	public static final State STATE;
	
	static {
		
		try {
			State s = Init.createState();
			Context<?> ctx = Context.createFake(s, HERO);
			for (int i = 0; i < NUM_CONTRACTS; i++) {
				// Construct code for each contract
				ACell contractCode = Reader.read(
						"(do " + "(def my-data nil)" + "(defn write [x] (def my-data x)) "
								+ "(defn read [] my-data)" + "(defn who-called-me [] *caller*)"
								+ "(defn my-address [] *address*)" + "(defn my-number [] "+i+")" + "(defn foo [] :bar)"
								+ "(export write read who-called-me my-address my-number foo)" + ")");

				ctx = ctx.deployActor(contractCode);
				CONTRACTS[i] = (Address) ctx.getResult();
			}
								
			s= ctx.getState();
			STATE = s;
			INITIAL_CONTEXT = Context.createFake(STATE, TestState.HERO);
		} catch (Throwable e) {
			e.printStackTrace();
			throw new Error(e);
		}
	}
	
	/**
	 * Initial juice for TestState.INITIAL_CONTEXT
	 */
	public static final long INITIAL_JUICE = Constants.MAX_TRANSACTION_JUICE;

	/**
	 * Initial juice price
	 */
	public static final CVMLong JUICE_PRICE = STATE.getJuicePrice();

	/**
	 * A test context set up with a few accounts
	 */
	public static final Context<?> INITIAL_CONTEXT;

	/**
	 * Balance of hero's account before spending any juice / funds
	 */
	public static final long HERO_BALANCE = STATE.getAccount(HERO).getBalance();

	/**
	 * Balance of hero's account before spending any juice / funds
	 */
	public static final long VILLAIN_BALANCE = STATE.getAccount(VILLAIN).getBalance();

	/**
	 * Total funds in the test state, minus those subtracted for juice in the
	 * initial context
	 */
	public static final Long TOTAL_FUNDS = Constants.MAX_SUPPLY;
	


	@SuppressWarnings("unchecked")
	static <T extends ACell> AOp<T> compile(Context<?> c, String source) {
		c=c.fork();
		try {
			ACell form = Reader.read(source);
			AOp<T> op = (AOp<T>) c.expandCompile(form).getResult();
			return op;
		} catch (Exception e) {
			throw Utils.sneakyThrow(e);
		}
	}

	public static <T extends ACell> T eval(Context<?> c, String source) {
		c=c.fork();
		try {
			AOp<T> op = compile(c, source);
			Context<T> rc = c.execute(op);
			return rc.getResult();
		} catch (Exception e) {
			throw Utils.sneakyThrow(e);
		}
	}
	
	// Deploy actor code directly into a Context
	public static Context<?> deploy(Context<?> ctx,String actorResource) {
		String source;
		try {
			source = Utils.readResourceAsString(actorResource);
			ACell contractCode=Reader.read(source);
			ctx=ctx.deployActor(contractCode);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ctx;
	}

	@Test
	public void testInitial() {
		Context<?> ctx = Context.createFake(STATE,Init.HERO);
		State s = ctx.getState();
		assertEquals(STATE, s);
		assertEquals(Init.HERO,ctx.computeSpecial(Symbols.STAR_ADDRESS).getResult());
		assertSame(Core.COUNT, ctx.lookup(Symbols.COUNT).getResult());
		assertCVMEquals(Constants.INITIAL_TIMESTAMP, ctx.lookup(Symbols.STAR_TIMESTAMP).getResult());
		assertCVMEquals(Constants.INITIAL_TIMESTAMP, s.getTimeStamp());
	}

	@Test
	public void testContractCall() {
		Context<?> ctx0 = Context.createFake(STATE, HERO);
		Address TARGET = CONTRACTS[0];
		ctx0 = ctx0.execute(compile(ctx0, "(def target (address \"" + TARGET.toHexString() + "\"))"));
		ctx0 = ctx0.execute(compile(ctx0, "(def hero *address*)"));
		final Context<?> ctx = ctx0;

		assertEquals(HERO, ctx.lookup(Symbols.HERO).getResult());
		assertEquals(Keyword.create("bar"), eval(ctx, "(call target (foo))"));
		assertEquals(HERO, eval(ctx, "(call target (who-called-me))"));
		assertEquals(TARGET, eval(ctx, "(call target (my-address))"));

		assertEquals(0L, evalL(ctx, "(call target (my-number))"));

		assertStateError(TestState.step(ctx, "(call target (missing-function))"));
	}

	public static boolean evalB(String source) {
		return ((CVMBool)eval(source)).booleanValue();
	}

	public static boolean evalB(Context<?> ctx, String source) {
		return ((CVMBool)eval(ctx, source)).booleanValue();
	}

	public static double evalD(Context<?> ctx, String source) {
		return ((CVMDouble) eval(ctx, source)).doubleValue();
	}
	
	public static double evalD(String source) {
		return ((CVMDouble) eval(source)).doubleValue();
	}

	public static long evalL(Context<?> ctx, String source) {
		return RT.castLong(eval(ctx, source)).longValue();
	}

	public static long evalL(String source) {
		ACell r=eval(source);
		CVMLong rl=RT.castLong(r);
		if (rl==null) throw new Error("Can't cast result to Long: "+r);
		return rl.longValue();
	}
	
	public static String evalS(String source) {
		return eval(source).toString();
	}

	@SuppressWarnings("unchecked")
	public static <T extends ACell> T eval(String source) {
		return (T) step(source).getResult();
	}

	public static <T extends ACell> Context<T> step(String source) {
		return step(INITIAL_CONTEXT, source);
	}

	/**
	 * Steps execution in a new forked Context
	 * @param <T>
	 * @param ctx Initial context to fork
	 * @param source
	 * @return New forked context containing step result
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ACell> Context<T> step(Context<?> ctx, String source) {
		// Compile form in forked context
		Context<AOp<ACell>> cctx=ctx.fork();
		ACell form = Reader.read(source);
		cctx = cctx.expandCompile(form);
		if (cctx.isExceptional()) return (Context<T>) cctx;
		AOp<ACell> op = cctx.getResult();

		// Run form in separate forked context to get result context
		Context<T> rctx = ctx.fork();
		rctx=(Context<T>) rctx.run(op);
		assert(rctx.getDepth()==0):"Invalid depth after step: "+rctx.getDepth();
		return rctx;
	}

	/**
	 * Runs an execution step as a different address. Returns value after restoring
	 * the original address.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ACell> Context<T> stepAs(Address address, Context<?> c, String source) {
		Context<?> rc = Context.createFake(c.getState(), address);
		rc = step(rc, source);
		return (Context<T>) Context.createFake(rc.getState(), c.getAddress()).withValue(rc.getValue());
	}
	
	@Test public void testStateSetup() {
		assertEquals(0,INITIAL_CONTEXT.getDepth());
		assertFalse(INITIAL_CONTEXT.isExceptional());
		assertNull(INITIAL_CONTEXT.getResult());
		assertEquals(TestState.TOTAL_FUNDS, STATE.computeTotalFunds());

	}

	public static void main(String[] args) {
		System.out.println(Utils.ednString(STATE));
	}
	
}
