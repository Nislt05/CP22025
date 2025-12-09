package atm;

import java.text.NumberFormat;

public class BankUtils {
	public static String displayAccountNo(String accountNo) {
		return accountNo.replaceAll("(\\d{4})(\\d{1})(\\d{4})", "$1-$2-$3");
	}
	
	public static String displayBalance(long balance) {
		return NumberFormat.getInstance().format(balance);
	}
}
