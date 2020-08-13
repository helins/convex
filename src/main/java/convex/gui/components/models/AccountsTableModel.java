package convex.gui.components.models;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import convex.core.Init;
import convex.core.State;
import convex.core.data.AMap;
import convex.core.data.AccountStatus;
import convex.core.data.Address;
import convex.core.data.Keyword;
import convex.core.data.MapEntry;
import convex.core.data.Symbol;
import convex.core.util.Utils;

@SuppressWarnings("serial")
public class AccountsTableModel extends AbstractTableModel implements TableModel {

	private State state;

	public AccountsTableModel(State state) {
		this.state = state;
	}

	private static final String[] FIXED_COLS = new String[] { "Address", "Type", "Count", "Balance", "Name", "Env.Size", "Allowance" };

	public String getColumnName(int col) {
		if (col < FIXED_COLS.length) return FIXED_COLS[col];
		return "FOO";
	}

	@Override
	public int getRowCount() {
		return Utils.checkedInt(state.getAccounts().count());
	}

	@Override
	public int getColumnCount() {
		// TODO token columns?
		return FIXED_COLS.length;
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		MapEntry<Address, AccountStatus> me = getEntry(rowIndex);
		Address address = me.getKey();
		AccountStatus as = me.getValue();
		switch (columnIndex) {
		case 0:
			return address.toChecksumHex();
		case 1:
			return as.isActor()?"Actor":"User";
		case 2: {
			long seq=as.getSequence();
			return (seq>=0)?seq:"";
		}
		case 3:
			return as.getBalance().toFriendlyString();
		case 4: {
			AMap<Address, Object> reg = state.getAccount(Init.REGISTRY_ADDRESS).getEnvironment()
					.get(Symbol.create("registry")).getValue();
			Object o = reg.get(address);
			if (o == null) return "";
			if (!(o instanceof AMap)) return "<Invalid registration, not a map!>";
			AMap<Keyword, Object> a = (AMap<Keyword, Object>) reg.get(address);
			return a.get(Keyword.create("name"));
		}
		case 5:
			return as.getMemorySize();
		case 6:
			return as.getAllowance();
		default:
			return "";
		}
	}

	public void setState(State newState) {
		if (state != newState) {
			state = newState;
			fireTableDataChanged();
		}
	}

	public MapEntry<Address, AccountStatus> getEntry(int row) {
		return state.getAccounts().entryAt(row);
	}

}