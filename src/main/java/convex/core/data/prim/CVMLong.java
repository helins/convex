package convex.core.data.prim;

import convex.core.data.Format;
import convex.core.data.INumeric;
import convex.core.data.Tag;
import convex.core.data.type.AType;
import convex.core.data.type.Types;
import convex.core.exceptions.InvalidDataException;

/**
 * Class for CVM long values.
 * 
 * Longs are signed 64-bit integers, and are the primary fixed point integer type on the CVM.
 */
public final class CVMLong extends APrimitive implements INumeric {

	public static final CVMLong ZERO = CVMLong.create(0L);
	public static final CVMLong ONE = CVMLong.create(1L);
	public static final CVMLong MINUS_ONE = CVMLong.create(-1L);
	public static final CVMLong MAX_VALUE = CVMLong.create(Long.MAX_VALUE);
	public static final CVMLong MIN_VALUE = CVMLong.create(Long.MIN_VALUE);
	
	private final long value;
	
	public CVMLong(long value) {
		this.value=value;
	}

	public static CVMLong create(long value) {
		return new CVMLong(value);
	}
	
	@Override
	public AType getType() {
		return Types.LONG;
	}
	
	@Override
	public long longValue() {
		return value;
	}
	
	@Override
	public CVMLong toLong() {
		return this;
	}

	@Override
	public CVMDouble toDouble() {
		return CVMDouble.create(doubleValue());
	}
	
	@Override
	public int estimatedEncodingSize() {
		return 1+Format.MAX_VLC_LONG_LENGTH;
	}

	@Override
	public void validateCell() throws InvalidDataException {
		// Nothing to check. Always valid
	}

	@Override
	public int encode(byte[] bs, int pos) {
		bs[pos++]=Tag.LONG;
		return encodeRaw(bs,pos);
	}

	@Override
	public int encodeRaw(byte[] bs, int pos) {
		return Format.writeVLCLong(bs, pos, value);
	}

	@Override
	public void ednString(StringBuilder sb) {
		sb.append(value);
	}

	@Override
	public void print(StringBuilder sb) {
		sb.append(value);
	}

	@Override
	public Class<?> numericType() {
		return Long.class;
	}

	@Override
	public double doubleValue() {
		return (double)value;
	}
	
	public static CVMLong parse(String s) {
		return create(Long.parseLong(s));
	}
	
	@Override
	public byte getTag() {
		return Tag.LONG;
	}

	@Override
	public CVMLong signum() {
		if (value>0) return CVMLong.ONE;
		if (value<0) return CVMLong.MINUS_ONE;
		return CVMLong.ZERO;
	}

}
