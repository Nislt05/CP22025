package atm;

import common.CommandDTO;
import common.RequestType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

//*******************************************************************
// Name : PanViewAccount
// Type : Class
// Description :  계좌조회 화면 패널을 구현한 Class 이다.
//*******************************************************************
public class PanViewAccount extends JPanel implements ActionListener
{
    private JLabel Label_Account;
    private  JTextArea Text_Account;
    private JLabel Label_balance;
    private  JTextArea Text_balance;

    private JButton Btn_Close;

    ATMMain MainFrame;

    //*******************************************************************
    // Name : PanViewAccount()
    // Type : 생성자
    // Description :  PanViewAccount Class의 생성자 구현
    //*******************************************************************
    public PanViewAccount(ATMMain parent)
    {
        MainFrame = parent;
        InitGUI();
    }

    //*******************************************************************
    // Name : InitGUI
    // Type : Method
    // Description :  계좌조회 화면 패널의 GUI를 초기화 하는 메소드 구현
    //*******************************************************************
    private void InitGUI()
    {
        setLayout(null);
        setBounds(0,0,480,320);

        Label_Account = new JLabel("계좌 번호");
        Label_Account.setBounds(0,70,100,20);
        Label_Account.setHorizontalAlignment(JLabel.LEFT);
        add(Label_Account);

        Text_Account = new JTextArea();
        Text_Account.setBounds(100,70,350,20);
        Text_Account.setEditable(false);
        add(Text_Account);

        Label_balance = new JLabel("잔액");
        Label_balance.setBounds(0,120,100,20);
        Label_balance.setHorizontalAlignment(JLabel.LEFT);
        add(Label_balance);

        Text_balance = new JTextArea();
        Text_balance.setBounds(100,120,350,20);
        Text_balance.setEditable(false);
        add(Text_balance);

        Btn_Close = new JButton("닫기");
        Btn_Close.setBounds(250,250,70,20);
        Btn_Close.addActionListener(this);
        add(Btn_Close);
    }

    //*******************************************************************
    // Name : actionPerformed
    // Type : Listner
    // Description :  취소 버튼의 동작을 구현
    //                취소 동작 후 메인 화면으로 변경되도록 구현
    //*******************************************************************
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == Btn_Close)
        {
            this.setVisible(false);
            MainFrame.display("Main");
        }
    }

    //*******************************************************************
    // Name : GetBalance()
    // Type : Method
    // Description :  ATMMain의 Send 기능을 호출하여 서버에 계좌조회 요청 메시지를 전달 하는 기능.
    //*******************************************************************
    public void GetBalance()
    {
        MainFrame.send(new CommandDTO(RequestType.VIEW), new CompletionHandler<Integer, ByteBuffer>() {
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
                    SwingUtilities.invokeLater(() -> {
                        String accountNumber = BankUtils.displayAccountNo(command.getUserAccountNo());
                        Text_Account.setText(accountNumber);
                        String balance = BankUtils.displayBalance(command.getBalance());
                        Text_balance.setText(balance + "원");
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
            }
        });
    }

}
