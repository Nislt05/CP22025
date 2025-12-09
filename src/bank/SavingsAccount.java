package bank;

import common.AccountType;
import java.sql.Date;

//*******************************************************************
// Name : SavingsAccount
// Type : Class
// Requirements : 저축예금계좌 구현
//*******************************************************************
public class SavingsAccount extends Account {
    // 이자율, 자동이체 최대 금액
    private double interestRate;
    private long maxTransferAmountToChecking;

    public SavingsAccount(String owner, String accountNo, long balance, Date openDate, double interestRate) {
        super(owner, accountNo, balance, openDate);
        this.interestRate = interestRate;
        this.maxTransferAmountToChecking = 0; // 기본값 0
    }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public long getMaxTransferAmountToChecking() { return maxTransferAmountToChecking; }
    public void setMaxTransferAmountToChecking(long amount) { this.maxTransferAmountToChecking = amount; }

    @Override
    public AccountType getAccountType() {
        return AccountType.SAVINGS;
    }

    @Override
    public void display() {
        System.out.println("저축계좌 [번호:" + accountNo + ", 예금주:" + owner + ", 잔액:" + balance + ", 이자율:" + interestRate + "]");
    }
}