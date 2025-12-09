package bank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

//*******************************************************************
// Name : CustomerVO
// Type : Class
// Description :  고객정보를 정의 하기 위해 필요한 VO(ValueObject)이다.
//                생성자와, 오브젝트 내부 데이터 get, set 동작이 구현되어 있다.
//                오브젝트 형태로 txt에 저장할수 있도록 implements Serializable를 통해
//                직렬화 되어있다.
//*******************************************************************
public class CustomerVO implements Serializable {
    private String id;
    private String name;
    private String password;
    private String address;
    private String phone;

    private List<Account> accountList;

    public CustomerVO() {
        this.accountList = new ArrayList<>();
    }

    public CustomerVO(String id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.accountList = new ArrayList<>(); //생성 시 리스트 초기화
    }

    public CustomerVO(String id, String name, String password, Account account) {
        this(id, name, password);
        this.accountList.add(account);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<Account> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<Account> accountList) {
        this.accountList = accountList;
    }

    // [추가됨] 새로운 계좌 추가 편의 메서드
    public void addAccount(Account account) {
        if (this.accountList == null) {
            this.accountList = new ArrayList<>();
        }
        this.accountList.add(account);
    }

    // [추가됨] 특정 계좌번호를 가진 계좌 찾기
    public Account findAccount(String accountNo) {
        for (Account acc : accountList) {
            if (acc.getAccountNo().equals(accountNo)) {
                return acc;
            }
        }
        return null;
    }

    // [추가됨] 특정 계좌 삭제
    public boolean removeAccount(String accountNo) {
        Account target = findAccount(accountNo);
        if (target != null) {
            return accountList.remove(target);
        }
        return false;
    }

    @Override
    public String toString() {
        return "CustomerVO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", password='" + password + '\'' +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", accountList=" + accountList +
                '}';
    }
}
