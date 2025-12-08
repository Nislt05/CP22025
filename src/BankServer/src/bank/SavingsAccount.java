package BankServer.src.bank;

import BankServer.src.common.AccountType;
import java.sql.Date;

public class SavingsAccount extends Account {
    private double interestRate; // 이자율
    private long maxTransferAmountToChecking; // 당좌계좌로 자동이체 가능한 최대 금액

    public SavingsAccount(String owner, String accountNo, long balance, Date openDate, double interestRate) {
        super(owner, accountNo, AccountType.SAVINGS, balance, openDate);
        this.interestRate = interestRate;
        this.maxTransferAmountToChecking = 1_000_000; // 기본값 100만원 설정 (예시)
    }

    public long getMaxTransferAmountToChecking() {
        return maxTransferAmountToChecking;
    }

    // 일반적인 출금: 잔액 부족시 실패
    @Override
    public boolean withdraw(long amount) throws Exception {
        if (this.balance < amount) {
            throw new Exception("잔액이 부족합니다.");
        }
        this.balance -= amount;
        return true;
    }

    @Override
    public String display() {
        return String.format("[저축] 계좌:%s, 예금주:%s, 잔액:%d원, 이자율:%.1f%%",
                accountNo, owner, balance, interestRate * 100);
    }
}