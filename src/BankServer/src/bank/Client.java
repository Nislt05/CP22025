package BankServer.src.bank;

import BankServer.src.common.CommandDTO;
import BankServer.src.common.ResponseType;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Client {
    private Socket clientSocket;
    private ClientHandler handler;
    private List<Customer> customerList; // 변경: CustomerVO -> Customer
    private InputStream inputStream;
    private OutputStream outputStream;

    public Client(Socket clientSocket, ClientHandler handler, List<Customer> customerList) {
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
                            default -> {}
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                // e.printStackTrace();
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
            if (!clientSocket.isClosed()) clientSocket.close();
            handler.removeClient(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 헬퍼 메소드: 고객의 계좌 리스트에서 계좌번호로 계좌 찾기
    private Account findAccount(Customer customer, String accountNo) {
        return customer.getAccountList().stream()
                .filter(acc -> acc.getAccountNo().equals(accountNo))
                .findFirst()
                .orElse(null);
    }

    private synchronized void login(CommandDTO commandDTO) {
        Optional<Customer> customer = this.customerList.stream()
                .filter(c -> Objects.equals(c.getId(), commandDTO.getId())
                        && Objects.equals(c.getPassword(), commandDTO.getPassword()))
                .findFirst();

        if (customer.isPresent()) {
            commandDTO.setResponseType(ResponseType.SUCCESS);
            // 로그인 성공 시, 첫 번째 계좌 번호를 기본값으로 넣어줌 (ATM 편의를 위해)
            if (!customer.get().getAccountList().isEmpty()) {
                commandDTO.setUserAccountNo(customer.get().getAccountList().get(0).getAccountNo());
            }
            handler.displayInfo(customer.get().getName() + "님이 로그인하였습니다.");
        } else {
            commandDTO.setResponseType(ResponseType.FAILURE);
        }
        send(commandDTO);
    }

    private synchronized void view(CommandDTO commandDTO) {
        Customer customer = this.customerList.stream()
                .filter(c -> Objects.equals(c.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        if (customer != null) {
            // 요청된 계좌번호가 있으면 그 계좌를, 없으면 첫 번째 계좌를 조회
            String targetAccountNo = commandDTO.getUserAccountNo();
            Account account = findAccount(customer, targetAccountNo);

            if (account == null && !customer.getAccountList().isEmpty()) {
                account = customer.getAccountList().get(0);
            }

            if (account != null) {
                commandDTO.setBalance(account.getBalance());
                commandDTO.setUserAccountNo(account.getAccountNo());
                handler.displayInfo(account.getOwner() + "님의 계좌(" + account.getAccountNo() + ") 잔액 조회: " + account.getBalance());
            }
        }
        send(commandDTO);
    }

    private synchronized void transfer(CommandDTO commandDTO) {
        // 1. 보내는 사람 찾기
        Customer user = this.customerList.stream()
                .filter(c -> Objects.equals(c.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        // 2. 받는 사람 찾기 (전체 고객의 전체 계좌를 뒤져야 함)
        Account receiverAccount = null;
        Customer receiverCustomer = null;

        for (Customer c : this.customerList) {
            for (Account acc : c.getAccountList()) {
                if (acc.getAccountNo().equals(commandDTO.getReceivedAccountNo())) {
                    receiverAccount = acc;
                    receiverCustomer = c;
                    break;
                }
            }
            if (receiverAccount != null) break;
        }

        if (user == null) {
            commandDTO.setResponseType(ResponseType.FAILURE); // 유저 못찾음 (비정상)
            send(commandDTO);
            return;
        }

        // 보내는 계좌 찾기
        Account userAccount = findAccount(user, commandDTO.getUserAccountNo());
        if (userAccount == null && !user.getAccountList().isEmpty()) userAccount = user.getAccountList().get(0);

        if (receiverAccount == null) {
            commandDTO.setResponseType(ResponseType.WRONG_ACCOUNT_NO);
        } else if (!user.getPassword().equals(commandDTO.getPassword())) {
            commandDTO.setResponseType(ResponseType.WRONG_PASSWORD);
        } else {
            // 이체 시도 (출금 -> 입금)
            try {
                // 출금 (마이너스 통장 로직 포함됨)
                if (userAccount.withdraw(commandDTO.getAmount())) {
                    receiverAccount.deposit(commandDTO.getAmount());
                    commandDTO.setResponseType(ResponseType.SUCCESS);
                    handler.displayInfo(userAccount.getAccountNo() + " -> " + receiverAccount.getAccountNo() + " : " + commandDTO.getAmount() + "원 이체");
                }
            } catch (Exception e) {
                // 잔액 부족 등 예외 발생 시
                commandDTO.setResponseType(ResponseType.INSUFFICIENT);
                handler.displayInfo("이체 실패: " + e.getMessage());
            }
        }
        send(commandDTO);
    }

    private synchronized void deposit(CommandDTO commandDTO) {
        Customer user = this.customerList.stream()
                .filter(c -> Objects.equals(c.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        if (user != null) {
            Account account = findAccount(user, commandDTO.getUserAccountNo());
            // 계좌가 명시되지 않았으면 첫 번째 계좌에 입금
            if (account == null && !user.getAccountList().isEmpty()) account = user.getAccountList().get(0);

            if (account != null) {
                account.deposit(commandDTO.getAmount());
                commandDTO.setResponseType(ResponseType.SUCCESS);
                handler.displayInfo(user.getName() + " 입금: " + commandDTO.getAmount() + "원 (" + account.getAccountNo() + ")");
            } else {
                commandDTO.setResponseType(ResponseType.FAILURE);
            }
        }
        send(commandDTO);
    }

    private synchronized void withdraw(CommandDTO commandDTO) {
        Customer user = this.customerList.stream()
                .filter(c -> Objects.equals(c.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        if (user != null) {
            Account account = findAccount(user, commandDTO.getUserAccountNo());
            if (account == null && !user.getAccountList().isEmpty()) account = user.getAccountList().get(0);

            if (account != null) {
                try {
                    // 변경된 부분: account.withdraw()가 boolean을 리턴하거나 예외를 던짐
                    if (account.withdraw(commandDTO.getAmount())) {
                        commandDTO.setResponseType(ResponseType.SUCCESS);
                        handler.displayInfo(user.getName() + " 출금: " + commandDTO.getAmount() + "원 (" + account.getAccountNo() + ")");
                    }
                } catch (Exception e) {
                    // 잔액 부족 (CheckingAccount의 경우 연결 계좌까지 털었으나 부족한 경우)
                    commandDTO.setResponseType(ResponseType.INSUFFICIENT);
                    handler.displayInfo("출금 실패 (" + user.getName() + "): " + e.getMessage());
                }
            } else {
                commandDTO.setResponseType(ResponseType.FAILURE);
            }
        }
        send(commandDTO);
    }
}