package BankATM.src.atm;

import BankATM.src.common.CommandDTO;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public class ATMMain extends JFrame implements ActionListener, BankServiceHandler {

    // ... (GUI 컴포넌트 선언부는 기존과 동일, 생략 가능하지만 전체 복붙 권장)
    private JLabel Label_Title;
    private JButton Btn_ViewAccount;
    private JButton Btn_Transfer;
    private JButton Btn_Login;
    private JButton Btn_Deposite;
    private JButton Btn_Withdrawal;
    private JButton Btn_Exit;
    private ImageIcon IconCNU;
    private JLabel Label_Image;

    PanViewAccount Pan_ViewAccount;
    PanTransfer Pan_Transfer;
    PanDeposite Pan_Deposite;
    PanWithdrawal Pan_Withdrawal;
    PanLogin Pan_Login;

    public static String userId;
    public static String userAccountNo; // [추가됨] 현재 선택된 계좌번호

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    public ATMMain() {
        startClient();
        InitGui();
        setVisible(true);
    }

    // ... InitGui() 및 기타 메소드는 기존과 동일합니다.
    // ... 다만 display() 메소드에서 userId 체크 로직은 그대로 둡니다.

    private void InitGui() {
        setLayout(null);
        setTitle("ATM GUI");
        setBounds(0, 0, 480, 320);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        try {
            // 이미지 경로는 환경에 따라 다를 수 있으므로 에러 발생 시 예외처리
            File imgFile = new File("src/BankATM/cnu.jpg");
            if(imgFile.exists()){
                Image Img_CNULogo = ImageIO.read(imgFile);
                IconCNU = new ImageIcon(Img_CNULogo.getScaledInstance(200, 200, Image.SCALE_SMOOTH));
                Label_Image = new JLabel();
                Label_Image.setIcon(IconCNU);
                Label_Image.setBounds(135, 70, IconCNU.getIconWidth(), IconCNU.getIconHeight());
                add(Label_Image);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Label_Title = new JLabel("CNU Bank ATM");
        Label_Title.setFont(new Font("Arial", Font.PLAIN, 30));
        Label_Title.setSize(getWidth(), 60);
        Label_Title.setLocation(0, 0);
        Label_Title.setHorizontalAlignment(JLabel.CENTER);
        add(Label_Title);

        Btn_ViewAccount = new JButton("계좌 조회");
        Btn_ViewAccount.setSize(100, 70);
        Btn_ViewAccount.setLocation(0, 60);
        Btn_ViewAccount.addActionListener(this);
        add(Btn_ViewAccount);

        Btn_Transfer = new JButton("계좌 이체");
        Btn_Transfer.setSize(100, 70);
        Btn_Transfer.setLocation(0, 130);
        Btn_Transfer.addActionListener(this);
        add(Btn_Transfer);

        Btn_Login = new JButton("로그인");
        Btn_Login.setSize(100, 70);
        Btn_Login.setLocation(0, 200);
        Btn_Login.addActionListener(this);
        add(Btn_Login);

        Btn_Deposite = new JButton("입금");
        Btn_Deposite.setSize(100, 70);
        Btn_Deposite.setLocation(365, 60);
        Btn_Deposite.addActionListener(this);
        add(Btn_Deposite);

        Btn_Withdrawal = new JButton("출금");
        Btn_Withdrawal.setSize(100, 70);
        Btn_Withdrawal.setLocation(365, 130);
        Btn_Withdrawal.addActionListener(this);
        add(Btn_Withdrawal);

        Btn_Exit = new JButton("종료");
        Btn_Exit.setSize(100, 70);
        Btn_Exit.setLocation(365, 200);
        Btn_Exit.addActionListener(this);
        add(Btn_Exit);

        Pan_ViewAccount = new PanViewAccount(this);
        add(Pan_ViewAccount);
        Pan_ViewAccount.setVisible(false);

        Pan_Transfer = new PanTransfer(this);
        add(Pan_Transfer);
        Pan_Transfer.setVisible(false);

        Pan_Deposite = new PanDeposite(this);
        add(Pan_Deposite);
        Pan_Deposite.setVisible(false);

        Pan_Withdrawal = new PanWithdrawal(this);
        add(Pan_Withdrawal);
        Pan_Withdrawal.setVisible(false);

        Pan_Login = new PanLogin(this);
        add(Pan_Login);
        Pan_Login.setVisible(false);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == Btn_ViewAccount) {
            display("ViewAccount");
            Pan_ViewAccount.GetBalance();
        } else if (e.getSource() == Btn_Transfer) {
            display("Transfer");
        } else if (e.getSource() == Btn_Login) {
            display("Login");
        } else if (e.getSource() == Btn_Deposite) {
            display("Deposite");
        } else if (e.getSource() == Btn_Withdrawal) {
            display("Withdrawal");
        } else if (e.getSource() == Btn_Exit) {
            dispose();
        }
    }

    public void display(String viewName) {
        if (userId == null) {
            if (!viewName.equals("Login") && !viewName.equals("Main")) {
                JOptionPane.showMessageDialog(null, "로그인이 필요합니다.", "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        SetFrameUI(false);
        switch (viewName) {
            case "ViewAccount" -> Pan_ViewAccount.setVisible(true);
            case "Transfer" -> Pan_Transfer.setVisible(true);
            case "Deposite" -> Pan_Deposite.setVisible(true);
            case "Withdrawal" -> Pan_Withdrawal.setVisible(true);
            case "Login" -> Pan_Login.setVisible(true);
            case "Main" -> SetFrameUI(true);
        }
    }

    void SetFrameUI(Boolean bOn) {
        Label_Title.setVisible(bOn);
        Btn_ViewAccount.setVisible(bOn);
        Btn_Transfer.setVisible(bOn);
        Btn_Login.setVisible(bOn);
        Btn_Deposite.setVisible(bOn);
        Btn_Withdrawal.setVisible(bOn);
        Btn_Exit.setVisible(bOn);
        Label_Image.setVisible(bOn);
    }

    private void startClient() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", 5002));
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            System.out.println("뱅크 서버 접속");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(CommandDTO commandDTO, CompletionHandler<Integer, ByteBuffer> handlers) {
        new Thread(() -> {
            commandDTO.setId(userId);
            // [중요] 계좌번호도 함께 전송
            if(commandDTO.getUserAccountNo() == null) {
                commandDTO.setUserAccountNo(userAccountNo);
            }

            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(commandDTO);
                objectOutputStream.flush();

                outputStream.write(byteArrayOutputStream.toByteArray());
                outputStream.flush();

                byte[] buffer = new byte[4096]; // 버퍼 크기 증가
                int bytesRead = inputStream.read(buffer);

                if (bytesRead != -1) {
                    ByteBuffer responseBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                    handlers.completed(bytesRead, responseBuffer);
                } else {
                    handlers.failed(new IOException("No response"), null);
                }

            } catch (IOException e) {
                e.printStackTrace();
                handlers.failed(e, null);
            }
        }).start();
    }

    public static void main(String[] args) {
        new ATMMain();
    }
}