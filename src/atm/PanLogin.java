package atm;

import common.CommandDTO;
import common.RequestType;
import common.ResponseType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

//*******************************************************************
// Name : PanLogin
// Type : Class
// Description :  로그인 화면 패널을 구현한 Class 이다.
//*******************************************************************
public class PanLogin extends JPanel implements ActionListener {
    private JLabel Label_Title;

    private JLabel Label_ID;
    private JTextField Text_ID;

    private JLabel Label_Password;
    private JTextField Text_Password;

    private JButton Btn_Transfer;
    private JButton Btn_Close;

    ATMMain MainFrame;
    private BankServiceHandler handler;

    //*******************************************************************
    // Name : PanLogin()
    // Type : 생성자
    // Description :  PanLogin Class의 생성자 구현
    //*******************************************************************
    public PanLogin(ATMMain parent) {
        MainFrame = parent;
        InitGUI();
    }

    //*******************************************************************
    // Name : InitGUI
    // Type : Method
    // Description :  로그인 화면 패널의 GUI를 초기화 하는 메소드 구현
    //*******************************************************************
    private void InitGUI() {
        setLayout(null);
        setBounds(0, 0, 480, 320);

        Label_Title = new JLabel("로그인");
        Label_Title.setBounds(0, 0, 480, 40);
        Label_Title.setHorizontalAlignment(JLabel.CENTER);
        add(Label_Title);

        Label_ID = new JLabel("아이디");
        Label_ID.setBounds(0, 60, 100, 20);
        Label_ID.setHorizontalAlignment(JLabel.LEFT);
        add(Label_ID);

        Text_ID = new JTextField();
        Text_ID.setBounds(100, 60, 350, 20);
        Text_ID.setEditable(true);
        add(Text_ID);

        Label_Password = new JLabel("비밀번호");
        Label_Password.setBounds(0, 100, 100, 20);
        Label_Password.setHorizontalAlignment(JLabel.LEFT);
        add(Label_Password);

        Text_Password = new JTextField();
        Text_Password.setBounds(100, 100, 350, 20);
        Text_Password.setEditable(true);
        add(Text_Password);

        Btn_Transfer = new JButton("로그인");
        Btn_Transfer.setBounds(100, 250, 70, 20);
        Btn_Transfer.addActionListener(this);
        add(Btn_Transfer);

        Btn_Close = new JButton("취소");
        Btn_Close.setBounds(250, 250, 70, 20);
        Btn_Close.addActionListener(this);
        add(Btn_Close);
    }

    //*******************************************************************
    // Name : actionPerformed
    // Type : Listner
    // Description :  로그인 버튼, 취소 버튼의 동작을 구현
    //                로그인, 취소 동작 후 메인 화면으로 변경되도록 구현
    //*******************************************************************
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == Btn_Transfer) {
            Login();
        }
        if (e.getSource() == Btn_Close) {
            this.setVisible(false);
            MainFrame.display("Main");
        }
    }

    //*******************************************************************
    // Name : Login()
    // Type : Method
    // Description :  로그인 화면의 데이터를 가지고 있는 CommandDTO를 생성하고,
    //                ATMMain의 Send 기능을 호출하여 서버에 로그인 요청 메시지를 전달 하는 기능.
    //*******************************************************************
    public void Login() {
        String id = Text_ID.getText();
        String password = Text_Password.getText();
        MainFrame.userId = id;

        CommandDTO loginCommand = new CommandDTO(RequestType.LOGIN, id, password);
        MainFrame.send(loginCommand, new CompletionHandler<>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result == -1) {
                    return;
                }

                // Flip the buffer for reading mode
//                attachment.flip();
                try {
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(attachment.array(), 0, attachment.limit());
                    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                    CommandDTO command = (CommandDTO) objectInputStream.readObject();

                    SwingUtilities.invokeLater(() -> {
                        String contentText = null;
                        if (command.getResponseType() == ResponseType.SUCCESS) {
                            MainFrame.userId = id;
                            contentText = "로그인되었습니다.";
                            JOptionPane.showMessageDialog(null, contentText, "SUCCESS_MESSAGE", JOptionPane.PLAIN_MESSAGE);
                            setVisible(false);
                            MainFrame.display("Main");
                        } else if (command.getResponseType() == ResponseType.FAILURE) {
                            contentText = "아이디 또는 비밀번호가 일치하지 않습니다.";
                            JOptionPane.showMessageDialog(null, contentText, "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
                        } else {
                            contentText = "ERROR.";
                            JOptionPane.showMessageDialog(null, contentText, "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
                        }
                    });

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    // Clear buffer for next write operation
                    attachment.clear();
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                JOptionPane.showMessageDialog(null, "서버 통신 실패: " + exc.getMessage(), "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
