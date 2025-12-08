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
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class ServerMain extends JFrame implements ActionListener, ClientHandler {
    private JLabel Label_UserCount;
    private JLabel Label_UserCount_2;
    private JToggleButton Btn_StartStop;
    private JButton Btn_Reset;
    private JTextArea TextArea_Log;
    private JScrollPane sp;

    private ServerSocket serverSocket;
    // 변경: CustomerVO -> Customer
    private List<Customer> customerList;
    private List<Client> clientList = new Vector<>();
    private boolean isRunning;

    public ServerMain() {
        InitGui();
        customerList = ReadCustomerFile("./Account.txt");
        printCustomerList(); // 시작 시 콘솔에 정보 출력 (확인용)
        printAccountList();
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                SaveCustomerFile(customerList, "./Account.txt");
            }
        });
    }

    // 변경: Customer 리스트 구조에 맞게 출력 로직 수정
    public void printAccountList() {
        addMsg("=== 전체 계좌 목록 출력 ===");
        if (customerList == null || customerList.isEmpty()) {
            addMsg("등록된 계좌가 없습니다.");
            return;
        }

        for (Customer customer : customerList) {
            for (Account account : customer.getAccountList()) {
                addMsg(account.display());
            }
        }
        addMsg("=========================");
    }

    public void printCustomerList() {
        addMsg("=== 전체 고객 목록 출력 ===");
        if (customerList == null || customerList.isEmpty()) {
            addMsg("등록된 고객이 없습니다.");
            return;
        }

        for (Customer customer : customerList) {
            String info = String.format("ID: %s, 이름: %s, 계좌수: %d",
                    customer.getId(), customer.getName(), customer.getAccountList().size());
            addMsg(info);
        }
        addMsg("=========================");
    }

    // 변경: 초기 데이터 생성 시 Checking/Savings 및 연결 계좌 설정
    private static List<Customer> GetDefaultCustomers() {
        List<Customer> customerList = new ArrayList<>();

        // 1. 광수: 당좌(Checking) + 저축(Savings) 보유, 서로 연결됨
        Customer c1 = new Customer("202400001", "광수", "202400001");
        SavingsAccount s1 = new SavingsAccount("광수", "111-1111", 5_000_000, Date.valueOf(LocalDate.now()), 0.02); // 이자율 2%
        CheckingAccount k1 = new CheckingAccount("광수", "111-2222", 100_000, Date.valueOf(LocalDate.now()), s1); // 잔액 10만원, s1과 연결
        c1.addAccount(s1);
        c1.addAccount(k1);
        customerList.add(c1);

        // 2. 영철: 당좌만 보유 (연결 계좌 없음 -> 잔액 부족 시 에러 나야 함)
        Customer c2 = new Customer("202400002", "영철", "202400002");
        CheckingAccount k2 = new CheckingAccount("영철", "222-2222", 1_000_000, Date.valueOf(LocalDate.now()), null);
        c2.addAccount(k2);
        customerList.add(c2);

        // 3. 영숙: 저축만 보유
        Customer c3 = new Customer("202400003", "영숙", "202400003");
        SavingsAccount s3 = new SavingsAccount("영숙", "333-3333", 10_000_000, Date.valueOf(LocalDate.now()), 0.05);
        c3.addAccount(s3);
        customerList.add(c3);

        return customerList;
    }

    public void SaveCustomerFile(List<Customer> customers, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(customers);
            System.out.println("Objects saved to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Customer> ReadCustomerFile(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            List<Customer> customers = (List<Customer>) ois.readObject();
            System.out.println("Objects read from " + filePath);
            return customers;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("File not found or incompatible. Initializing with default data.");
            List<Customer> defaultCustomers = GetDefaultCustomers();
            SaveCustomerFile(defaultCustomers, filePath);
            return defaultCustomers;
        }
    }

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
                    SwingUtilities.invokeLater(() -> Label_UserCount_2.setText(String.valueOf(clientList.size())));
                }
            } catch (IOException e) {
                if(isRunning) e.printStackTrace();
                stopServer();
            }
        }).start();
    }

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

    @Override
    public void removeClient(Client client) {
        clientList.remove(client);
        addMsg("클라이언트 연결 해제됨");
        SwingUtilities.invokeLater(() -> Label_UserCount_2.setText(String.valueOf(clientList.size())));
    }

    @Override
    public void displayInfo(String msg) {
        addMsg(msg);
    }

    public void addMsg(String data) {
        TextArea_Log.append(data + "\n");
        TextArea_Log.setCaretPosition(TextArea_Log.getDocument().getLength());
    }

    public static void main(String[] args) {
        new ServerMain();
    }
}