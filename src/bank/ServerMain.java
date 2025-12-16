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

//*******************************************************************
// Name : ServerMain
// Type : Class
// Description :  BankServer의 GUI 프레임이며, ATM과의 소켓통신을 담당한다.
//                계좌 정보들을 보유하고 있으며, 관련 기능들을 가지고 있다.
//*******************************************************************

public class ServerMain extends JFrame implements ActionListener, ClientHandler {
    private JLabel Label_UserCount;
    private JLabel Label_UserCount_2;
    private JToggleButton Btn_StartStop;
    private JButton Btn_Reset;
    private JTextArea TextArea_Log;
    private JScrollPane sp;
    private JButton Btn_Manager;

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
    // Name : GetDefaultCustomers()
    // Type : Method
    // Description :  Server 시작 시 저장된 계좌 정보가 없으면 Default 계좌를 생성하는 기능
    //*******************************************************************
    private static List<CustomerVO> GetDefaultCustomers() {
        List<CustomerVO> list = new Vector<>();
        Date now = Date.valueOf(LocalDate.now());

        // 1. 광수 (자동이체 테스트용)
        // - 당좌계좌(CheckingAccount): 잔액 100,000원
        // - 저축계좌(SavingsAccount): 잔액 1,000,000원
        CheckingAccount chkAccount = new CheckingAccount("광수", "202400001-1", 100_000, now);
        SavingsAccount savAccount = new SavingsAccount("광수", "202400001-2", 1_000_000, now, 2.0);

        // (수정됨) 한도 설정 제거함. 단순히 두 계좌를 연결만 수행
        chkAccount.setLinkedSavings(savAccount);

        CustomerVO user1 = new CustomerVO("202400001", "광수", "202400001");
        user1.addAccount(chkAccount);
        user1.addAccount(savAccount);
        list.add(user1);

        // 2. 영철 (일반 계좌)
        CustomerVO user2 = new CustomerVO("202400002", "영철", "202400002");
        user2.addAccount(new CheckingAccount("영철", "202400002", 10_000_000, now));
        list.add(user2);

        // 3. 영숙
        CustomerVO user3 = new CustomerVO("202400003", "영숙", "202400003");
        user3.addAccount(new CheckingAccount("영숙", "202400003", 5_000_000, now));
        list.add(user3);

        // 4. 옥순
        CustomerVO user4 = new CustomerVO("202400004", "옥순", "202400004");
        user4.addAccount(new CheckingAccount("옥순", "202400004", 1_000_000, now));
        list.add(user4);

        return list;
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

        Btn_Manager = new JButton("관리자 모드");
        Btn_Manager.addActionListener(this);
        bottomPanel.add(Btn_Manager);

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
        else if (e.getSource() == Btn_Manager) {
            // [추가 구현] 관리자 인증 절차 (authenticateUser)
            JPanel panel = new JPanel(new GridLayout(2, 2));
            JTextField txtId = new JTextField();
            JPasswordField txtPass = new JPasswordField();

            panel.add(new JLabel("관리자 ID:"));
            panel.add(txtId);
            panel.add(new JLabel("비밀번호:"));
            panel.add(txtPass);

            // 로그인 팝업 띄우기
            int option = JOptionPane.showConfirmDialog(this, panel, "관리자 로그인", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (option == JOptionPane.OK_OPTION) {
                String id = txtId.getText();
                String pw = new String(txtPass.getPassword());

                // 위에서 만든 관리자 인증 메소드 호출
                if (authenticateManager(id, pw)) {
                    new ManagerGUI(this); // 성공 시 GUI 오픈
                    addMsg("관리자(" + id + ") 접속 성공");
                } else {
                    JOptionPane.showMessageDialog(this, "관리자 인증 실패\n(ID: admin / PW: 1234)", "경고", JOptionPane.ERROR_MESSAGE);
                    addMsg("관리자 접속 실패 (ID: " + id + ")");
                }
            }
        }
    }

    //*******************************************************************
    // Name : startServer
    // Type : Method
    // Description :  서버 소켓을 port 5002 로 bind 하여 open 하는 기능 및
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
                if(isRunning) e.printStackTrace();
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

    //*******************************************************************
    // Name : addCustomer
    // Requirements : 신규고객추가
    // Description : 중복 ID 체크 후 신규 고객 생성 및 리스트 추가
    //*******************************************************************
    public boolean addCustomer(String id, String name, String password, String address, String phone) {
        for (CustomerVO c : customerList) {
            if (c.getId().equals(id)) {
                addMsg("오류: 이미 존재하는 ID입니다 (" + id + ")");
                return false;
            }
        }

        CustomerVO newCustomer = new CustomerVO(id, name, password);
        newCustomer.setAddress(address);
        newCustomer.setPhone(phone);

        customerList.add(newCustomer);
        addMsg("관리자: 신규 고객 추가 완료 (" + name + ")");
        SaveCustomerFile(customerList, "./Account.txt");
        return true;
    }

    //*******************************************************************
    // Name : deleteCustomer
    // Requirements : 고객삭제
    // Description : ID로 고객을 찾아 삭제
    //*******************************************************************
    public boolean deleteCustomer(String id) {
        for (CustomerVO c : customerList) {
            if (c.getId().equals(id)) {
                customerList.remove(c);
                addMsg("관리자: 고객 삭제 완료 (" + id + ")");
                SaveCustomerFile(customerList, "./Account.txt");
                return true;
            }
        }
        addMsg("오류: 해당 ID의 고객을 찾을 수 없습니다.");
        return false;
    }

    //*******************************************************************
    // Name : addAccount
    // Requirements : 계좌추가
    // Description : 특정 고객에게 새로운 계좌 추가 (중복 계좌번호 체크 포함)
    //*******************************************************************
    public boolean addAccount(String customerId, String accountNo, AccountType type, long balance) {
        for (CustomerVO c : customerList) {
            if (c.findAccount(accountNo) != null) {
                addMsg("오류: 이미 존재하는 계좌번호입니다 (" + accountNo + ")");
                return false;
            }
        }

        for (CustomerVO c : customerList) {
            if (c.getId().equals(customerId)) {
                Account newAccount;
                Date now = Date.valueOf(LocalDate.now());

                if (type == AccountType.CHECKING) {
                    newAccount = new CheckingAccount(c.getName(), accountNo, balance, now);
                } else {
                    newAccount = new SavingsAccount(c.getName(), accountNo, balance, now, 2.0);
                }

                c.addAccount(newAccount);

                addMsg("관리자: 계좌 개설 완료 (" + accountNo + " -> " + c.getName() + ")");
                SaveCustomerFile(customerList, "./Account.txt");
                return true;
            }
        }
        addMsg("오류: 고객 ID를 찾을 수 없습니다.");
        return false;
    }

    //*******************************************************************
    // Name : deleteAccount
    // Requirements : 계좌삭제
    // Description : 특정 고객의 특정 계좌 삭제
    //*******************************************************************
    public boolean deleteAccount(String customerId, String accountNo) {
        for (CustomerVO c : customerList) {
            if (c.getId().equals(customerId)) {
                boolean result = c.removeAccount(accountNo);
                if (result) {
                    addMsg("관리자: 계좌 삭제 완료 (" + accountNo + ")");
                    SaveCustomerFile(customerList, "./Account.txt");
                    return true;
                } else {
                    addMsg("오류: 해당 고객에게서 계좌를 찾을 수 없습니다.");
                    return false;
                }
            }
        }
        addMsg("오류: 고객 ID를 찾을 수 없습니다.");
        return false;
    }

    @Override
    public void displayInfo(String msg) {
        addMsg(msg);
    }

    public void addMsg(String data) {
        TextArea_Log.append(data + "\n");
    }

    // ------------------------------------------------------------------
    // [추가 구현] 설명서 요구사항: 통계 및 전체 출력 기능
    // ------------------------------------------------------------------

    // 1. 모든 고객 정보 출력
    public void printCustomerList() {
        addMsg("========================================");
        addMsg("           [ 모든 고객 목록 출력 ]");
        addMsg("----------------------------------------");
        if (customerList.isEmpty()) {
            addMsg("등록된 고객이 없습니다.");
        } else {
            for (CustomerVO c : customerList) {
                String info = String.format("ID: %s | 이름: %s | 연락처: %s",
                        c.getId(), c.getName(), c.getPhone());
                addMsg(info);
            }
        }
        addMsg("========================================");
    }

    // 2. 모든 계좌 정보 출력
    public void printAccountList() {
        addMsg("========================================");
        addMsg("           [ 모든 계좌 목록 출력 ]");
        addMsg("----------------------------------------");
        boolean hasAccount = false;

        for (CustomerVO c : customerList) {
            List<Account> accounts = c.getAccountList();
            if (accounts != null && !accounts.isEmpty()) {
                hasAccount = true;
                for (Account a : accounts) {
                    String info = String.format("[%s] 계좌: %s | 예금주: %s | 잔액: %,d원 | 타입: %s",
                            c.getName(), a.getAccountNo(), a.getOwner(), a.getBalance(), a.getAccountType());
                    addMsg(info);
                }
            }
        }

        if (!hasAccount) addMsg("등록된 계좌가 없습니다.");
        addMsg("========================================");
    }

    // 3. 모든 고객의 수 출력
    public void getNumberOfCustomers() {
        int count = customerList.size();
        addMsg("[통계] 현재 등록된 총 고객 수: " + count + "명");
    }

    // 4. 총 보유 잔고 출력
    public void getTotalBankBalance() {
        long totalBalance = 0;

        for (CustomerVO c : customerList) {
            List<Account> accounts = c.getAccountList();
            if (accounts != null) {
                for (Account a : accounts) {
                    totalBalance += a.getBalance();
                }
            }
        }
        addMsg("[통계] 은행 총 보유 잔고: " + String.format("%,d", totalBalance) + "원");
    }

    // ------------------------------------------------------------------
    // [요구사항 구현] authenticateUser: 고객 및 관리자 권한 인증
    // ------------------------------------------------------------------

    // 1. 고객 인증 (ATM 로그인용)
    public CustomerVO authenticateUser(String id, String password) {
        for (CustomerVO c : customerList) {
            if (c.getId().equals(id) && c.getPassword().equals(password)) {
                return c;
            }
        }
        return null;
    }

    // 2. 관리자 인증 (관리자 모드 접속용)
    public boolean authenticateManager(String id, String password) {
        return "admin".equals(id) && "1234".equals(password);
    }

    public static void main(String[] args) throws Exception {
        ServerMain f = new ServerMain();
    }
}