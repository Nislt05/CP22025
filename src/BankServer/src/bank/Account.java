package BankServer.src.bank;

import BankServer.src.common.AccountType;
import java.io.Serializable;
import java.sql.Date;

// 요구사항: Account 클래스를 추상클래스로 구현 [cite: 123]
public abstract class Account implements Serializable {
    protected String owner;
    protected String accountNo;
    protected AccountType type;
    protected long balance;      // TotalBalance
    protected Date openDate;

    public Account(String owner, String accountNo, AccountType type, long balance, Date openDate) {
        this.owner = owner;
        this.accountNo = accountNo;
        this.type = type;
        this.balance = balance;
        this.openDate = openDate;
    }

    // 공통 Getter/Setter
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }

    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }

    public Date getOpenDate() { return openDate; }
    public void setOpenDate(Date openDate) { this.openDate = openDate; }

    // 입금 (공통 기능) [cite: 86]
    public void deposit(long amount) {
        this.balance += amount;
    }

    // 출금 (추상 메소드 - 자식에서 구체적 구현) [cite: 86]
    // 당좌계좌는 마이너스 통장 기능이 필요하므로 로직이 다름
    public abstract boolean withdraw(long amount) throws Exception;

    // 계좌 정보 출력 (추상 메소드) [cite: 125]
    public abstract String display();

    @Override
    public String toString() {
        return display();
    }
}