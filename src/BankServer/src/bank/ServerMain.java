package BankServer.src.bank;

import BankServer.src.common.AccountType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Vector;

//*******************************************************************
// Name : ServerMain
// Type : Class
// Description :  BankServer의 GUI 프레임이며, ATM과의 소켓통신을 담당한다.
//                계좌 정보들을 보유하고 있으며, 관련 기능들을 가지고 있다.
//*******************************************************************

class ServerMain extends JFrame implements ActionListener, ClientHandler {
    private JLabel Label_UserCount;
    private JLabel Label_UserCount_2;
    private JToggleButton Btn_StartStop;
    private JButton Btn_Reset;
    private JTextArea TextArea_Log;
    private JScrollPane sp;

    private ServerSocket serverSocket;
    private List<CustomerVO> customerList;
    private List<Client> clientList = new Vector<>();
    private boolean isRunning;

    //*******************************************************************
    // Name : ServerMain()
    // Type : 생성자
    // Description :  ServerMain Class의 생성자로서 계좌 정보를 Load 하고, GUI를 초기화 한다.
    //                계좌 정보는 ./Account.txt에 저장하며 Server 실행시 Load, 종료시 Save 동작을 한다
    //*******************************************************************
    public ServerMain() {
        InitGui();
        customerList = ReadCustomerFile("./Account.txt");
        setVisible(true);

        // WindowListener 추가
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 프레임이 종료될 때 SaveCustomerFile 메서드 호출
                SaveCustomerFile(customerList, "./Account.txt");
            }
        });
    }

    //*******************************************************************
    // Name : printAccountList()
    // Type : Method
    // Description :  모든 고객의 계좌 정보 출력
    //*******************************************************************
    public void printAccountList() {
        addMsg("=== 전체 계좌 목록 출력 ===");
        if (customerList == null || customerList.isEmpty()) {
            addMsg("등록된 계좌가 없습니다.");
            return;
        }

        for (CustomerVO customer : customerList) {
            AccountVO account = customer.getAccount();
            if (account != null) {
                String info = String.format("계좌번호: %s, 예금주: %s, 잔액: %d원, 개설일: %s",
                        account.getAccountNo(), account.getOwner(), account.getBalance(), account.getOpenDate());
                addMsg(info);
            } else {
                addMsg("고객(" + customer.getName() + ")의 계좌 정보가 없습니다.");
            }
            addMsg("=========================");
        }
    }

    //*******************************************************************
    // Name : printCustomerList()
    // Type : Method
    // Description :  모든 고객 정보 출력
    //*******************************************************************
    public void printCustomerList() {
        addMsg("=== 전체 고객 목록 출력 ===");
        if (customerList == null || customerList.isEmpty()) {
            addMsg("등록된 고객이 없습니다.");
            return;
        }

        for (CustomerVO customer : customerList) {
            String info = String.format("ID: %s, 이름: %s, 전화번호: %s",
                    customer.getId(), customer.getName(), customer.getPhone());
            addMsg(info);
        }
        addMsg("=========================");
    }

    //*******************************************************************
    // Name : GetDefaultCustomers()
    // Type : Method
    // Description :  Server 시작 시 저장된 계좌 정보가 없으면 Default 계좌를 생성하는 기능
    //*******************************************************************
    private static List<CustomerVO> GetDefaultCustomers() {
        List<CustomerVO> customerList = new Vector<>();
        customerList.add(new CustomerVO("202400001", "광수", "202400001",
                new AccountVO("광수", "202400001", AccountType.CHECKING, 100_000_000, Date.valueOf(LocalDate.now()))));
        customerList.add(new CustomerVO("202400002", "영철", "202400002",
                new AccountVO("영철", "202400002", AccountType.CHECKING, 10_000_000, Date.valueOf(LocalDate.now()))));
        customerList.add(new CustomerVO("202400003", "영숙", "202400003",
                new AccountVO("영숙", "202400003", AccountType.CHECKING, 5_000_000, Date.valueOf(LocalDate.now()))));
        customerList.add(new CustomerVO("202400004", "옥순", "202400004",
                new AccountVO("옥순", "202400004", AccountType.CHECKING, 1_000_000, Date.valueOf(LocalDate.now()))));
        return customerList;
    }

    //*******************************************************************
    // Name : SaveCustomerFile()
    // Type : Method
    // Description :  현재까지의 계좌 정보를 txt 파일로 저장하는 기능
    //*******************************************************************
    public void SaveCustomerFile(List<CustomerVO> customers, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(customers);
            System.out.println("Objects saved to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //*******************************************************************
    // Name : SaveCustomerFile()
    // Type : Method
    // Description :  txt 파일로 저장된 계좌 정보를 Load 하는 기능
    //*******************************************************************
    public List<CustomerVO> ReadCustomerFile(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            List<CustomerVO> customers = (List<CustomerVO>) ois.readObject();
            System.out.println("Objects read from " + filePath);
            return customers;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("File not found. Initializing with default data.");
            List<CustomerVO> defaultCustomers = GetDefaultCustomers();
            SaveCustomerFile(defaultCustomers, filePath);
            return defaultCustomers;
        }
    }

    //*******************************************************************
    // Name : InitGui
    // Type : Method
    // Description :  ServerMain Class의 GUI 컴포넌트를 할당하고 초기화 한다.
    //                ServerMain Frame은 서버 시작 버튼 및 텍스트 창 초기화 버튼을 가지고 있다
    //*******************************************************************
    private void InitGui() {
        setTitle("서버 GUI");
        setSize(480, 320);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        Label_UserCount = new JLabel("현재 유저 수: ");
        topPanel.add(Label_UserCount);

        Label_UserCount_2 = new JLabel("0");
        topPanel.add(Label_UserCount_2);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        TextArea_Log = new JTextArea();
        TextArea_Log.setEditable(false);
        sp = new JScrollPane(TextArea_Log);
        mainPanel.add(sp, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        Btn_StartStop = new JToggleButton("시작");
        Btn_StartStop.addActionListener(this);
        bottomPanel.add(Btn_StartStop);

        Btn_Reset = new JButton("텍스트 창 초기화");
        Btn_Reset.addActionListener(this);
        bottomPanel.add(Btn_Reset);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    //*******************************************************************
    // Name : actionPerformed
    // Type : Listener
    // Description :  ServerMain Frame의 버튼 컴포넌트들의 동작을 구현한 부분
    //                아래 코드에서는 서버 Start/Stop 토글 버튼 기능 및 텍스트창 초기화 버튼기능이 구현 되어 있다.
    //*******************************************************************
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == Btn_StartStop) {
            if (Btn_StartStop.isSelected()) {
                startServer();
            } else {
                stopServer();
            }
        } else if (e.getSource() == Btn_Reset) {
            TextArea_Log.setText(null);
        }
    }

    //*******************************************************************
    // Name : startServer
    // Type : Method
    // Description :  서버 소켓을 port 5001 로 bind 하여 open 하는 기능 및
    //                클라이언트 소켓의 접속 시도시 accept 하여 연결 시키는 기능이 구현 되어 있다.
    //*******************************************************************
    public void startServer() {
        isRunning = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(5002);
                SwingUtilities.invokeLater(() -> {
                    addMsg("서버 시작");
                    Btn_StartStop.setText("정지");
                });

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    addMsg("클라이언트 접속: " + clientSocket.getInetAddress());
                    Client client = new Client(clientSocket, ServerMain.this, customerList);
                    clientList.add(client);

                    // Update the user count
                    SwingUtilities.invokeLater(() -> Label_UserCount_2.setText(String.valueOf(clientList.size())));
                }
            } catch (IOException e) {
                e.printStackTrace();
                stopServer();
            }
        }).start();
    }

    //*******************************************************************
    // Name : stopServer
    // Type : Method
    // Description :  서버 소켓을 연결 해제 하는 기능
    //*******************************************************************
    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            clientList.clear();
            SwingUtilities.invokeLater(() -> {
                addMsg("서버 정지");
                Btn_StartStop.setText("시작");
                Label_UserCount_2.setText("0");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //*******************************************************************
    // Name : removeClient()
    // Type : Method
    // Description :  클라이언트 소켓이 해제 되었을 때
    //                ServerMain 의 clientList 리스트 에서 해당 인덱스를 제거하는 기능
    //*******************************************************************
    @Override
    public void removeClient(Client client) {
        clientList.remove(client);
        addMsg(client + " 제거됨");

        // Update the user count
        SwingUtilities.invokeLater(() -> Label_UserCount_2.setText(String.valueOf(clientList.size())));
    }

    @Override
    public void displayInfo(String msg) {
        addMsg(msg);
    }

    public void addMsg(String data) {
        TextArea_Log.append(data + "\n");
    }

    public static void main(String[] args) throws Exception {
        ServerMain f = new ServerMain();
    }
}
