package bank;
import common.AccountType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ManagerGUI extends JFrame implements ActionListener {
    private ServerMain serverMain;

    private JTabbedPane tabbedPane;
    private JPanel panelCustomer;
    private JPanel panelAccount;

    // 고객 관리 컴포넌트
    private JTextField tfCustId, tfCustName, tfCustPw, tfCustAddr, tfCustPhone;
    private JButton btnAddCust, btnDelCust;

    // 계좌 관리 컴포넌트
    private JTextField tfAccCustId, tfAccNo, tfAccBalance;
    private JComboBox<AccountType> cbAccType;
    private JButton btnAddAcc, btnDelAcc;

    public ManagerGUI(ServerMain serverMain) {
        this.serverMain = serverMain;
        setTitle("Bank Manager (관리자 모드)");
        setSize(400, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // 창을 닫아도 서버는 안 꺼짐

        InitGUI();
        setVisible(true);
    }

    private void InitGUI() {
        tabbedPane = new JTabbedPane();

        // --- 1. 고객 관리 탭 ---
        panelCustomer = new JPanel(null);
        JLabel l1 = new JLabel("ID:"); l1.setBounds(20, 20, 80, 25); panelCustomer.add(l1);
        tfCustId = new JTextField(); tfCustId.setBounds(100, 20, 200, 25); panelCustomer.add(tfCustId);
        JLabel l2 = new JLabel("이름:"); l2.setBounds(20, 50, 80, 25); panelCustomer.add(l2);
        tfCustName = new JTextField(); tfCustName.setBounds(100, 50, 200, 25); panelCustomer.add(tfCustName);
        JLabel l3 = new JLabel("비밀번호:"); l3.setBounds(20, 80, 80, 25); panelCustomer.add(l3);
        tfCustPw = new JTextField(); tfCustPw.setBounds(100, 80, 200, 25); panelCustomer.add(tfCustPw);
        JLabel l4 = new JLabel("주소:"); l4.setBounds(20, 110, 80, 25); panelCustomer.add(l4);
        tfCustAddr = new JTextField(); tfCustAddr.setBounds(100, 110, 200, 25); panelCustomer.add(tfCustAddr);
        JLabel l5 = new JLabel("전화번호:"); l5.setBounds(20, 140, 80, 25); panelCustomer.add(l5);
        tfCustPhone = new JTextField(); tfCustPhone.setBounds(100, 140, 200, 25); panelCustomer.add(tfCustPhone);

        btnAddCust = new JButton("고객 추가"); btnAddCust.setBounds(50, 200, 100, 30);
        btnAddCust.addActionListener(this); panelCustomer.add(btnAddCust);
        btnDelCust = new JButton("고객 삭제"); btnDelCust.setBounds(180, 200, 100, 30);
        btnDelCust.addActionListener(this); panelCustomer.add(btnDelCust);
        tabbedPane.addTab("고객 관리", panelCustomer);

        // --- 2. 계좌 관리 탭 ---
        panelAccount = new JPanel(null);
        JLabel la1 = new JLabel("고객 ID:"); la1.setBounds(20, 20, 80, 25); panelAccount.add(la1);
        tfAccCustId = new JTextField(); tfAccCustId.setBounds(100, 20, 200, 25); panelAccount.add(tfAccCustId);
        JLabel la2 = new JLabel("계좌번호:"); la2.setBounds(20, 50, 80, 25); panelAccount.add(la2);
        tfAccNo = new JTextField(); tfAccNo.setBounds(100, 50, 200, 25); panelAccount.add(tfAccNo);
        JLabel la3 = new JLabel("초기잔액:"); la3.setBounds(20, 80, 80, 25); panelAccount.add(la3);
        tfAccBalance = new JTextField(); tfAccBalance.setBounds(100, 80, 200, 25); panelAccount.add(tfAccBalance);
        JLabel la4 = new JLabel("계좌타입:"); la4.setBounds(20, 110, 80, 25); panelAccount.add(la4);
        cbAccType = new JComboBox<>(AccountType.values());
        cbAccType.setBounds(100, 110, 200, 25); panelAccount.add(cbAccType);

        btnAddAcc = new JButton("계좌 추가"); btnAddAcc.setBounds(50, 200, 100, 30);
        btnAddAcc.addActionListener(this); panelAccount.add(btnAddAcc);
        btnDelAcc = new JButton("계좌 삭제"); btnDelAcc.setBounds(180, 200, 100, 30);
        btnDelAcc.addActionListener(this); panelAccount.add(btnDelAcc);
        tabbedPane.addTab("계좌 관리", panelAccount);

        // --- [추가됨] 3. 현황/통계 탭 ---
        JPanel panelStats = new JPanel(null);

        JButton btnPrintAllCust = new JButton("모든 고객 목록 출력");
        btnPrintAllCust.setBounds(50, 30, 250, 40);
        btnPrintAllCust.addActionListener(e -> serverMain.printCustomerList()); // ServerMain의 메소드 호출
        panelStats.add(btnPrintAllCust);

        JButton btnPrintAllAcc = new JButton("모든 계좌 목록 출력");
        btnPrintAllAcc.setBounds(50, 90, 250, 40);
        btnPrintAllAcc.addActionListener(e -> serverMain.printAccountList());
        panelStats.add(btnPrintAllAcc);

        JButton btnStats = new JButton("총 고객 수 및 총 잔고 확인");
        btnStats.setBounds(50, 150, 250, 40);
        btnStats.addActionListener(e -> {
            serverMain.getNumberOfCustomers();
            serverMain.getTotalBankBalance();
        });
        panelStats.add(btnStats);

        tabbedPane.addTab("현황/통계", panelStats);

        add(tabbedPane);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // --- 고객 추가 ---
        if (e.getSource() == btnAddCust) {
            String id = tfCustId.getText();
            String name = tfCustName.getText();
            String pw = tfCustPw.getText();
            String addr = tfCustAddr.getText();
            String phone = tfCustPhone.getText();

            if(id.isEmpty() || name.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "ID, 이름, 비밀번호는 필수입니다.");
                return;
            }

            boolean result = serverMain.addCustomer(id, name, pw, addr, phone);
            if (result) {
                JOptionPane.showMessageDialog(this, "고객 추가 성공!");
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "실패: 이미 존재하는 ID입니다.");
            }
        }
        // --- 고객 삭제 ---
        else if (e.getSource() == btnDelCust) {
            String id = tfCustId.getText();
            if(id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "삭제할 고객 ID를 입력하세요.");
                return;
            }
            boolean result = serverMain.deleteCustomer(id);
            if (result) {
                JOptionPane.showMessageDialog(this, "고객 삭제 성공!");
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "실패: 존재하지 않는 ID입니다.");
            }
        }
        // --- 계좌 추가 ---
        else if (e.getSource() == btnAddAcc) {
            String custId = tfAccCustId.getText();
            String accNo = tfAccNo.getText();
            String balStr = tfAccBalance.getText();
            AccountType type = (AccountType) cbAccType.getSelectedItem();

            if(custId.isEmpty() || accNo.isEmpty() || balStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "모든 필드를 입력하세요.");
                return;
            }

            try {
                long balance = Long.parseLong(balStr);
                boolean result = serverMain.addAccount(custId, accNo, type, balance);
                if (result) {
                    JOptionPane.showMessageDialog(this, "계좌 개설 성공!");
                    clearFields();
                } else {
                    JOptionPane.showMessageDialog(this, "실패: 고객ID가 없거나 계좌번호 중복.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "잔액은 숫자만 입력하세요.");
            }
        }
        // --- 계좌 삭제 ---
        else if (e.getSource() == btnDelAcc) {
            String custId = tfAccCustId.getText();
            String accNo = tfAccNo.getText();

            if(custId.isEmpty() || accNo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "고객ID와 계좌번호를 입력하세요.");
                return;
            }

            boolean result = serverMain.deleteAccount(custId, accNo);
            if (result) {
                JOptionPane.showMessageDialog(this, "계좌 삭제 성공!");
                clearFields();
            } else {
                JOptionPane.showMessageDialog(this, "실패: 정보를 확인하세요.");
            }
        }
    }

    private void clearFields() {
        tfCustId.setText(""); tfCustName.setText(""); tfCustPw.setText("");
        tfCustAddr.setText(""); tfCustPhone.setText("");
        tfAccCustId.setText(""); tfAccNo.setText(""); tfAccBalance.setText("");
    }
}
