package common;



//*******************************************************************
// Name : ResponseType
// Type : Enum
// Description :  ATM의 요청에 Server가 응답할 때의 겨로가 값을 Enum으로 나타낸 열거형 데이터를 구현
//                생성자와, 오브젝트 내부 데이터 get, set 동작이 구현되어 있다.
//*******************************************************************
public enum ResponseType {
    SUCCESS("성공", 200),
    INSUFFICIENT("잔액부족", 400),
    WRONG_PASSWORD("비밀번호오류", 401),
    WRONG_ACCOUNT_NO("계좌번호오류", 402),
    FAILURE("로그인실패", 404);

    private String name;
    private int number;

    ResponseType(String name, int number) {
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
