package BankATM.src.atm;

import BankATM.src.common.CommandDTO;
import BankATM.src.common.RequestType;
import BankATM.src.common.ResponseType;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public class ManagerMain extends JFrame implements BankServiceHandler {
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private JTextField tfId, tfPw, tfNewId, tfNewPw, tfNewAcc;
    private JTextArea logArea;

    public ManagerMain() {
        startClient();
        initGUI();
    }

    private void initGUI() {
        setTitle("Bank Manager Client");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 상단: 로그인 패널
        JPanel loginPanel = new JPanel();
        tfId = new JTextField("admin", 5);
        tfPw = new JTextField("1234", 5);
        JButton btnLogin = new JButton("Login");

        loginPanel.add(new JLabel("Admin ID:")); loginPanel.add(tfId);
        loginPanel.add(new JLabel("PW:")); loginPanel.add(tfPw);
        loginPanel.add(btnLogin);

        add(loginPanel, BorderLayout.NORTH);

        // 중앙: 기능 패널 (로그인 후 활성화)
        JPanel centerPanel = new JPanel(new GridLayout(4, 2));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Add Customer"));

        tfNewId = new JTextField();
        tfNewPw = new JTextField();
        tfNewAcc = new JTextField(); // 초기 계좌번호
        JButton btnAddCustomer = new JButton("Add Customer");

        centerPanel.add(new JLabel("New User ID:")); centerPanel.add(tfNewId);
        centerPanel.add(new JLabel("New User PW:")); centerPanel.add(tfNewPw);
        centerPanel.add(new JLabel("Initial Account No:")); centerPanel.add(tfNewAcc);
        centerPanel.add(new JLabel("")); centerPanel.add(btnAddCustomer);

        add(centerPanel, BorderLayout.CENTER);

        // 하단: 로그
        logArea = new JTextArea(10, 40);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        // 이벤트 리스너
        btnLogin.addActionListener(e -> {
            CommandDTO cmd = new CommandDTO(RequestType.MANAGER_LOGIN, tfId.getText(), tfPw.getText());
            send(cmd, new CompletionHandler<>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    processResponse(attachment, "Login");
                }
                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {}
            });
        });

        btnAddCustomer.addActionListener(e -> {
            // 편의상 CommandDTO의 생성자 중 적절한 것을 재사용 (ID, PW, AccountNo를 실어 보냄)
            CommandDTO cmd = new CommandDTO(RequestType.ADD_CUSTOMER, tfNewId.getText(), tfNewPw.getText());
            cmd.setUserAccountNo(tfNewAcc.getText()); // 계좌번호 필드 재사용

            send(cmd, new CompletionHandler<>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    processResponse(attachment, "Add Customer");
                }
                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {}
            });
        });

        setVisible(true);
    }

    private void processResponse(ByteBuffer buffer, String action) {
        buffer.flip();
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(buffer.array());
            ObjectInputStream ois = new ObjectInputStream(bis);
            CommandDTO res = (CommandDTO) ois.readObject();

            SwingUtilities.invokeLater(() -> {
                if (res.getResponseType() == ResponseType.SUCCESS) {
                    logArea.append("[" + action + "] Success\n");
                } else {
                    logArea.append("[" + action + "] Failed\n");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startClient() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", 5002));
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            logArea.setText("Connected to Server.\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(CommandDTO commandDTO, CompletionHandler<Integer, ByteBuffer> handlers) {
        new Thread(() -> {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(commandDTO);
                oos.flush();
                outputStream.write(bos.toByteArray());
                outputStream.flush();

                byte[] buffer = new byte[4096];
                int read = inputStream.read(buffer);
                if (read != -1) {
                    handlers.completed(read, ByteBuffer.wrap(buffer, 0, read));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        new ManagerMain();
    }
}