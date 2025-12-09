package bank;

import common.AccountType;
import java.io.Serializable;
import java.sql.Date;

//*******************************************************************
// Name : Account
// Type : Abstract Class
// Requirements : Account 클래스 정의 및 추상클래스 구현
// Description : 모든 계좌의 공통 속성과 기능을 정의함.
//*******************************************************************
public abstract class Account implements Serializable {
    protected String owner;
    protected String accountNo;
    protected long balance;
    protected Date openDate;

    public Account(String owner, String accountNo, long balance, Date openDate) {
        this.owner = owner;
        this.accountNo = accountNo;
        this.balance = balance;
        this.openDate = openDate;
    }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }

    public Date getOpenDate() { return openDate; }
    public void setOpenDate(Date openDate) { this.openDate = openDate; }

    // 입금은 모든 계좌 공통
    public void deposit(long amount) {
        this.balance += amount;
    }

    // 출금은 계좌 유형에 따라 다를 수 있으므로(마이너스 통장 등) 메서드 정의
    // 구체적인 구현은 자식 클래스에서 오버라이딩하거나 공통 사용
    public boolean withdraw(long amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    // 계좌 정보를 출력하는 추상 메서드 (자식에서 구현 강제)
    public abstract void display();

    // 계좌 타입을 반환하는 추상 메서드
    public abstract AccountType getAccountType();
}