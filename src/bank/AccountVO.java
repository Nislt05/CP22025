/*

package BankServer.src.bank;

import BankServer.src.common.AccountType;

import java.io.Serializable;
import java.sql.Date;

//*******************************************************************
// Name : AccountVO
// Type : Class
// Description :  계좌정보를 정의 하기 위해 필요한 VO(ValueObject)이다.
//                생성자와, 오브젝트 내부 데이터 get, set 동작이 구현되어 있다.
//                오브젝트 형태로 txt에 저장할수 있도록 implements Serializable를 통해
//                직렬화 되어있다.
//*******************************************************************
public class AccountVO implements Serializable {
    private String owner;
    private String accountNo;
    private AccountType type;
    private long balance;
    private Date openDate;

    public AccountVO() {
    }

    public AccountVO(String owner, String accountNo, AccountType type, long balance, Date openDate) {
        this.owner = owner;
        this.accountNo = accountNo;
        this.type = type;
        this.balance = balance;
        this.openDate = openDate;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public Date getOpenDate() {
        return openDate;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    @Override
    public String toString() {
        return "Account{" +
                "owner='" + owner + '\'' +
                ", accountNo='" + accountNo + '\'' +
                ", type=" + type +
                ", balance=" + balance +
                ", openDate=" + openDate +
                '}';
    }
}

 */