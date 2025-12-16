package bank;

import common.CommandDTO;
import common.ResponseType;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList; // [추가] ArrayList 임포트
import java.util.List;
import java.util.Objects;

public class Client {
    private Socket clientSocket;
    private ClientHandler handler;
    private List<CustomerVO> customerList;
    private InputStream inputStream;
    private OutputStream outputStream;

    public Client(Socket clientSocket, ClientHandler handler, List<CustomerVO> customerList) {
        this.clientSocket = clientSocket;
        this.handler = handler;
        this.customerList = customerList;

        try {
            outputStream = clientSocket.getOutputStream();
            inputStream = clientSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        receive();
    }

    // getTargetAccount 메소드는 더 이상 view에서 쓰이지 않지만, 입출금/이체 등 다른 곳에서 쓰일 수 있으므로 유지

    private void receive() {
        new Thread(() -> {
            try {
                while (!clientSocket.isClosed()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        disconnectClient();
                        break;
                    }
                    try {
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, 0, bytesRead);
                        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                        CommandDTO command = (CommandDTO) objectInputStream.readObject();

                        if (command != null) {
                            switch (command.getRequestType()) {
                                case VIEW -> view(command);
                                case LOGIN -> login(command);
                                case TRANSFER -> transfer(command);
                                case DEPOSIT -> deposit(command);
                                case WITHDRAW -> withdraw(command);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("요청 처리 중 에러 발생:");
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.out.println("클라이언트 연결 종료됨.");
                disconnectClient();
            }
        }).start();
    }

    private void send(CommandDTO commandDTO) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(commandDTO);
            objectOutputStream.flush();

            outputStream.write(byteArrayOutputStream.toByteArray());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            disconnectClient();
        }
    }

    private void disconnectClient() {
        try {
            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }
            handler.removeClient(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void login(CommandDTO commandDTO) {
        ServerMain server = (ServerMain) handler;
        CustomerVO customer = server.authenticateUser(commandDTO.getId(), commandDTO.getPassword());

        if (customer != null) {
            commandDTO.setResponseType(ResponseType.SUCCESS);
            handler.displayInfo(customer.getName() + "님이 로그인하였습니다.");
        } else {
            commandDTO.setResponseType(ResponseType.FAILURE);
        }
        send(commandDTO);
    }

    // [수정됨] 계좌 조회 기능 리팩토링: 전체 계좌 목록 반환
    private synchronized void view(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        if (user != null) {
            // 1. 리스트 초기화
            List<String> accListInfo = new ArrayList<>();
            List<Account> userAccounts = user.getAccountList();

            // 2. 계좌 순회 및 포맷팅
            if (userAccounts != null) {
                for (Account acc : userAccounts) {
                    String typeStr = "알수없음";
                    // 계좌 타입 확인 (instanceof 사용)
                    if (acc instanceof CheckingAccount) {
                        typeStr = "당좌";
                    } else if (acc instanceof SavingsAccount) {
                        typeStr = "저축";
                    }

                    // 포맷: 종류/번호/잔액
                    String info = typeStr + "/" + acc.getAccountNo() + "/" + acc.getBalance();
                    accListInfo.add(info);
                }
            }

            // 3. DTO에 담고 성공 응답 설정
            commandDTO.setAccountList(accListInfo);
            commandDTO.setResponseType(ResponseType.SUCCESS);
            handler.displayInfo(user.getName() + "님의 전체 계좌 목록 조회 완료.");

        } else {
            commandDTO.setResponseType(ResponseType.FAILURE);
        }
        send(commandDTO);
    }

    private synchronized void transfer(CommandDTO commandDTO) {
        // 1. 보내는 사람 찾기
        CustomerVO sender = this.customerList.stream()
                .filter(c -> Objects.equals(c.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        if (sender == null) {
            commandDTO.setResponseType(ResponseType.FAILURE);
            send(commandDTO);
            return;
        }

        // 2. 보내는 사람 비밀번호 인증
        if (!sender.getPassword().equals(commandDTO.getPassword())) {
            commandDTO.setResponseType(ResponseType.WRONG_PASSWORD);
            send(commandDTO);
            return;
        }

        // 3. 보내는 사람 계좌 찾기 (명시적으로 첫 번째 계좌 사용)
        if (sender.getAccountList() == null || sender.getAccountList().isEmpty()) {
            commandDTO.setResponseType(ResponseType.FAILURE); // 이체할 계좌가 없음
            send(commandDTO);
            return;
        }
        Account senderAccount = sender.getAccountList().get(0);

        // 4. 받는 사람 계좌 찾기
        CustomerVO receiver = null;
        Account receiverAccount = null;
        for (CustomerVO c : customerList) {
            Account acc = c.findAccount(commandDTO.getReceivedAccountNo());
            if (acc != null) {
                receiver = c;
                receiverAccount = acc;
                break;
            }
        }

        if (receiverAccount == null) {
            commandDTO.setResponseType(ResponseType.WRONG_ACCOUNT_NO);
            send(commandDTO);
            return;
        }

        // 5. 이체 실행
        if (senderAccount.withdraw(commandDTO.getAmount())) {
            receiverAccount.deposit(commandDTO.getAmount());
            commandDTO.setResponseType(ResponseType.SUCCESS);
            handler.displayInfo(sender.getName() + "님이 " + receiver.getName() + "님에게 " + commandDTO.getAmount() + "원 이체 완료.");
        } else {
            commandDTO.setResponseType(ResponseType.INSUFFICIENT);
        }
        send(commandDTO);
    }

    private synchronized void deposit(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        if (user != null && user.getAccountList() != null && !user.getAccountList().isEmpty()) {
            // 클라이언트가 선택한 계좌번호로 입금 대상 찾기
            String targetAccountNo = commandDTO.getReceivedAccountNo();
            Account targetAccount = user.findAccount(targetAccountNo);

            if (targetAccount != null) {
                targetAccount.deposit(commandDTO.getAmount());
                commandDTO.setResponseType(ResponseType.SUCCESS);
                handler.displayInfo(user.getName() + "님 계좌(" + targetAccount.getAccountNo() + ")에 " + commandDTO.getAmount() + "원 입금 완료.");
            } else {
                commandDTO.setResponseType(ResponseType.FAILURE); // 해당 계좌번호를 찾을 수 없음
            }
        } else {
            commandDTO.setResponseType(ResponseType.FAILURE); // 사용자 또는 계좌가 없으면 실패 응답 전송
        }
        send(commandDTO);
    }

    // [수정됨] 출금 로직 개선: 당좌계좌 자동이체 발생 시 상세 메시지 출력
    private synchronized void withdraw(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        if (user != null && user.getAccountList() != null && !user.getAccountList().isEmpty()) {
            // 클라이언트가 선택한 계좌번호로 출금 대상 찾기
            String targetAccountNo = commandDTO.getReceivedAccountNo();
            Account targetAccount = user.findAccount(targetAccountNo);

            if (targetAccount != null) {
                // 출금 실행
                if (targetAccount.withdraw(commandDTO.getAmount())) {
                    commandDTO.setResponseType(ResponseType.SUCCESS);

                    // 기본 메시지 생성
                    String logMsg = user.getName() + "님 계좌(" + targetAccount.getAccountNo() + ")에서 " + commandDTO.getAmount() + "원 출금 완료.";

                    // (추가됨) 만약 당좌계좌(CheckingAccount)라면, 자동이체 발생 여부를 확인하여 메시지에 추가
                    if (targetAccount instanceof CheckingAccount) {
                        CheckingAccount ca = (CheckingAccount) targetAccount;
                        long autoTransferred = ca.getLastAutoTransferAmount();

                        if (autoTransferred > 0) {
                            logMsg += " (잔액 부족으로 저축계좌에서 " + autoTransferred + "원 자동이체됨)";
                        }
                    }

                    handler.displayInfo(logMsg);
                } else {
                    commandDTO.setResponseType(ResponseType.INSUFFICIENT); // 잔액 부족 (자동이체 실패 포함)
                }
            } else {
                commandDTO.setResponseType(ResponseType.FAILURE); // 해당 계좌번호 찾을 수 없음
            }
        } else {
            commandDTO.setResponseType(ResponseType.FAILURE); // 사용자 또는 계좌가 없으면 실패 응답 전송
        }
        send(commandDTO);
    }
}