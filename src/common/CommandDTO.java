package common;

import java.io.Serializable;


//*******************************************************************
// Name : CommandDTO
// Type : Class
// Description :  ATM 과 Sever 사이의 통신 프로토콜을 정의 하기 위해 필요한 DTO(DataTransferObject)이다.
//                생성자와, 오브젝트 내부 데이터 get, set 동작이 구현되어 있다.
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

    public CommandDTO() {
    }

    public CommandDTO(RequestType requestType) {
        this.requestType = requestType;
    }

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

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserAccountNo() {
        return userAccountNo;
    }

    public void setUserAccountNo(String userAccountNo) {
        this.userAccountNo = userAccountNo;
    }

    public String getReceivedAccountNo() {
        return receivedAccountNo;
    }

    public void setReceivedAccountNo(String receivedAccountNo) {
        this.receivedAccountNo = receivedAccountNo;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    @Override
    public String toString() {
        return "CommandDTO{" +
                "requestType=" + requestType +
                ", id='" + id + '\'' +
                ", password='" + password + '\'' +
                ", userAccountNo='" + userAccountNo + '\'' +
                ", receivedAccountNo='" + receivedAccountNo + '\'' +
                ", amount=" + amount +
                ", balance=" + balance +
                ", responseType=" + responseType +
                '}';
    }
}
