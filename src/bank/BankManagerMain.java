package bank;

import common.AccountType;
import common.CommandDTO;
import common.RequestType;
import common.ResponseType;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class BankManagerMain extends JFrame {
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private boolean isConnected = false;

    // Login Components
    private JPanel panelLogin;
    private JTextField txtLoginId;
    private JPasswordField txtLoginPw;

    // Main Manager Components
    private JTabbedPane tabbedPane;
    private JPanel panelCustomer, panelAccount;

    // Customer Tab Components
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JTextField tfId, tfName, tfPw, tfPhone, tfAddress;
    private JButton btnAddCust, btnUpdateCust, btnDelCust;

    // Account Tab Components
    private JTextField tfAccCustId, tfAccNo, tfAccBal;
    private JComboBox<AccountType> cbAccType;
    private JButton btnSearchAcc, btnAddAcc, btnDelAcc;
    private JTextArea textAreaAccList;

    public BankManagerMain() {
        setTitle("CNU Bank Manager System");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        connectToServer();

        if (isConnected) {
            initLoginUI();
        } else {
            JOptionPane.showMessageDialog(this, "서버에 연결할 수 없습니다.\n서버를 먼저 실행해주세요.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", 5002));
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            isConnected = true;
        } catch (IOException e) {
            isConnected = false;
        }
    }

    // [중요 수정] 서버 통신 중 예외 발생 시(서버 꺼짐 등) 즉시 알림 후 종료 처리
    private CommandDTO sendRequest(CommandDTO req) {
        if (!isConnected || socket.isClosed()) {
            handleDisconnection();
            return null;
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(req);
            oos.flush();
            byte[] data = bos.toByteArray();

            outputStream.write(data);
            outputStream.flush();

            byte[] buffer = new byte[4096];
            int bytesRead = inputStream.read(buffer);

            // 서버가 강제로 종료되면 -1을 반환함
            if (bytesRead == -1) {
                throw new IOException("Server disconnected");
            }

            ByteArrayInputStream bis = new ByteArrayInputStream(buffer, 0, bytesRead);
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (CommandDTO) ois.readObject();

        } catch (Exception e) {
            // 통신 도중 에러 발생 시 처리
            handleDisconnection();
            return null;
        }
    }

    private void handleDisconnection() {
        isConnected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}

        JOptionPane.showMessageDialog(this, "서버와의 연결이 끊어졌습니다.\n프로그램을 종료합니다.", "Critical Error", JOptionPane.ERROR_MESSAGE);
        System.exit(0); // 강제 종료하여 오작동 방지
    }

    private void initLoginUI() {
        panelLogin = new JPanel(new GridBagLayout());
        panelLogin.setBackground(new Color(240, 240, 240));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel title = new JLabel("관리자 로그인");
        title.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panelLogin.add(title, gbc);

        gbc.gridwidth = 1; gbc.gridy = 1;
        panelLogin.add(new JLabel("ID:"), gbc);
        txtLoginId = new JTextField(15);
        gbc.gridx = 1;
        panelLogin.add(txtLoginId, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panelLogin.add(new JLabel("PW:"), gbc);
        txtLoginPw = new JPasswordField(15);
        gbc.gridx = 1;
        panelLogin.add(txtLoginPw, gbc);

        JButton btnLogin = new JButton("Login");
        btnLogin.setBackground(new Color(100, 150, 255));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setOpaque(true);
        btnLogin.setBorderPainted(false);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        btnLogin.addActionListener(e -> tryLogin());
        panelLogin.add(btnLogin, gbc);

        setContentPane(panelLogin);
    }

    private void tryLogin() {
        String id = txtLoginId.getText();
        String pw = new String(txtLoginPw.getPassword());

        CommandDTO req = new CommandDTO(RequestType.MANAGER_LOGIN, id, pw);
        CommandDTO res = sendRequest(req);

        if (res != null && res.getResponseType() == ResponseType.SUCCESS) {
            initManagerUI();
        } else {
            JOptionPane.showMessageDialog(this, "로그인 실패: ID 또는 PW 확인");
        }
    }

    private void initManagerUI() {
        tabbedPane = new JTabbedPane();

        // --- 1. 고객 관리 탭 ---
        panelCustomer = new JPanel(new BorderLayout());

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBorder(new TitledBorder("고객 목록"));
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedCustomer();
        });
        panelCustomer.add(new JScrollPane(userList), BorderLayout.WEST);

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(new TitledBorder("상세 정보 및 수정"));

        formPanel.add(new JLabel("ID (수정불가)")); tfId = new JTextField(); tfId.setEditable(false); formPanel.add(tfId);
        formPanel.add(new JLabel("이름")); tfName = new JTextField(); formPanel.add(tfName);
        formPanel.add(new JLabel("비밀번호")); tfPw = new JTextField(); formPanel.add(tfPw);
        formPanel.add(new JLabel("전화번호")); tfPhone = new JTextField(); formPanel.add(tfPhone);
        formPanel.add(new JLabel("주소")); tfAddress = new JTextField(); formPanel.add(tfAddress);

        JPanel btnPanel = new JPanel(new FlowLayout());
        btnAddCust = new JButton("신규등록"); btnAddCust.addActionListener(e -> addCustomerPopup()); // [수정] 팝업 호출
        btnUpdateCust = new JButton("수정저장"); btnUpdateCust.addActionListener(e -> updateCustomer());
        btnDelCust = new JButton("삭제"); btnDelCust.setBackground(Color.PINK); btnDelCust.addActionListener(e -> deleteCustomer());

        btnDelCust.setOpaque(true); btnDelCust.setBorderPainted(false);

        btnPanel.add(btnAddCust); btnPanel.add(btnUpdateCust); btnPanel.add(btnDelCust);

        panelCustomer.add(formPanel, BorderLayout.CENTER);
        panelCustomer.add(btnPanel, BorderLayout.SOUTH);

        // --- 2. 계좌 관리 탭 ---
        panelAccount = new JPanel(new BorderLayout());

        JPanel inputForm = new JPanel(new GridBagLayout());
        inputForm.setBorder(new TitledBorder("계좌 작업"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        inputForm.add(new JLabel("고객 ID:"), gbc);
        gbc.gridx = 1;
        tfAccCustId = new JTextField(12);
        inputForm.add(tfAccCustId, gbc);
        gbc.gridx = 2;
        btnSearchAcc = new JButton("조회");
        btnSearchAcc.setBackground(new Color(100, 200, 100));
        btnSearchAcc.setOpaque(true); btnSearchAcc.setBorderPainted(false);
        btnSearchAcc.addActionListener(e -> searchCustomerAccounts());
        inputForm.add(btnSearchAcc, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputForm.add(new JLabel("계좌번호:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        tfAccNo = new JTextField();
        inputForm.add(tfAccNo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        inputForm.add(new JLabel("초기잔액:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        tfAccBal = new JTextField();
        inputForm.add(tfAccBal, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        inputForm.add(new JLabel("계좌종류:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        cbAccType = new JComboBox<>(AccountType.values());
        inputForm.add(cbAccType, gbc);

        JPanel accActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnAddAcc = new JButton("계좌 추가");
        btnAddAcc.addActionListener(e -> addAccount());
        btnDelAcc = new JButton("계좌 삭제");
        btnDelAcc.setBackground(Color.PINK);
        btnDelAcc.addActionListener(e -> deleteAccount());

        btnAddAcc.setOpaque(true); btnAddAcc.setBorderPainted(false);
        btnDelAcc.setOpaque(true); btnDelAcc.setBorderPainted(false);

        accActionPanel.add(btnAddAcc);
        accActionPanel.add(btnDelAcc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
        inputForm.add(accActionPanel, gbc);

        textAreaAccList = new JTextArea();
        textAreaAccList.setEditable(false);
        textAreaAccList.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(textAreaAccList);
        scrollPane.setBorder(new TitledBorder("보유 계좌 목록 조회 결과"));

        panelAccount.add(inputForm, BorderLayout.NORTH);
        panelAccount.add(scrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("고객 관리", panelCustomer);
        tabbedPane.addTab("계좌 관리", panelAccount);

        setContentPane(tabbedPane);
        revalidate();
        refreshCustomerList();
    }

    // --- Logic Methods ---

    private void refreshCustomerList() {
        CommandDTO res = sendRequest(new CommandDTO(RequestType.MANAGE_GET_CUSTOMERS));
        if (res != null && res.getResponseType() == ResponseType.SUCCESS) {
            userListModel.clear();
            if (res.getAccountList() != null) {
                for (String s : res.getAccountList()) {
                    userListModel.addElement(s);
                }
            }
        }
    }

    private void loadSelectedCustomer() {
        String selected = userList.getSelectedValue();
        if (selected == null) return;
        String[] parts = selected.split("\\|");
        if (parts.length >= 5) {
            tfId.setText(parts[0]);
            tfName.setText(parts[1]);
            tfPhone.setText(parts[2]);
            tfAddress.setText(parts[3]);
            tfPw.setText(parts[4]);
            tfAccCustId.setText(parts[0]);
        }
    }

    // [중요 수정] 신규 등록 시 별도 팝업창으로 상세 정보 입력받기
    private void addCustomerPopup() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));

        JTextField pId = new JTextField();
        JTextField pName = new JTextField();
        JPasswordField pPw = new JPasswordField();
        JTextField pPhone = new JTextField();
        JTextField pAddr = new JTextField();

        panel.add(new JLabel("ID:")); panel.add(pId);
        panel.add(new JLabel("이름:")); panel.add(pName);
        panel.add(new JLabel("비밀번호:")); panel.add(pPw);
        panel.add(new JLabel("전화번호:")); panel.add(pPhone);
        panel.add(new JLabel("주소:")); panel.add(pAddr);

        int result = JOptionPane.showConfirmDialog(this, panel, "신규 고객 등록", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String id = pId.getText().trim();
            String name = pName.getText().trim();
            String pw = new String(pPw.getPassword()).trim();

            if(id.isEmpty() || name.isEmpty() || pw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "ID, 이름, 비밀번호는 필수 입력 사항입니다.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }

            CommandDTO req = new CommandDTO(RequestType.MANAGE_ADD_CUSTOMER);
            req.setId(id);
            req.setUserName(name);
            req.setPassword(pw);
            req.setUserPhone(pPhone.getText());
            req.setUserAddress(pAddr.getText());

            CommandDTO res = sendRequest(req);
            if (res != null && res.getResponseType() == ResponseType.SUCCESS) {
                JOptionPane.showMessageDialog(this, "고객 추가 성공!");
                refreshCustomerList();
            } else {
                JOptionPane.showMessageDialog(this, "추가 실패: 이미 존재하는 ID입니다.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateCustomer() {
        if (tfId.getText().isEmpty()) return;
        CommandDTO req = new CommandDTO(RequestType.MANAGE_UPDATE_CUSTOMER);
        req.setId(tfId.getText());
        req.setUserName(tfName.getText());
        req.setPassword(tfPw.getText());
        req.setUserPhone(tfPhone.getText());
        req.setUserAddress(tfAddress.getText());

        CommandDTO res = sendRequest(req);
        if (res != null && res.getResponseType() == ResponseType.SUCCESS) {
            JOptionPane.showMessageDialog(this, "수정 완료");
            refreshCustomerList();
        }
    }

    private void deleteCustomer() {
        String id = tfId.getText();
        if (id.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this, "정말로 고객 [" + id + "]를 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            CommandDTO req = new CommandDTO(RequestType.MANAGE_DEL_CUSTOMER);
            req.setId(id);
            CommandDTO res = sendRequest(req);
            if (res != null && res.getResponseType() == ResponseType.SUCCESS) {
                JOptionPane.showMessageDialog(this, "삭제 완료");
                tfId.setText(""); tfName.setText(""); tfPw.setText("");
                refreshCustomerList();
            } else JOptionPane.showMessageDialog(this, "삭제 실패");
        }
    }

    private void searchCustomerAccounts() {
        String targetId = tfAccCustId.getText().trim();
        if (targetId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "조회할 고객 ID를 입력하세요.");
            return;
        }

        CommandDTO req = new CommandDTO(RequestType.VIEW);
        req.setId(targetId);

        CommandDTO res = sendRequest(req);

        textAreaAccList.setText("");
        if (res != null && res.getResponseType() == ResponseType.SUCCESS) {
            java.util.List<String> list = res.getAccountList();
            if (list == null || list.isEmpty()) {
                textAreaAccList.append(">> 고객 [" + targetId + "]님의 보유 계좌가 없습니다.\n");
            } else {
                textAreaAccList.append(">> 고객 [" + targetId + "]님의 계좌 목록:\n\n");
                textAreaAccList.append(String.format("%-10s %-20s %s\n", " [종류]", " [계좌번호]", " [잔액]"));
                textAreaAccList.append("--------------------------------------------------\n");

                for (String info : list) {
                    String[] parts = info.split("/");
                    if (parts.length >= 3) {
                        String type = parts[0];
                        String no = parts[1];
                        String bal = parts[2];
                        try {
                            long b = Long.parseLong(bal);
                            bal = String.format("%,d원", b);
                        } catch(Exception e){}

                        textAreaAccList.append(String.format(" %-10s %-20s %s\n", type, no, bal));
                    }
                }
            }
        } else {
            textAreaAccList.setText("조회 실패: 존재하지 않는 고객 ID입니다.");
        }
    }

    private void addAccount() {
        CommandDTO req = new CommandDTO(RequestType.MANAGE_ADD_ACCOUNT);
        req.setId(tfAccCustId.getText());
        req.setUserAccountNo(tfAccNo.getText());
        req.setAccountType((AccountType) cbAccType.getSelectedItem());
        try {
            req.setAmount(Long.parseLong(tfAccBal.getText()));
            CommandDTO res = sendRequest(req);
            if(res != null && res.getResponseType() == ResponseType.SUCCESS) {
                JOptionPane.showMessageDialog(this, "계좌 추가 성공");
                searchCustomerAccounts();
            }
            else JOptionPane.showMessageDialog(this, "실패 (ID없음 또는 계좌중복)");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "잔액은 숫자만 입력");
        }
    }

    private void deleteAccount() {
        String acc = tfAccNo.getText();
        int confirm = JOptionPane.showConfirmDialog(this, "정말로 계좌 [" + acc + "]를 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            CommandDTO req = new CommandDTO(RequestType.MANAGE_DEL_ACCOUNT);
            req.setId(tfAccCustId.getText());
            req.setUserAccountNo(acc);
            CommandDTO res = sendRequest(req);
            if(res != null && res.getResponseType() == ResponseType.SUCCESS) {
                JOptionPane.showMessageDialog(this, "계좌 삭제 완료");
                searchCustomerAccounts();
            }
            else JOptionPane.showMessageDialog(this, "실패");
        }
    }

    public static void main(String[] args) {
        new BankManagerMain();
    }
}