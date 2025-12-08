package BankServer.src.bank;

import BankServer.src.common.CommandDTO;
import BankServer.src.common.RequestType;
import BankServer.src.common.ResponseType;
import BankServer.src.common.AccountType;

import java.io.*;
import java.net.Socket;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Client {
    private Socket clientSocket;
    private ClientHandler handler;
    private List<Customer> customerList;
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

                            // --- 관리자 기능 핸들러 추가 ---
                            case MANAGER_LOGIN -> managerLogin(command);
                            case ADD_CUSTOMER -> addCustomer(command);
                            // 간단한 구현을 위해 계좌 추가/삭제 등은 확장이 필요하지만
                            // 여기서는 핵심인 '고객 추가' 예시를 보여드립니다.
                            case ALL_CUSTOMERS -> sendAllCustomers(command);

                            default -> {}
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
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

    private Account findAccount(Customer customer, String accountNo) {
        return customer.getAccountList().stream()
                .filter(acc -> acc.getAccountNo().equals(accountNo))
                .findFirst()
                .orElse(null);
    }

    // --- 기존 메소드들 (login, view, transfer...)은 그대로 유지 ---
    // (위에서 작성했던 코드를 그대로 두시면 됩니다. 아래는 새로 추가된 메소드입니다.)

    private synchronized void managerLogin(CommandDTO command) {
        // 관리자 ID/PW 하드코딩 (실무에서는 DB 관리)
        if ("admin".equals(command.getId()) && "1234".equals(command.getPassword())) {
            command.setResponseType(ResponseType.SUCCESS);
            handler.displayInfo("관리자가 로그인했습니다.");
        } else {
            command.setResponseType(ResponseType.FAILURE);
        }
        send(command);
    }

    private synchronized void addCustomer(CommandDTO command) {
        // 중복 ID 체크
        boolean exists = customerList.stream().anyMatch(c -> c.getId().equals(command.getId()));
        if (exists) {
            command.setResponseType(ResponseType.FAILURE); // 이미 존재함
        } else {
            Customer newCustomer = new Customer(command.getId(), command.getId(), command.getPassword()); // 이름=ID로 임시 설정
            newCustomer.setName("신규고객"); // 필요시 DTO에 Name 필드 추가하여 받기

            // 기본 계좌 하나 생성해주기 (Checking)
            CheckingAccount basicAccount = new CheckingAccount(
                    newCustomer.getName(),
                    command.getUserAccountNo(), // DTO의 계좌번호 필드 재사용
                    0,
                    Date.valueOf(LocalDate.now()),
                    null
            );
            newCustomer.addAccount(basicAccount);

            customerList.add(newCustomer);
            command.setResponseType(ResponseType.SUCCESS);
            handler.displayInfo("신규 고객 추가됨: " + command.getId());
        }
        send(command);
    }

    private synchronized void sendAllCustomers(CommandDTO command) {
        // 모든 고객 정보를 문자열로 만들어서 보냄 (DTO 구조 한계상 이름만 나열 등 간소화)
        StringBuilder sb = new StringBuilder();
        for(Customer c : customerList) {
            sb.append(c.getId()).append(" / ").append(c.getName()).append("\n");
        }
        // DTO에 데이터를 실어 보낼 필드가 마땅치 않으므로, 임시로 ID 필드 등에 넣거나
        // DTO를 수정해야 하지만, 여기서는 성공 여부만 보냄.
        // 실제로는 CommandDTO에 List<Customer> 필드가 있거나 String result 필드가 있어야 함.
        command.setResponseType(ResponseType.SUCCESS);
        send(command);
    }

    // login, view, transfer, deposit, withdraw 구현부는 이전 답변의 코드와 동일하게 유지해주세요.
    // 편의상 생략합니다. (이전 답변의 코드를 사용하세요)
    private synchronized void login(CommandDTO commandDTO) {
        Optional<Customer> customer = this.customerList.stream()
                .filter(c -> Objects.equals(c.getId(), commandDTO.getId())
                        && Objects.equals(c.getPassword(), commandDTO.getPassword()))
                .findFirst();

        if (customer.isPresent()) {
            commandDTO.setResponseType(ResponseType.SUCCESS);
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
            String targetAccountNo = commandDTO.getUserAccountNo();
            Account account = findAccount(customer, targetAccountNo);
            if (account == null && !customer.getAccountList().isEmpty()) {
                account = customer.getAccountList().get(0);
            }
            if (account != null) {
                commandDTO.setBalance(account.getBalance());
                commandDTO.setUserAccountNo(account.getAccountNo());
                handler.displayInfo(account.getOwner() + " 조회");
            }
        }
        send(commandDTO);
    }

    private synchronized void transfer(CommandDTO commandDTO) {
        Customer user = this.customerList.stream()
                .filter(c -> Objects.equals(c.getId(), commandDTO.getId()))
                .findFirst().orElse(null);
        Account receiverAccount = null;
        for (Customer c : this.customerList) {
            for (Account acc : c.getAccountList()) {
                if (acc.getAccountNo().equals(commandDTO.getReceivedAccountNo())) {
                    receiverAccount = acc; break;
                }
            }
            if (receiverAccount != null) break;
        }
        if (user == null) { send(commandDTO); return; }
        Account userAccount = findAccount(user, commandDTO.getUserAccountNo());
        if (userAccount == null && !user.getAccountList().isEmpty()) userAccount = user.getAccountList().get(0);

        if (receiverAccount == null) {
            commandDTO.setResponseType(ResponseType.WRONG_ACCOUNT_NO);
        } else if (!user.getPassword().equals(commandDTO.getPassword())) {
            commandDTO.setResponseType(ResponseType.WRONG_PASSWORD);
        } else {
            try {
                if (userAccount.withdraw(commandDTO.getAmount())) {
                    receiverAccount.deposit(commandDTO.getAmount());
                    commandDTO.setResponseType(ResponseType.SUCCESS);
                    handler.displayInfo("이체 성공");
                }
            } catch (Exception e) {
                commandDTO.setResponseType(ResponseType.INSUFFICIENT);
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
            if (account == null && !user.getAccountList().isEmpty()) account = user.getAccountList().get(0);
            if (account != null) {
                account.deposit(commandDTO.getAmount());
                commandDTO.setResponseType(ResponseType.SUCCESS);
                handler.displayInfo("입금 성공");
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
                    if (account.withdraw(commandDTO.getAmount())) {
                        commandDTO.setResponseType(ResponseType.SUCCESS);
                        handler.displayInfo("출금 성공");
                    }
                } catch (Exception e) {
                    commandDTO.setResponseType(ResponseType.INSUFFICIENT);
                }
            }
        }
        send(commandDTO);
    }
}