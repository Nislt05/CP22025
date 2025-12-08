package BankServer.src.bank;

import BankServer.src.common.AccountType;
import java.sql.Date;

public class CheckingAccount extends Account {
    private SavingsAccount linkedSavings; // 연결된 저축예금계좌 [cite: 77]

    public CheckingAccount(String owner, String accountNo, long balance, Date openDate, SavingsAccount linkedSavings) {
        super(owner, accountNo, AccountType.CHECKING, balance, openDate);
        this.linkedSavings = linkedSavings;
    }

    // 당좌 출금 로직: 잔액 부족시 연결된 계좌에서 가져옴 [cite: 89]
    @Override
    public boolean withdraw(long amount) throws Exception {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        } else {
            // 잔액 부족 시 자동이체 로직
            long needed = amount - this.balance;
            if (linkedSavings != null && linkedSavings.getBalance() >= needed) {
                // 연결된 계좌의 이체 한도 체크 등을 여기서 수행할 수 있음
                if(needed > linkedSavings.getMaxTransferAmountToChecking()) {
                    throw new Exception("자동이체 한도를 초과했습니다.");
                }

                linkedSavings.withdraw(needed); // 저축계좌에서 부족분 인출
                this.balance = 0; // 현재 잔액은 0원이 됨 (부족분을 메꿨으므로)
                return true;
            } else {
                throw new Exception("잔액이 부족하며, 연결된 계좌에서도 충당할 수 없습니다.");
            }
        }
    }

    @Override
    public String display() {
        String linkedInfo = (linkedSavings == null) ? "없음" : linkedSavings.getAccountNo();
        return String.format("[당좌] 계좌:%s, 예금주:%s, 잔액:%d원, 연결계좌:%s",
                accountNo, owner, balance, linkedInfo);
    }
}