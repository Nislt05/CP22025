package common;

import java.io.Serializable;
import java.util.List;

//*******************************************************************
// Name : CommandDTO
// Type : Class
// Description :  ATM 과 Sever 사이의 통신 프로토콜을 정의 하기 위해 필요한 DTO
//*******************************************************************
public class CommandDTO implements Serializable {
    private RequestType requestType;
    private String id;
    private String password;
    private String userAccountNo;
    private String receivedAccountNo;
    private long amount;
    private long balance;
    private ResponseType responseType;

    // [추가] 여러 계좌 정보를 담기 위한 리스트 필드
    private List<String> accountList;

    // [추가] 관리자 모드 및 고객 정보 전달을 위한 필드
    private String userName;
    private String userPhone;
    private String userAddress;
    private AccountType accountType; // 계좌 생성 시 필요
    private String errorMessage;     // 에러 메시지 전달용

    public CommandDTO() {
    }

    public CommandDTO(RequestType requestType) {
        this.requestType = requestType;
    }

    // (기존 생성자들 유지...)
    public CommandDTO(ResponseType responseType) {
        this.responseType = responseType;
    }

    public CommandDTO(RequestType requestType, String userAccountNo) {
        this.requestType = requestType;
        this.userAccountNo = userAccountNo;
    }

    public CommandDTO(RequestType requestType, String userAccountNo, long amount) {
        this.requestType = requestType;
        this.userAccountNo = userAccountNo;
        this.amount = amount;
    }

    public CommandDTO(RequestType requestType, String id, String password) {
        this.requestType = requestType;
        this.id = id;
        this.password = password;
    }

    public CommandDTO(RequestType requestType, String password, String userAccountNo, String receivedAccountNo, long amount) {
        this.requestType = requestType;
        this.password = password;
        this.userAccountNo = userAccountNo;
        this.receivedAccountNo = receivedAccountNo;
        this.amount = amount;
    }

    public CommandDTO(RequestType requestType, String userAccountNo, String receivedAccountNo, long amount, long balance) {
        this.requestType = requestType;
        this.userAccountNo = userAccountNo;
        this.receivedAccountNo = receivedAccountNo;
        this.amount = amount;
        this.balance = balance;
    }

    // --- Getters and Setters ---

    public RequestType getRequestType() { return requestType; }
    public void setRequestType(RequestType requestType) { this.requestType = requestType; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUserAccountNo() { return userAccountNo; }
    public void setUserAccountNo(String userAccountNo) { this.userAccountNo = userAccountNo; }

    public String getReceivedAccountNo() { return receivedAccountNo; }
    public void setReceivedAccountNo(String receivedAccountNo) { this.receivedAccountNo = receivedAccountNo; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }

    public ResponseType getResponseType() { return responseType; }
    public void setResponseType(ResponseType responseType) { this.responseType = responseType; }

    public List<String> getAccountList() { return accountList; }
    public void setAccountList(List<String> accountList) { this.accountList = accountList; }

    // [추가된 Getter/Setter]
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getUserAddress() { return userAddress; }
    public void setUserAddress(String userAddress) { this.userAddress = userAddress; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}