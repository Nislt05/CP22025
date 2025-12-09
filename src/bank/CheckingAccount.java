package bank;

import common.AccountType;
import java.sql.Date;

//*******************************************************************
// Name : CheckingAccount
// Type : Class
// Requirements : 당좌예금계좌 구현
//*******************************************************************
public class CheckingAccount extends Account {
    // 연결된 저축예금계좌
    private SavingsAccount linkedSavings;

    public CheckingAccount(String owner, String accountNo, long balance, Date openDate) {
        super(owner, accountNo, balance, openDate);
    }

    public SavingsAccount getLinkedSavings() { return linkedSavings; }

    // 저축계좌와 연결 설정
    public void setLinkedSavings(SavingsAccount linkedSavings) {
        this.linkedSavings = linkedSavings;
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.CHECKING;
    }

    // 출금 재정의: 잔액 부족 시 자동이체 로직
    @Override
    public boolean withdraw(long amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        } else {
            // 잔액 부족 시 연결된 저축계좌 확인
            if (linkedSavings != null) {
                long needed = amount - balance;
                // 저축계좌의 이체 한도 확인
                long transferLimit = linkedSavings.getMaxTransferAmountToChecking();

                // 한도 내이고, 저축계좌에 돈이 충분한지 확인
                if (needed <= transferLimit && linkedSavings.getBalance() >= needed) {
                    // 자동 이체 수행
                    linkedSavings.withdraw(needed);
                    balance += needed; // 부족한 만큼 채움
                    balance -= amount; // 원래 출금 수행 (결국 0원이 됨)
                    System.out.println("알림: 잔액 부족으로 연결된 저축계좌에서 " + needed + "원이 자동 이체되었습니다.");
                    return true;
                }
            }
            return false; // 연결된 계좌 없거나 잔액/한도 부족
        }
    }

    @Override
    public void display() {
        String linkedInfo = (linkedSavings == null) ? "없음" : linkedSavings.getAccountNo();
        System.out.println("당좌계좌 [번호:" + accountNo + ", 예금주:" + owner + ", 잔액:" + balance + ", 연결계좌:" + linkedInfo + "]");
    }
}