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
// Name : PanTransfer
// Type : Class
// Description :  계좌 이체 패널을 구현한 Class 이다.
//*******************************************************************

public class PanTransfer extends JPanel implements ActionListener
{
    private JLabel Label_RecvAccount;
    private  JTextField Text_RecvAccount;


    private JLabel Label_Amount;
    private JTextField Text_Amount;


    private JLabel Label_Password;
    private JTextField Text_Password;


    private JButton Btn_Transfer;
    private JButton Btn_Close;

    ATMMain MainFrame;

    //*******************************************************************
    // Name : PanTransfer()
    // Type : 생성자
    // Description :  PanTransfer Class의 생성자 구현
    //*******************************************************************
    public PanTransfer(ATMMain parent)
    {
        MainFrame = parent;
        InitGUI();
    }

    //*******************************************************************
    // Name : InitGUI
    // Type : Method
    // Description :  계좌 이체 화면 패널의 GUI를 초기화 하는 메소드 구현
    //*******************************************************************
    private void InitGUI()
    {
        setLayout(null);
        setBounds(0,0,480,320);

        Label_RecvAccount = new JLabel("받는 분 계좌번호");
        Label_RecvAccount.setBounds(0,20,100,20);
        Label_RecvAccount.setHorizontalAlignment(JLabel.LEFT);
        add(Label_RecvAccount);

        Text_RecvAccount = new JTextField();
        Text_RecvAccount.setBounds(100,20,350,20);
        Text_RecvAccount.setEditable(true);
        Text_RecvAccount.setToolTipText("숫자만 입력");
        add(Text_RecvAccount);

        Label_Amount = new JLabel("이체금액");
        Label_Amount.setBounds(0,70,100,20);
        Label_Amount.setHorizontalAlignment(JLabel.LEFT);
        add(Label_Amount);

        Text_Amount = new JTextField();
        Text_Amount.setBounds(100,70,350,20);
        Text_Amount.setEditable(true);
        Text_Amount.setToolTipText("숫자만 입력");
        add(Text_Amount);

        Label_Password = new JLabel("비밀번호");
        Label_Password.setBounds(0,120,100,20);
        Label_Password.setHorizontalAlignment(JLabel.LEFT);
        add(Label_Password);

        Text_Password = new JTextField();
        Text_Password.setBounds(100,120,350,20);
        Text_Password.setEditable(true);
        add(Text_Password);

        Btn_Transfer = new JButton("이체");
        Btn_Transfer.setBounds(100,250,70,20);
        Btn_Transfer.addActionListener(this);
        add(Btn_Transfer);

        Btn_Close = new JButton("닫기");
        Btn_Close.setBounds(250,250,70,20);
        Btn_Close.addActionListener(this);
        add(Btn_Close);
    }


    //*******************************************************************
    // Name : actionPerformed
    // Type : Listner
    // Description :  이체 버튼, 취소 버튼의 동작을 구현
    //                이체, 취소 동작 후 메인 화면으로 변경되도록 구현
    //*******************************************************************
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == Btn_Transfer)
        {
            Transfer();
            this.setVisible(false);
            MainFrame.display("Main");
        }
        if (e.getSource() == Btn_Close)
        {
            this.setVisible(false);
            MainFrame.display("Main");
        }
    }

    //*******************************************************************
    // Name : Transfer()
    // Type : Method
    // Description :  계좌 이체 화면의 데이터를 가지고 있는 CommandDTO를 생성하고,
    //                ATMMain의 Send 기능을 호출하여 서버에 계좌이체 요청 메시지를 전달 하는 기능.
    //*******************************************************************
    public void Transfer()
    {
        String receiveAccountNo = Text_RecvAccount.getText();
        long amount = Long.parseLong(Text_Amount.getText());
        String password = Text_Password.getText();

        CommandDTO commandDTO = new CommandDTO(RequestType.TRANSFER, password, MainFrame.userId, receiveAccountNo, amount);
        MainFrame.send(commandDTO, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result == -1) {
                    return;
                }
                attachment.flip();
                try {
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(attachment.array());
                    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                    CommandDTO command = (CommandDTO) objectInputStream.readObject();
                    SwingUtilities.invokeLater(() ->
                    {
                        String contentText = null;

                        if (command.getResponseType() == ResponseType.INSUFFICIENT)
                        {
                            contentText = "잔액이 부족합니다.";
                            JOptionPane.showMessageDialog(null, contentText, "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
                        }
                        else if (command.getResponseType() == ResponseType.WRONG_ACCOUNT_NO)
                        {
                            contentText = "계좌번호가 존재하지 않습니다.";
                            JOptionPane.showMessageDialog(null, contentText, "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
                        }
                        else if (command.getResponseType() == ResponseType.WRONG_PASSWORD)
                        {
                            contentText = "비밀번호가 일치하지 않습니다.";
                            JOptionPane.showMessageDialog(null, contentText, "ERROR_MESSAGE", JOptionPane.ERROR_MESSAGE);
                        }
                        else
                        {
                            contentText = "이체 되었습니다.";
                            JOptionPane.showMessageDialog(null, contentText, "SUCCESS_MESSAGE", JOptionPane.PLAIN_MESSAGE);
                        }
                    });
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                catch (ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
            }
        });
    }

}
