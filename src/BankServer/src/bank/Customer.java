package BankServer.src.bank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Customer implements Serializable {
    private String id;
    private String name;
    private String password;
    private String address;
    private String phone;

    // 요구사항: 계좌 목록을 리스트 구조로 관리 [cite: 121]
    private List<Account> accountList;

    public Customer(String id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.accountList = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public List<Account> getAccountList() { return accountList; }
    public void setAccountList(List<Account> accountList) { this.accountList = accountList; }

    // 계좌 추가 편의 메서드
    public void addAccount(Account account) {
        this.accountList.add(account);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", accountCount=" + accountList.size() +
                '}';
    }
}