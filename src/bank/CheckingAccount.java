package bank;

import common.AccountType;
import java.sql.Date;

//*******************************************************************
// Name : CheckingAccount
// Type : Class
// Requirements : 당좌예금계좌 구현
//*******************************************************************
public class CheckingAccount extends Account {
    private SavingsAccount linkedSavings;
    private long lastAutoTransferAmount = 0;

    public CheckingAccount(String owner, String accountNo, long balance, Date openDate) {
        super(owner, accountNo, balance, openDate);
    }

    public SavingsAccount getLinkedSavings() { return linkedSavings; }
    public void setLinkedSavings(SavingsAccount linkedSavings) { this.linkedSavings = linkedSavings; }
    public long getLastAutoTransferAmount() { return lastAutoTransferAmount; }

    @Override
    public AccountType getAccountType() {
        return AccountType.CHECKING;
    }

    // 출금 재정의: 잔액 부족 시 자동이체 로직
    @Override
    public boolean withdraw(long amount) {
        lastAutoTransferAmount = 0;

        if (balance >= amount) {
            balance -= amount;
            return true;
        } else {
            // 잔액 부족 시 연결된 저축계좌 확인
            if (linkedSavings != null) {
                long needed = amount - balance;

                // (수정됨) 저축계좌 잔액 확인
                if (linkedSavings.getBalance() >= needed) {
                    linkedSavings.withdraw(needed);
                    balance += needed;
                    balance -= amount;
                    lastAutoTransferAmount = needed;
                    System.out.println("알림: 잔액 부족으로 연결된 저축계좌에서 " + needed + "원이 자동 이체되었습니다.");
                    return true;
                } else {
                    // (추가됨) 둘 다 잔액이 부족한 경우 RuntimeException 발생 -> Client에서 캐치
                    throw new RuntimeException("당좌계좌 및 연결된 저축계좌의 잔액이 모두 부족합니다.");
                }
            } else {
                throw new RuntimeException("잔액 부족 (연결된 저축계좌 없음)");
            }
        }
    }

    @Override
    public void display() {
        String linkedInfo = (linkedSavings == null) ? "없음" : linkedSavings.getAccountNo();
        System.out.println("당좌계좌 [번호:" + accountNo + ", 예금주:" + owner + ", 잔액:" + balance + ", 연결계좌:" + linkedInfo + "]");
    }
}