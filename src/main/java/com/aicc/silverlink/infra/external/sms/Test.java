package com.aicc.silverlink.infra.external.sms;


import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;

public class Test {



    public static void main(String[] args) {

        DefaultMessageService messageService =  SolapiClient.INSTANCE.createInstance("NCSH8PDTAPZASHOD", "ODZZMAXH09Y0MMSE789I8T5QDAKBC3ED");
        // Message 패키지가 중복될 경우 com.solapi.sdk.message.model.Message로 치환하여 주세요
        Message message = new Message();
        message.setFrom("01034147808");
//        message.setTo("01034392813");
        message.setText("SilverLink 테스트 문자 입니다.");

        try {
            // send 메소드로 ArrayList<Message> 객체를 넣어도 동작합니다!
            messageService.send(message);
        } catch (SolapiMessageNotReceivedException exception) {
            // 발송에 실패한 메시지 목록을 확인할 수 있습니다!
            System.out.println(exception.getFailedMessageList());
            System.out.println(exception.getMessage());
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }

    }

}
