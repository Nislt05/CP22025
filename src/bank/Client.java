package bank;

import common.CommandDTO;
import common.ResponseType;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    private Account getTargetAccount(CustomerVO user, String requestedAccountNo) {
        if (user.getAccountList() == null || user.getAccountList().isEmpty()) {
            return null;
        }
        if (requestedAccountNo != null && !requestedAccountNo.isEmpty()) {
            Account target = user.findAccount(requestedAccountNo);
            if (target != null) return target;
        }
        return user.getAccountList().get(0);
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
                            // [디버깅] 어떤 요청이 왔는지 서버 로그에 출력
                            System.out.println("요청 수신: " + command.getRequestType());

                            switch (command.getRequestType()) {
                                case VIEW -> view(command);
                                case LOGIN -> login(command);
                                case TRANSFER -> transfer(command);
                                case DEPOSIT -> deposit(command);
                                case WITHDRAW -> withdraw(command);
                            }
                        }
                    } catch (Exception e) {
                        // [중요] 처리 중 에러가 나면 서버 로그에 출력
                        System.err.println("요청 처리 중 에러 발생:");
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                // 연결 끊김은 자연스러운 현상이므로 로그만 남김
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
            System.out.println("응답 전송 완료: " + commandDTO.getResponseType()); // [디버깅]
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
        // null 체크 추가 (안정성 강화)
        if (commandDTO.getId() == null || commandDTO.getPassword() == null) {
            commandDTO.setResponseType(ResponseType.FAILURE);
            send(commandDTO);
            return;
        }

        Optional<CustomerVO> customer = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getId(), commandDTO.getId())
                        && Objects.equals(customerVO.getPassword(), commandDTO.getPassword()))
                .findFirst();

        if (customer.isPresent()) {
            commandDTO.setResponseType(ResponseType.SUCCESS);
            handler.displayInfo(customer.get().getName() + "님이 로그인하였습니다.");
        } else {
            commandDTO.setResponseType(ResponseType.FAILURE);
            // 로그인 실패 시에도 서버 로그에는 남겨서 확인 가능하게 함
            System.out.println("로그인 실패 - ID: " + commandDTO.getId());
        }
        send(commandDTO);
    }

    // ... 나머지 메소드(view, transfer 등)는 기존과 동일 ...
    // (이전 답변의 코드 내용을 유지해 주세요. 지면 관계상 login과 receive 부분만 강조했습니다.)
    // 아래 코드는 이전과 동일하므로 그대로 두셔도 됩니다.

    private synchronized void view(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        if (user != null) {
            Account target = getTargetAccount(user, commandDTO.getUserAccountNo());
            if (target != null) {
                commandDTO.setBalance(target.getBalance());
                commandDTO.setUserAccountNo(target.getAccountNo());
                handler.displayInfo(user.getName() + "님의 계좌 잔액은 " + target.getBalance() + "원 입니다.");
            } else {
                commandDTO.setBalance(0);
            }
        }
        send(commandDTO);
    }

    private synchronized void transfer(CommandDTO commandDTO) {
        CustomerVO sender = this.customerList.stream()
                .filter(c -> Objects.equals(c.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

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

        if (sender != null) {
            Account senderAccount = getTargetAccount(sender, commandDTO.getUserAccountNo());

            if (receiverAccount == null || (receiver != null && receiverAccount.getAccountNo().equals(senderAccount.getAccountNo()))) {
                commandDTO.setResponseType(ResponseType.WRONG_ACCOUNT_NO);
            } else if (!sender.getPassword().equals(commandDTO.getPassword())) {
                commandDTO.setResponseType(ResponseType.WRONG_PASSWORD);
            } else {
                boolean success = senderAccount.withdraw(commandDTO.getAmount());
                if (success) {
                    receiverAccount.deposit(commandDTO.getAmount());
                    commandDTO.setResponseType(ResponseType.SUCCESS);
                    handler.displayInfo(senderAccount.getAccountNo() + " -> " + receiverAccount.getAccountNo() + " 이체: " + commandDTO.getAmount());
                } else {
                    commandDTO.setResponseType(ResponseType.INSUFFICIENT);
                }
            }
        } else {
            commandDTO.setResponseType(ResponseType.FAILURE);
        }
        send(commandDTO);
    }

    private synchronized void deposit(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        if (user != null) {
            Account target = getTargetAccount(user, commandDTO.getUserAccountNo());
            if (target != null) {
                target.deposit(commandDTO.getAmount());
                commandDTO.setResponseType(ResponseType.SUCCESS);
                handler.displayInfo(user.getName() + " 입금: " + commandDTO.getAmount());
            }
        }
        send(commandDTO);
    }

    private synchronized void withdraw(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getId(), commandDTO.getId()))
                .findFirst().orElse(null);

        if (user != null) {
            Account target = getTargetAccount(user, commandDTO.getUserAccountNo());
            if (target != null) {
                boolean success = target.withdraw(commandDTO.getAmount());
                if (success) {
                    commandDTO.setResponseType(ResponseType.SUCCESS);
                    handler.displayInfo(user.getName() + " 출금: " + commandDTO.getAmount());
                } else {
                    commandDTO.setResponseType(ResponseType.INSUFFICIENT);
                }
            }
        }
        send(commandDTO);
    }
}