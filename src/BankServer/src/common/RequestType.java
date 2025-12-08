package BankServer.src.common; // BankATM 쪽은 패키지명이 다를 수 있으니 주의하세요

public enum RequestType {
    VIEW("계좌조회", 10),
    TRANSFER("계좌이체", 20),
    DEPOSIT("입금", 30),
    WITHDRAW("출금", 40),
    LOGIN("로그인", 50),

    // --- 관리자 기능 추가 ---
    MANAGER_LOGIN("관리자 로그인", 60),
    ADD_CUSTOMER("고객 추가", 70),
    ADD_ACCOUNT("계좌 추가", 71),
    DELETE_CUSTOMER("고객 삭제", 72),
    DELETE_ACCOUNT("계좌 삭제", 73),
    ALL_CUSTOMERS("전체 고객 조회", 80),
    ALL_ACCOUNTS("전체 계좌 조회", 81),

    BANK_INFO("은행 정보", 99);

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