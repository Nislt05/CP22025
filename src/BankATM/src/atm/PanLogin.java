package BankATM.src.atm;

import BankATM.src.common.CommandDTO;
import BankATM.src.common.RequestType;
import BankATM.src.common.ResponseType;

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
        // 1. 사용자가 텍스트 필드에 입력한 ID와 password값 가져오기
        String id = Text_ID.getText();
        String password = Text_Password.getText();

        // 메인 프레임에 ID 저장
        MainFrame.userId = id;

        // 2. 서버로 보낼 데이터 객체 생성
        CommandDTO loginCommand = new CommandDTO(RequestType.LOGIN, id, password);

        // 3. 비동기 전송 시작
        // 두 번째 인자인 CompletionHandler는 "통신이 완료되었을 때" 실행될 콜백이다.
        MainFrame.send(loginCommand, new CompletionHandler<>() {

            // [성공 시] 서버와 통신이 성공적으로 끝나고 데이터를 받았을 때 실행된다.
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                // result : 읽어들인 바이트 수, -1이면 연결이 끊겼다는 뜻이다.
                if (result == -1) {
                    return;
                }

                // [중요] NIO 버퍼 처리
                // flip()은 쓰기 모드 -> 읽기 모드로 전환하는 메서드입니다.
                attachment.flip();

                try {
                    // 4. 역직렬화 : 바이트 데이터 -> 객체 변환
                    // 자바가 이해할 수 있는 스트림으로 변환한다.
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(attachment.array(), 0, attachment.limit());
                    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

                    // 스트림에서 CommandDTO 객체를 꺼낸다. (서버가 보낸 응답)
                    CommandDTO command = (CommandDTO) objectInputStream.readObject();

                    // 5. UI 업데이트
                    // invokeLater를 이용해 화면 갱신 작업을 예약한다.
                    SwingUtilities.invokeLater(() -> {
                        String contentText = null;

                        // 로그인 성공 시
                        if (command.getResponseType() == ResponseType.SUCCESS) {
                            MainFrame.userId = id; // 로그인 유저 ID 확정
                            contentText = "로그인되었습니다.";

                            // 성공 메시지 창 띄우기
                            JOptionPane.showMessageDialog(null, contentText, "SUCCESS_MESSAGE", JOptionPane.PLAIN_MESSAGE);

                            // 현재 로그인 패널 숨기고 메인 화면으로 전환
                            setVisible(false);
                            MainFrame.display("Main");

                        // 로그인 실패 시
                        } else if (command.getResponseType() == ResponseType.FAILURE) {
                            contentText = "아이디 또는 비밀번호가 일치하지 않습니다.";

                            // 에러 메시지 띄우기
                            JOptionPane.showMessageDialog(null, contentText, "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
                        } else {
                            contentText = "ERROR.";
                            JOptionPane.showMessageDialog(null, contentText, "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
                        }
                    });

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    // 다음 통신을 위해 버퍼 초기화
                    // Clear buffer for next write operation
                    attachment.clear();
                }
            }

            // [실패 시] 통신 자체가 실패했을 때 실행된다.
            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                JOptionPane.showMessageDialog(null, "서버 통신 실패: " + exc.getMessage(), "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
