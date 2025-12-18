package common;

//*******************************************************************
// Name : RequestType
// Type : Enum
// Description :  ATM 이 Server에 요청할 기능을 Enum으로 나타낸 열거형 데이터를 구현
//*******************************************************************
public enum RequestType {
    VIEW("계좌조회", 10),
    TRANSFER("계좌이체", 20),
    DEPOSIT("입금", 30),
    WITHDRAW("출금", 40),
    LOGIN("로그인", 50),
    BANK_INFO("은행 정보", 99),

    // (추가됨) 관리자 모드 관련 요청
    MANAGER_LOGIN("관리자로그인", 100),
    MANAGE_GET_CUSTOMERS("고객목록조회", 101),
    MANAGE_ADD_CUSTOMER("고객추가", 102),
    MANAGE_UPDATE_CUSTOMER("고객수정", 103),
    MANAGE_DEL_CUSTOMER("고객삭제", 104),
    MANAGE_ADD_ACCOUNT("계좌추가", 105),
    MANAGE_DEL_ACCOUNT("계좌삭제", 106);

    private String name;
    private int number;

    RequestType(String name, int number) {
        this.name = name;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}