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

    // 수신 작업: 서버에서 메시지를 받기 위한 메서드
    private void receive() {
        new Thread(() -> {
            try {
                while (!clientSocket.isClosed()) {
                    byte[] buffer = new byte[4096];  // 버퍼 크기를 지정
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        disconnectClient();
                        break;
                    }

                    // 받은 바이트 데이터를 객체로 역직렬화
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, 0, bytesRead);
                    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                    CommandDTO command = (CommandDTO) objectInputStream.readObject();

                    // 요청 타입에 따른 처리
                    if (command != null) {
                        switch (command.getRequestType()) {
                            case VIEW -> view(command);
                            case LOGIN -> login(command);
                            case TRANSFER -> transfer(command);
                            case DEPOSIT -> deposit(command);
                            case WITHDRAW -> withdraw(command);
                            default -> {
                                // 알 수 없는 요청 타입 처리
                            }
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                disconnectClient();
            }
        }).start();
    }

    // 서버에서 클라이언트로 CommandDTO를 전송하는 메서드 (바이트 배열 전송)
    private void send(CommandDTO commandDTO) {
        try {
            System.out.println("sending commandDTO: " + commandDTO);

            // CommandDTO 객체를 직렬화하여 바이트 배열로 변환
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(commandDTO);
            objectOutputStream.flush();

            System.out.println("will send length of " + byteArrayOutputStream.toByteArray().length + " bytes");

            // 직렬화된 바이트 데이터를 서버로 전송
            outputStream.write(byteArrayOutputStream.toByteArray());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            disconnectClient();
        }
    }

    // 클라이언트 연결 종료 처리
    private void disconnectClient() {
        try {
            clientSocket.close();  // 소켓 닫기
            handler.removeClient(this);  // 클라이언트 제거
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 로그인 처리
    private synchronized void login(CommandDTO commandDTO) {
        Optional<CustomerVO> customer = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getId(), commandDTO.getId())
                        && Objects.equals(customerVO.getPassword(), commandDTO.getPassword()))
                .findFirst();

        if (customer.isPresent()) {
            commandDTO.setResponseType(ResponseType.SUCCESS);
            handler.displayInfo(customer.get().getName() + "님이 로그인하였습니다.");
        } else {
            commandDTO.setResponseType(ResponseType.FAILURE);
        }
        send(commandDTO);
    }

    // 계좌 조회 처리
    private synchronized void view(CommandDTO commandDTO) {
        CustomerVO customer = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getId(), commandDTO.getId()))
                .findFirst().get();

        commandDTO.setBalance(customer.getAccount().getBalance());
        commandDTO.setUserAccountNo(customer.getAccount().getAccountNo());
        handler.displayInfo(customer.getAccount().getOwner() + "님의 계좌 잔액은 " + customer.getAccount().getBalance() + "원 입니다.");
        send(commandDTO);
    }

    // 계좌 이체 처리
    private synchronized void transfer(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getAccount().getAccountNo(), commandDTO.getUserAccountNo()))
                .findFirst().get();
        Optional<CustomerVO> receiverOptional = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getAccount().getAccountNo(), commandDTO.getReceivedAccountNo()))
                .findFirst();

        if (!receiverOptional.isPresent() || receiverOptional.get().getAccount().getAccountNo().equals(user.getAccount().getAccountNo())) {
            commandDTO.setResponseType(ResponseType.WRONG_ACCOUNT_NO);
        } else if (!user.getPassword().equals(commandDTO.getPassword())) {
            commandDTO.setResponseType(ResponseType.WRONG_PASSWORD);
        } else if (user.getAccount().getBalance() < commandDTO.getAmount()) {
            commandDTO.setResponseType(ResponseType.INSUFFICIENT);
        } else {
            CustomerVO receiver = receiverOptional.get();
            commandDTO.setResponseType(ResponseType.SUCCESS);
            user.getAccount().setBalance(user.getAccount().getBalance() - commandDTO.getAmount());
            receiver.getAccount().setBalance(receiver.getAccount().getBalance() + commandDTO.getAmount());
            handler.displayInfo(user.getAccount().getAccountNo() + " 계좌에서 " + receiver.getAccount().getAccountNo() + " 계좌로 " + commandDTO.getAmount() + "원 이체하였습니다.");
        }
        send(commandDTO);
    }

    // 입금 처리
    private synchronized void deposit(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getAccount().getAccountNo(), commandDTO.getUserAccountNo()))
                .findFirst().get();
        user.getAccount().setBalance(user.getAccount().getBalance() + commandDTO.getAmount());
        commandDTO.setResponseType(ResponseType.SUCCESS);
        handler.displayInfo(user.getName() + "님이 " + user.getAccount().getAccountNo() + " 계좌에 " + commandDTO.getAmount() + "원 입금하였습니다.");
        send(commandDTO);
    }

    // 출금 처리
    private synchronized void withdraw(CommandDTO commandDTO) {
        CustomerVO user = this.customerList.stream()
                .filter(customerVO -> Objects.equals(customerVO.getAccount().getAccountNo(), commandDTO.getUserAccountNo()))
                .findFirst().get();
        if (user.getAccount().getBalance() < commandDTO.getAmount()) {
            commandDTO.setResponseType(ResponseType.INSUFFICIENT);
        } else {
            commandDTO.setResponseType(ResponseType.SUCCESS);
            user.getAccount().setBalance(user.getAccount().getBalance() - commandDTO.getAmount());
            handler.displayInfo(user.getName() + "님이 " + user.getAccount().getAccountNo() + " 계좌에서 " + commandDTO.getAmount() + "원 출금하였습니다.");
        }
        send(commandDTO);
    }
}
