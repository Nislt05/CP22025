package bank;

import common.AccountType;
import common.CommandDTO;
import common.ResponseType;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

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
                                // (추가됨) 관리자 기능 처리
                                case MANAGER_LOGIN -> managerLogin(command);
                                case MANAGE_GET_CUSTOMERS -> getCustomerList(command);
                                case MANAGE_ADD_CUSTOMER -> addCustomer(command);
                                case MANAGE_UPDATE_CUSTOMER -> updateCustomer(command);
                                case MANAGE_DEL_CUSTOMER -> deleteCustomer(command);
                                case MANAGE_ADD_ACCOUNT -> addAccount(command);
                                case MANAGE_DEL_ACCOUNT -> deleteAccount(command);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("요청 처리 중 에러 발생: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("클라이언트 연결 종료됨.");
                disconnectClient();
            }
        }).start();
    }

    // ... (send, disconnectClient 메소드는 기존 유지)
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

    public void disconnectClient() {
        try {
            if (!clientSocket.isClosed()) clientSocket.close();
            handler.removeClient(this);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- 기존 ATM 기능 (login, view, transfer, deposit) 유지 ---
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

    // (view, transfer, deposit 메소드는 기존 코드 그대로 유지해주세요. 분량상 생략하지만 꼭 있어야 합니다!)
    private synchronized void view(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream()
                .filter(c -> Objects.equals(c.getId(), commandDTO.getId())).findFirst().orElse(null);
        if (user != null) {
            List<String> accListInfo = new ArrayList<>();
            List<Account> userAccounts = user.getAccountList();
            if (userAccounts != null) {
                for (Account acc : userAccounts) {
                    String typeStr = (acc instanceof CheckingAccount) ? "당좌" : "저축";
                    accListInfo.add(typeStr + "/" + acc.getAccountNo() + "/" + acc.getBalance());
                }
            }
            commandDTO.setAccountList(accListInfo);
            commandDTO.setResponseType(ResponseType.SUCCESS);
        } else {
            commandDTO.setResponseType(ResponseType.FAILURE);
        }
        send(commandDTO);
    }

    private synchronized void transfer(CommandDTO commandDTO) {
        CustomerVO sender = this.customerList.stream().filter(c -> Objects.equals(c.getId(), commandDTO.getId())).findFirst().orElse(null);
        if (sender == null || !sender.getPassword().equals(commandDTO.getPassword())) {
            commandDTO.setResponseType(ResponseType.WRONG_PASSWORD); send(commandDTO); return;
        }
        if (sender.getAccountList() == null || sender.getAccountList().isEmpty()) {
            commandDTO.setResponseType(ResponseType.FAILURE); send(commandDTO); return;
        }
        Account senderAccount = sender.getAccountList().get(0);
        Account receiverAccount = null;
        for (CustomerVO c : customerList) {
            Account acc = c.findAccount(commandDTO.getReceivedAccountNo());
            if (acc != null) { receiverAccount = acc; break; }
        }
        if (receiverAccount == null) {
            commandDTO.setResponseType(ResponseType.WRONG_ACCOUNT_NO); send(commandDTO); return;
        }

        try {
            if (senderAccount.withdraw(commandDTO.getAmount())) {
                receiverAccount.deposit(commandDTO.getAmount());
                commandDTO.setResponseType(ResponseType.SUCCESS);
                handler.displayInfo(sender.getName() + " -> " + receiverAccount.getOwner() + " 이체 완료");
                ((ServerMain)handler).saveAllData();
            } else {
                commandDTO.setResponseType(ResponseType.INSUFFICIENT);
            }
        } catch (RuntimeException e) {
            commandDTO.setResponseType(ResponseType.INSUFFICIENT);
            commandDTO.setErrorMessage(e.getMessage());
        }
        send(commandDTO);
    }

    private synchronized void deposit(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream().filter(c -> Objects.equals(c.getId(), commandDTO.getId())).findFirst().orElse(null);
        Account targetAccount = null;
        if (user != null) targetAccount = user.findAccount(commandDTO.getReceivedAccountNo());

        if (targetAccount != null) {
            targetAccount.deposit(commandDTO.getAmount());
            commandDTO.setResponseType(ResponseType.SUCCESS);
            handler.displayInfo(user.getName() + " 입금 완료");
            ((ServerMain)handler).saveAllData();
        } else {
            commandDTO.setResponseType(ResponseType.FAILURE);
        }
        send(commandDTO);
    }

    // [수정됨] 출금: 예외 메시지 처리 추가
    private synchronized void withdraw(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        if (user != null && user.findAccount(commandDTO.getReceivedAccountNo()) != null) {
            Account targetAccount = user.findAccount(commandDTO.getReceivedAccountNo());
            try {
                if (targetAccount.withdraw(commandDTO.getAmount())) {
                    commandDTO.setResponseType(ResponseType.SUCCESS);
                    String logMsg = user.getName() + " 출금 완료";
                    if (targetAccount instanceof CheckingAccount) {
                        long auto = ((CheckingAccount) targetAccount).getLastAutoTransferAmount();
                        if (auto > 0) logMsg += " (자동이체 " + auto + "원 포함)";
                    }
                    handler.displayInfo(logMsg);
                    ((ServerMain) handler).saveAllData();
                } else {
                    commandDTO.setResponseType(ResponseType.INSUFFICIENT);
                }
            } catch (RuntimeException e) {
                // (추가됨) CheckingAccount에서 던진 예외를 잡아서 클라이언트에 전달
                commandDTO.setResponseType(ResponseType.INSUFFICIENT);
                commandDTO.setErrorMessage(e.getMessage());
            }
        } else {
            commandDTO.setResponseType(ResponseType.FAILURE);
        }
        send(commandDTO);
    }

    // --- (추가됨) 관리자 기능 구현 ---

    private void managerLogin(CommandDTO dto) {
        ServerMain server = (ServerMain) handler;
        if (server.authenticateManager(dto.getId(), dto.getPassword())) {
            dto.setResponseType(ResponseType.SUCCESS);
            handler.displayInfo("관리자 접속: " + dto.getId());
        } else {
            dto.setResponseType(ResponseType.FAILURE);
        }
        send(dto);
    }

    private void getCustomerList(CommandDTO dto) {
        List<String> list = new ArrayList<>();
        for (CustomerVO c : customerList) {
            // ID, 이름, 전화번호, 주소, 비번 순으로 묶어서 보냄
            String info = c.getId() + "|" + c.getName() + "|" + c.getPhone() + "|" + c.getAddress() + "|" + c.getPassword();
            list.add(info);
        }
        dto.setAccountList(list); // 리스트 재활용
        dto.setResponseType(ResponseType.SUCCESS);
        send(dto);
    }

    private void addCustomer(CommandDTO dto) {
        ServerMain server = (ServerMain) handler;
        boolean res = server.addCustomer(dto.getId(), dto.getUserName(), dto.getPassword(), dto.getUserAddress(), dto.getUserPhone());
        dto.setResponseType(res ? ResponseType.SUCCESS : ResponseType.FAILURE);
        send(dto);
    }

    private void updateCustomer(CommandDTO dto) {
        // 고객 정보 수정 기능
        CustomerVO target = null;
        for(CustomerVO c : customerList) {
            if(c.getId().equals(dto.getId())) { target = c; break; }
        }
        if(target != null) {
            target.setName(dto.getUserName());
            target.setPhone(dto.getUserPhone());
            target.setAddress(dto.getUserAddress());
            target.setPassword(dto.getPassword());
            ((ServerMain)handler).saveAllData();
            dto.setResponseType(ResponseType.SUCCESS);
            handler.displayInfo("관리자: 고객 정보 수정 (" + dto.getId() + ")");
        } else {
            dto.setResponseType(ResponseType.FAILURE);
        }
        send(dto);
    }

    private void deleteCustomer(CommandDTO dto) {
        ServerMain server = (ServerMain) handler;
        boolean res = server.deleteCustomer(dto.getId());
        dto.setResponseType(res ? ResponseType.SUCCESS : ResponseType.FAILURE);
        send(dto);
    }

    private void addAccount(CommandDTO dto) {
        ServerMain server = (ServerMain) handler;
        boolean res = server.addAccount(dto.getId(), dto.getUserAccountNo(), dto.getAccountType(), dto.getAmount());
        dto.setResponseType(res ? ResponseType.SUCCESS : ResponseType.FAILURE);
        send(dto);
    }

    private void deleteAccount(CommandDTO dto) {
        ServerMain server = (ServerMain) handler;
        boolean res = server.deleteAccount(dto.getId(), dto.getUserAccountNo());
        dto.setResponseType(res ? ResponseType.SUCCESS : ResponseType.FAILURE);
        send(dto);
    }
}