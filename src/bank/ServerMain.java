package bank;

import common.AccountType;
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
import java.util.ArrayList;

public class ServerMain extends JFrame implements ActionListener, ClientHandler {
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

    public ServerMain() {
        InitGui();
        customerList = ReadCustomerFile("./Account.txt");
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAllData();
            }
        });
    }

    // [수정됨] 초기 데이터 생성 메소드 (광수 + 영철, 영숙, 옥순 추가)
    private static List<CustomerVO> GetDefaultCustomers() {
        List<CustomerVO> list = new Vector<>();
        Date now = Date.valueOf(LocalDate.now());

        // 1. 광수 (자동이체 테스트용: 당좌+저축 연결)
        CheckingAccount chk1 = new CheckingAccount("광수", "202400001-1", 100_000, now);
        SavingsAccount sav1 = new SavingsAccount("광수", "202400001-2", 1_000_000, now, 2.0);
        chk1.setLinkedSavings(sav1); // 연결 설정

        CustomerVO u1 = new CustomerVO("202400001", "광수", "202400001");
        u1.addAccount(chk1);
        u1.addAccount(sav1);
        list.add(u1);

        // 2. 영철 (일반 당좌계좌: 1,000만원)
        CustomerVO u2 = new CustomerVO("202400002", "영철", "202400002");
        u2.addAccount(new CheckingAccount("영철", "202400002", 10_000_000, now));
        list.add(u2);

        // 3. 영숙 (일반 당좌계좌: 500만원)
        CustomerVO u3 = new CustomerVO("202400003", "영숙", "202400003");
        u3.addAccount(new CheckingAccount("영숙", "202400003", 5_000_000, now));
        list.add(u3);

        // 4. 옥순 (일반 당좌계좌: 100만원)
        CustomerVO u4 = new CustomerVO("202400004", "옥순", "202400004");
        u4.addAccount(new CheckingAccount("옥순", "202400004", 1_000_000, now));
        list.add(u4);

        return list;
    }

    public void saveAllData() {
        SaveCustomerFile(customerList, "./Account.txt");
    }

    public void SaveCustomerFile(List<CustomerVO> customers, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(customers);
            System.out.println("Objects saved to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private void InitGui() {
        setTitle("서버 GUI");
        setSize(480, 320);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        Label_UserCount = new JLabel("현재 접속자 수: ");
        Label_UserCount_2 = new JLabel("0");
        topPanel.add(Label_UserCount);
        topPanel.add(Label_UserCount_2);
        add(topPanel, BorderLayout.NORTH);

        TextArea_Log = new JTextArea();
        TextArea_Log.setEditable(false);
        sp = new JScrollPane(TextArea_Log);
        add(sp, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        Btn_StartStop = new JToggleButton("시작");
        Btn_StartStop.addActionListener(this);
        bottomPanel.add(Btn_StartStop);

        Btn_Reset = new JButton("로그 초기화");
        Btn_Reset.addActionListener(this);
        bottomPanel.add(Btn_Reset);

        add(bottomPanel, BorderLayout.SOUTH);
        setLocationRelativeTo(null);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == Btn_StartStop) {
            if (Btn_StartStop.isSelected()) startServer();
            else stopServer();
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
                    addMsg("서버 시작됨");
                    Btn_StartStop.setText("정지");
                });
                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    addMsg("접속: " + clientSocket.getInetAddress());
                    Client client = new Client(clientSocket, ServerMain.this, customerList);
                    clientList.add(client);
                    SwingUtilities.invokeLater(() -> Label_UserCount_2.setText(String.valueOf(clientList.size())));
                }
            } catch (IOException e) {
                if(isRunning) stopServer();
            }
        }).start();
    }

    public void stopServer() {
        isRunning = false;
        saveAllData(); // 종료 시 저장
        try {
            // 1. 새로운 접속을 받는 소켓 닫기
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();

            // 2. [추가됨] 이미 접속해있는 모든 클라이언트 강제 종료
            // (ConcurrentModificationException 방지를 위해 리스트 복사본으로 반복)
            if (!clientList.isEmpty()) {
                for (Client c : new ArrayList<>(clientList)) {
                    c.disconnectClient();
                }
            }
            clientList.clear();

            SwingUtilities.invokeLater(() -> {
                addMsg("서버 정지됨 (모든 연결 해제 및 데이터 저장 완료)");
                Btn_StartStop.setText("시작");
                Label_UserCount_2.setText("0");
            });
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void removeClient(Client client) {
        clientList.remove(client);
        addMsg("클라이언트 연결 해제됨");
        SwingUtilities.invokeLater(() -> Label_UserCount_2.setText(String.valueOf(clientList.size())));
    }

    // --- 관리자 기능 지원 메소드 ---

    public boolean addCustomer(String id, String name, String password, String address, String phone) {
        for (CustomerVO c : customerList) { if (c.getId().equals(id)) return false; }
        CustomerVO newC = new CustomerVO(id, name, password);
        newC.setAddress(address); newC.setPhone(phone);
        customerList.add(newC);
        saveAllData();
        addMsg("관리자: 고객 추가 (" + name + ")");
        return true;
    }

    public boolean deleteCustomer(String id) {
        for (CustomerVO c : customerList) {
            if (c.getId().equals(id)) {
                customerList.remove(c);
                saveAllData();
                addMsg("관리자: 고객 삭제 (" + id + ")");
                return true;
            }
        }
        return false;
    }

    public boolean addAccount(String customerId, String accountNo, AccountType type, long balance) {
        for (CustomerVO c : customerList) { if (c.findAccount(accountNo) != null) return false; }
        for (CustomerVO c : customerList) {
            if (c.getId().equals(customerId)) {
                Account acc = (type == AccountType.CHECKING) ?
                        new CheckingAccount(c.getName(), accountNo, balance, Date.valueOf(LocalDate.now())) :
                        new SavingsAccount(c.getName(), accountNo, balance, Date.valueOf(LocalDate.now()), 2.0);
                c.addAccount(acc);
                saveAllData();
                addMsg("관리자: 계좌 추가 (" + accountNo + ")");
                return true;
            }
        }
        return false;
    }

    public boolean deleteAccount(String customerId, String accountNo) {
        for (CustomerVO c : customerList) {
            if (c.getId().equals(customerId)) {
                boolean res = c.removeAccount(accountNo);
                if(res) { saveAllData(); addMsg("관리자: 계좌 삭제 (" + accountNo + ")"); }
                return res;
            }
        }
        return false;
    }

    public CustomerVO authenticateUser(String id, String password) {
        for (CustomerVO c : customerList) if (c.getId().equals(id) && c.getPassword().equals(password)) return c;
        return null;
    }
    public boolean authenticateManager(String id, String password) { return "admin".equals(id) && "1234".equals(password); }
    public void displayInfo(String msg) { addMsg(msg); }
    public void addMsg(String data) { TextArea_Log.append(data + "\n"); }
    public static void main(String[] args) { new ServerMain(); }
}