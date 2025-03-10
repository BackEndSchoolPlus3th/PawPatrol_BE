//package com.patrol.domain.member.auth.service;
//
//
//
//import com.patrol.global.exceptions.ErrorCodes;
//import com.patrol.global.exceptions.ServiceException;
//import lombok.RequiredArgsConstructor;
//import net.nurigo.sdk.message.model.Message;
//import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
//import net.nurigo.sdk.message.service.DefaultMessageService;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class SmsService {
//
//  private final DefaultMessageService messageService;
//
//  public void sendMessage(String to, String content) {
//    Message message = new Message();
//    message.setFrom("01087967561");
//    message.setTo(to);
//    message.setText(content);
//
//    try {
//      messageService.sendOne(new SingleMessageSendingRequest(message));
//
//    } catch (Exception e) {
//      System.out.printf("발생 에러 : " + e);
//      throw new ServiceException(ErrorCodes.SMS_SEND_FAILED);
//    }
//  }
//}
